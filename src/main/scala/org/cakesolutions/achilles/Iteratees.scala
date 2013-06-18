
package org.cakesolutions.achilles

import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder._
import scalaz._
import Scalaz._
import iteratee._
import Iteratee._
import language.implicitConversions
import scala.collection.JavaConversions._
import scala.language.higherKinds

trait CassandraImplicits {

  implicit def asScalaStream(iter: java.util.Iterator[Row]): Stream[Row] = {
    asScalaIterator(iter).toStream
  }

  implicit def asScalaStreamRS(rs: ResultSet): Stream[Row] = {
    asScalaIterator(rs.iterator()).toStream
  }

}

trait CassandraPipes extends CassandraImplicits
  with CassandraIteratees
  with CassandraEnumeratees
  with CassandraEnumerators

/* Generally speaking this trait focus more on the ease of use than
 * on "purity". In theory we should enumerate directly an Iterator without
 * converting it into a stream, but doing so would force our entire code to
 * live in the IO monad. using "enumerate" allow us to run pure iteratees
 * in the Id.
 */
trait CassandraIteratees {

  //Implicitly call "collect" under the hood,
  //collecting value trasformed by @f.
  def gather[A,B](f: A => B): IterateeT[A, Id, List[B]] = {
    collect[B, List] %= map(f)
  }

  //An iteratee which consume its input and finally dispose the
  //generated session. Return the number of executed queries.
  def withSession(implicit session: Session): Iteratee[Query, Int] = {
     def step(acc: Int)(s: Input[Query]): Iteratee[Query, Int] =
       s(el = e => {
           session.execute(e)
           cont(step(acc + 1))},
         empty = cont(step(acc)),
         eof = {
          session.shutdown()
          done(acc, eofInput[Query]) }
       )
     cont(step(0))
  }

}

trait CassandraEnumeratees {
  
  //EnumerateeT which converts a list of String into a list of Query.
  def toQuery: EnumerateeT[String, Query, Id] = {
    map((rq:String) => new SimpleStatement(rq))
  }

}

trait CassandraEnumerators {

  //Handle with care. Enumerating a ResultSetFuture will cause
  //the future to block and wait for the result.
  def enumerateRSF(rsf: ResultSetFuture): Enumerator[Row] = {
    enumerate(asScalaIterator(rsf.get().iterator()).toStream)
  }

  def enumQueries(queries: List[Query]): EnumeratorT[Query, Id] = {
    enumList(queries)
  }

  def enumRawQueries(queries: List[String]): EnumeratorT[String, Id] = {
    enumList(queries)
  }
}
