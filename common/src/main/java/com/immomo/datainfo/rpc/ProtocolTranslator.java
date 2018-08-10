package com.immomo.datainfo.rpc;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;

/**
 * An interface implemented by client-side protocol translators to get the
 * underlying proxy object the translator is operating on.
 */
@InterfaceAudience.Private
public interface ProtocolTranslator {

    /**
     * Return the proxy object underlying this protocol translator.
     * @return the proxy object underlying this protocol translator.
     */
    public Object getUnderlyingProxyObject();

}

