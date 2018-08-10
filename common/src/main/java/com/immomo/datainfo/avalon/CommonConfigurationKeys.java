package com.immomo.datainfo.avalon;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * This class contains constants for configuration keys used
 * in the common code.
 *
 * It inherits all the publicly documented configuration keys
 * and adds unsupported keys.
 *
 */

@InterfaceAudience.Private
@InterfaceStability.Unstable
public class CommonConfigurationKeys {
    public static final String  AVALON_SECURITY_AUTHORIZATION = "avalon.security.authorization";

    public static final String  IO_FILE_BUFFER_SIZE = "io.file.buffer.size";
    public static final int     IO_FILE_BUFFER_SIZE_DEFAULT = 4096;

    public static final String  IO_SERIALIZATIONS_KEY = "io.serializations";
    public static final String  IO_SERIALIZATIONS_VALUE_DEFAULT = "com.immomo.datainfo.io.serializer.WritableSerialization";

    public static final String  RPC_MAXIMUM_DATA_LENGTH = "rpc.maximum.data.length";
    public static final int     RPC_MAXIMUM_DATA_LENGTH_DEFAULT = 64 * 1024 * 1024;

    /** How many calls per handler are allowed in the queue. */
    public static final String  RPC_SERVER_HANDLER_QUEUE_SIZE_KEY = "rpc.server.handler.queue.size";
    /** Default value for RPC_SERVER_HANDLER_QUEUE_SIZE_KEY */
    public static final int     RPC_SERVER_HANDLER_QUEUE_SIZE_DEFAULT = 100;

    /** Responses larger than this will be logged */
    public static final String  RPC_SERVER_RPC_MAX_RESPONSE_SIZE_KEY = "rpc.server.max.response.size";
    /** Default value for RPC_SERVER_RPC_MAX_RESPONSE_SIZE_KEY */
    public static final int     RPC_SERVER_RPC_MAX_RESPONSE_SIZE_DEFAULT = 1024*1024;

    /** Number of threads in RPC server reading from the socket */
    public static final String  RPC_SERVER_RPC_READ_THREADS_KEY = "rpc.server.read.threadpool.size";
    /** Default value for RPC_SERVER_RPC_READ_THREADS_KEY */
    public static final int     RPC_SERVER_RPC_READ_THREADS_DEFAULT = 1;

    /** Number of pending connections that may be queued per socket reader */
    public static final String RPC_SERVER_RPC_READ_CONNECTION_QUEUE_SIZE_KEY = "rpc.server.read.connection-queue.size";
    /** Default value for RPC_SERVER_RPC_READ_CONNECTION_QUEUE_SIZE */
    public static final int RPC_SERVER_RPC_READ_CONNECTION_QUEUE_SIZE_DEFAULT = 100;

    public static final String  RPC_SERVER_TCPNODELAY_KEY = "rpc.server.tcpnodelay";
    /** Default value for RPC_SERVER_TCPNODELAY_KEY */
    public static final boolean RPC_SERVER_TCPNODELAY_DEFAULT = true;


    public static final String  RPC_CLIENT_CONNECT_MAX_RETRIES_KEY = "rpc.client.connect.max.retries";
    /** Default value for RPC_CLIENT_CONNECT_MAX_RETRIES_KEY */
    public static final int     RPC_CLIENT_CONNECT_MAX_RETRIES_DEFAULT = 10;

    public static final String  RPC_CLIENT_CONNECT_RETRY_INTERVAL_KEY = "rpc.client.connect.retry.interval";
    /** Default value for RPC_CLIENT_CONNECT_RETRY_INTERVAL_KEY */
    public static final int     RPC_CLIENT_CONNECT_RETRY_INTERVAL_DEFAULT = 1000;

    public static final String  RPC_CLIENT_CONNECT_TIMEOUT_KEY = "rpc.client.connect.timeout";
    /** Default value for RPC_CLIENT_CONNECT_TIMEOUT_KEY */
    public static final int     RPC_CLIENT_CONNECT_TIMEOUT_DEFAULT = 20000; // 20s

