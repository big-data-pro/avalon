package com.immomo.datainfo.conf;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Created by kangkai on 18/2/1.
 */

/** Something that may be configured with a {@link Configuration}. */
@InterfaceAudience.Public
@InterfaceStability.Stable
public interface Configurable {

    /** Set the configuration to be used by this object. */
    void setConf(Configuration conf);

    /** Return the configuration used by this object. */
    Configuration getConf();
}