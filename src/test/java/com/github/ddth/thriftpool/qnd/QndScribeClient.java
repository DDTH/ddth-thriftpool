package com.github.ddth.thriftpool.qnd;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import scribe.thrift.scribe;

import com.github.ddth.thriftpool.ITProtocolFactory;
import com.github.ddth.thriftpool.PoolConfig;
import com.github.ddth.thriftpool.ThriftClientPool;

public class QndScribeClient {

    final static String SCRIBE_HOST = "localhost";
    final static int SCRIBE_PORT = 1463;
    final static String SCRIBE_CATE = "test";

    public static void main(String[] args) throws Exception {
        ITProtocolFactory protocolFactory = new ITProtocolFactory() {
            @Override
            public TProtocol create(int hash) {
                TTransport transport = new TFramedTransport(new TSocket(SCRIBE_HOST, SCRIBE_PORT));
                TProtocol protocol = new TBinaryProtocol(transport);
                return protocol;
            }
        };

        ThriftClientPool<scribe.Client, scribe.Iface> pool = new ThriftClientPool<scribe.Client, scribe.Iface>();
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaxActive(1).setMaxIdle(0).setMinIdle(0);
        pool.setClientClass(scribe.Client.class).setClientInterface(scribe.Iface.class)
                .setPoolConfig(poolConfig);
        pool.setTProtocolFactory(protocolFactory);
        pool.init();

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

        pool.destroy();
    }
}
