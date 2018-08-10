package com.immomo.datainfo.avalon;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;
/**
 * Created by kangkai on 18/2/1.
 */

/**
 * Indicates that a method has been passed illegal or invalid argument.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class AvalonIllegalArgumentException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs exception with the specified detail message.
     * @param message detailed message.
     */
    public AvalonIllegalArgumentException(final String message) {
        super(message);
    }
}
