package my.adam.smo.server;

import com.google.protobuf.*;
import my.adam.smo.RPCommunication;
import my.adam.smo.common.DummyRpcController;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.Executors;

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
@Component
public class SocketServer extends Server {
    @InjectLogger
    private Logger logger;

    @Inject
    public SocketServer(@Value("${server_worker_threads}") int workerCount) {
        bootstrap.setFactory(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(),
                workerCount));

        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline p = Channels.pipeline();
                if (enableTrafficLogging) {
                    p.addLast("logger", new LoggingHandler(InternalLogLevel.DEBUG));
                }

                p.addLast("frameEncoder", new LengthFieldPrepender(4));//DownstreamHandler
                p.addLast("protobufEncoder", new ProtobufEncoder());//DownstreamHandler

                p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_FRAME_BYTES_LENGTH, 0, 4, 0, 4));//UpstreamHandler
                p.addLast("protobufDecoder", new ProtobufDecoder(RPCommunication.Request.getDefaultInstance()));//UpstreamHandler
                p.addLast("handler", new SimpleChannelUpstreamHandler() {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
                        RPCommunication.Request request = (RPCommunication.Request) e.getMessage();
                        logger.trace("received request:" + request.toString());

                        if (enableAsymmetricEncryption) {
                            request = getAsymDecryptedRequest(request);
                            logger.trace("asymmetric encryption enabled, decrypted request: " + request.toString());
                        }

                        if (enableSymmetricEncryption) {
                            request = getDecryptedRequest(request);
                            logger.trace("symmetric encryption enabled, decrypted request: " + request.toString());
                        }

                        final RPCommunication.Request protoRequest = request;

                        RpcController dummyController = new DummyRpcController();
                        Service service = serviceMap.get(request.getServiceName());

                        logger.trace("got service: " + service + " for name " + request.getServiceName());

                        Descriptors.MethodDescriptor methodToCall = service
                                .getDescriptorForType()
                                .findMethodByName(request.getMethodName());

                        logger.trace("got method: " + methodToCall + " for name " + request.getMethodName());

                        Message methodArguments = service
                                .getRequestPrototype(methodToCall)
                                .newBuilderForType()
                                .mergeFrom(request.getMethodArgument())
                                .build();

                        logger.trace("get method arguments from request " + methodArguments.toString());

                        RpcCallback<Message> callback = new RpcCallback<Message>() {
                            @Override
                            public void run(Message parameter) {
                                RPCommunication.Response response = RPCommunication
                                        .Response
                                        .newBuilder()
                                        .setResponse(parameter.toByteString())
                                        .setRequestId(protoRequest.getRequestId())
                                        .build();

                                //encryption
                                if (enableSymmetricEncryption) {
                                    response = getEncryptedResponse(response);
                                    logger.trace("symmetric encryption enabled, encrypted response: " + response.toString());
                                }

                                if (enableAsymmetricEncryption) {
                                    response = getAsymEncryptedResponse(response);
                                    logger.trace("asymmetric encryption enabled, encrypted response: " + response.toString());
                                }

                                e.getChannel().write(response);
                                logger.debug("finishing call, response sent");
                            }
                        };
                        logger.debug("calling " + methodToCall.getFullName());
                        service.callMethod(methodToCall, dummyController, methodArguments, callback);
                    }
                });
                return p;
            }
        };
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
