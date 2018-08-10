package com.immomo.datainfo.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.*;
import com.immomo.datainfo.io.DataOutputOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.SocketFactory;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import com.immomo.datainfo.conf.Configuration;
import com.immomo.datainfo.io.RpcWrapper;
import com.immomo.datainfo.io.retry.RetryPolicy;
import com.immomo.datainfo.rpc.Client.ConnectionId;
import com.immomo.datainfo.rpc.RPC.RpcInvoker;
import com.immomo.datainfo.rpc.protobuf.ProtobufRpcEngineProtos.RequestHeaderProto;
import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto;
import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto;

import com.immomo.datainfo.util.ProtoUtil;
import com.immomo.datainfo.util.Time;

import com.google.protobuf.BlockingService;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.ServiceException;
import com.google.protobuf.TextFormat;


/**
 * RPC Engine for for protobuf based RPCs.
 */
@InterfaceStability.Evolving
public class ProtobufRpcEngine implements RpcEngine {
    public static final Log LOG = LogFactory.getLog(ProtobufRpcEngine.class);

    static { // Register the rpcRequest deserializer for WritableRpcEngine
        Server.registerProtocolEngine(
                RPC.RpcKind.RPC_PROTOCOL_BUFFER, RpcRequestWrapper.class,
                new Server.ProtoBufRpcInvoker());
    }

    private static final ClientCache CLIENTS = new ClientCache();

