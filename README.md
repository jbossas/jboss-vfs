## JBoss Virtual File System (JBoss VFS)

JBoss VFS provides an API to represent a deployment as a read-only hierarchical file system
regardless of whether it is packaged or not. It also allows deployments to be accessed both locally and remotely
using a pluggable design so that new protocols can easily be added to those supplied by default (file, jar, memory).

## Building

Prerequisites:

* JDK 8 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`

To build with your own Maven installation:

    mvn install

## Documentation

All documentation lives at https://docs.jboss.org/jbossmc/docs/2.0.x/userGuide/ch20.html

## Issue tracker

All issues can be reported at https://issues.jboss.org/browse/JBVFS

## Code

All code can be found at https://github.com/jbossas/jboss-vfs

## License

All code distributed under [ASL 2.0](LICENSE.txt).
