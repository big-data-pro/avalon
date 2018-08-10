package com.immomo.datainfo.rpc;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.protobuf.ServiceException;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import com.immomo.datainfo.rpc.protocol.TestProtos;
import com.immomo.datainfo.rpc.protocol.TestProtos.EmptyRequestProto;
import com.immomo.datainfo.rpc.protocol.TestProtos.EmptyResponseProto;

/**
 * Created by kangkai on 18/2/28.
 */

@InterfaceAudience.Private
@InterfaceStability.Stable
public class TestProtocolTranslatorPB implements
        ProtocolMetaInterface, TestProtocol, Closeable, ProtocolTranslator {
    private final static EmptyRequestProto VOID_PING_REQUEST = EmptyRequestProto.newBuilder().build();

    final private TestProtocolPB rpcProxy;
    public TestProtocolTranslatorPB(TestProtocolPB proxy) {
        rpcProxy = proxy;
    }

    @Override
    public void close() {
        RPC.stopProxy(rpcProxy);
    }

    @Override
    public boolean isMethodSupported(String methodName) throws IOException {
        return RpcClientUtil.isMethodSupported(rpcProxy,
                TestProtocolTranslatorPB.class, RPC.RpcKind.RPC_PROTOCOL_BUFFER,
                RPC.getProtocolVersion(TestProtocolTranslatorPB.class), methodName);
    }

    public Object getUnderlyingProxyObject() {
        return rpcProxy;
    }

    public void ping() throws IOException {
        try {
            rpcProxy.ping(null, VOID_PING_REQUEST);
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }

    public void slowPing(boolean shouldSlow) throws IOException {
        TestProtos.SlowPingRequestProto requestProto = TestProtos.SlowPingRequestProto.newBuilder().setShouldSlow(shouldSlow).build();
        try {
            rpcProxy.slowPing(null, requestProto);
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }

    public void sleep(long delay) throws IOException, InterruptedException {
        TestProtos.SleepRequestProto requestProto = TestProtos.SleepRequestProto.newBuilder().setDelay(delay).build();
        try {
            rpcProxy.sleep(null, requestProto);
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }

    public String echo(String value) throws IOException {
        TestProtos.EchoRequestProto requestProto = TestProtos.EchoRequestProto.newBuilder().setMessage(value).build();
        try {
            return (rpcProxy.echo(null, requestProto)).getMessage();
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }

    public int add(int v1, int v2) throws IOException {
        TestProtos.AddRequestProto requestProto = TestProtos.AddRequestProto.newBuilder().setV1(v1).setV2(v2).build();
        try {
            return rpcProxy.add(null, requestProto).getValue();
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }

    public int[] exchange(int[] values) throws IOException {
        java.util.List<Integer> value = new ArrayList<Integer>();
        for(int i=0;i<values.length;i++) {
            value.add(values[i]);
        }
        TestProtos.ExchangeRequestProto requestProto = TestProtos.ExchangeRequestProto.newBuilder().addAllValue(value).build();
        try {
            java.util.List<Integer> result = (rpcProxy.exchange(null, requestProto)).getValueList();
            int[] results = new int[result.size()];
            for(int i=0;i<result.size();i++) {
                results[i] = (result.get(i)).intValue();
            }
           return results;
        } catch (ServiceException e) {
            throw ProtobufHelper.getRemoteException(e);
        }
    }
}