    public static final String  RPC_CLIENT_CONNECT_MAX_RETRIES_ON_SOCKET_TIMEOUTS_KEY = "rpc.client.connect.max.retries.on.timeouts";
    /** Default value for RPC_CLIENT_CONNECT_MAX_RETRIES_ON_SOCKET_TIMEOUTS_KEY */
    public static final int     RPC_CLIENT_CONNECT_MAX_RETRIES_ON_SOCKET_TIMEOUTS_DEFAULT = 45;

    public static final String  RPC_CLIENT_TCPNODELAY_KEY = "rpc.client.tcpnodelay";
    /** Defalt value for RPC_CLIENT_TCPNODELAY_KEY */
    public static final boolean RPC_CLIENT_TCPNODELAY_DEFAULT = true;

    /** Enables pings from RPC client to the server */
    public static final String  RPC_CLIENT_PING_KEY = "rpc.client.ping";
    /** Default value of RPC_CLIENT_PING_KEY */
    public static final boolean RPC_CLIENT_PING_DEFAULT = true;
    public static final String  RPC_PING_INTERVAL_KEY = "rpc.ping.interval";
    /** Default value for RPC_PING_INTERVAL_KEY */
    public static final int     RPC_PING_INTERVAL_DEFAULT = 60000; // 1 min

    /**
     * CallQueue related settings. These are not used directly, but rather
     * combined with a namespace and port. For instance:
     * RPC_CALLQUEUE_NAMESPACE + ".8020." + RPC_CALLQUEUE_IMPL_KEY
     */
    public static final String  RPC_CALLQUEUE_NAMESPACE = "rpc";
    public static final String  RPC_CALLQUEUE_IMPL_KEY = "callqueue.impl";

    public static final String  RPC_SERVER_LISTEN_QUEUE_SIZE_KEY = "rpc.server.listen.queue.size";
    /** Default value for RPC_SERVER_LISTEN_QUEUE_SIZE_KEY */
    public static final int     RPC_SERVER_LISTEN_QUEUE_SIZE_DEFAULT = 128;

    public static final String  RPC_CLIENT_IDLETHRESHOLD_KEY = "rpc.client.idlethreshold";
    /** Default value for RPC_CLIENT_IDLETHRESHOLD_DEFAULT */
    public static final int     RPC_CLIENT_IDLETHRESHOLD_DEFAULT = 4000;

    /** How often the server scans for idle connections */
    public static final String RPC_CLIENT_CONNECTION_IDLESCANINTERVAL_KEY = "rpc.client.connection.idle-scan-interval.ms";
    /** Default value for RPC_SERVER_CONNECTION_IDLE_SCAN_INTERVAL_KEY */
    public static final int RPC_CLIENT_CONNECTION_IDLESCANINTERVAL_DEFAULT = 10000;

    public static final String  RPC_CLIENT_CONNECTION_MAXIDLETIME_KEY = "rpc.client.connection.maxidletime";
    /** Default value for RPC_CLIENT_CONNECTION_MAXIDLETIME_KEY */
    public static final int     RPC_CLIENT_CONNECTION_MAXIDLETIME_DEFAULT = 10000; // 10s

    public static final String  RPC_CLIENT_KILL_MAX_KEY = "rpc.client.kill.max";
    /** Default value for RPC_CLIENT_KILL_MAX_KEY */
    public static final int     RPC_CLIENT_KILL_MAX_DEFAULT = 10;

    public static final String  AVALON_RPC_SOCKET_FACTORY_CLASS_DEFAULT_KEY = "avalon.rpc.socket.factory.class.default";
    public static final String  AVALON_RPC_SOCKET_FACTORY_CLASS_DEFAULT_DEFAULT = "com.immomo.datainfo.net.StandardSocketFactory";

    public static final String AVALON_SERVER_HANDLER_COUNT_KEY = "avalon.handler.count";
    public static final int AVALON_SERVER_HANDLER_COUNT_DEFAULT = 1;

    public static final String AVALON_SERVICE_HANDLER_COUNT_KEY = "avalon.service.handler.count";
    public static final int AVALON_SERVICE_HANDLER_COUNT_DEFAULT = 1;


}
