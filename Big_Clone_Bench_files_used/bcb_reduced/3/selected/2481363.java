package de.fhg.igd.semoa.security;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.TreeSet;
import de.fhg.igd.logging.LogLevel;
import de.fhg.igd.logging.Logger;
import de.fhg.igd.logging.LoggerFactory;
import de.fhg.igd.semoa.server.AgentCard;
import de.fhg.igd.semoa.server.AgentContext;
import de.fhg.igd.semoa.server.AgentFilter;
import de.fhg.igd.semoa.server.ErrorCode;
import de.fhg.igd.semoa.server.FieldType;
import de.fhg.igd.semoa.server.People;
import de.fhg.igd.semoa.service.AbstractFilter;
import de.fhg.igd.util.Resource;
import de.fhg.igd.util.Resources;

/**
 * This security filter computes the hash code of the zipped agent
 * resource. This hash code is stored as part of the <code>AgentContext</code>
 * (see <code>FieldType.ZIPPED_RESOURCE_HASH</code>).
 * 
 * The computed hash code corresponds to the ZIP archive, generated and
 * stored by the <code>StoreFilter</code> filter.
 * 
 * Further, this hash code is used to certify an agent's reception against
 * the sending agent server through the <code>ReceiptFilter</code>.
 *
 * @author Jan Peters
 * 
 * @see {@link de.fhg.igd.net.ReceiptServer}
 * @see {@link de.fhg.igd.security.ReceiptFilter}
 * @see {@link de.fhg.igd.server.StoreFilter}
 * 
 * @version "$Id: DigestFilter.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class DigestFilter extends AbstractFilter implements AgentFilter.In, AgentFilter.Out {

    /**
     * The <code>Logger</code> instance for this class
     */
    private static Logger log_ = LoggerFactory.getLogger("semoa/core");

    /**
     * The message digest algorithm to hash the agent resource.
     */
    public static final String MESSAGE_DIGEST = "SHA1";

    /**
     * The dependencies to other objects in the global <code>
     * Environment</code>.
     */
    private static final String[] DEPEND_ = {};

    /**
     * Default constructor.
     */
    public DigestFilter() {
    }

    public String info() {
        return "This filter computes the hash code of the zipped agent resource.";
    }

    public String author() {
        return People.JPETERS;
    }

    public String revision() {
        return "$Revision: 1913 $";
    }

    public String[] dependencies() {
        return (String[]) DEPEND_.clone();
    }

    /**
	 * Computes the hash code of the zipped agent resource
     * and stores it as part of the <code>AgentContext</code>.
	 * 
     * @param ctx The agent context of the agent that is to be filtered.
	 * @return <code>Error.OK</code>.
	 */
    public ErrorCode filter(AgentContext ctx) {
        ByteArrayOutputStream bos;
        MessageDigest md;
        AgentCard card;
        Resource resource;
        byte[] hash;
        try {
            card = (AgentCard) ctx.get(FieldType.CARD);
            if (card == null) {
                throw new NullPointerException("card");
            }
            resource = (Resource) ctx.get(FieldType.RESOURCE);
            if (resource == null) {
                throw new NullPointerException("resource");
            }
            bos = new ByteArrayOutputStream();
            Resources.zip(resource, new LinkedList(new TreeSet(resource.list())), bos);
            md = MessageDigest.getInstance(MESSAGE_DIGEST);
            hash = md.digest(bos.toByteArray());
            ctx.set(FieldType.ZIPPED_RESOURCE_HASH, hash);
            log_.info("Hash code of zipped agent resource for agent '" + card + "': " + toHexString(hash));
        } catch (Exception e) {
            log_.caught(LogLevel.ERROR, "Could not compute hash code of the zipped agent resource", e);
        }
        return ErrorCode.OK;
    }

    public static String toHexString(byte[] buf) {
        StringBuffer strbuf;
        String str;
        int hex;
        int i;
        strbuf = new StringBuffer();
        for (i = 0; i < buf.length; i++) {
            hex = ((int) buf[i]) & 0xff;
            str = Integer.toHexString(hex);
            if (str.length() == 1) {
                strbuf.append("0");
            }
            strbuf.append(str);
        }
        return strbuf.toString();
    }
}