    public <T> ProtocolProxy<T> getProxy(Class<T> protocol, long clientVersion,
                                         InetSocketAddress addr, Configuration conf,
                                         SocketFactory factory, int rpcTimeout) throws IOException {
        return getProxy(protocol, clientVersion, addr, conf, factory,
                rpcTimeout, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ProtocolProxy<T> getProxy(Class<T> protocol, long clientVersion,
                                         InetSocketAddress addr, Configuration conf,
                                         SocketFactory factory, int rpcTimeout, RetryPolicy connectionRetryPolicy) throws IOException {
        final Invoker invoker = new Invoker(protocol, addr, conf, factory,
                rpcTimeout, connectionRetryPolicy);
        return new ProtocolProxy<T>(protocol, (T) Proxy.newProxyInstance(
                protocol.getClassLoader(), new Class[]{protocol}, invoker), false);
    }

    @Override
    public ProtocolProxy<ProtocolMetaInfoPB> getProtocolMetaInfoProxy(
            ConnectionId connId, Configuration conf, SocketFactory factory)
            throws IOException {
        Class<ProtocolMetaInfoPB> protocol = ProtocolMetaInfoPB.class;
        return new ProtocolProxy<ProtocolMetaInfoPB>(protocol,
                (ProtocolMetaInfoPB) Proxy.newProxyInstance(protocol.getClassLoader(),
                        new Class[] { protocol }, new Invoker(protocol, connId, conf,
                                factory)), false);
    }

    private static class Invoker implements RpcInvocationHandler {
        private final Map<String, Message> returnTypes =
                new ConcurrentHashMap<String, Message>();
        private boolean isClosed = false;
        private final ConnectionId remoteId;
        private final Client client;
        private final long clientProtocolVersion;
        private final String protocolName;

        private Invoker(Class<?> protocol, InetSocketAddress addr,
                        Configuration conf, SocketFactory factory,
                        int rpcTimeout, RetryPolicy connectionRetryPolicy) throws IOException {
            this(protocol, ConnectionId.getConnectionId(
                            addr, protocol, rpcTimeout, connectionRetryPolicy, conf),
                    conf, factory);
        }

        /**
         * This constructor takes a connectionId, instead of creating a new one.
         */
        private Invoker(Class<?> protocol, ConnectionId connId,
                        Configuration conf, SocketFactory factory) {
            this.remoteId = connId;
            this.client = CLIENTS.getClient(conf, factory, RpcResponseWrapper.class);
            this.protocolName = RPC.getProtocolName(protocol);
            this.clientProtocolVersion = RPC
                    .getProtocolVersion(protocol);
        }

        private RequestHeaderProto constructRpcRequestHeader(Method method) {
            RequestHeaderProto.Builder builder = RequestHeaderProto
                    .newBuilder();
            builder.setMethodName(method.getName());


            // For protobuf, {@code protocol} used when creating client side proxy is
            // the interface extending BlockingInterface, which has the annotations
            // such as ProtocolName etc.
            //
            // Using Method.getDeclaringClass(), as in WritableEngine to get at
            // the protocol interface will return BlockingInterface, from where
            // the annotation ProtocolName and Version cannot be
            // obtained.
            //
            // Hence we simply use the protocol class used to create the proxy.
            // For PB this may limit the use of mixins on client side.
            builder.setDeclaringClassProtocolName(protocolName);
            builder.setClientProtocolVersion(clientProtocolVersion);
            return builder.build();
        }

        /**
         * This is the client side invoker of RPC method. It only throws
         * ServiceException, since the invocation proxy expects only
         * ServiceException to be thrown by the method in case protobuf service.
         *
         * ServiceException has the following causes:
         * <ol>
         * <li>Exceptions encountered on the client side in this method are
         * set as cause in ServiceException as is.</li>
         * <li>Exceptions from the server are wrapped in RemoteException and are
         * set as cause in ServiceException</li>
         * </ol>
         *
         * Note that the client calling protobuf RPC methods, must handle
         * ServiceException by getting the cause from the ServiceException. If the
         * cause is RemoteException, then unwrap it to get the exception thrown by
         * the server.
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws ServiceException {
            long startTime = 0;
            if (LOG.isDebugEnabled()) {
                startTime = Time.now();
            }
            if (args.length != 2) { // RpcController + Message
                throw new ServiceException("Too many parameters for request. Method: ["
                        + method.getName() + "]" + ", Expected: 2, Actual: "
                        + args.length);
            }
            if (args[1] == null) {
                throw new ServiceException("null param while calling Method: ["
                        + method.getName() + "]");
            }
            RequestHeaderProto rpcRequestHeader = constructRpcRequestHeader(method);
            if (LOG.isTraceEnabled()) {
                LOG.trace(Thread.currentThread().getId() + ": Call -> " +
                        remoteId + ": " + method.getName() +
                        " {" + TextFormat.shortDebugString((Message) args[1]) + "}");
            }


            Message theRequest = (Message) args[1];
            final RpcResponseWrapper val;
            try {
                val = (RpcResponseWrapper) client.call(RPC.RpcKind.RPC_PROTOCOL_BUFFER,
                        new RpcRequestWrapper(rpcRequestHeader, theRequest), remoteId);

            } catch (Throwable e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(Thread.currentThread().getId() + ": Exception <- " +
                            remoteId + ": " + method.getName() +
                            " {" + e + "}");
                }

                throw new ServiceException(e);
            } finally {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("RPC ivoke finished.");
                }
            }

            if (LOG.isDebugEnabled()) {
                long callTime = Time.now() - startTime;
                LOG.debug("Call: " + method.getName() + " took " + callTime + "ms");
            }

            Message prototype = null;
            try {
                prototype = getReturnProtoType(method);
            } catch (Exception e) {
                throw new ServiceException(e);
            }
            Message returnMessage;
            try {
                returnMessage = prototype.newBuilderForType()
                        .mergeFrom(val.theResponseRead).build();

                if (LOG.isTraceEnabled()) {
                    LOG.trace(Thread.currentThread().getId() + ": Response <- " +
                            remoteId + ": " + method.getName() +
                            " {" + TextFormat.shortDebugString(returnMessage) + "}");
                }

            } catch (Throwable e) {
                throw new ServiceException(e);
            }
            return returnMessage;
        }

        @Override
        public void close() throws IOException {
            if (!isClosed) {
                isClosed = true;
                CLIENTS.stopClient(client);
            }
        }

        private Message getReturnProtoType(Method method) throws Exception {
            if (returnTypes.containsKey(method.getName())) {
                return returnTypes.get(method.getName());
            }

            Class<?> returnType = method.getReturnType();
            Method newInstMethod = returnType.getMethod("getDefaultInstance");
            newInstMethod.setAccessible(true);
            Message prototype = (Message) newInstMethod.invoke(null, (Object[]) null);
            returnTypes.put(method.getName(), prototype);
            return prototype;
        }

        @Override //RpcInvocationHandler
        public ConnectionId getConnectionId() {
            return remoteId;
        }
    }
    /**
     * Wrapper for Protocol Buffer Requests
     *
     * Note while this wrapper is writable, the request on the wire is in
     * Protobuf. Several methods on {@link com.immomo.datainfo.rpc.Server and RPC}
     * use type Writable as a wrapper to work across multiple RpcEngine kinds.
     */
    private static abstract class RpcMessageWithHeader<T extends GeneratedMessage>
            implements RpcWrapper {
        T requestHeader;
        Message theRequest; // for clientSide, the request is here
        byte[] theRequestRead; // for server side, the request is here

        public RpcMessageWithHeader() {
        }

        public RpcMessageWithHeader(T requestHeader, Message theRequest) {
            this.requestHeader = requestHeader;
            this.theRequest = theRequest;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            OutputStream os = DataOutputOutputStream.constructOutputStream(out);

            ((Message)requestHeader).writeDelimitedTo(os);
            theRequest.writeDelimitedTo(os);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            requestHeader = parseHeaderFrom(readVarintBytes(in));
            theRequestRead = readMessageRequest(in);
        }

        abstract T parseHeaderFrom(byte[] bytes) throws IOException;

        byte[] readMessageRequest(DataInput in) throws IOException {
            return readVarintBytes(in);
        }

        private static byte[] readVarintBytes(DataInput in) throws IOException {
            final int length = ProtoUtil.readRawVarint32(in);
            final byte[] bytes = new byte[length];
            in.readFully(bytes);
            return bytes;
        }

        public T getMessageHeader() {
            return requestHeader;
        }

        public byte[] getMessageBytes() {
            return theRequestRead;
        }

        @Override
        public int getLength() {
            int headerLen = requestHeader.getSerializedSize();
            int reqLen;
            if (theRequest != null) {
                reqLen = theRequest.getSerializedSize();
            } else if (theRequestRead != null ) {
                reqLen = theRequestRead.length;
            } else {
                throw new IllegalArgumentException(
                        "getLength on uninitialized RpcWrapper");
            }
            return CodedOutputStream.computeRawVarint32Size(headerLen) +  headerLen
                    + CodedOutputStream.computeRawVarint32Size(reqLen) + reqLen;
        }
    }

    public static class RpcRequestWrapper
            extends RpcMessageWithHeader<RequestHeaderProto> {
        @SuppressWarnings("unused")
        public RpcRequestWrapper() {}

        public RpcRequestWrapper(
                RequestHeaderProto requestHeader, Message theRequest) {
            super(requestHeader, theRequest);
        }

        @Override
        RequestHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
            return RequestHeaderProto.parseFrom(bytes);
        }

        @Override
        public String toString() {
            return requestHeader.getDeclaringClassProtocolName() + "." +
                    requestHeader.getMethodName();
        }
    }

    @InterfaceAudience.LimitedPrivate({"RPC"})
    public static class RpcRequestMessageWrapper
            extends RpcMessageWithHeader<RpcRequestHeaderProto> {
        public RpcRequestMessageWrapper() {}

        public RpcRequestMessageWrapper(
                RpcRequestHeaderProto requestHeader, Message theRequest) {
            super(requestHeader, theRequest);
        }

        @Override
        RpcRequestHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
            return RpcRequestHeaderProto.parseFrom(bytes);
        }
    }

    @InterfaceAudience.LimitedPrivate({"RPC"})
    public static class RpcResponseMessageWrapper
            extends RpcMessageWithHeader<RpcResponseHeaderProto> {
        public RpcResponseMessageWrapper() {}

        public RpcResponseMessageWrapper(
                RpcResponseHeaderProto responseHeader, Message theRequest) {
            super(responseHeader, theRequest);
        }

        @Override
        byte[] readMessageRequest(DataInput in) throws IOException {
            // error message contain no message body
            switch (requestHeader.getStatus()) {
                case ERROR:
                case FATAL:
                    return null;
                default:
                    return super.readMessageRequest(in);
            }
        }

        @Override
        RpcResponseHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
            return RpcResponseHeaderProto.parseFrom(bytes);
        }
    }

