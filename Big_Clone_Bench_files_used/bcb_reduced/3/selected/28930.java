package org.eclipse.osgi.internal.signedcontent;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.util.NLS;

public class SignatureBlockProcessor implements SignedContentConstants {

    private final SignedBundleFile signedBundle;

    private ArrayList signerInfos = new ArrayList();

    private HashMap contentMDResults = new HashMap();

    private HashMap tsaSignerInfos;

    private final int supportFlags;

    public SignatureBlockProcessor(SignedBundleFile signedContent, int supportFlags) {
        this.signedBundle = signedContent;
        this.supportFlags = supportFlags;
    }

    public SignedContentImpl process() throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        BundleFile wrappedBundleFile = signedBundle.getWrappedBundleFile();
        BundleEntry be = wrappedBundleFile.getEntry(META_INF_MANIFEST_MF);
        if (be == null) return createUnsignedContent();
        Enumeration en = wrappedBundleFile.getEntryPaths(META_INF);
        List signers = new ArrayList(2);
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            if ((name.endsWith(DOT_DSA) || name.endsWith(DOT_RSA)) && name.indexOf('/') == name.lastIndexOf('/')) signers.add(name);
        }
        if (signers.size() == 0) return createUnsignedContent();
        byte manifestBytes[] = readIntoArray(be);
        Iterator iSigners = signers.iterator();
        for (int i = 0; iSigners.hasNext(); i++) processSigner(wrappedBundleFile, manifestBytes, (String) iSigners.next());
        SignerInfo[] allSigners = (SignerInfo[]) signerInfos.toArray(new SignerInfo[signerInfos.size()]);
        for (Iterator iResults = contentMDResults.entrySet().iterator(); iResults.hasNext(); ) {
            Entry entry = (Entry) iResults.next();
            ArrayList[] value = (ArrayList[]) entry.getValue();
            SignerInfo[] entrySigners = (SignerInfo[]) value[0].toArray(new SignerInfo[value[0].size()]);
            byte[][] entryResults = (byte[][]) value[1].toArray(new byte[value[1].size()][]);
            entry.setValue(new Object[] { entrySigners, entryResults });
        }
        SignedContentImpl result = new SignedContentImpl(allSigners, (supportFlags & SignedBundleHook.VERIFY_RUNTIME) != 0 ? contentMDResults : null);
        result.setContent(signedBundle);
        result.setTSASignerInfos(tsaSignerInfos);
        return result;
    }

    private SignedContentImpl createUnsignedContent() {
        SignedContentImpl result = new SignedContentImpl(new SignerInfo[0], contentMDResults);
        result.setContent(signedBundle);
        return result;
    }

    private void processSigner(BundleFile bf, byte[] manifestBytes, String signer) throws IOException, SignatureException, InvalidKeyException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        BundleEntry be = bf.getEntry(signer);
        byte pkcs7Bytes[] = readIntoArray(be);
        int dotIndex = signer.lastIndexOf('.');
        be = bf.getEntry(signer.substring(0, dotIndex) + DOT_SF);
        byte sfBytes[] = readIntoArray(be);
        String baseFile = bf.getBaseFile() != null ? bf.getBaseFile().toString() : null;
        PKCS7Processor processor = new PKCS7Processor(pkcs7Bytes, 0, pkcs7Bytes.length, signer, baseFile);
        processor.verifySFSignature(sfBytes, 0, sfBytes.length);
        String digAlg = getDigAlgFromSF(sfBytes);
        if (digAlg == null) throw new SignatureException(NLS.bind(SignedContentMessages.SF_File_Parsing_Error, new String[] { bf.toString() }));
        verifyManifestAndSignatureFile(manifestBytes, sfBytes);
        SignerInfoImpl signerInfo = new SignerInfoImpl(processor.getCertificates(), null, digAlg);
        if ((supportFlags & SignedBundleHook.VERIFY_RUNTIME) != 0) populateMDResults(manifestBytes, signerInfo);
        signerInfos.add(signerInfo);
        Certificate[] tsaCerts = processor.getTSACertificates();
        Date signingTime = processor.getSigningTime();
        if (tsaCerts != null && signingTime != null) {
            SignerInfoImpl tsaSignerInfo = new SignerInfoImpl(tsaCerts, null, digAlg);
            if (tsaSignerInfos == null) tsaSignerInfos = new HashMap(2);
            tsaSignerInfos.put(signerInfo, new Object[] { tsaSignerInfo, signingTime });
        }
    }

    /**
	 * Verify the digest listed in each entry in the .SF file with corresponding section in the manifest
	 * @throws SignatureException 
	 */
    private void verifyManifestAndSignatureFile(byte[] manifestBytes, byte[] sfBytes) throws SignatureException {
        String sf = new String(sfBytes);
        sf = stripContinuations(sf);
        int off = sf.indexOf(digestManifestSearch);
        if (off != -1) {
            int start = sf.lastIndexOf('\n', off);
            String manifestDigest = null;
            if (start != -1) {
                String digestName = sf.substring(start + 1, off);
                if (digestName.equalsIgnoreCase(MD5_STR)) manifestDigest = calculateDigest(getMessageDigest(MD5_STR), manifestBytes); else if (digestName.equalsIgnoreCase(SHA1_STR)) manifestDigest = calculateDigest(getMessageDigest(SHA1_STR), manifestBytes);
                off += digestManifestSearchLen;
                int nIndex = sf.indexOf('\n', off);
                String digestValue = sf.substring(off, nIndex - 1);
                if (!digestValue.equals(manifestDigest)) {
                    SignatureException se = new SignatureException(NLS.bind(SignedContentMessages.Security_File_Is_Tampered, new String[] { signedBundle.getBaseFile().toString() }));
                    SignedBundleHook.log(se.getMessage(), FrameworkLogEntry.ERROR, se);
                    throw se;
                }
            }
        }
    }

    private void populateMDResults(byte mfBuf[], SignerInfo signerInfo) throws NoSuchAlgorithmException {
        String mfStr = new String(mfBuf);
        int entryStartOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME);
        int length = mfStr.length();
        while ((entryStartOffset != -1) && (entryStartOffset < length)) {
            int entryEndOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME, entryStartOffset + 1);
            if (entryEndOffset == -1) {
                entryEndOffset = mfStr.length();
            }
            String entryStr = mfStr.substring(entryStartOffset + 1, entryEndOffset);
            entryStr = stripContinuations(entryStr);
            String entryName = getEntryFileName(entryStr);
            if (entryName != null) {
                String aDigestLine = getDigestLine(entryStr, signerInfo.getMessageDigestAlgorithm());
                if (aDigestLine != null) {
                    String msgDigestAlgorithm = getDigestAlgorithmFromString(aDigestLine);
                    if (!msgDigestAlgorithm.equalsIgnoreCase(signerInfo.getMessageDigestAlgorithm())) continue;
                    byte digestResult[] = getDigestResultsList(aDigestLine);
                    ArrayList[] mdResult = (ArrayList[]) contentMDResults.get(entryName);
                    if (mdResult == null) {
                        mdResult = new ArrayList[2];
                        mdResult[0] = new ArrayList();
                        mdResult[1] = new ArrayList();
                        contentMDResults.put(entryName, mdResult);
                    }
                    mdResult[0].add(signerInfo);
                    mdResult[1].add(digestResult);
                }
            }
            entryStartOffset = entryEndOffset;
        }
    }

    private static byte[] getDigestResultsList(String digestLines) {
        byte resultsList[] = null;
        if (digestLines != null) {
            String sDigestLine = digestLines;
            int indexDigest = sDigestLine.indexOf(MF_DIGEST_PART);
            indexDigest += MF_DIGEST_PART.length();
            if (indexDigest >= sDigestLine.length()) {
                resultsList = null;
            }
            String sResult = sDigestLine.substring(indexDigest);
            try {
                resultsList = Base64.decode(sResult.getBytes());
            } catch (Throwable t) {
                resultsList = null;
            }
        }
        return resultsList;
    }

    private static String getDigestAlgorithmFromString(String digestLines) throws NoSuchAlgorithmException {
        if (digestLines != null) {
            int indexDigest = digestLines.indexOf(MF_DIGEST_PART);
            String sDigestAlgType = digestLines.substring(0, indexDigest);
            if (sDigestAlgType.equalsIgnoreCase(MD5_STR)) {
                return MD5_STR;
            } else if (sDigestAlgType.equalsIgnoreCase(SHA1_STR)) {
                return SHA1_STR;
            } else {
                throw new NoSuchAlgorithmException(NLS.bind(SignedContentMessages.Algorithm_Not_Supported, sDigestAlgType));
            }
        }
        return null;
    }

    private static String getEntryFileName(String manifestEntry) {
        int nameStart = manifestEntry.indexOf(MF_ENTRY_NAME);
        if (nameStart == -1) {
            return null;
        }
        int nameEnd = manifestEntry.indexOf('\n', nameStart);
        if (nameEnd == -1) {
            return null;
        }
        if (manifestEntry.charAt(nameEnd - 1) == '\r') {
            nameEnd--;
        }
        nameStart += MF_ENTRY_NAME.length();
        if (nameStart >= nameEnd) {
            return null;
        }
        return manifestEntry.substring(nameStart, nameEnd);
    }

    /**
	 * Returns the Base64 encoded digest of the passed set of bytes.
	 */
    private static String calculateDigest(MessageDigest digest, byte[] bytes) {
        return new String(Base64.encode(digest.digest(bytes)));
    }

    static synchronized MessageDigest getMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
        }
        return null;
    }

    /**
	 * Read the .SF file abd assuming that same digest algorithm will be used through out the whole 
	 * .SF file.  That digest algorithm name in the last entry will be returned. 
	 * 
	 * @param SFBuf			a .SF file in bytes 
	 * @return				the digest algorithm name used in the .SF file
	 */
    private static String getDigAlgFromSF(byte SFBuf[]) {
        String mfStr = new String(SFBuf);
        String entryStr = null;
        int entryStartOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME);
        int length = mfStr.length();
        while ((entryStartOffset != -1) && (entryStartOffset < length)) {
            int entryEndOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME, entryStartOffset + 1);
            if (entryEndOffset == -1) {
                entryEndOffset = mfStr.length();
            }
            entryStr = mfStr.substring(entryStartOffset + 1, entryEndOffset);
            entryStr = stripContinuations(entryStr);
            break;
        }
        if (entryStr != null) {
            String digestLine = getDigestLine(entryStr, null);
            return getMessageDigestName(digestLine);
        }
        return null;
    }

    /**
	 * 
	 * @param manifestEntry contains a single MF file entry of the format
	 * 				   "Name: foo"
	 * 				   "MD5-Digest: [base64 encoded MD5 digest data]"
	 * 				   "SHA1-Digest: [base64 encoded SHA1 digest dat]"
	 * 
	 * @param	desireDigestAlg	a string representing the desire digest value to be returned if there are
	 * 							multiple digest lines.
	 * 							If this value is null, return whatever digest value is in the entry.
	 * 
	 * @return this function returns a digest line based on the desire digest algorithm value
	 * 		   (since only MD5 and SHA1 are recognized here),
	 * 		   or a 'null' will be returned if none of the digest algorithms
	 * 		   were recognized.
	 */
    private static String getDigestLine(String manifestEntry, String desireDigestAlg) {
        String result = null;
        int indexDigest = manifestEntry.indexOf(MF_DIGEST_PART);
        if (indexDigest == -1) return null;
        while (indexDigest != -1) {
            int indexStart = manifestEntry.lastIndexOf('\n', indexDigest);
            if (indexStart == -1) return null;
            int indexEnd = manifestEntry.indexOf('\n', indexDigest);
            if (indexEnd == -1) return null;
            int indexEndToUse = indexEnd;
            if (manifestEntry.charAt(indexEndToUse - 1) == '\r') indexEndToUse--;
            int indexStartToUse = indexStart + 1;
            if (indexStartToUse >= indexEndToUse) return null;
            String digestLine = manifestEntry.substring(indexStartToUse, indexEndToUse);
            String digAlg = getMessageDigestName(digestLine);
            if (desireDigestAlg != null) {
                if (desireDigestAlg.equalsIgnoreCase(digAlg)) return digestLine;
            }
            result = digestLine;
            indexDigest = manifestEntry.indexOf(MF_DIGEST_PART, indexEnd);
        }
        return result;
    }

    /**
	 * Return the Message Digest name
	 * 
	 * @param digLine		the message digest line is in the following format.  That is in the 
	 * 						following format:
	 * 								DIGEST_NAME-digest: digest value
	 * @return				a string representing a message digest.
	 */
    private static String getMessageDigestName(String digLine) {
        String rtvValue = null;
        if (digLine != null) {
            int indexDigest = digLine.indexOf(MF_DIGEST_PART);
            if (indexDigest != -1) {
                rtvValue = digLine.substring(0, indexDigest);
            }
        }
        return rtvValue;
    }

    private static String stripContinuations(String entry) {
        if (entry.indexOf("\n ") < 0) return entry;
        StringBuffer buffer = new StringBuffer(entry.length());
        int cont = entry.indexOf("\n ");
        int start = 0;
        while (cont >= 0) {
            buffer.append(entry.substring(start, cont - 1));
            start = cont + 2;
            cont = cont + 2 < entry.length() ? entry.indexOf("\n ", cont + 2) : -1;
        }
        if (start < entry.length()) buffer.append(entry.substring(start));
        return buffer.toString();
    }

    private static byte[] readIntoArray(BundleEntry be) throws IOException {
        int size = (int) be.getSize();
        InputStream is = be.getInputStream();
        byte b[] = new byte[size];
        int rc = readFully(is, b);
        if (rc != size) {
            throw new IOException("Couldn't read all of " + be.getName() + ": " + rc + " != " + size);
        }
        return b;
    }

    private static int readFully(InputStream is, byte b[]) throws IOException {
        int count = b.length;
        int offset = 0;
        int rc;
        while ((rc = is.read(b, offset, count)) > 0) {
            count -= rc;
            offset += rc;
        }
        return offset;
    }
}
