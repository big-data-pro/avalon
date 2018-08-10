package com.immomo.datainfo.rpc;

import com.immomo.datainfo.rpc.protobuf.ProtocolInfoProtos.ProtocolInfoService;
/**
 * Created by kangkai on 18/2/2.
 */



/**
 * Protocol to get versions and signatures for supported protocols from the
 * server.
 *
 * Note: This extends the protocolbuffer service based interface to
 * add annotations.
 */
@ProtocolInfo(
        protocolName = "com.immomo.datainfo.rpc.ProtocolMetaInfoPB",
        protocolVersion = 1)
public interface ProtocolMetaInfoPB extends
        ProtocolInfoService.BlockingInterface {
}
