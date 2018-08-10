package com.immomo.datainfo.rpc;

import com.immomo.datainfo.conf.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;

import static org.junit.Assert.fail;

/**
 * Created by kangkai on 18/3/2.
 */
public class TestClient {
    public static void main(String[] args) {
        final String ADDRESS = "0.0.0.0";
        Configuration conf  = new Configuration();
        conf.addResource("avalon-default.xml");
        try {
            TestProtocolPB proxyPB = RPC.getProxy(
                    TestProtocolPB.class, TestProtocol.versionID,new InetSocketAddress(ADDRESS, 8020), conf);
//            TestProtocolPB translatorProxy = RPC.getProtocolProxy(TestProtocolPB.class, TestProtocol.versionID, addr, conf,NetUtils.getDefaultSocketFactory(conf),60000,null).getProxy();
            TestProtocol proxy = new TestProtocolTranslatorPB(proxyPB);
            TestProtocol proxy2 = RPC.waitForProxy(TestProtocol.class,
                    TestProtocol.versionID, new InetSocketAddress(ADDRESS,8022), conf, 15000L);
            System.out.println(proxy.echo("test client"));
            System.out.println(proxy.add(1,45));
            //fail("We should not have reached here");
        } catch (ConnectException ioe) {
            //this is what we expected
        } catch (IOException e) {

        }
    }
}
