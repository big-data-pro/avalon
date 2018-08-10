package com.immomo.datainfo.rpc;

import com.google.protobuf.DescriptorProtos;
import com.immomo.datainfo.avalon.AvalonIllegalArgumentException;
import com.immomo.datainfo.io.retry.RetryPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import javax.net.SocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import com.immomo.datainfo.conf.Configuration;
import com.immomo.datainfo.avalon.CommonConfigurationKeys;
import com.immomo.datainfo.io.retry.RetryPolicies;
import com.immomo.datainfo.io.UTF8;
import com.immomo.datainfo.io.Writable;
import com.immomo.datainfo.io.retry.RetryProxy;
import com.immomo.datainfo.rpc.Client.ConnectionId;
import com.immomo.datainfo.rpc.RPC.RpcKind;
import com.immomo.datainfo.rpc.ProtobufRpcEngine.RpcRequestWrapper;
import com.immomo.datainfo.rpc.Server.Connection;
import com.immomo.datainfo.rpc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto;
import com.immomo.datainfo.net.ConnectTimeoutException;
import com.immomo.datainfo.net.NetUtils;
import com.immomo.datainfo.test.MockitoUtil;


/** Unit tests for RPC. */

public class TestRPC {
    private static final String ADDRESS = "0.0.0.0";

    public static final Log LOG =
            LogFactory.getLog(TestRPC.class);

    private static Configuration conf;


    @Before
    public void setupConf() {
        conf = new Configuration();
        conf.addResource("avalon-default.xml");
        conf.setClass("rpc.engine." + StoppedProtocol.class.getName(),
                StoppedRpcEngine.class, RpcEngine.class);
    }

    int datasize = 100;
    int numThreads = 2;

    public static class TestImpl implements TestProtocol,VersionedProtocol {
        int fastPingCounter = 0;

        @Override
        public long getProtocolVersion(String protocol, long clientVersion) {
            return TestProtocol.versionID;
        }

        @Override
        public ProtocolSignature getProtocolSignature(String protocol, long clientVersion,
                                                      int hashcode) {
            return new ProtocolSignature(TestProtocol.versionID, null);
        }

        @Override
        public void ping() {LOG.info("do ping");}

        @Override
        public synchronized void slowPing(boolean shouldSlow) {
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
        }

        @Override
        public void sleep(long delay) throws InterruptedException {
            Thread.sleep(delay);
        }

        @Override
        public String echo(String value) throws IOException { return value; }

        @Override
        public int add(int v1, int v2) {
            return v1 + v2;
        }
        @Override
        public int[] exchange(int[] values) {
            for (int i = 0; i < values.length; i++) {
                values[i] = i;
            }
            return values;
        }

    }

    //
    // an object that does a bunch of transactions
    //
    static class Transactions implements Runnable {
        int datasize;
        TestProtocol proxy;

        Transactions(TestProtocol proxy, int datasize) {
            this.proxy = proxy;
            this.datasize = datasize;
        }

        // do two RPC that transfers data.
        @Override
        public void run() {
            int[] indata = new int[datasize];
            int[] outdata = null;
            int val = 0;
            try {
                outdata = proxy.exchange(indata);
                val = proxy.add(1,2);
            } catch (IOException e) {
                assertTrue("Exception from RPC exchange() "  + e, false);
            }
            assertEquals(indata.length, outdata.length);
            assertEquals(3, val);
            for (int i = 0; i < outdata.length; i++) {
                assertEquals(outdata[i], i);
            }
        }
    }

    //
    // A class that does an RPC but does not read its response.
    //
    static class SlowRPC implements Runnable {
        private TestProtocol proxy;
        private volatile boolean done;

        SlowRPC(TestProtocol proxy) {
            this.proxy = proxy;
            done = false;
        }

        boolean isDone() {
            return done;
        }

        @Override
        public void run() {
            try {
                proxy.slowPing(true);   // this would hang until two fast pings happened
                done = true;
            } catch (IOException e) {
                assertTrue("SlowRPC ping exception " + e, false);
            }
        }
    }

