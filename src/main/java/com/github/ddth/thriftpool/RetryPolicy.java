package com.github.ddth.thriftpool;

/**
 * Thrift client retry policy.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class RetryPolicy implements Cloneable {

    /**
     * @since 0.2.0
     */
    public enum RetryType {
        ROUND_ROBIN, RANDOM
    }

    public static RetryPolicy DEFAULT = new RetryPolicy(3, 1000, RetryType.ROUND_ROBIN);

    private int counter = 0;
    private int numRetries = 3;
    private long sleepMsBetweenRetries = 1000;
    private RetryType retryType = RetryType.ROUND_ROBIN;

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

    public void reset() {
        counter = 0;
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

    @Override
    public RetryPolicy clone() throws CloneNotSupportedException {
        RetryPolicy obj = (RetryPolicy) super.clone();
        return obj;
    }
}
