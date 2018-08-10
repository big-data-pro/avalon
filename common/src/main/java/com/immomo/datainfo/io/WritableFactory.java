package com.immomo.datainfo.io;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/** A factory for a class of Writable.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public interface WritableFactory {
    /** Return a new instance. */
    Writable newInstance();
}