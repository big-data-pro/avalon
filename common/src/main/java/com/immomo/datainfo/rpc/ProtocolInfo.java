package com.immomo.datainfo.rpc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The protocol name that is used when a client and server connect.
 * By default the class name of the protocol interface is the protocol name.
 *
 * Why override the default name (i.e. the class name)?
 * One use case overriding the default name (i.e. the class name) is when
 * there are multiple implementations of the same protocol, each with say a
 *  different version/serialization.
 * In Hadoop this is used to allow multiple server and client adapters
 * for different versions of the same protocol service.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolInfo {
    String protocolName();  // the name of the protocol (i.e. rpc service)
    long protocolVersion() default -1; // default means not defined use old way
}
