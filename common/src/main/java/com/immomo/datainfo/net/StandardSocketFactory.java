package com.immomo.datainfo.net;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.immomo.datainfo.annotations.classification.InterfaceAudience;
import com.immomo.datainfo.annotations.classification.InterfaceStability;

/**
 * Created by kangkai on 18/2/5.
 */

/**
 * Specialized SocketFactory to create sockets with a SOCKS proxy
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class StandardSocketFactory extends SocketFactory {

    /**
     * Default empty constructor (for use with the reflection API).
     */
    public StandardSocketFactory() {
    }

    @Override
    public Socket createSocket() throws IOException {
    /*
     * NOTE: This returns an NIO socket so that it has an associated
     * SocketChannel. As of now, this unfortunately makes streams returned
     * by Socket.getInputStream() and Socket.getOutputStream() unusable
     * (because a blocking read on input stream blocks write on output stream
     * and vice versa).
     *
     * So users of these socket factories should use
     * NetUtils.getInputStream(socket) and
     * NetUtils.getOutputStream(socket) instead.
     *
     * A solution for hiding from this from user is to write a
     * 'FilterSocket' on the lines of FilterInputStream and extend it by
     * overriding getInputStream() and getOutputStream().
     */
        return SocketChannel.open().socket();
    }

    @Override
    public Socket createSocket(InetAddress addr, int port) throws IOException {

        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(addr, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress addr, int port,
                               InetAddress localHostAddr, int localPort) throws IOException {

        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHostAddr, localPort));
        socket.connect(new InetSocketAddress(addr, port));
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {

        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port,
                               InetAddress localHostAddr, int localPort) throws IOException,
            UnknownHostException {

        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHostAddr, localPort));
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return obj.getClass().equals(this.getClass());
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

}

