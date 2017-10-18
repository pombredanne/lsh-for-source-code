package org.archive.modules.extractor;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.archive.io.ReplayCharSequence;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.util.TextUtils;

/**
 * A processor for calculating custum HTTP content digests in place of the 
 * default (if any) computed by the HTTP fetcher processors.
 * <p>
 * This processor allows the user to specify a regular expression called 
 * <i>strip-reg-expr<i>. Any segment of a document (text only, binary files will
 * be skipped) that matches this regular expression will by rewritten with 
 * the blank character (character 32 in the ANSI character set) <b> for the 
 * purpose of the digest</b> this has no effect on the document for subsequent 
 * processing or archiving.
 * <p>
 * NOTE: Content digest only accounts for the document body, not headers.
 * <p>
 * The operator will also be able to specify a maximum length for documents 
 * being evaluated by this processors. Documents exceeding that length will be 
 * ignored.
 * <p>
 * To further discriminate by file type or URL, an operator should use the 
 * override and refinement options. 
 * <p>
 * It is generally recommended that this recalculation only be performed when 
 * absolutely needed (because of stripping data that changes automatically each 
 * time the URL is fetched) as this is an expensive operation.
 *
 * @author Kristinn Sigurdsson
 */
public class HTTPContentDigest extends Processor {

    private static final long serialVersionUID = 3L;

    private static Logger logger = Logger.getLogger(HTTPContentDigest.class.getName());

    /**
     * A regular expression that matches those portions of downloaded documents
     * that need to be ignored when calculating the content digest. Segments
     * matching this expression will be rewritten with the blank character for
     * the content digest.
     */
    public static final Key<Pattern> STRIP_REG_EXPR = Key.makeNull(Pattern.class);

    /** Maximum file size for - longer files will be ignored. -1 = unlimited*/
    public static final Key<Long> MAX_SIZE_BYTES = Key.make(1048576L);

    private static final String SHA1 = "SHA1";

    /**
     * Constructor.
     */
    public HTTPContentDigest() {
    }

    protected boolean shouldProcess(ProcessorURI uri) {
        if (!uri.getContentType().startsWith("text")) {
            return false;
        }
        long maxSize = uri.get(this, MAX_SIZE_BYTES);
        if ((maxSize > -1) && (maxSize < uri.getContentSize())) {
            return false;
        }
        return true;
    }

    protected void innerProcess(ProcessorURI curi) throws InterruptedException {
        Pattern regexpr = curi.get(this, STRIP_REG_EXPR);
        ReplayCharSequence cs = null;
        try {
            cs = curi.getRecorder().getReplayCharSequence();
        } catch (Exception e) {
            curi.getNonFatalFailures().add(e);
            logger.warning("Failed get of replay char sequence " + curi.toString() + " " + e.getMessage() + " " + Thread.currentThread().getName());
            return;
        }
        MessageDigest digest = null;
        try {
            try {
                digest = MessageDigest.getInstance(SHA1);
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
                return;
            }
            digest.reset();
            String s = null;
            if (regexpr != null) {
                s = cs.toString();
            } else {
                Matcher m = regexpr.matcher(cs);
                s = m.replaceAll(" ");
            }
            digest.update(s.getBytes());
            byte[] newDigestValue = digest.digest();
            curi.setContentDigest(SHA1, newDigestValue);
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    logger.warning(TextUtils.exceptionToString("Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
    }

    static {
        KeyManager.addKeys(HTTPContentDigest.class);
    }
}
