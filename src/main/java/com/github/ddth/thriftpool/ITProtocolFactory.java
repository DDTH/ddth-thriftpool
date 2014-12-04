package com.github.ddth.thriftpool;

import org.apache.thrift.protocol.TProtocol;

/**
 * Factory to create {@link TProtocol}.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public interface ITProtocolFactory {
    public TProtocol create();
}
