
package org.cakesolutions.achilles

import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import scalaz._
import Scalaz._
import iteratee.{ Iteratee => I, _ }
import scala.language.higherKinds

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

trait CassandraIteratees extends CassandraImplicits {

  //Implicitly call "collect" under the hood,
  //collecting value trasformed by @f.
  def gather[A,B](f: A => B): IterateeT[A, Id, List[B]] = {
    I.collect[B, List] %= I.map(f)
  }
}
