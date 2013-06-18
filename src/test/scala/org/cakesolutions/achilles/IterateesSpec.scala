
package org.cakesolutions.achilles

import org.specs2.mutable.{ Specification, Before }
import org.specs2.specification.{ Step, Fragments }
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import java.util.UUID
import scalaz._
import Scalaz._
import iteratee.{ Iteratee => I }

class IterateesSpec extends Specification with CassandraPipes {

  override def map(fs: =>Fragments) = Step(createDb) ^ fs ^ Step(dropDb)
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()

  def createDb = {
    implicit val session = cluster.connect()
    val prevKeyspaces = cluster.getMetadata().getKeyspaces().size
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

      """CREATE TABLE simplex.playlist ( id uuid PRIMARY KEY,
                       title text, ordering int);""")

    (1 to 10) map { order =>
      val uuid = UUID.randomUUID().toString
      queries = queries :+ ("""INSERT INTO simplex.playlist (id, title, ordering)
                            VALUES ( %s, 'What a Boring Playlist', %s)""" format(uuid, order))
    }

    (withSession %= toQuery &= enumRawQueries(queries)).run
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
      val clause = QueryBuilder.eq("id", UUID.fromString("756716f7-2e54-4715-9f00-91dcbea6cf50"))
      val query = QueryBuilder.select.all.from("simplex", "songs").where(clause)
      val results = session.execute(query)
      val size = (I.length[Row, Id] &= I.enumerate(results)).run
      size mustEqual(1)
    }

    "collects all songs name with appropriate iteratee" in {

      def getTitle(r: Row): String = r.getString("title")

      val session = cluster.connect()
      val query = QueryBuilder.select.all.from("simplex", "songs")
      val results = session.execute(query)
      val titles = (I.collect[String, List] %=
                    I.map(getTitle) &=
                    I.enumerate(results)).run
      titles.head mustEqual("La Petite Tonkinoise")
    }

    "collects all songs name using \"gather\"" in {
      val session = cluster.connect()
      val query = QueryBuilder.select.all.from("simplex", "songs")
      val results = session.execute(query)
      val titles = (gather((r:Row) => r.getString("title")) &= I.enumerate(results)).run
      titles.head mustEqual("La Petite Tonkinoise")
    }

    "allow async IO" in {
      val session = cluster.connect()
      val query2 = QueryBuilder.select.all.from("simplex", "playlist")
      val res2 = session.executeAsync(query2)
      val orders = (gather((r:Row) => r.getInt("ordering")) &= enumerateRSF(res2)).run
      orders.sum mustEqual(55)
    }

  }
}
