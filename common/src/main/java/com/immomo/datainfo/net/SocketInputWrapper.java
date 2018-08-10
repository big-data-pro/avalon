package com.immomo.datainfo.net;

import com.google.common.base.Preconditions;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ReadableByteChannel;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Created by kangkai on 18/2/5.
 */

/**
 * A wrapper stream around a socket which allows setting of its timeout. If the
 * socket has a channel, this uses non-blocking IO via the package-private
 * {@link SocketInputStream} implementation. Otherwise, timeouts are managed by
 * setting the underlying socket timeout itself.
 */
@InterfaceAudience.LimitedPrivate("AVALON")
@InterfaceStability.Unstable
public class SocketInputWrapper extends FilterInputStream {
    private final Socket socket;
    private final boolean hasChannel;

    SocketInputWrapper(Socket s, InputStream is) {
        super(is);
        this.socket = s;
        this.hasChannel = s.getChannel() != null;
        if (hasChannel) {
            Preconditions.checkArgument(is instanceof SocketInputStream,
                    "Expected a SocketInputStream when there is a channel. " +
                            "Got: %s", is);
        }
    }

    /**
     * Set the timeout for reads from this stream.
     *
     * Note: the behavior here can differ subtly depending on whether the
     * underlying socket has an associated Channel. In particular, if there is no
     * channel, then this call will affect the socket timeout for <em>all</em>
     * readers of this socket. If there is a channel, then this call will affect
     * the timeout only for <em>this</em> stream. As such, it is recommended to
     * only create one {@link SocketInputWrapper} instance per socket.
     *
     * @param timeoutMs
     *          the new timeout, 0 for no timeout
     * @throws SocketException
     *           if the timeout cannot be set
     */
    public void setTimeout(long timeoutMs) throws SocketException {
        if (hasChannel) {
            ((SocketInputStream)in).setTimeout(timeoutMs);
        } else {
            socket.setSoTimeout((int)timeoutMs);
        }
    }

    /**
     * @return an underlying ReadableByteChannel implementation.
     * @throws IllegalStateException if this socket does not have a channel
     */
    public ReadableByteChannel getReadableByteChannel() {
        Preconditions.checkState(hasChannel,
                "Socket %s does not have a channel",
                this.socket);
        return (SocketInputStream)in;
    }
}