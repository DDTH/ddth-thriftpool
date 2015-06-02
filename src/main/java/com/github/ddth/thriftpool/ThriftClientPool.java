package com.github.ddth.thriftpool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ddth.thriftpool.RetryPolicy.RetryType;
import com.google.common.collect.Sets;

/**
 * Pool of Thrift clients.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * 
 * @param <T>
 *            Thrift client class
 * @param <I>
 *            Thrift client interface
 * @since 0.1.0
 */
public class ThriftClientPool<T extends TServiceClient, I> {

    private final Logger LOGGER = LoggerFactory.getLogger(ThriftClientPool.class);

    private Class<T> clientClass;
    private Class<I> clientInterface;
    private PoolConfig poolConfig;
    private RetryPolicy retryPolicy;
    private ObjectPool<I> thriftClientPool;
    private ITProtocolFactory tprotocolFactory;

    public ThriftClientPool() {
        // EMPTY
    }

    public ThriftClientPool(Class<T> clientClass, Class<I> clientInterface,
            ITProtocolFactory tprotocolFactory) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
    }

    public ThriftClientPool(Class<T> clientClass, Class<I> clientInterface,
            ITProtocolFactory tprotocolFactory, PoolConfig poolConfig) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
        this.poolConfig = poolConfig;
    }

    public ThriftClientPool(Class<T> clientClass, Class<I> clientInterface,
            ITProtocolFactory tprotocolFactory, RetryPolicy retryPolicy) {
        this.clientClass = clientClass;
        this.clientInterface = clientInterface;
        this.tprotocolFactory = tprotocolFactory;
        this.retryPolicy = retryPolicy;
    }

    public ThriftClientPool(Class<T> clientClass, Class<I> clientInterface,
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

    public ThriftClientPool<T, I> setClientClass(Class<T> clientClass) {
        this.clientClass = clientClass;
        return this;
    }

    public Class<I> getClientInterface() {
        return clientInterface;
    }

    public ThriftClientPool<T, I> setClientInterface(Class<I> clientInterface) {
        this.clientInterface = clientInterface;
        return this;
    }

    public PoolConfig getPoolConfig() {
        return poolConfig;
    }

    public ThriftClientPool<T, I> setPoolConfig(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        return this;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public ThriftClientPool<T, I> setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    public ITProtocolFactory getTProtocolFactory() {
        return tprotocolFactory;
    }

    public ThriftClientPool<T, I> setTProtocolFactory(ITProtocolFactory tprotocolFactory) {
        this.tprotocolFactory = tprotocolFactory;
        return this;
    }

    synchronized public ThriftClientPool<T, I> init() {
        if (thriftClientPool == null) {
            if (tprotocolFactory == null) {
                throw new IllegalStateException("No ITProtocolFactory instance found!");
            }
            if (retryPolicy == null) {
                retryPolicy = RetryPolicy.DEFAULT;
            }

            ThriftClientFactory factory = new ThriftClientFactory();
            GenericObjectPool<I> pool = new GenericObjectPool<I>(factory);
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
    public I borrowObject() throws Exception {
        return thriftClientPool.borrowObject();
    }

    /**
     * Returns a borrowed Thrift client object back to pool.
     * 
     * @param borrowedClient
     * @throws Exception
     */
    public void returnObject(I borrowedClient) throws Exception {
        thriftClientPool.returnObject(borrowedClient);
    }

    /*----------------------------------------------------------------------*/
    private final class ThriftClientFactory extends BasePooledObjectFactory<I> {

        @SuppressWarnings("unchecked")
        @Override
        public I create() throws Exception {
            Object proxyObj = Proxy.newProxyInstance(clientInterface.getClassLoader(),
                    new Class<?>[] { clientInterface },
                    new ReconnectingClientProxy(retryPolicy.clone()));
            return (I) proxyObj;
        }

        @Override
        public PooledObject<I> wrap(I obj) {
            return new DefaultPooledObject<I>(obj);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public void destroyObject(PooledObject<I> pooledObj) throws Exception {
            I obj = pooledObj.getObject();
            if (Proxy.isProxyClass(obj.getClass())) {
                InvocationHandler iv = Proxy.getInvocationHandler(obj);
                if (iv instanceof ThriftClientPool.ReconnectingClientProxy) {
                    ((ReconnectingClientProxy) iv).destroy();
                }
            }
        }
    }

    /*----------------------------------------------------------------------*/

    private static final Set<Integer> RESTARTABLE_CAUSES = Sets.newHashSet(
            TTransportException.NOT_OPEN, TTransportException.END_OF_FILE,
            TTransportException.TIMED_OUT, TTransportException.UNKNOWN);

    private Random random = new Random(System.currentTimeMillis());

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
        private RetryPolicy retryPolicy;
        private UUID id = UUID.randomUUID();
        private T clientObj;

        public ReconnectingClientProxy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            ReconnectingClientProxy other = (ReconnectingClientProxy) obj;
            return id.equals(other.id);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public void destroy() {
            if (clientObj != null) {
                try {
                    clientObj.getInputProtocol().getTransport().close();
                } catch (Exception e) {
                }
            }
            clientObj = null;
        }

        /**
         * Creates a new thrift client object.
         * 
         * @param hash
         * @return
         * @throws Exception
         */
        private T newClientObj(int hash) throws Exception {
            TProtocol protocol = tprotocolFactory.create(hash);
            T clientObj = ConstructorUtils.invokeConstructor(clientClass, protocol);
            return clientObj;
        }

        private T getClientId(boolean renew, int hash) throws Exception {
            if (clientObj == null || renew) {
                if (clientObj != null) {
                    try {
                        clientObj.getInputProtocol().getTransport().close();
                    } catch (Exception e) {
                    }
                }
                clientObj = newClientObj(hash);
            }
            return clientObj;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (StringUtils.equals("hashCode", method.getName())) {
                return hashCode();
            }
            if (StringUtils.equals("equals", method.getName())) {
                return equals(args[0]);
            }
            if (StringUtils.equals("toString", method.getName())) {
                return toString();
            }

            retryPolicy.reset();
            return invokeWithRetries(proxy, method, args);
        }

        private Object invokeWithRetries(Object proxy, Method method, Object[] args)
                throws Throwable {
            boolean hasError = false;
            while (!retryPolicy.exceedsMaxRetries()) {
                int hash = retryPolicy.getRetryType() == RetryType.RANDOM ? random.nextInt(255)
                        : retryPolicy.getCounter();
                try {
                    T clientObj = getClientId(hasError, hash);
                    return method.invoke(clientObj, args);
                } catch (InvocationTargetException e) {
                    hasError = true;
                    Throwable target = e.getTargetException();
                    if (target instanceof TTransportException) {
                        TTransportException cause = (TTransportException) target;
                        if (RESTARTABLE_CAUSES.contains(cause.getType())) {
                            LOGGER.info("Attempting to retry [" + (retryPolicy.getCounter() + 1)
                                    + "/" + retryPolicy.getNumRetries() + "]...");
                            retryPolicy.sleep();
                            if (retryPolicy.exceedsMaxRetries()) {
                                throw target;
                            } else {
                                continue;
                            }
                        }
                    }
                    throw e;
                }
            }
            return null;
        }
    }
}
