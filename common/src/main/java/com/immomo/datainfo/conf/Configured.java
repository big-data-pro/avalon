package com.immomo.datainfo.conf;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Created by kangkai on 18/2/1.
 */

/** Base class for things that may be configured with a {@link Configuration}. */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class Configured implements Configurable {

    private Configuration conf;

    /** Construct a Configured. */
    public Configured() {
        this(null);
    }

    /** Construct a Configured. */
    public Configured(Configuration conf) {
        setConf(conf);
    }

    // inherit javadoc
    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    // inherit javadoc
    @Override
    public Configuration getConf() {
        return conf;
    }

}
