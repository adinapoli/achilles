
package org.cakesolutions.achilles

import org.specs2.mutable.{Specification, BeforeAfter }
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import java.util.UUID

class PlaygroundSpec extends Specification {

  trait ClusterLifeCycle extends BeforeAfter {
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()

    def before = {
    }

    def after = {

      cluster.shutdown()
    }
  }

  sequential

  "the new Cassandra driver" should {

    "connect to a node" in new ClusterLifeCycle {
      cluster.getMetadata().getClusterName() mustEqual("Test Cluster")
    }

    "create a keyspace named \"simplex\"" in new ClusterLifeCycle {
      val session = cluster.connect()
      val prevKeyspaces = cluster.getMetadata().getKeyspaces().size
      val query = """CREATE KEYSPACE simplex WITH replication 
                  = {'class':'SimpleStrategy', 'replication_factor':3};"""
      session.execute(query)
      cluster.getMetadata().getKeyspaces().size mustEqual(prevKeyspaces + 1)
    }

    "store and get result back from simplex" in new ClusterLifeCycle {
      val session = cluster.connect()
      session.execute("""CREATE TABLE simplex.songs (
                         id uuid PRIMARY KEY,
                         title text,
                         album text,
                         artist text,
                         tags set<text>,
                         data blob);""")
      session.execute("""INSERT INTO simplex.songs (id, title, album, artist, tags)
                         VALUES ( 756716f7-2e54-4715-9f00-91dcbea6cf50,
                                  'La Petite Tonkinoise', 'Bye Bye Blackbird',
                                  'Joséphine Baker', {'jazz', '2013'})""")

      //Using the new Query Builder
      val query = QueryBuilder.select.all.from("simplex", "songs")
      val results = session.execute(query)
      results.all.size mustEqual(1)
    }

    "allow compositional queries with QueryBuilder" in new ClusterLifeCycle {
      val session = cluster.connect()

      val clause = QueryBuilder.eq("id", UUID.fromString("756716f7-2e54-4715-9f00-91dcbea6cf50"))
      val query = QueryBuilder.select.all.from("simplex", "songs").where(clause)
      val results = session.execute(query)
      results.all.size mustEqual(1)

      session.execute("DROP KEYSPACE simplex;")
    }
  }
}
