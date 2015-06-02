package com.github.ddth.thriftpool;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.protocol.TProtocol;

/**
 * Abstract implementation of {@link ITProtocolFactory}.
 * 
 * @author ThanhNB
 * @since 0.2.0
 */
public abstract class AbstractTProtocolFactory implements ITProtocolFactory {

    protected static class HostAndPort {
        public String host;
        public int port;
    }

    private List<HostAndPort> hostAndPortList = new ArrayList<HostAndPort>();
    private String hostsAndPorts;

    /**
     * Constructs a new {@link AbstractTProtocolFactory} object.
     */
    public AbstractTProtocolFactory() {
    }

    /**
     * Constructs a new {@link AbstractTProtocolFactory} object.
     * 
     * @param hostAndPortList
     *            in format {@code host1:port1,host2:port2,host3:port3,...}
     */
    public AbstractTProtocolFactory(String hostsAndPorts) {
        setHostsAndPorts(hostsAndPorts);
    }

    public String getHostsAndPorts() {
        return hostsAndPorts;
    }

    public AbstractTProtocolFactory setHostsAndPorts(String hostsAndPorts) {
        this.hostsAndPorts = hostsAndPorts;
        parseHostAndPortList();
        return this;
    }

    protected void parseHostAndPortList() {
        String[] hostAndPortTokens = hostsAndPorts.split("[,\\s]+");

        this.hostAndPortList.clear();
        for (String hostAndPort : hostAndPortTokens) {
            String[] tokens = hostAndPort.split("[:]+");
            HostAndPort hap = new HostAndPort();
            hap.host = tokens.length > 0 ? tokens[0] : "";
            try {
                hap.port = Integer.parseInt(tokens[1]);
            } catch (Exception e) {
                hap.port = 0;
            }
            this.hostAndPortList.add(hap);
        }
    }

    protected HostAndPort getHostAndPort(int hash) {
        if (hostAndPortList == null || hostAndPortList.size() == 0) {
            return null;
        }
        return hostAndPortList.get(Math.abs(hash % hostAndPortList.size()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TProtocol create(int hash) throws Exception {
        return create(getHostAndPort(hash));
    }

    /**
     * Creates a new {@link TProtocol} object.
     * 
     * @param hostAndPort
     * @return
     * @throws Exception
     */
    protected abstract TProtocol create(HostAndPort hostAndPort) throws Exception;
}
