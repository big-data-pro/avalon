package com.immomo.datainfo.rpc;

import com.google.protobuf.DescriptorProtos;
import com.immomo.datainfo.io.Writable;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import java.io.IOException;

/**
 * Created by kangkai on 18/2/28.
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving

public interface TestProtocol  {
    public static final long versionID = 1L;
    void ping() throws IOException;
    void slowPing(boolean shouldSlow) throws IOException;
    void sleep(long delay) throws IOException, InterruptedException;
    String echo(String value) throws IOException;
    int add(int v1, int v2) throws IOException;
    int[] exchange(int[] values) throws IOException;
}
