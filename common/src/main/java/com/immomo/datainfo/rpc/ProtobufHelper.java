package com.immomo.datainfo.rpc;

import com.google.protobuf.ServiceException;

import java.io.IOException;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;

/**
 * Helper methods for protobuf related RPC implementation
 */
@InterfaceAudience.Private
public class ProtobufHelper {
    private ProtobufHelper() {
        // Hidden constructor for class with only static helper methods
    }

    /**
     * Return the IOException thrown by the remote server wrapped in
     * ServiceException as cause.
     * @param se ServiceException that wraps IO exception thrown by the server
     * @return Exception wrapped in ServiceException or
     *         a new IOException that wraps the unexpected ServiceException.
     */
    public static IOException getRemoteException(ServiceException se) {
        Throwable e = se.getCause();
        if (e == null) {
            return new IOException(se);
        }
        return e instanceof IOException ? (IOException) e : new IOException(se);
    }
}
