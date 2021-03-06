
package org.cakesolutions.achilles

import org.specs2.mutable.{ Specification, Before }
import org.specs2.specification.{ Step, Fragments }
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.{QueryBuilder => QB}
import java.util.UUID
import scalaz._
import Scalaz._
import iteratee.{ Iteratee => I }

class IterateesSpec extends Specification with CassandraPipes {

  /* To use our pipes, just mixin the trait @CassandraPipes. */

  override def map(fs: =>Fragments) = Step(createDb) ^ fs ^ Step(dropDb)
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()

  //Boilerplate to initialise the Db just once for each Specification.
  def createDb = {

    /* As stated in the driver documentation, a session is your
     * communication entrypoint with the cluster. One session is generally
     * enough per-application, but has the snag to be tied to one keyspace,
     * so in case you need to query more than one you need more than just one
     * session. The reason is implicit is because we are implicitly passing it
     * to our iteratee @execute (see below).
     */
    implicit val session = cluster.connect()

    /* We bundle our queries into a list, so that we can pipe them to our
    * iteratee. */
    var queries = List(
      """CREATE KEYSPACE simplex WITH replication 
                = {'class':'SimpleStrategy', 'replication_factor':3};""",

      """CREATE TABLE simplex.songs ( id uuid PRIMARY KEY,
                title text, album text, artist text,
                tags set<text>, data blob);""",

      """INSERT INTO simplex.songs (id, title, album, artist, tags)
                VALUES ( 756716f7-2e54-4715-9f00-91dcbea6cf50,
                         'La Petite Tonkinoise', 'Bye Bye Blackbird',
                         'Joséphine Baker', {'jazz', '2013'})""",

      """CREATE TABLE simplex.playlist ( id uuid,
                       title text, ordering int PRIMARY KEY);""")

    (1 to 10) map { order =>
      val uuid = UUID.randomUUID().toString
      queries = queries :+ ("""INSERT INTO simplex.playlist (id, title, ordering)
                            VALUES ( %s, 'What a Boring Playlist', %s)""" format(uuid, order))
    }

    /* it might seems quite terse at first, but here is the breakdown:
    * First of all, we need to create an enumerator. Scalaz provides lots
    * of out-of-the-box enumerators, @enumRawQueries is syntactic sugar we
    * created to enumerate a list of strings, our queries. If you look at 
    * the type signature of @execute you'll notice it's our Iteratee, but
    * it process @Query objects, not strings, so we need to modify our stream
    * from a List[String] -> List[Query], and here's where an enumeratee comes
    * into play. @toQuery just does that.
    * 
    * Finally, we can compose our pipeline:
    * (%=) chains an iteratee to an enumeratee, yielding a new iteratee
    * (&=) attaches our  iteraee to an enumeratee. Done that, we are ready
    * to run the Iteratee and to get the result back!
    */

    (execute %= toQuery &= enumRawQueries(queries)).run
  }

  def dropDb = {
    val session = cluster.connect()
    session.execute("DROP KEYSPACE simplex;")
    cluster.shutdown()
  }

  sequential

  "Cassandra Iteratees" should {

    "enumerate underlying iterators" in {
      val session = cluster.connect()
      val clause = QB.eq("id", UUID.fromString("756716f7-2e54-4715-9f00-91dcbea6cf50"))
      val query = QB.select.all.from("simplex", "songs").where(clause)
      val results = session.execute(query)

      /* Here we are enumerating our results directly. There is an
      * implicits which converts a ResultSet into a Stream[Query], so that
      * @enumerate just works. Then we use the Scalaz's builtin lenght 
      * iteratee to count the number of returned results.
      */
      val size = (I.length[Row, Id] &= I.enumerate(results)).run
      size mustEqual(1)
    }

    "collects all songs name with appropriate iteratee" in {

      def getTitle(r: Row): String = r.getString("title")

      val session = cluster.connect()
      val query = QB.select.all.from("simplex", "songs")
      val results = session.execute(query)

      /* The only new bit here is that we are composing collect and map
      * to actually build a new iteratee that get a Row from the enumerator
      * and yield a List[String] back. The patter is so common we abstracted
      * the pipe into the @gather function (see below).
      */
      val titles = (I.collect[String, List] %=
                    I.map(getTitle) &=
                    I.enumerate(results)).run
      titles.head mustEqual("La Petite Tonkinoise")
    }

    "collects all songs name using \"gather\"" in {
      val session = cluster.connect()
      val query = QB.select.all.from("simplex", "songs")
      val results = session.execute(query)

      /* We are using @gather, passing a function which transform our processing
      * element from the Enumerator to a String, and we finally accumulate the
      * result into a Monoid, a List.
      */
      val titles = (gather((r:Row) => r.getString("title")) &= I.enumerate(results)).run
      titles.head mustEqual("La Petite Tonkinoise")
    }

    "allow async IO" in {
      val session = cluster.connect()
      val query2 = QB.select.all.from("simplex", "playlist")
      val res2 = session.executeAsync(query2)
      val orders = (gather((r:Row) => r.getInt("ordering")) &= enumRS(res2)).run
      orders.sum mustEqual(55)
    }

    "not dispose a session when using \"executeP\"" in {
      implicit val session = cluster.connect()
      val query1 = QB.select.all.from("simplex", "playlist")
      val query2 = QB.select.all.from("simplex", "songs")
      val orders = (gather((r:Row) => r.getInt("ordering")) &=
                    I.enumerate(session.execute(query1))).run
      orders.sum mustEqual(55)
      val titles = (gather((r:Row) => r.getString("title")) &=
                   I.enumerate(session.execute(query2))).run
      titles.length mustEqual(1)
    }

    "update a song title" in {
      implicit val session = cluster.connect()

      val clause = QB.eq("id", UUID.fromString("756716f7-2e54-4715-9f00-91dcbea6cf50"))
      val query1 = QB.update("simplex", "songs")
                    .`with`(QB.set("title", "Le petite Maison")).where(clause)

      (executeP &= enumQueries(query1)).run
      val query2 = QB.select.all.from("simplex", "songs").where(clause)

      val titles = (gather((r:Row) => r.getString("title")) &=
                    I.enumerate(session.execute(query2))).run
      titles.head mustEqual("Le petite Maison")
    }

    "chain more than one query together" in {
      implicit val session = cluster.connect()

      val query1 = QB.select.all.from("simplex", "playlist").where(QB.eq("ordering", 1))
      val query2 = QB.select.all.from("simplex", "playlist").where(QB.eq("ordering", 2))
      
      //I still haven't found how to chain together these two iteratees,
      //because in order to enumerate the result set list we need the list
      //back from running the first iteratee. Bear in mind this is more an
      //exercise of style than a real need; due to the fact @execute 
      //yield a list of @ResultSet, we are allocating a potentially huge list
      //anyway, which make the entire iteratees scaffolding useless.
      //Ideally what we want is a way to stream our @ResultSet in constant
      //memory allocation.
      val rs = (execute &= enumQueries(List(query1, query2))).run
      val titles = (I.length[Row, Id] &= enumRSList(rs)).run
      titles mustEqual(2)
    }

  }
}
