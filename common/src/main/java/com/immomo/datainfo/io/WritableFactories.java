package com.immomo.datainfo.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
import com.immomo.datainfo.util.ReflectionUtils;
import com.immomo.datainfo.conf.Configurable;
import com.immomo.datainfo.conf.Configuration;

/** Factories for non-public writables.  Defining a factory permits  to be able to construct instances of non-public classes. */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class WritableFactories {
    private static final Map<Class, WritableFactory> CLASS_TO_FACTORY =
            new ConcurrentHashMap<Class, WritableFactory>();

    private WritableFactories() {}                  // singleton

    /** Define a factory for a class. */
    public static void setFactory(Class c, WritableFactory factory) {
        CLASS_TO_FACTORY.put(c, factory);
    }

    /** Define a factory for a class. */
    public static WritableFactory getFactory(Class c) {
        return CLASS_TO_FACTORY.get(c);
    }

    /** Create a new instance of a class with a defined factory. */
    public static Writable newInstance(Class<? extends Writable> c, Configuration conf) {
        WritableFactory factory = WritableFactories.getFactory(c);
        if (factory != null) {
            Writable result = factory.newInstance();
            if (result instanceof Configurable) {
                ((Configurable) result).setConf(conf);
            }
            return result;
        } else {
            return ReflectionUtils.newInstance(c, conf);
        }
    }

    /** Create a new instance of a class with a defined factory. */
    public static Writable newInstance(Class<? extends Writable> c) {
        return newInstance(c, null);
    }

}

