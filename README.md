
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

* `PlaygroundSpec` gives you the gist about how to use the new driver to perform
  basic operations like keyspaces/columns families creation, querying etc.
