package my.adam.smo.serviceimpl;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import my.adam.smo.POC;
import my.adam.smo.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
public class UsefullThing implements POC.NewUsefullService.Interface {

    @Autowired
    private Server server;

    @PostConstruct
    public void init(){
        server.register(POC.NewUsefullService.newReflectiveService(this));
    }

    @Override
    public void doGoodJob(RpcController controller, POC.In request, RpcCallback<POC.Out> done) {
        done.run(POC.Out.newBuilder().setResult(request.getOperand1() + request.getOperand2()).build());
    }
}
