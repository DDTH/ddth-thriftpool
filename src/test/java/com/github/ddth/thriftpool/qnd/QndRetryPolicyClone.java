package com.github.ddth.thriftpool.qnd;

import com.github.ddth.thriftpool.RetryPolicy;

public class QndRetryPolicyClone {
    public static void main(String[] args) throws CloneNotSupportedException, InterruptedException {
        RetryPolicy retryPolicy = RetryPolicy.DEFAULT.clone();
        System.out.println(RetryPolicy.DEFAULT);
        retryPolicy.sleep();
        System.out.println(retryPolicy);
        System.out.println(retryPolicy.clone());
    }
}
