package com.immomo.datainfo.rpc;

import com.google.protobuf.BlockingService;
import com.immomo.datainfo.avalon.CommonConfigurationKeys;
import com.immomo.datainfo.conf.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.immomo.datainfo.rpc.protocol.TestProtos.TestProtocolService;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/**
 * Created by kangkai on 18/2/28.
 */
public class TestRpcServer implements TestProtocol {
    private static final Log LOG = LogFactory.getLog(TestRpcServer.class.getName());
    private Configuration conf;
    int fastPingCounter = 0;
    /** The RPC server that listens to requests from DataNodes */
    private final RPC.Server serviceRpcServer;
    private final InetSocketAddress serviceRPCAddress;

    /** The RPC server that listens to requests from clients */
    protected final RPC.Server clientRpcServer;
    protected final InetSocketAddress clientRpcAddress;
    private static final String ADDRESS = "0.0.0.0";
    private static final int PORT = 8020;

    public static void addPBProtocol(Configuration conf, Class<?> protocol,
                                     BlockingService service, RPC.Server server) throws IOException {
        RPC.setProtocolEngine(conf, protocol, ProtobufRpcEngine.class);
        server.addProtocol(RPC.RpcKind.RPC_PROTOCOL_BUFFER, protocol, service);
    }

    public TestRpcServer(Configuration conf) throws IOException{
        int handlerCount =
                conf.getInt(CommonConfigurationKeys.AVALON_SERVER_HANDLER_COUNT_KEY,
                        CommonConfigurationKeys.AVALON_SERVER_HANDLER_COUNT_DEFAULT);

        RPC.setProtocolEngine(conf, TestProtocolPB.class,
                ProtobufRpcEngine.class);

        TestProtocolServerSideTranslatorPB
                testProtocolServerTranslator =
                new TestProtocolServerSideTranslatorPB(this);
        BlockingService testPbService = TestProtocolService.newReflectiveBlockingService(testProtocolServerTranslator);
        int serviceHandlerCount =
                conf.getInt(CommonConfigurationKeys.AVALON_SERVICE_HANDLER_COUNT_KEY,
                        CommonConfigurationKeys.AVALON_SERVICE_HANDLER_COUNT_DEFAULT);
        this.serviceRpcServer = new RPC.Builder(conf)
                .setProtocol(TestProtocolPB.class)
                .setInstance(testPbService)
                .setBindAddress(ADDRESS)
                .setPort(PORT).setNumHandlers(serviceHandlerCount)
                .setVerbose(false)
                .build();


        // Add all the RPC protocols that the namenode implements
        addPBProtocol(conf, TestProtocolPB.class, testPbService,
                serviceRpcServer);
        // Update the address with the correct port
        InetSocketAddress listenAddr = serviceRpcServer.getListenerAddress();
        serviceRPCAddress = new InetSocketAddress(
                "localhost", listenAddr.getPort());

        this.clientRpcServer = new RPC.Builder(conf)
                .setProtocol(TestProtocolPB.class)
                .setInstance(testPbService).setBindAddress(ADDRESS)
                .setPort(8022).setNumHandlers(handlerCount)
                .setVerbose(false)
                .build();

        // Add all the RPC protocols that the namenode implements
        addPBProtocol(conf, TestProtocolPB.class, testPbService,
                clientRpcServer);
        clientRpcAddress = new InetSocketAddress("localhost", listenAddr.getPort());

        this.conf = conf;
    }

    public void ping() throws IOException {
        LOG.info("testRpcServer:" + "ping");
    }

    public synchronized void slowPing(boolean shouldSlow) throws IOException {
        if (shouldSlow) {
            while (fastPingCounter < 2) {
                try {
                    wait();  // slow response until two fast pings happened
                } catch (InterruptedException ignored) {}
            }
            fastPingCounter -= 2;
        } else {
            fastPingCounter++;
            notify();
        }
        LOG.info("Slow Ping ;" + fastPingCounter);
    }

    public void sleep(long delay) throws IOException, InterruptedException {
        LOG.info("delay:" + delay);
    }

    public String echo(String value) throws IOException {
        LOG.info("TestRpcServer:" + value);
        return value;
    }

    public int add(int v1, int v2) throws IOException {
        System.out.println(v1 + " + " + v2 );
        return v1 + v2;

    }
    @Override
    public int[] exchange(int[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        return values;
    }

    /**
     * Start client and service RPC servers.
     */
    void start() {
        clientRpcServer.start();
        if (serviceRpcServer != null) {
            serviceRpcServer.start();
        }
    }

    /**
     * Wait until the RPC servers have shutdown.
     */
    void join() throws InterruptedException {
        clientRpcServer.join();
        if (serviceRpcServer != null) {
            serviceRpcServer.join();
        }
    }

    /**
     * Stop client and service RPC servers.
     */
    void stop() {
        if (clientRpcServer != null) {
            clientRpcServer.stop();
        }
        if (serviceRpcServer != null) {
            serviceRpcServer.stop();
        }
    }
    InetSocketAddress getServiceRpcAddress() {
        return serviceRPCAddress;
    }

    InetSocketAddress getRpcAddress() {
        return clientRpcAddress;
    }

    RPC.Server getServiceRpcServer() {
        return serviceRpcServer;
    }

    RPC.Server getClientRpcServer() {
        return clientRpcServer;
    }
    int getMaxQueueSize() {
        return serviceRpcServer.getMaxQueueSize();
    }
    int getNumReaders() {
        return serviceRpcServer.getNumReaders();
    }
}