    /**
     *  Wrapper for Protocol Buffer Responses
     *
     * Note while this wrapper is writable, the request on the wire is in
     * Protobuf. Several methods on {@link com.immomo.datainfo.rpc.Server and RPC}
     * use type Writable as a wrapper to work across multiple RpcEngine kinds.
     */
    @InterfaceAudience.LimitedPrivate({"RPC"}) // temporarily exposed
    public static class RpcResponseWrapper implements RpcWrapper {
        Message theResponse; // for senderSide, the response is here
        byte[] theResponseRead; // for receiver side, the response is here

        public RpcResponseWrapper() {
        }

        public RpcResponseWrapper(Message message) {
            LOG.info("RpcResponseWrapper:" + message);
            this.theResponse = message;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            OutputStream os = DataOutputOutputStream.constructOutputStream(out);
            theResponse.writeDelimitedTo(os);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            int length = ProtoUtil.readRawVarint32(in);
            theResponseRead = new byte[length];
            in.readFully(theResponseRead);
        }

        @Override
        public int getLength() {
            int resLen;
            if (theResponse != null) {
                resLen = theResponse.getSerializedSize();
            } else if (theResponseRead != null ) {
                resLen = theResponseRead.length;
            } else {
                throw new IllegalArgumentException(
                        "getLength on uninitialized RpcWrapper");
            }
            return CodedOutputStream.computeRawVarint32Size(resLen) + resLen;
        }
    }

