package com.immomo.datainfo.rpc;


import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcErrorCodeProto;
import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto;

/**
 * Indicates an exception on the RPC server
 */
public class RpcServerException extends RpcException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs exception with the specified detail message.
     * @param message detailed message.
     */
    public RpcServerException(final String message) {
        super(message);
    }

    /**
     * Constructs exception with the specified detail message and cause.
     *
     * @param message message.
     * @param cause the cause (can be retried by the {@link #getCause()} method).
     *          (A <tt>null</tt> value is permitted, and indicates that the cause
     *          is nonexistent or unknown.)
     */
    public RpcServerException(final String message, final Throwable cause) {
        super(message, cause);
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
        return RpcErrorCodeProto.ERROR_RPC_SERVER;
    }
}
