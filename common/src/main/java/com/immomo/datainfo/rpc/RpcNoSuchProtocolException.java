package com.immomo.datainfo.rpc;

import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcErrorCodeProto;
import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto;

/**
 * No such protocol (i.e. interface) for and Rpc Call
 *
 */
public class RpcNoSuchProtocolException extends RpcServerException {
    private static final long serialVersionUID = 1L;
    public RpcNoSuchProtocolException(final String message) {
        super(message);
    }

    /**
     * get the rpc status corresponding to this exception
     */
    public RpcStatusProto getRpcStatusProto() {
        return RpcStatusProto.ERROR;
    }

    /**
     * get the detailed rpc status corresponding to this exception
     */
    public RpcErrorCodeProto getRpcErrorCodeProto() {
        return RpcErrorCodeProto.ERROR_NO_SUCH_PROTOCOL;
    }
}
