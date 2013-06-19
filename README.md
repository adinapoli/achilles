
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


## Streaming interface
The streaming interfaces live inside [here](https://github.com/adinapoli/achilles/blob/master/src/main/scala/org/cakesolutions/achilles/pipes.scala)
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

## Using the library
Using the trait is nothing difficult, if you have already a working knowledge
of Iteratees. If you have not, here is a crash course. Conceptually, there are
three entities you should be aware of:

* Emumerators, object which describe how, given a resource ``R``, this resource
  should be enumerated.

* Iteratees, entities where the computation actually happens.

* Enumeratees, which are commonly defined as "stream adapter": they transforms
  the underlying enumerators (more example later!)

There are then other types on top of that, but let's ignore them for now.

[IterateesSpec](https://github.com/adinapoli/achilles/blob/master/src/test/scala/org/cakesolutions/achilles/IterateesSpec.scala)
gives you a glimpse about the experimental streaming interface using aforementioned
Scalaz's [iteratee](https://github.com/scalaz/scalaz/tree/scalaz-seven/iteratee/src/main/scala/scalaz/iteratee)

