package com.github.ddth.thriftpool;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Thrift client retry policy.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class RetryPolicy implements Cloneable {

    /**
     * @since 0.2.0
     * @since 0.2.2 add retry types FAILOVER and RANDOM_FAILOVER
     */
    public static enum RetryType {
        /**
         * <ul>
         * <li>First connection: choose a random server.</li>
         * <li>Sub-sequence connections: choose next server from server list.</li>
         * </ul>
         */
        ROUND_ROBIN,

        /**
         * Choose a random server for every connection.
         */
        RANDOM,

        /**
         * <ul>
         * <li>First connection: always choose the first server from server
         * list.</li>
         * <li>Sub-sequence connections: choose next server from server list.</li>
         * </ul>
         * 
         * @since 0.2.2
         */
        FAILOVER,

        /**
         * <ul>
         * <li>First connection: always choose the first server from server
         * list.</li>
         * <li>Sub-sequence connections: choose a random server from server list
         * (except for the first server).</li>
         * </ul>
         * 
         * @since 0.2.2
         */
        RANDOM_FAILOVER
    }

    public static RetryPolicy DEFAULT = new RetryPolicy(3, 1000, RetryType.ROUND_ROBIN);

    private int counter = 0;
    private int numRetries = 3;
    private long sleepMsBetweenRetries = 1000;
    private RetryType retryType = RetryType.ROUND_ROBIN;
    private int lastServerIndexHash = 0;

    public RetryPolicy() {
    }

    public RetryPolicy(int numRetries, long sleepMsBetweenRetries) {
        this.numRetries = numRetries;
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
    }

    public RetryPolicy(int numRetries, long sleepMsBetweenRetries, RetryType retryType) {
        this.numRetries = numRetries;
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
        this.retryType = retryType;
    }

    public int getNumRetries() {
        return numRetries;
    }

    public RetryPolicy setNumRetries(int numRetries) {
        this.numRetries = numRetries;
        return this;
    }

    public long getSleepMsBetweenRetries() {
        return sleepMsBetweenRetries;
    }

    public RetryPolicy setSleepMsBetweenRetries(long sleepMsBetweenRetries) {
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
        return this;
    }

    public RetryType getRetryType() {
        return retryType;
    }

    public RetryPolicy setRetryType(RetryType retryType) {
        this.retryType = retryType;
        return this;
    }

    /**
     * @return
     * @since 0.2.2
     */
    public int getLastServerIndexHash() {
        return lastServerIndexHash;
    }

    /**
     * @param serverIndexHash
     * @return
     * @since 0.2.2
     */
    public RetryPolicy setLastServerIndexHash(int serverIndexHash) {
        this.lastServerIndexHash = serverIndexHash;
        return this;
    }

    public void reset() {
        counter = 0;
        lastServerIndexHash = 0;
    }

    public int getCounter() {
        return counter;
    }

    public boolean exceedsMaxRetries() {
        return counter >= numRetries;
    }

    public void sleep() throws InterruptedException {
        counter++;
        Thread.sleep(sleepMsBetweenRetries);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.2.2
     */
    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder(this);
        tsb.append("counter", counter).append("numRetries", numRetries)
                .append("sleepMsBetweenRetries", sleepMsBetweenRetries)
                .append("retryType", retryType).append("lastServerIndexHash", lastServerIndexHash);
        return tsb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetryPolicy clone() throws CloneNotSupportedException {
        RetryPolicy obj = (RetryPolicy) super.clone();
        return obj;
    }
}
