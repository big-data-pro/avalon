package com.immomo.datainfo.rpc;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import com.immomo.datainfo.rpc.protocol.TestProtos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kangkai on 18/2/28.
 */
@InterfaceAudience.Private
@InterfaceStability.Stable
public class TestProtocolServerSideTranslatorPB implements TestProtocolPB {

/**
 * This class is used on the server side. Calls come across the wire for the
 * for protocol {@link TestProtocolTranslatorPB}.
 * This class translates the PB data types
 * to the native data types used inside the NN as specified in the generic
 * ClientProtocol.
 */
    final private TestProtos.EmptyResponseProto VOID_PING_RESPONSE = TestProtos.EmptyResponseProto.newBuilder().build();
    final private TestProtos.SlowPingResponseProto VOID_SLOWPING_RESPONSE = TestProtos.SlowPingResponseProto.newBuilder().build();
    final private TestProtos.SleepResponseProto VOID_SLEEP_RESPONSE = TestProtos.SleepResponseProto.newBuilder().build();
    final private TestProtocol server;

    public TestProtocolServerSideTranslatorPB (TestProtocol server) throws IOException {
        this.server = server;
    }

    public TestProtos.EmptyResponseProto ping(RpcController controller, TestProtos.EmptyRequestProto request) throws ServiceException {
        try {
            server.ping();
            return VOID_PING_RESPONSE;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    public TestProtos.SlowPingResponseProto slowPing(RpcController controller, TestProtos.SlowPingRequestProto request) throws ServiceException {

        try {
            server.slowPing(request.getShouldSlow());
            return VOID_SLOWPING_RESPONSE;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    public TestProtos.SleepResponseProto sleep(RpcController controller, TestProtos.SleepRequestProto request) throws ServiceException {

        try {
            server.sleep(request.getDelay());
            return VOID_SLEEP_RESPONSE;
        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }

    public TestProtos.EchoResponseProto echo(RpcController controller, TestProtos.EchoRequestProto request) throws ServiceException {
        try {
            String result = server.echo(request.getMessage());
            TestProtos.EchoResponseProto responseProto = TestProtos.EchoResponseProto.newBuilder().setMessage(result).build();
            return responseProto;
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public TestProtos.AddResponseProto add(RpcController controller, TestProtos.AddRequestProto request) throws ServiceException {
        try {
            int result = server.add(request.getV1(), request.getV2());
            TestProtos.AddResponseProto responseProto = TestProtos.AddResponseProto.newBuilder().setValue(result).build();
            return responseProto;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    public TestProtos.ExchangeResponseProto exchange(RpcController controller, TestProtos.ExchangeRequestProto request) throws ServiceException {
        try {
            java.util.List<Integer> value = request.getValueList();
            int[] values = new int[value.size()];
            for(int i=0;i<value.size();i++) {
                values[i] = value.get(i).intValue();
            }
            int[] result = server.exchange(values);
            ArrayList<Integer> results = new ArrayList<Integer>();
            for(int i=0;i<result.length;i++) {
                results.add(result[i]);
            }
            TestProtos.ExchangeResponseProto responseProto = TestProtos.ExchangeResponseProto.newBuilder().addAllValue(results).build();
            return responseProto;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }
}