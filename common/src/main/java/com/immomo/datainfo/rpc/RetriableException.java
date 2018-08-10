package com.immomo.datainfo.rpc;

import java.io.IOException;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Exception thrown by a server typically to indicate that server is in a state
 * where request cannot be processed temporarily (such as still starting up).
 * Client may retry the request. If the service is up, the server may be able to
 * process a retried request.
 */
@InterfaceStability.Evolving
public class RetriableException extends IOException {
    private static final long serialVersionUID = 1915561725516487301L;

    public RetriableException(Exception e) {
        super(e);
    }

    public RetriableException(String msg) {
        super(msg);
    }
}
