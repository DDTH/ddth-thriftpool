ddth-thriftpool
===============

DDTH's Apache Thrift Client Pooling library.

By Thanh Ba Nguyen (btnguyen2k (at) gmail.com)

Project home:
[https://github.com/DDTH/ddth-dao](https://github.com/DDTH/ddth-thriftpool)

OSGi Environment: ddth-thriftpool is packaged as an OSGi bundle.


## License ##

See LICENSE.txt for details. Copyright (c) 2014 Thanh Ba Nguyen.

Third party libraries are distributed under their own license(s).


## Installation #

Latest release version: `0.1.0`. See [RELEASE-NOTES.md](RELEASE-NOTES.md).

Maven dependency:

```xml
<dependency>
	<groupId>com.github.ddth</groupId>
	<artifactId>ddth-thriftpool</artifactId>
	<version>0.1.0</version>
</dependency>
```

## Usage ##

Example of creating a pool of Facebook's Scribe Clients:

```java
import com.github.ddth.thriftpool.ITProtocolFactory;
...
// create a TProtocol Factory
ITProtocolFactory protocolFactory = new ITProtocolFactory() {
    @Override
    public TProtocol create() {
        TTransport transport = new TFramedTransport(new TSocket(SCRIBE_HOST, SCRIBE_PORT));
        TProtocol protocol = new TBinaryProtocol(transport);
        return protocol;
    }
};
...

// create and initialize the pool
import scribe.thrift.scribe;
...
ThriftClientPool<scribe.Client, scribe.Iface> pool = new ThriftClientPool<scribe.Client, scribe.Iface>();
pool.setClientClass(scribe.Client.class).setClientInterface(scribe.Iface.class);
pool.setTProtocolFactory(protocolFactory);
pool.init();
...

// do some stuff
scribe.Iface client = pool.borrowObject();
try {
    System.out.println("Name:\t\t" + client.getName());
    System.out.println("Version:\t" + client.getVersion());
    System.out.println("Status:\t\t" + client.getStatus());
    System.out.println("Alive since:\t" + client.aliveSince());
    System.out.println("Options:\t" + client.getOptions());
    System.out.println("Counters:\t" + client.getCounters());
} finally {
    pool.returnObject(client);
}
....

// destroy the pool when done
pool.destroy();
```