    @VisibleForTesting
    @InterfaceAudience.Private
    @InterfaceStability.Unstable
    static Client getClient(Configuration conf) {
        return CLIENTS.getClient(conf, SocketFactory.getDefault(),
                RpcResponseWrapper.class);
    }



    @Override
    public RPC.Server getServer(Class<?> protocol, Object protocolImpl,
                                String bindAddress, int port, int numHandlers, int numReaders,
                                int queueSizePerHandler, boolean verbose, Configuration conf,
                                String portRangeConfig)
            throws IOException {
        return new Server(protocol, protocolImpl, conf, bindAddress, port,
                numHandlers, numReaders, queueSizePerHandler, verbose,
                portRangeConfig);
    }

    public static class Server extends RPC.Server {
        /**
         * Construct an RPC server.
         *
         * @param protocolClass the class of protocol
         * @param protocolImpl the protocolImpl whose methods will be called
         * @param conf the configuration to use
         * @param bindAddress the address to bind on to listen for connection
         * @param port the port to listen for connections on
         * @param numHandlers the number of method handler threads to run
         * @param verbose whether each call should be logged
         * @param portRangeConfig A config parameter that can be used to restrict
         * the range of ports used when port is 0 (an ephemeral port)
         */
        public Server(Class<?> protocolClass, Object protocolImpl,
                      Configuration conf, String bindAddress, int port, int numHandlers,
                      int numReaders, int queueSizePerHandler, boolean verbose,
                      String portRangeConfig)
                throws IOException {

            super(bindAddress, port, null, numHandlers,
                    numReaders, queueSizePerHandler, conf, classNameBase(protocolImpl
                            .getClass().getName()), portRangeConfig);
            this.verbose = verbose;
            registerProtocolAndImpl(RPC.RpcKind.RPC_PROTOCOL_BUFFER, protocolClass,
                    protocolImpl);
        }

