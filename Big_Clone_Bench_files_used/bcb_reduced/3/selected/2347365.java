package org.josso.gateway.assertion.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.josso.util.id.AbstractIdGenerator;

/**
 *
 * @org.apache.xbean.XBean element="id-generator"
 *
 * Creates unique assertion identifiers based on random and hashing algorithms.
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id$
 */
public class AssertionIdGeneratorImpl extends AbstractIdGenerator implements AssertionIdGenerator {

    private static final Log logger = LogFactory.getLog(AssertionIdGeneratorImpl.class);

    private int assertionIdLength = 8;

    /**
     * Generate and return a new assertion identifier.
     */
    public synchronized String generateId() {
        byte random[] = new byte[16];
        StringBuffer result = new StringBuffer();
        int resultLenBytes = 0;
        while (resultLenBytes < assertionIdLength) {
            getRandomBytes(random);
            random = getDigest().digest(random);
            for (int j = 0; j < random.length && resultLenBytes < assertionIdLength; j++) {
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
