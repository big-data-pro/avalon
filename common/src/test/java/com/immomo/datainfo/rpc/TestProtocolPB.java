package com.immomo.datainfo.rpc;
import com.immomo.datainfo.rpc.protocol.TestProtos.TestProtocolService;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
/**
 * Created by kangkai on 18/2/27.
 */
@InterfaceAudience.Private
@InterfaceStability.Stable
@ProtocolInfo(protocolName = "com.immomo.datainfo.rpc.protocol.TestProtocol",
        protocolVersion = 1)
public interface TestProtocolPB extends TestProtocolService.BlockingInterface{
}
