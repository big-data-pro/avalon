package com.immomo.datainfo.rpc;

import java.io.IOException;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
/**
 * Created by kangkai on 18/2/2.
 */

/**
 * Thrown by a remote server when it is up, but is not the active server in a
 * set of servers in which only a subset may be active.
 */
@InterfaceStability.Evolving
public class StandbyException extends IOException {
    static final long serialVersionUID = 0x12308AD010L;
    public StandbyException(String msg) {
        super(msg);
    }
}