        /**
         * Protobuf invoker for {@link RpcInvoker}
         */
        static class ProtoBufRpcInvoker implements RpcInvoker {
            private static ProtoClassProtoImpl getProtocolImpl(RPC.Server server,
                                                               String protoName, long clientVersion) throws RpcServerException {
                ProtoNameVer pv = new ProtoNameVer(protoName, clientVersion);
                ProtoClassProtoImpl impl =
                        server.getProtocolImplMap(RPC.RpcKind.RPC_PROTOCOL_BUFFER).get(pv);
                if (impl == null) { // no match for Protocol AND Version
                    VerProtocolImpl highest =
                            server.getHighestSupportedProtocol(RPC.RpcKind.RPC_PROTOCOL_BUFFER,
                                    protoName);
                    if (highest == null) {
                        throw new RpcNoSuchProtocolException(
                                "Unknown protocol: " + protoName);
                    }
                    // protocol supported but not the version that client wants
                    throw new RPC.VersionMismatch(protoName, clientVersion,
                            highest.version);
                }
                return impl;
            }

            @Override
            /**
             * This is a server side method, which is invoked over RPC. On success
             * the return response has protobuf response payload. On failure, the
             * exception name and the stack trace are return in the resposne.
             * See {@link HadoopRpcResponseProto}
             *
             * In this method there three types of exceptions possible and they are
             * returned in response as follows.
             * <ol>
             * <li> Exceptions encountered in this method that are returned
             * as {@link RpcServerException} </li>
             * <li> Exceptions thrown by the service is wrapped in ServiceException.
             * In that this method returns in response the exception thrown by the
             * service.</li>
             * <li> Other exceptions thrown by the service. They are returned as
             * it is.</li>
             * </ol>
             */
            public RpcWrapper call(RPC.Server server, String protocol,
                                 RpcWrapper writableRequest, long receiveTime) throws Exception {
                RpcRequestWrapper request = (RpcRequestWrapper) writableRequest;
                RequestHeaderProto rpcRequest = request.requestHeader;
                String methodName = rpcRequest.getMethodName();
                String protoName = rpcRequest.getDeclaringClassProtocolName();
                long clientVersion = rpcRequest.getClientProtocolVersion();
                if (server.verbose)
                    LOG.info("Call: protocol=" + protocol + ", method=" + methodName);

                ProtoClassProtoImpl protocolImpl = getProtocolImpl(server, protoName,
                        clientVersion);
                BlockingService service = (BlockingService) protocolImpl.protocolImpl;
                Descriptors.MethodDescriptor methodDescriptor = service.getDescriptorForType()
                        .findMethodByName(methodName);
                if (methodDescriptor == null) {
                    String msg = "Unknown method " + methodName + " called on " + protocol
                            + " protocol.";
                    LOG.warn(msg);
                    throw new RpcNoSuchMethodException(msg);
                }
                Message prototype = service.getRequestPrototype(methodDescriptor);
                Message param = prototype.newBuilderForType()
                        .mergeFrom(request.theRequestRead).build();

                Message result;
                long startTime = Time.now();
                int qTime = (int) (startTime - receiveTime);
                Exception exception = null;
                try {
                    result = service.callBlockingMethod(methodDescriptor, null, param);
                } catch (ServiceException e) {
                    exception = (Exception) e.getCause();
                    throw (Exception) e.getCause();
                } catch (Exception e) {
                    exception = e;
                    throw e;
                } finally {
                    int processingTime = (int) (Time.now() - startTime);
                    if (LOG.isDebugEnabled()) {
                        String msg = "Served: " + methodName + " queueTime= " + qTime +
                                " procesingTime= " + processingTime;
                        if (exception != null) {
                            msg += " exception= " + exception.getClass().getSimpleName();
                        }
                        LOG.debug(msg);
                    }
                    String detailedMetricsName = (exception == null) ?
                            methodName :
                            exception.getClass().getSimpleName();
//                    LOG.info("RPC Served: " + methodName + " [" + detailedMetricsName + ",query time:" + qTime + ", process time:" + processingTime + "].");
                }
                return new RpcResponseWrapper(result);
            }
        }
    }
}
