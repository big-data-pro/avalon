package com.immomo.datainfo.rpc;

import java.io.IOException;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * This interface is implemented by the client side translators and can be used
 * to obtain information about underlying protocol e.g. to check if a method is
 * supported on the server side.
 */
@InterfaceAudience.Private
@InterfaceStability.Stable
public interface ProtocolMetaInterface {

    /**
     * Checks whether the given method name is supported by the server.
     * It is assumed that all method names are unique for a protocol.
     * @param methodName The name of the method
     * @return true if method is supported, otherwise false.
     * @throws IOException
     */
    public boolean isMethodSupported(String methodName) throws IOException;
}