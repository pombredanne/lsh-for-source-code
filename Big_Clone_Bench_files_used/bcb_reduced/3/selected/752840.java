package net.infordata.ifw2.web.view;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  
 * @author valentino.proietti
 */
public class ContentPart implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPart.class);

    public static final String UID_SEPARATOR = "_";

    private static final char[] EMPTY_CHARS = new char[0];

    private static final ContentPart[] EMPTY_CHILDS = new ContentPart[0];

    private static final char[] BEGTAG = "<!--IFW!@#&-->".toCharArray();

    private static final char[] ENDTAG = "<!--&#@!IFW-->".toCharArray();

    private static final char[][] TAGS = new char[][] { BEGTAG, ENDTAG };

    private static final int TAGLEN;

    /** If buffer lenght is greater than SWITCH2DIGEST then use digest to discover if
   *  content changes. */
    private static final int SWITCH2DIGEST = 128;

    private int ivDigestLength = -1;

    private byte[] ivDigest;

    private byte[] ivDigestContent;

    private boolean ivChanged;

    private char[] ivChangedContent;

    private final String ivId;

    private String ivUniqueId;

    private LinkedHashMap<String, ContentPart> ivChilds;

    private ContentPart ivParent;

    private String ivClientScriptAccessCode;

    private boolean ivEnabled;

    private final boolean ivInvalidateAll;

    private int ivTouchId;

    private boolean ivFreezed;

    static {
        int tlen = Integer.MIN_VALUE;
        for (int i = 0; i < TAGS.length; i++) {
            if (tlen < 0) tlen = TAGS[i].length; else if (tlen != TAGS[i].length) throw new IllegalStateException("Tag " + i + " len: " + TAGS[i].length + " while expecting: " + tlen);
        }
        if (tlen <= 0) throw new IllegalStateException();
        TAGLEN = tlen;
    }

    /**
   * @param id
   */
    public ContentPart(String id) {
        this(id, false);
    }

    /**
   * @param id
   * @param invalidateAll - when setted if one of its childs changes then the entire part
   *     is considered changed
   */
    public ContentPart(String id, boolean invalidateAll) {
        if (id == null) throw new NullPointerException("null id");
        if (id.contains(UID_SEPARATOR)) throw new IllegalArgumentException("ContentPart id cannot contain " + UID_SEPARATOR);
        if (Character.isDigit(id.charAt(0))) throw new IllegalArgumentException("ContentPart id cannot start with " + id.charAt(0));
        ivId = id;
        ivUniqueId = id;
        ivInvalidateAll = invalidateAll;
    }

    public final String getClientScriptAccessCode() {
        return ivClientScriptAccessCode;
    }

    public final boolean isChainEnabled() {
        return ivEnabled;
    }

    final void setClientScriptAccessCode(String clientScriptAccessCode, boolean enabled) {
        ivClientScriptAccessCode = clientScriptAccessCode;
        ivEnabled = enabled;
    }

    public final char[] getContent() {
        return ivChangedContent;
    }

    public final void releaseContent() {
        if (!ivFreezed) {
            ivChangedContent = null;
            releaseChildContent();
        }
    }

    private final void releaseChildContent() {
        if (ivChilds != null) {
            for (Iterator<ContentPart> it = ivChilds.values().iterator(); it.hasNext(); ) {
                ContentPart cp = it.next();
                if (cp.isRemoved()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("removing " + cp.getUniqueId());
                    }
                    it.remove();
                    cp.ivDigest = null;
                    cp.ivChangedContent = null;
                } else {
                    cp.releaseContent();
                }
            }
        }
    }

    /**
   * @return an array with the top-most changed parts.
   */
    public ContentPart[] getTopMostChangedChilds() {
        if (isRemoved() || isChanged()) return new ContentPart[] { this };
        if (ivFreezed) return EMPTY_CHILDS;
        if (ivChilds == null) return EMPTY_CHILDS;
        List<ContentPart> res = null;
        for (ContentPart cp : ivChilds.values()) {
            ContentPart[] changed = cp.getTopMostChangedChilds();
            for (ContentPart part : changed) {
                if (res == null) res = new ArrayList<ContentPart>();
                res.add(part);
            }
        }
        return (res == null) ? EMPTY_CHILDS : res.toArray(new ContentPart[res.size()]);
    }

    public final boolean isChanged() {
        return ivChanged;
    }

    public final boolean isNew() {
        return ivDigest == null;
    }

    public final boolean isRemoved() {
        return ivParent != null && ivParent.ivTouchId != ivTouchId;
    }

    public final boolean isFreezed() {
        return ivFreezed;
    }

    public final String getId() {
        return ivId;
    }

    public final String getUniqueId() {
        return ivUniqueId;
    }

    public final ContentPart getParent() {
        return ivParent;
    }

    public final ContentPart getAncestor(String id) {
        ContentPart parent = getParent();
        while (parent != null && !id.equals(parent.getId())) parent = parent.getParent();
        return parent;
    }

    public final ContentPart[] getChilds() {
        return ivChilds == null ? EMPTY_CHILDS : (ContentPart[]) ivChilds.values().toArray(new ContentPart[ivChilds.values().size()]);
    }

    public final ContentPart getChild(String id) {
        return ivChilds == null ? null : ivChilds.get(id);
    }

    private ContentPart getSubChild(final int level, final String id, int[] best) {
        ContentPart res = null;
        ContentPart xxx = getChild(id);
        if (xxx != null && level < best[0]) {
            best[0] = level;
            res = xxx;
        } else if (level + 1 < best[0] && ivChilds != null) {
            for (ContentPart ch : ivChilds.values()) {
                xxx = ch.getSubChild(level + 1, id, best);
                if (xxx != null) res = xxx;
            }
        }
        return res;
    }

    public ContentPart getSubChild(String id) {
        return getSubChild(0, id, new int[] { Integer.MAX_VALUE });
    }

    /**
   * Next time {@link ContentPart#isChanged()} is true.
   */
    public void cleanUp() {
        releaseContent();
        ivDigestLength = 0;
        ivDigest = new byte[0];
    }

    void registerChild(ContentPart child) {
        if (child == null) throw new IllegalArgumentException();
        if (child.getParent() != null) throw new IllegalArgumentException("Child has already a parent.");
        if (ivChilds == null) ivChilds = new LinkedHashMap<String, ContentPart>();
        if (ivChilds.containsKey(child.getId())) throw new IllegalArgumentException("A child exists with id: " + child.getId());
        if (ivChilds.containsValue(child)) throw new IllegalArgumentException("Child already registered.");
        child.ivParent = this;
        child.ivUniqueId = ivUniqueId + UID_SEPARATOR + child.ivId;
        ivChilds.put(child.getId(), child);
    }

    private final void beginTag(String tag, String tagAttrs, Writer writer, RendererContext context) throws IOException {
        writer.write("<" + tag + " id='" + context.idToExtId(getUniqueId()) + "'" + (tagAttrs == null || tagAttrs.length() <= 0 ? " class='IFW2'" : " " + tagAttrs) + ">");
        writer.write(BEGTAG);
        if (!"span".equalsIgnoreCase(tag)) writer.write('\n');
    }

    private final void endTag(String tag, String tagAttrs, Writer writer, RendererContext context) throws IOException {
        writer.write(ENDTAG);
        writer.write("</" + tag + ">");
    }

    /**
   * Used instead of {@link #wrapContent(String, String, Writer, char[], RendererContext)}
   * to freeze the {@link ContentPart} which means that its inner content is not 
   * generated and it is not flagged as changed. 
   * @param tag - tag used to wrap html-code (usually span or div)
   * @param tagAttrs - extra tag attributes
   * @param writer
   * @param context -
   * @throws IOException
   */
    void freezeContent(String tag, String tagAttrs, Writer writer, RendererContext context) throws IOException {
        if (ivDigest == null) throw new IllegalStateException("Cannnot freeze a never generated content " + ivUniqueId);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("freezingContent: " + ivUniqueId);
        ivFreezed = true;
        ivTouchId = context.getId();
        ivChanged = false;
        beginTag(tag, tagAttrs, writer, context);
        endTag(tag, tagAttrs, writer, context);
    }

    /**
   * @param tag - tag used to wrap html-code (usually span or div)
   * @param tagAttrs - extra tag attributes
   * @param writer
   * @param content - cannot be changed by the caller if keepIfChanged.
   * @param context -
   * @return true - if content is changed from the previous request 
   * @throws IOException 
   */
    boolean wrapContent(String tag, String tagAttrs, Writer writer, char[] content, RendererContext context) throws IOException {
        ivTouchId = context.getId();
        ivFreezed = false;
        if (content == null) content = EMPTY_CHARS;
        beginTag(tag, tagAttrs, writer, context);
        int oldDigestLenght = ivDigestLength;
        byte[] oldDigest = ivDigest;
        {
            int wrtContentMark = 0;
            int nested = 0;
            int digestSize = 0;
            byte[] bdigest = new byte[content.length * 2];
            for (int i = 0; i < content.length; i++) {
                char[] theTag = checkSpecialTag(content, i);
                if (theTag != null) {
                    if (LOGGER.isTraceEnabled()) {
                    } else {
                        writer.write(content, wrtContentMark, i - wrtContentMark);
                        wrtContentMark = i + TAGLEN;
                    }
                    i += TAGLEN - 1;
                }
                if (!ivInvalidateAll && BEGTAG == theTag) {
                    ++nested;
                    continue;
                }
                if (!ivInvalidateAll && ENDTAG == theTag) {
                    --nested;
                    continue;
                }
                if (nested > 0) continue;
                short sh = (short) content[i];
                bdigest[digestSize++] = (byte) (sh & 0x00FF);
                bdigest[digestSize++] = (byte) ((sh >> 8) & 0x00FF);
            }
            ivDigestLength = digestSize;
            if (LOGGER.isTraceEnabled() || digestSize <= SWITCH2DIGEST) {
                if (digestSize != bdigest.length) {
                    ivDigest = new byte[digestSize];
                    System.arraycopy(bdigest, 0, ivDigest, 0, digestSize);
                } else {
                    ivDigest = bdigest;
                }
                ivDigestContent = ivDigest;
            } else {
                ivDigestContent = null;
                ivDigest = context.digest(bdigest, 0, digestSize);
            }
            if (wrtContentMark < content.length) writer.write(content, wrtContentMark, content.length - wrtContentMark);
        }
        endTag(tag, tagAttrs, writer, context);
        ivChanged = !(oldDigest != null && oldDigestLenght == ivDigestLength && MessageDigest.isEqual(oldDigest, ivDigest));
        if ((context.getKeepChangedContentIndicator() && ivChanged)) {
            releaseChildContent();
            ivChangedContent = content;
        } else ivChangedContent = null;
        if (LOGGER.isDebugEnabled() && ivChangedContent != null) {
            if (!LOGGER.isTraceEnabled() && ivDigestContent == null) {
                LOGGER.debug("changedContent:" + ivUniqueId + " invalidateAll:" + ivInvalidateAll);
            } else {
                byte[] oDigest = (oldDigest == null) ? new byte[0] : oldDigest;
                LOGGER.trace("changedContent:" + ivUniqueId + " invalidateAll:" + ivInvalidateAll + " old-lenght:" + oDigest.length + " new-lenght:" + ivDigest.length);
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw);
                out.println();
                for (int i = 0; i < Math.min(ivDigest.length, oDigest.length); i += 2) {
                    char nc = (char) (ivDigest[i] | (ivDigest[i + 1] << 8));
                    char oc = (char) (oDigest[i] | (oDigest[i + 1] << 8));
                    if (nc != oc) {
                        StringBuilder nb = new StringBuilder();
                        StringBuilder ob = new StringBuilder();
                        for (int j = i; j < Math.min(i + 20, Math.min(ivDigest.length, oDigest.length)); j += 2) {
                            nb.append((char) (ivDigest[j] | (ivDigest[j + 1] << 8)));
                            ob.append((char) (oDigest[j] | (oDigest[j + 1] << 8)));
                        }
                        out.println("<<--DIFF:[old=" + ob + "][new=" + nb + "]");
                        break;
                    } else out.print(nc);
                }
                LOGGER.trace(sw.getBuffer().toString());
            }
        }
        return ivChanged;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("uid=").append(ivUniqueId).append(",").append("changed=").append(ivChanged).append(",").append("digest=").append(Arrays.toString(ivDigest)).append(",").append("childs=[").append('\n');
        if (ivChilds != null) {
            for (ContentPart cp : ivChilds.values()) {
                sb.append(cp.toString()).append('\n');
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private static final char[] checkSpecialTag(char[] buf, int from) {
        char[][] tags = new char[TAGS.length][];
        System.arraycopy(TAGS, 0, tags, 0, TAGS.length);
        int tagslen = tags.length;
        for (int i = 0; (i + from) < buf.length && i < TAGLEN; i++) {
            int tagsidx = -1;
            for (int j = 0; j < tagslen; j++) {
                if (buf[i + from] == tags[j][i]) {
                    tags[++tagsidx] = tags[j];
                }
            }
            tagslen = tagsidx + 1;
            if (tagslen <= 0) break;
        }
        return tagslen <= 0 ? null : tags[0];
    }

    /**
   * A shadow content is a piece of a content part which is served to the client
   * but it is not included in the digest used to detect changes ie. it can change
   * but doesn't cause the entire {@link ContentPart} to be served and it is served
   * to the client only if other pieces changes or a parent part changes.
   * @param content - html code
   * @return the wrapped content.
   */
    public static final String wrapShadowContent(String content) {
        if (content == null || content.length() <= 0) return "";
        StringBuilder sb = new StringBuilder(content.length() + 20);
        sb.append(BEGTAG);
        sb.append(content);
        sb.append(ENDTAG);
        return sb.toString();
    }
}
