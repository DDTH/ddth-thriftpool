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
     * @param hash
     *            in the case there are two or more servers, use {@code hash} to
     *            determine which server to connect to
     * @return
     * @since 0.2.0
     */
    public TProtocol create(int hash);

}
