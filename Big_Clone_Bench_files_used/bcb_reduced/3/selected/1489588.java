package com.itextpdf.text.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ExceptionConverter;

/**
 * PdfSmartCopy has the same functionality as PdfCopy,
 * but when resources (such as fonts, images,...) are
 * encountered, a reference to these resources is saved
 * in a cache, so that they can be reused.
 * This requires more memory, but reduces the file size
 * of the resulting PDF document.
 */
public class PdfSmartCopy extends PdfCopy {

    /** the cache with the streams and references. */
    private HashMap<ByteStore, PdfIndirectReference> streamMap = null;

    /** Creates a PdfSmartCopy instance. */
    public PdfSmartCopy(Document document, OutputStream os) throws DocumentException {
        super(document, os);
        this.streamMap = new HashMap<ByteStore, PdfIndirectReference>();
    }

    /**
     * Translate a PRIndirectReference to a PdfIndirectReference
     * In addition, translates the object numbers, and copies the
     * referenced object to the output file if it wasn't available
     * in the cache yet. If it's in the cache, the reference to
     * the already used stream is returned.
     *
     * NB: PRIndirectReferences (and PRIndirectObjects) really need to know what
     * file they came from, because each file has its own namespace. The translation
     * we do from their namespace to ours is *at best* heuristic, and guaranteed to
     * fail under some circumstances.
     */
    @Override
    protected PdfIndirectReference copyIndirect(PRIndirectReference in) throws IOException, BadPdfFormatException {
        PdfObject srcObj = PdfReader.getPdfObjectRelease(in);
        ByteStore streamKey = null;
        boolean validStream = false;
        if (srcObj.isStream()) {
            streamKey = new ByteStore((PRStream) srcObj);
            validStream = true;
            PdfIndirectReference streamRef = streamMap.get(streamKey);
            if (streamRef != null) {
                return streamRef;
            }
        }
        PdfIndirectReference theRef;
        RefKey key = new RefKey(in);
        IndirectReferences iRef = indirects.get(key);
        if (iRef != null) {
            theRef = iRef.getRef();
            if (iRef.getCopied()) {
                return theRef;
            }
        } else {
            theRef = body.getPdfIndirectReference();
            iRef = new IndirectReferences(theRef);
            indirects.put(key, iRef);
        }
        if (srcObj.isDictionary()) {
            PdfObject type = PdfReader.getPdfObjectRelease(((PdfDictionary) srcObj).get(PdfName.TYPE));
            if (type != null && PdfName.PAGE.equals(type)) {
                return theRef;
            }
        }
        iRef.setCopied();
        if (validStream) {
            streamMap.put(streamKey, theRef);
        }
        PdfObject obj = copyObject(srcObj);
        addToBody(obj, theRef);
        return theRef;
    }

    static class ByteStore {

        private byte[] b;

        private int hash;

        private MessageDigest md5;

        private void serObject(PdfObject obj, int level, ByteBuffer bb) throws IOException {
            if (level <= 0) return;
            if (obj == null) {
                bb.append("$Lnull");
                return;
            }
            obj = PdfReader.getPdfObject(obj);
            if (obj.isStream()) {
                bb.append("$B");
                serDic((PdfDictionary) obj, level - 1, bb);
                if (level > 0) {
                    md5.reset();
                    bb.append(md5.digest(PdfReader.getStreamBytesRaw((PRStream) obj)));
                }
            } else if (obj.isDictionary()) {
                serDic((PdfDictionary) obj, level - 1, bb);
            } else if (obj.isArray()) {
                serArray((PdfArray) obj, level - 1, bb);
            } else if (obj.isString()) {
                bb.append("$S").append(obj.toString());
            } else if (obj.isName()) {
                bb.append("$N").append(obj.toString());
            } else bb.append("$L").append(obj.toString());
        }

        private void serDic(PdfDictionary dic, int level, ByteBuffer bb) throws IOException {
            bb.append("$D");
            if (level <= 0) return;
            Object[] keys = dic.getKeys().toArray();
            Arrays.sort(keys);
            for (int k = 0; k < keys.length; ++k) {
                serObject((PdfObject) keys[k], level, bb);
                serObject(dic.get((PdfName) keys[k]), level, bb);
            }
        }

        private void serArray(PdfArray array, int level, ByteBuffer bb) throws IOException {
            bb.append("$A");
            if (level <= 0) return;
            for (int k = 0; k < array.size(); ++k) {
                serObject(array.getPdfObject(k), level, bb);
            }
        }

        ByteStore(PRStream str) throws IOException {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
            ByteBuffer bb = new ByteBuffer();
            int level = 100;
            serObject(str, level, bb);
            this.b = bb.toByteArray();
            md5 = null;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteStore)) return false;
            if (hashCode() != obj.hashCode()) return false;
            return Arrays.equals(b, ((ByteStore) obj).b);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                int len = b.length;
                for (int k = 0; k < len; ++k) {
                    hash = hash * 31 + (b[k] & 0xff);
                }
            }
            return hash;
        }
    }
}
