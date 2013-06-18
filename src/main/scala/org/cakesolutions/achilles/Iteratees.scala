
package org.cakesolutions.achilles

import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import scalaz._
import Scalaz._
import iteratee._
import Iteratee._

trait CassandraImplicits {
  import language.implicitConversions
  import scala.collection.JavaConversions._

  implicit def asScalaStream(iter: java.util.Iterator[Row]): Stream[Row] = {
    asScalaIterator(iter).toStream
  }

  implicit def asScalaStreamRS(rs: ResultSet): Stream[Row] = {
    asScalaIterator(rs.iterator()).toStream
  }
}
