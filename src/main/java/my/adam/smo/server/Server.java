package my.adam.smo.server;

import com.google.protobuf.Service;
import my.adam.smo.common.AbstractCommunicator;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The MIT License
 * <p/>
 * Copyright (c) 2013 Adam Smolarek
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public abstract class Server extends AbstractCommunicator {
    protected final ServerBootstrap bootstrap = new ServerBootstrap();
    protected ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<String, Service>();
    protected static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

    public void start(SocketAddress sa) {
        try {
            bootstrap.bind(sa);
        } catch (ChannelException e) {
            getLogger().error("error while starting server ", e);
        }
    }

    public void stop() {
        bootstrap.releaseExternalResources();
    }

    public void register(Service service) {
        serviceMap.put(service.getDescriptorForType().getFullName(), service);
    }

    public abstract Logger getLogger();
}
