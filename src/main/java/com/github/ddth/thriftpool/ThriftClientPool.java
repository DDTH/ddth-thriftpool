package com.github.ddth.thriftpool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Pool of Thrift clients.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * 
 * @param <T>
 * @param <C>
 * @since 0.1.0
 */
public class ThriftClientPool<T extends TServiceClient, C> {

    private final Logger LOGGER = LoggerFactory.getLogger(ThriftClientPool.class);

    private Class<T> clientClass;
    private Class<C> clientInterface;
    private PoolConfig poolConfig;
    private RetryPolicy retryPolicy;
    private ObjectPool<C> thriftClientPool;
    private ITProtocolFactory tprotocolFactory;

    public ThriftClientPool() {
        // EMPTY
    }

    public ThriftClientPool(Class<T> clientClass, Class<C> clientInterface,
            ITProtocolFactory tprotocolFactory) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
    }

    public ThriftClientPool(Class<T> clientClass, Class<C> clientInterface,
            ITProtocolFactory tprotocolFactory, PoolConfig poolConfig) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
        this.poolConfig = poolConfig;
    }

    public ThriftClientPool(Class<T> clientClass, Class<C> clientInterface,
            ITProtocolFactory tprotocolFactory, RetryPolicy retryPolicy) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
        this.retryPolicy = retryPolicy;
    }

    public ThriftClientPool(Class<T> clientClass, Class<C> clientInterface,
            ITProtocolFactory tprotocolFactory, PoolConfig poolConfig, RetryPolicy retryPolicy) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.poolConfig = poolConfig;
        this.tprotocolFactory = tprotocolFactory;
        this.retryPolicy = retryPolicy;
    }

    /*----------------------------------------------------------------------*/
    public Class<T> getClientClass() {
        return clientClass;
    }

    public ThriftClientPool<T, C> setClientClass(Class<T> clientClass) {
        this.clientClass = clientClass;
        return this;
    }

    public Class<C> getClientInterface() {
        return clientInterface;
    }

    public ThriftClientPool<T, C> setClientInterface(Class<C> clientInterface) {
        this.clientInterface = clientInterface;
        return this;
    }

    public PoolConfig getPoolConfig() {
        return poolConfig;
    }

    public ThriftClientPool<T, C> setPoolConfig(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        return this;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public ThriftClientPool<T, C> setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    public ITProtocolFactory getTProtocolFactory() {
        return tprotocolFactory;
    }

    public ThriftClientPool<T, C> setTProtocolFactory(ITProtocolFactory tprotocolFactory) {
        this.tprotocolFactory = tprotocolFactory;
        return this;
    }

    synchronized public ThriftClientPool<T, C> init() {
        if (thriftClientPool == null) {
            if (tprotocolFactory == null) {
                throw new IllegalStateException("No ITProtocolFactory instance found!");
            }
            if (retryPolicy == null) {
                retryPolicy = RetryPolicy.DEFAULT;
            }

            ThriftClientFactory factory = new ThriftClientFactory();
            GenericObjectPool<C> pool = new GenericObjectPool<C>(factory);
            pool.setBlockWhenExhausted(true);
            pool.setTestOnReturn(false);
            int maxActive = poolConfig != null ? poolConfig.getMaxActive()
                    : PoolConfig.DEFAULT_MAX_ACTIVE;
            long maxWaitTime = poolConfig != null ? poolConfig.getMaxWaitTime()
                    : PoolConfig.DEFAULT_MAX_WAIT_TIME;
            int maxIdle = poolConfig != null ? poolConfig.getMaxIdle()
                    : PoolConfig.DEFAULT_MAX_IDLE;
            int minIdle = poolConfig != null ? poolConfig.getMinIdle()
                    : PoolConfig.DEFAULT_MIN_IDLE;
            pool.setMaxTotal(maxActive);
            pool.setMaxIdle(maxIdle);
            pool.setMinIdle(minIdle);
            pool.setMaxWaitMillis(maxWaitTime);
            pool.setTestOnBorrow(poolConfig != null ? poolConfig.isTestOnBorrow() : false);
            pool.setTestOnCreate(poolConfig != null ? poolConfig.isTestOnCreate() : false);
            pool.setTestWhileIdle(poolConfig != null ? poolConfig.isTestWhileIdle() : false);
            pool.setTimeBetweenEvictionRunsMillis(10000);
            this.thriftClientPool = pool;
        }
        return this;
    }

    synchronized public void destroy() {
        if (thriftClientPool != null) {
            try {
                thriftClientPool.close();
            } finally {
                thriftClientPool = null;
            }
        }
    }

    /*----------------------------------------------------------------------*/
    /**
     * Obtains a Thrift client object from pool.
     * 
     * @return
     * @throws Exception
     */
    public C borrowObject() throws Exception {
        return thriftClientPool.borrowObject();
    }

    /**
     * Returns a borrowed Thrift client object back to pool.
     * 
     * @param borrowedClient
     * @throws Exception
     */
    public void returnObject(C borrowedClient) throws Exception {
        thriftClientPool.returnObject(borrowedClient);
    }

    /*----------------------------------------------------------------------*/
    private final class ThriftClientFactory extends BasePooledObjectFactory<C> {
        @SuppressWarnings("unchecked")
        @Override
        public C create() throws Exception {
            TProtocol protocol = tprotocolFactory.create();
            T clientObj = ConstructorUtils.invokeConstructor(clientClass, protocol);

            // wrap
            Object proxyObj = Proxy.newProxyInstance(clientInterface.getClassLoader(),
                    new Class<?>[] { clientInterface }, new ReconnectingClientProxy(clientObj,
                            retryPolicy.clone()));
            return (C) proxyObj;
        }

        @Override
        public PooledObject<C> wrap(C obj) {
            return new DefaultPooledObject<C>(obj);
        }
    }

    /*----------------------------------------------------------------------*/

    private static final Set<Integer> RESTARTABLE_CAUSES = Sets.newHashSet(
            TTransportException.NOT_OPEN, TTransportException.END_OF_FILE,
            TTransportException.TIMED_OUT, TTransportException.UNKNOWN);

    /**
     * Helper proxy class. Attempts to call method on proxy object wrapped in
     * try/catch. If it fails, it attempts a reconnect and tries the method
     * again.
     * 
     * <p>
     * Credit: http://blog.liveramp.com/2014/04/10/reconnecting-thrift-client/
     * </p>
     * 
     * @param <T>
     */
    private final class ReconnectingClientProxy implements InvocationHandler {
        private T baseClient;
        private RetryPolicy retryPolicy;

        public ReconnectingClientProxy(T baseClient, RetryPolicy retryPolicy) {
            this.baseClient = baseClient;
            this.retryPolicy = retryPolicy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(baseClient, args);
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                if (target instanceof TTransportException) {
                    TTransportException cause = (TTransportException) target;
                    if (RESTARTABLE_CAUSES.contains(cause.getType())) {
                        reconnectOrThrowException(baseClient.getInputProtocol().getTransport());
                        return method.invoke(baseClient, args);
                    }
                }

                throw e;
            }
        }

        private void reconnectOrThrowException(TTransport transport) throws TTransportException {
            retryPolicy.reset();
            transport.close();
            while (!retryPolicy.exceedsMaxRetries()) {
                try {
                    LOGGER.info("Attempting to reconnect [" + (retryPolicy.getCounter() + 1) + "/"
                            + retryPolicy.getNumRetries() + "]...");
                    transport.open();
                    LOGGER.info("Reconnection successful.");
                    break;
                } catch (TTransportException e) {
                    LOGGER.error("Error while reconnecting:", e);
                    try {
                        retryPolicy.sleep();
                    } catch (InterruptedException e2) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (retryPolicy.exceedsMaxRetries()) {
                throw new TTransportException("Failed to reconnect.");
            }
        }
    }
}
