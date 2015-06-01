ddth-thriftpool
===============

DDTH's Apache Thrift Client Pooling library.

By Thanh Ba Nguyen (btnguyen2k (at) gmail.com)

Project home:
[https://github.com/DDTH/ddth-thriftpool](https://github.com/DDTH/ddth-thriftpool)

OSGi Environment: ddth-thriftpool is packaged as an OSGi bundle.


## License ##

See LICENSE.txt for details. Copyright (c) 2014-2015 Thanh Ba Nguyen.

Third party libraries are distributed under their own license(s).


## Installation ##

Latest release version: `0.2.0`. See [RELEASE-NOTES.md](RELEASE-NOTES.md).

Maven dependency:

```xml
<dependency>
	<groupId>com.github.ddth</groupId>
	<artifactId>ddth-thriftpool</artifactId>
	<version>0.2.0</version>
</dependency>
```

## Usage ##

Example of creating a pool of Facebook's Scribe Clients:

```java
import com.github.ddth.thriftpool.ITProtocolFactory;
...
// setup a TProtocol Factory, method 1: implement ITProtocolFactory interface
ITProtocolFactory protocolFactory = new ITProtocolFactory() {
    @Override
    public TProtocol create(int hash) {
        TTransport transport = new TFramedTransport(new TSocket(SCRIBE_HOST, SCRIBE_PORT));
        TProtocol protocol = new TBinaryProtocol(transport);
        return protocol;
    }
};

// setup a TProtocol Factory, method 2: extends AbstractTProtocolFactory class
ITProtocolFactory protocolFactory = new AbstractTProtocolFactory() {
    @Override
    public TProtocol create(HostAndPort hostAndPort) {
        TTransport transport = new TFramedTransport(new TSocket(hostAndPort.host, hostAndPort.port));
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

// additional stuff: pool configuration
//PoolConfig poolConfig = new PoolConfig();
//poolConfig.setMaxActive(4).setMaxIdle(2).setMinIdle(1);
//pool.setPoolConfig(poolConfig);

// additional stuff: retry policy
//RetryPolicy retryPolicy = new RetryPolicy()
//retryPolicy.setNumRetries(5);
//retryPolicy.setSleepMsBetweenRetries(1000);
//retryPolicy.setRetryType(RetryPolicy.RetryType.RANDOM);
//pool.setRetryPolicy(retryPolicy)

//remember to initialize the pool!
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