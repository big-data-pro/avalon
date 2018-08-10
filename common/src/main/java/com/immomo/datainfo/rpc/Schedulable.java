package com.immomo.datainfo.rpc;

/**
 * Created by kangkai on 18/2/5.
 */
import com.immomo.datainfo.annotations.classification.InterfaceAudience;

/**
 * Interface which allows extracting information necessary to
 * create schedulable identity strings.
 */
@InterfaceAudience.Private
public interface Schedulable {
    public String getInfo();
}
