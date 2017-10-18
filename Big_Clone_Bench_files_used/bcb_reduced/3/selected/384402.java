package org.atricore.idbus.kernel.main.session.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.session.SessionIdGenerator;
import org.atricore.idbus.kernel.main.util.AbstractIdGenerator;

/**
 * This is an implementation of a session id generatod based on Jakarta Tomcat 5.0
 * session id generation.
 * This implementation is thread safe.
 *
 * @author <a href="mailto:sgonzalez@josso.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: SessionIdGeneratorImpl.java 1183 2009-05-05 20:48:01Z sgonzalez $
 *
 * @org.apache.xbean.XBean element="id-generator"
 */
public class SessionIdGeneratorImpl extends AbstractIdGenerator implements SessionIdGenerator {

    private static final Log logger = LogFactory.getLog(SessionIdGeneratorImpl.class);

    private int _sessionIdLength = 16;

    /**
     * Generate and return a new session identifier.
     */
    public synchronized String generateId() {
        byte random[] = new byte[16];
        StringBuffer result = new StringBuffer();
        int resultLenBytes = 0;
        while (resultLenBytes < _sessionIdLength) {
            getRandomBytes(random);
            random = getDigest().digest(random);
            for (int j = 0; j < random.length && resultLenBytes < _sessionIdLength; j++) {
                byte b1 = (byte) ((random[j] & 0xf0) >> 4);
                byte b2 = (byte) (random[j] & 0x0f);
                if (b1 < 10) result.append((char) ('0' + b1)); else result.append((char) ('A' + (b1 - 10)));
                if (b2 < 10) result.append((char) ('0' + b2)); else result.append((char) ('A' + (b2 - 10)));
                resultLenBytes++;
            }
        }
        return (result.toString());
    }
}
