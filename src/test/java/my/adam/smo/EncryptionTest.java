package my.adam.smo;

import com.google.protobuf.ByteString;
import junit.framework.Assert;
import my.adam.smo.common.SymmetricEncryptionBox;
import my.adam.smo.server.HTTPServer;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

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
public class EncryptionTest {
    private Random random = new Random();

    @Test
    public void symetricEncryptionShortMessage() {
        ApplicationContext clientContext = new ClassPathXmlApplicationContext("Context.xml");
        SymmetricEncryptionBox box = clientContext.getBean(SymmetricEncryptionBox.class);
        int plainTextLength = 3;

        byte[] plainText = new byte[plainTextLength];
        random.nextBytes(plainText);

        byte[] cryptogram = box.encrypt(plainText);
        Assert.assertFalse("plain text leaked!!!", Arrays.equals(plainText, Arrays.copyOfRange(cryptogram, SymmetricEncryptionBox.ivLength, cryptogram.length)));

        byte[] decrypted = box.decrypt(cryptogram);
        Assert.assertTrue("unable to decrypt", Arrays.equals(plainText, decrypted));
    }

    @Test
    public void symetricEncryptionLongMessage() {
        ApplicationContext clientContext = new ClassPathXmlApplicationContext("Context.xml");
        SymmetricEncryptionBox box = clientContext.getBean(SymmetricEncryptionBox.class);
        int plainTextLength = 17;

        byte[] plainText = new byte[plainTextLength];
        random.nextBytes(plainText);

        byte[] cryptogram = box.encrypt(plainText);
        Assert.assertFalse("plain text leaked!!!", Arrays.equals(plainText, Arrays.copyOfRange(cryptogram, SymmetricEncryptionBox.ivLength, cryptogram.length)));

        byte[] decrypted = box.decrypt(cryptogram);
        Assert.assertTrue("unable to decrypt", Arrays.equals(plainText, decrypted));
    }

    @Test
    public void symmetricEncryptionGaveDifferentCryptogramForSamePlainText() {
        ApplicationContext clientContext = new ClassPathXmlApplicationContext("Context.xml");
        SymmetricEncryptionBox box = clientContext.getBean(SymmetricEncryptionBox.class);
        int plainTextLength = 17;
        Random random = new Random();

        byte[] plainText = new byte[plainTextLength];
        random.nextBytes(plainText);

        byte[] cryptogram1 = box.encrypt(plainText);
        byte[] cryptogram2 = box.encrypt(plainText);

        Assert.assertFalse("cryptograms are same", Arrays.equals(cryptogram1, cryptogram2));

        byte[] decrypted1 = box.decrypt(cryptogram1);
        Assert.assertTrue("unable to decrypt", Arrays.equals(plainText, decrypted1));

        byte[] decrypted2 = box.decrypt(cryptogram2);
        Assert.assertTrue("unable to decrypt", Arrays.equals(plainText, decrypted2));
    }

    @Test
    public void asymmetricRequestEncryptionTest() {
        ApplicationContext clientContext = new ClassPathXmlApplicationContext("Context.xml");
        HTTPServer s = clientContext.getBean(HTTPServer.class);

        SecureRandom secureRandom = new SecureRandom();

        RPCommunication.Response response = RPCommunication.Response
                .newBuilder()
                .setException("")
                .setRequestId(1l)
                .setResponse(ByteString.copyFrom(ServerCorrectnessTest.getMegaBytes(10)))
                .build();

        Assert.assertEquals(response.getResponse(), s.getAsymDecryptedResponse(s.getAsymEncryptedResponse(response)).getResponse());
        Assert.assertEquals(response.getResponse(), s.getDecryptedResponse(s.getEncryptedResponse(response)).getResponse());

        RPCommunication.Request request = RPCommunication.Request
                .newBuilder()
                .setMethodArgument(ByteString.copyFrom(ServerCorrectnessTest.getMegaBytes(10)))
                .setMethodName("ala")
                .setRequestId(1l)
                .setServiceName("ola")
                .build();

        Assert.assertEquals(request.getMethodArgument(), s.getAsymDecryptedRequest(s.getAsymEncryptedRequest(request)).getMethodArgument());
        Assert.assertEquals(request.getMethodArgument(), s.getDecryptedRequest(s.getEncryptedRequest(request)).getMethodArgument());
    }
}
