package org.atricore.idbus.kernel.main.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: UUIDGenerator.java 1305 2009-06-18 14:19:47Z sgonzalez $
 */
public class UUIDGenerator extends AbstractIdGenerator {

    private static final Log logger = LogFactory.getLog(UUIDGenerator.class);

    private int artifactLength = 8;

    /**
     * Generate and return an artifact
     */
    public synchronized String generateId() {
        byte random[] = new byte[16];
        StringBuffer result = new StringBuffer();
        int resultLenBytes = 0;
        while (resultLenBytes < artifactLength) {
            getRandomBytes(random);
            random = getDigest().digest(random);
            for (int j = 0; j < random.length && resultLenBytes < artifactLength; j++) {
                byte b1 = (byte) ((random[j] & 0xf0) >> 4);
                byte b2 = (byte) (random[j] & 0x0f);
                if (b1 < 10) result.append((char) ('0' + b1)); else result.append((char) ('A' + (b1 - 10)));
                if (b2 < 10) result.append((char) ('0' + b2)); else result.append((char) ('A' + (b2 - 10)));
                resultLenBytes++;
            }
        }
        return ("id" + result.toString());
    }
}
