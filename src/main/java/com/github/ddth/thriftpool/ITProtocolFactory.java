package com.github.ddth.thriftpool;

import org.apache.thrift.protocol.TProtocol;

/**
 * Factory to create {@link TProtocol}.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public interface ITProtocolFactory {

    /**
     * Creates a new {@link TProtocol} object.
     * 
     * @param serverIndexHash
     *            When there are 2 or more servers, use {@code serverIndexHash}
     *            to determine which server to connect to. Since
     *            {@code serverIndexHash} is an integer between {@code 0} and
     *            {@code Integer.MAX_VALUE}, the server is chosen as
     *            {@code serverIndexHash % getNumServers()}
     * @return
     * @since 0.2.0
     * @throws Exception
     */
    public TProtocol create(int serverIndexHash) throws Exception;

    /**
     * Returns number of servers.
     * 
     * @return
     * @since 0.2.2
     */
    public int getNumServers();

}
