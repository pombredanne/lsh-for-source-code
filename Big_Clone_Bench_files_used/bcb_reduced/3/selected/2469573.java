package org.brdevils.bf2sas.server;

import java.math.BigInteger;
import java.nio.Buffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.mina.core.buffer.AbstractIoBuffer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelnetProtocolHandler extends IoHandlerAdapter {

    final Logger logger = LoggerFactory.getLogger(TelnetProtocolHandler.class);

    private ReadListener reader;

    private String senha;

    public TelnetProtocolHandler(ReadListener reader, String senha) {
        this.reader = reader;
        this.senha = senha;
    }

    private String md5(String senha, String hashStr) {
        String sen = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest((hashStr + senha).getBytes()));
        sen = hash.toString(16);
        return sen;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        logger.info("message received: " + message);
        String msg = (String) message;
        if (msg.contains("### Digest seed:")) {
            String hash = msg.substring(17);
            session.write("login " + md5(senha, hash));
        }
        reader.onRead(msg);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        logger.info("message sent: " + message);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        logger.info("session idle: " + status);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        logger.info("session opened");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        logger.info("session closed");
    }
}
