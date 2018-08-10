package com.immomo.datainfo.rpc;

import javax.net.SocketFactory;
import java.util.HashMap;
import java.util.Map;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import com.immomo.datainfo.conf.Configuration;
import com.immomo.datainfo.io.RpcWrapper;
import com.immomo.datainfo.rpc.ProtobufRpcEngine.RpcResponseWrapper;


/**
 * Created by kangkai on 18/2/5.
 */

/* Cache a client using its socket factory as the hash key */
@InterfaceAudience.LimitedPrivate({"AVALON", "AGENT"})
@InterfaceStability.Evolving
public class ClientCache {
    private Map<SocketFactory, Client> clients =
            new HashMap<SocketFactory, Client>();

    /**
     * Construct & cache an IPC client with the user-provided SocketFactory
     * if no cached client exists.
     *
     * @param conf Configuration
     * @param factory SocketFactory for client socket
     * @param valueClass Class of the expected response
     * @return an IPC client
     */
    public synchronized Client getClient(Configuration conf,
                                         SocketFactory factory, Class<? extends RpcWrapper> valueClass) {
        // Construct & cache client.  The configuration is only used for timeout,
        // and Clients have connection pools.  So we can either (a) lose some
        // connection pooling and leak sockets, or (b) use the same timeout for all
        // configurations.  Since the IPC is usually intended globally, not
        // per-job, we choose (a).
        Client client = clients.get(factory);
        if (client == null) {
            client = new Client(valueClass, conf, factory);
            clients.put(factory, client);
        } else {
            client.incCount();
        }
        if (Client.LOG.isDebugEnabled()) {
            Client.LOG.debug("getting client out of cache: " + client);
        }
        return client;
    }

    /**
     * Construct & cache an IPC client with the default SocketFactory
     * and default valueClass if no cached client exists.
     *
     * @param conf Configuration
     * @return an IPC client
     */
    public synchronized Client getClient(Configuration conf) {
        return getClient(conf, SocketFactory.getDefault(), RpcResponseWrapper.class);
    }

    /**
     * Construct & cache an IPC client with the user-provided SocketFactory
     * if no cached client exists. Default response type is ObjectWritable.
     *
     * @param conf Configuration
     * @param factory SocketFactory for client socket
     * @return an IPC client
     */
    public synchronized Client getClient(Configuration conf, SocketFactory factory) {
        return this.getClient(conf, factory, RpcResponseWrapper.class);
    }

    /**
     * Stop a RPC client connection
     * A RPC client is closed only when its reference count becomes zero.
     */
    public void stopClient(Client client) {
        if (Client.LOG.isDebugEnabled()) {
            Client.LOG.debug("stopping client from cache: " + client);
        }
        synchronized (this) {
            client.decCount();
            if (client.isZeroReference()) {
                if (Client.LOG.isDebugEnabled()) {
                    Client.LOG.debug("removing client from cache: " + client);
                }
                clients.remove(client.getSocketFactory());
            }
        }
        if (client.isZeroReference()) {
            if (Client.LOG.isDebugEnabled()) {
                Client.LOG.debug("stopping actual client because no more references remain: "
                        + client);
            }
            client.stop();
        }
    }
}
