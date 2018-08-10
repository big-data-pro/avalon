package com.immomo.datainfo.net;

import java.net.SocketTimeoutException;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Created by kangkai on 18/2/5.
 */

/**
 * Thrown by NetUtils#connect(java.net.Socket, java.net.SocketAddress, int)}
 * if it times out while connecting to the remote host.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class ConnectTimeoutException extends SocketTimeoutException {
    private static final long serialVersionUID = 1L;

    public ConnectTimeoutException(String msg) {
        super(msg);
    }
}