    /**
     * A basic interface for testing client-side RPC resource cleanup.
     */
    private static interface StoppedProtocol {
        long versionID = 0;

        public void stop();
    }

    /**
     * A class used for testing cleanup of client side RPC resources.
     */
    private static class StoppedRpcEngine implements RpcEngine {

        @Override
        public <T> ProtocolProxy<T> getProxy(Class<T> protocol, long clientVersion,
                                             InetSocketAddress addr, Configuration conf,
                                             SocketFactory factory, int rpcTimeout,RetryPolicy connectionRetryPolicy
        ) throws IOException {
            T proxy = (T) Proxy.newProxyInstance(protocol.getClassLoader(),
                    new Class[]{protocol}, new StoppedInvocationHandler());
            return new ProtocolProxy<T>(protocol, proxy, false);
        }

        @Override
        public RPC.Server getServer(Class<?> protocol,
                                                          Object instance, String bindAddress, int port, int numHandlers,
                                                          int numReaders, int queueSizePerHandler, boolean verbose, Configuration conf,
                                                          String portRangeConfig) throws IOException {
            return null;
        }

        @Override
        public ProtocolProxy<ProtocolMetaInfoPB> getProtocolMetaInfoProxy(
                ConnectionId connId, Configuration conf, SocketFactory factory)
                throws IOException {
            throw new UnsupportedOperationException("This proxy is not supported");
        }
    }

    /**
     * An invocation handler which does nothing when invoking methods, and just
     * counts the number of times close() is called.
     */
    private static class StoppedInvocationHandler
            implements InvocationHandler, Closeable {

        private int closeCalled = 0;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            return null;
        }

        @Override
        public void close() throws IOException {
            closeCalled++;
        }

