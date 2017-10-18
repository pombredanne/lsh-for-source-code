package org.atricore.idbus.kernel.main.mediation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.util.AbstractIdGenerator;

/**
 * @org.apache.xbean.XBean element="artifact-generator"
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: ArtifactGeneratorImpl.java 1359 2009-07-19 16:57:57Z sgonzalez $
 */
public class ArtifactGeneratorImpl extends AbstractIdGenerator implements ArtifactGenerator {

    private static final Log logger = LogFactory.getLog(ArtifactGeneratorImpl.class);

    private int artifactLength = 8;

    private String node;

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
        if (node != null) return node + result.toString();
        return result.toString();
    }

    public Artifact generate() {
        return new ArtifactImpl(generateId());
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }
}
