
This is a toy playground to experiment with the newly-released
Datastax drivers for Cassandra, targeting versions >= 1.2 and
using CQL3 and the new transport layer (Thrift was dropped):

* http://www.datastax.com/dev/blog/new-datastax-drivers-a-new-face-for-cassandra
* https://github.com/datastax/java-driver
* [DOCS](http://www.datastax.com/doc-source/developer/java-driver/)
* [API](http://www.datastax.com/drivers/java/apidocs/)

## Using the new driver

In your sbt project:

```
  "com.datastax.cassandra"  % "cassandra-driver-core" % "1.0.0"
```

## Examples

Look inside test. Maybe an experimental streaming interface can be added later:

* [PlaygroundSpec](https://github.com/adinapoli/achilles/blob/master/src/test/scala/org/cakesolutions/achilles/PlaygroundSpec.scala)
  gives you the gist about how to use the new driver to perform
  basic operations like keyspaces/columns families creation, querying etc.

* [IterateesSpec](https://github.com/adinapoli/achilles/blob/master/src/test/scala/org/cakesolutions/achilles/IterateesSpec.scala)
  gives you a glimpse about the experimental streaming interface using
  Scalaz's [iteratee](https://github.com/scalaz/scalaz/tree/scalaz-seven/iteratee/src/main/scala/scalaz/iteratee)

## Streaming interface
The streaming interfaces live inside [Iteratees.scala](https://github.com/adinapoli/achilles/blob/master/src/main/scala/org/cakesolutions/achilles/Iteratees.scala)
and it consist of modular trait divided by semantic cohesion. During your
programming woes though, all you need to mixin in the trait ```CassandraPipes```
with conveniently brings in scope everything you need.

### Why a streaming interface?
Benefits of Iteratees-like streaming systems are well known in literature. These
are the mandatory readings:

* [Oleg's paper about Iteratees](http://okmij.org/ftp/Haskell/Iteratee/describe.pdf)
* [Learning Scalaz: Day 17](http://eed3si9n.com/node/123)

In a nutshell, iteratees gives you more control over the resource allocation/disposal,
are easily composable and usually yield to more concise and composable code.