        public int getCloseCalled() {
            return closeCalled;
        }

    }

    @Test
    public void testConfRpc() throws IOException {
//        Server server = new RPC.Builder(conf).setProtocol(TestProtocol.class)
//                .setInstance(new TestImpl()).setBindAddress(ADDRESS).setPort(0)
//                .setNumHandlers(1).setVerbose(false).build();
        System.out.println("testConfRpc");
        TestRpcServer server = new TestRpcServer(conf);
//        server.start()
        // Just one handler
        int confQ = conf.getInt(
                CommonConfigurationKeys.RPC_SERVER_HANDLER_QUEUE_SIZE_KEY,
                CommonConfigurationKeys.RPC_SERVER_HANDLER_QUEUE_SIZE_DEFAULT);
        assertEquals(confQ, server.getMaxQueueSize());

        int confReaders = conf.getInt(
                CommonConfigurationKeys.RPC_SERVER_RPC_READ_THREADS_KEY,
                CommonConfigurationKeys.RPC_SERVER_RPC_READ_THREADS_DEFAULT);
        assertEquals(confReaders, server.getNumReaders());
        server.stop();

        Server server1 = new RPC.Builder(conf).setProtocol(TestProtocol.class)
                .setInstance(new TestImpl()).setBindAddress(ADDRESS).setPort(0)
                .setNumHandlers(1).setnumReaders(3).setQueueSizePerHandler(200)
                .setVerbose(false).build();

        assertEquals(3, server1.getNumReaders());
        assertEquals(200, server1.getMaxQueueSize());
        server1.stop();
//        server.stop();
    }

    @Test
    public void testProxyAddress() throws IOException {

        System.out.println("testProxyAddress");
        TestProtocol proxy = null;
        TestRpcServer server = null;

        try {
            server = new TestRpcServer(conf);
            InetSocketAddress addr = NetUtils.getConnectAddress(server.getServiceRpcServer());
//            InetSocketAddress addr = server.getServiceRpcAddress();
            TestProtocolPB translatorProxy = RPC.getProtocolProxy(TestProtocolPB.class, TestProtocol.versionID, addr, conf,NetUtils.getDefaultSocketFactory(conf),60000,null).getProxy();
            proxy = new TestProtocolTranslatorPB(translatorProxy);
            server.start();
            LOG.info("testCalls:" + addr.getAddress() + ":" + addr.getHostName() + ":" + addr.getPort());

            // create a client
//            proxy = RPC.getProxy(TestProtocol.class, TestProtocol.versionID, addr, conf);

            assertEquals(addr, RPC.getServerAddress(proxy));
        } finally {
            server.stop();
            if (proxy != null) {
                RPC.stopProxy(proxy);
            }
        }
        System.out.println("End testProxyAddress");
    }

    @Test
    public void testSlowRpc() throws IOException {
        System.out.println("Testing Slow RPC");
        TestProtocol proxy = null;
        TestRpcServer server = null;


        try {
            server = new TestRpcServer(conf);
            InetSocketAddress addr = NetUtils.getConnectAddress(server.getServiceRpcServer());

            TestProtocolPB translatorProxy = RPC.getProtocolProxy(TestProtocolPB.class, TestProtocol.versionID, addr, conf,NetUtils.getDefaultSocketFactory(conf),60000,null).getProxy();
            proxy = new TestProtocolTranslatorPB(translatorProxy);
            server.start();

            LOG.info("testCalls:" + addr.getAddress() + ":" + addr.getHostName() + ":" + addr.getPort());

            SlowRPC slowrpc = new SlowRPC(proxy);
            Thread thread = new Thread(slowrpc, "SlowRPC");
            thread.start(); // send a slow RPC, which won't return until two fast pings
            assertTrue("Slow RPC should not have finished1.", !slowrpc.isDone());
            proxy.slowPing(true);
            proxy.slowPing(false); // first fast ping

            // verify that the first RPC is still stuck
            assertTrue("Slow RPC should not have finished2.", !slowrpc.isDone());

            proxy.slowPing(false); // second fast ping

            // Now the slow ping should be able to be executed
            while (!slowrpc.isDone()) {
                LOG.info("Waiting for slow RPC to get done.");
                try {
                    Thread.sleep(1000);
                    proxy.slowPing(true);
                } catch (InterruptedException e) {}
            }
        } finally {
            server.stop();
            if (proxy != null) {
                RPC.stopProxy(proxy);
            }
            System.out.println("Down slow rpc testing");
        }
    }

    @Test
    public void testCalls() throws IOException, InterruptedException {
        testCallsInternal(conf);
    }

    private void testCallsInternal(Configuration conf) throws IOException,InterruptedException {
        TestRpcServer server = null;
        TestProtocol proxy = null;

        try {
            server = new TestRpcServer(conf);
            server.start();
            /**
            InetSocketAddress addr = NetUtils.getConnectAddress(server.getServiceRpcServer());
            TestProtocolPB proxy2 = RPC.getProxy(
                    TestProtocolPB.class, TestProtocol.versionID, addr, conf);
            LOG.info("testCalls:" + addr.getAddress() + ":" + addr.getHostName() + ":" + addr.getPort());
//            TestProtocolPB translatorProxy = RPC.getProtocolProxy(TestProtocolPB.class, TestProtocol.versionID, addr, conf,NetUtils.getDefaultSocketFactory(conf),60000,null).getProxy();
            proxy = new TestProtocolTranslatorPB(proxy2);
            String result = proxy.echo("test");
            LOG.info(".....TestRpc result:" + result);
            proxy.ping();
            String stringResult = proxy.echo("foo");
            assertEquals(stringResult, "foo");

            int intResult = proxy.add(1, 2);
            assertEquals(intResult, 3);


            // create multiple threads and make them do large data transfers
            System.out.println("Starting multi-threaded RPC test...");

            Thread threadId[] = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                Transactions trans = new Transactions(proxy, datasize);
                threadId[i] = new Thread(trans, "TransactionThread-" + i);
                threadId[i].start();
            }

            // wait for all transactions to get over
            System.out.println("Waiting for all threads to finish RPCs...");
            for (int i = 0; i < numThreads; i++) {
                try {
                    threadId[i].join();
                } catch (InterruptedException e) {
                    i--;      // retry
                }
            }

            int pingInterval = 50;
            conf.setBoolean(CommonConfigurationKeys.RPC_CLIENT_PING_KEY, true);
            conf.setInt(CommonConfigurationKeys.RPC_PING_INTERVAL_KEY, pingInterval);
            proxy.sleep(pingInterval*4);
             */
        } catch(Exception e) {
            server.stop();
            if(proxy!=null) RPC.stopProxy(proxy);
        }
    }

    @Test
    public void testStandaloneClient() throws IOException {
        try {
            TestProtocol proxy = RPC.waitForProxy(TestProtocol.class,
                    TestProtocol.versionID, new InetSocketAddress(ADDRESS, 20), conf, 15000L);
            proxy.echo("");
            fail("We should not have reached here");
        } catch (ConnectException ioe) {
            //this is what we expected
        }
    }

    /**
     * Test stopping a non-registered proxy
     * @throws IOException
     */
    @Test(expected= AvalonIllegalArgumentException.class)
    public void testStopNonRegisteredProxy() throws IOException {
        RPC.stopProxy(null);
    }

    /**
     * Test that the mockProtocol helper returns mock proxies that can
     * be stopped without error.
     */
    @Test
    public void testStopMockObject() throws IOException {
        RPC.stopProxy(MockitoUtil.mockProtocol(TestProtocol.class));
    }

    @Test
    public void testStopProxy() throws IOException {
        StoppedProtocol proxy = RPC.getProxy(StoppedProtocol.class,
                StoppedProtocol.versionID, null, conf);
        StoppedInvocationHandler invocationHandler = (StoppedInvocationHandler)
                Proxy.getInvocationHandler(proxy);
        assertEquals(0, invocationHandler.getCloseCalled());
        RPC.stopProxy(proxy);
        assertEquals(1, invocationHandler.getCloseCalled());
    }

    @Test
    public void testWrappedStopProxy() throws IOException {
        StoppedProtocol wrappedProxy = RPC.getProxy(StoppedProtocol.class,
                StoppedProtocol.versionID, null, conf);
        StoppedInvocationHandler invocationHandler = (StoppedInvocationHandler)
                Proxy.getInvocationHandler(wrappedProxy);

        StoppedProtocol proxy = (StoppedProtocol) RetryProxy.create(StoppedProtocol.class,
                wrappedProxy, RetryPolicies.RETRY_FOREVER);

        assertEquals(0, invocationHandler.getCloseCalled());
        RPC.stopProxy(proxy);
        assertEquals(1, invocationHandler.getCloseCalled());
    }



    /**
     * Count the number of threads that have a stack frame containing
     * the given string
     */
    private static int countThreads(String search) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        int count = 0;
        ThreadInfo[] infos = threadBean.getThreadInfo(threadBean.getAllThreadIds(), 20);
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            for (StackTraceElement elem : info.getStackTrace()) {
                if (elem.getClassName().contains(search)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Test that server.stop() properly stops all threads
     */
    @Test
    public void testStopsAllThreads() throws IOException, InterruptedException {
        int threadsBefore = countThreads("Server$Listener$Reader");
        assertEquals("Expect no Reader threads running before test",
                0, threadsBefore);

        final Server server = new RPC.Builder(conf).setProtocol(TestProtocol.class)
                .setInstance(new TestImpl()).setBindAddress(ADDRESS).setPort(0)
                .setNumHandlers(5).setVerbose(true).build();
        server.start();
        try {
            // Wait for at least one reader thread to start
            int threadsRunning = 0;
            long totalSleepTime = 0;
            do {
                totalSleepTime += 10;
                Thread.sleep(10);
                threadsRunning = countThreads("Server$Listener$Reader");
            } while (threadsRunning == 0 && totalSleepTime < 5000);

            // Validate that at least one thread started (we didn't timeout)
            threadsRunning = countThreads("Server$Listener$Reader");
            assertTrue(threadsRunning > 0);
        } finally {
            server.stop();
        }
        int threadsAfter = countThreads("Server$Listener$Reader");
        assertEquals("Expect no Reader threads left running after test",
                0, threadsAfter);
    }



    @Test(timeout=90000)
    public void testRPCInterruptedSimple() throws IOException {
        final Configuration conf = new Configuration();
        Server server = new RPC.Builder(conf).setProtocol(TestProtocol.class)
                .setInstance(new TestImpl()).setBindAddress(ADDRESS)
                .setPort(0).setNumHandlers(5).setVerbose(true)
                .build();

        server.start();
        InetSocketAddress addr = NetUtils.getConnectAddress(server);

        final TestProtocol proxy = RPC.getProxy(
                TestProtocol.class, TestProtocol.versionID, addr, conf);
        // Connect to the server
        proxy.ping();
        // Interrupt self, try another call
        Thread.currentThread().interrupt();
        try {
            proxy.ping();
            fail("Interruption did not cause IPC to fail");
        } catch (IOException ioe) {
            if (!ioe.toString().contains("InterruptedException")) {
                throw ioe;
            }
            // clear interrupt status for future tests
            Thread.interrupted();
        } finally {
            server.stop();
        }
    }

    @Test(timeout=30000)
    public void testRPCInterrupted() throws IOException, InterruptedException {
        final Configuration conf = new Configuration();
        TestRpcServer server = new TestRpcServer(conf);

        server.start();

        final int numConcurrentRPC = 200;
        InetSocketAddress addr = NetUtils.getConnectAddress(server.getServiceRpcServer());
        final CyclicBarrier barrier = new CyclicBarrier(numConcurrentRPC);
        final CountDownLatch latch = new CountDownLatch(numConcurrentRPC);
        final AtomicBoolean leaderRunning = new AtomicBoolean(true);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Thread leaderThread = null;

        for (int i = 0; i < numConcurrentRPC; i++) {
            final int num = i;
            final TestProtocolPB traslatorProxy = RPC.getProxy(
                    TestProtocolPB.class, TestProtocol.versionID, addr, conf);
            final TestProtocol proxy = new TestProtocolTranslatorPB(traslatorProxy);
            Thread rpcThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        while (num == 0 || leaderRunning.get()) {
                            proxy.slowPing(false);
                        }

                        proxy.slowPing(false);
                    } catch (Exception e) {
                        if (num == 0) {
                            leaderRunning.set(false);
                        } else {
                            error.set(e);
                        }

                        LOG.error(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
            rpcThread.start();

            if (leaderThread == null) {
                leaderThread = rpcThread;
            }
        }
        // let threads get past the barrier
        Thread.sleep(1000);
        // stop a single thread
        while (leaderRunning.get()) {
            leaderThread.interrupt();
        }

        latch.await();

        // should not cause any other thread to get an error
        assertTrue("rpc got exception " + error.get(), error.get() == null);
        server.stop();
    }

    @Test
    public void testConnectionPing() throws Exception {
        Configuration conf = new Configuration();
        int pingInterval = 50;
        conf.setBoolean(CommonConfigurationKeys.RPC_CLIENT_PING_KEY, true);
        conf.setInt(CommonConfigurationKeys.RPC_PING_INTERVAL_KEY, pingInterval);
        final Server server = new RPC.Builder(conf)
                .setProtocol(TestProtocol.class).setInstance(new TestImpl())
                .setBindAddress(ADDRESS).setPort(0).setNumHandlers(5).setVerbose(true)
                .build();
        server.start();

        final TestProtocol proxy = RPC.getProxy(TestProtocol.class,
                TestProtocol.versionID, server.getListenerAddress(), conf);
        try {
            // this call will throw exception if server couldn't decode the ping
            proxy.sleep(pingInterval*4);
        } finally {
            if (proxy != null) RPC.stopProxy(proxy);
            server.stop();
        }
    }

    public static void main(String[] args) throws IOException,InterruptedException {
        TestRPC test = new TestRPC();
        test.setupConf();
//        test.testConfRpc();
        test.testCallsInternal(conf);
//        test.testSlowRpc();
//        test.testProxyAddress();
//        test.testConfRpc();
    }
}
