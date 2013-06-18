
package org.cakesolutions.achilles

import org.specs2.mutable.{ Specification, After }
import org.specs2.specification.{ Step, Fragments }
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import java.util.UUID
import scalaz._
import Scalaz._
import iteratee.{ Iteratee => I }

class IterateesSpec extends Specification with CassandraImplicits {

  override def map(fs: =>Fragments) = Step(createDb) ^ fs ^ Step(dropDb)

  def createDb = {
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()
    val prevKeyspaces = cluster.getMetadata().getKeyspaces().size
    val query = """CREATE KEYSPACE simplex WITH replication 
                = {'class':'SimpleStrategy', 'replication_factor':3};"""
    session.execute(query)

    session.execute("""CREATE TABLE simplex.songs ( id uuid PRIMARY KEY,
                       title text, album text, artist text,
                       tags set<text>, data blob);""")
    session.execute("""INSERT INTO simplex.songs (id, title, album, artist, tags)
                       VALUES ( 756716f7-2e54-4715-9f00-91dcbea6cf50,
                                'La Petite Tonkinoise', 'Bye Bye Blackbird',
                                'Joséphine Baker', {'jazz', '2013'})""")
    cluster.shutdown()
  }

  def dropDb = {
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()
    session.execute("DROP KEYSPACE simplex;")
    cluster.shutdown()
  }

  trait ClusterLifeCycle extends After {
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()

    def after = cluster.shutdown()
  }

  sequential

  "Cassandra Iteratees" should {

    "enumerate underlying iterators" in new ClusterLifeCycle {
      val session = cluster.connect()
      val clause = QueryBuilder.eq("id", UUID.fromString("756716f7-2e54-4715-9f00-91dcbea6cf50"))
      val query = QueryBuilder.select.all.from("simplex", "songs").where(clause)
      val results = session.execute(query)
      val size = (I.length[Row, Id] &= I.enumerate(results)).run
      size mustEqual(1)
    }

  }
}
