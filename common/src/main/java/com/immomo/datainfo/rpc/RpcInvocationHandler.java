package com.immomo.datainfo.rpc;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import com.immomo.datainfo.rpc.Client.ConnectionId;

/**
 * This interface must be implemented by all InvocationHandler
 * implementations.
 */
public interface RpcInvocationHandler extends InvocationHandler, Closeable {

    /**
     * Returns the connection id associated with the InvocationHandler instance.
     * @return ConnectionId
     */
    ConnectionId getConnectionId();
}
