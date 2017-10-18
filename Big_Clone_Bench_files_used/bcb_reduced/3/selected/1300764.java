package frost.transferlayer;

import java.io.*;
import java.util.logging.*;
import org.w3c.dom.*;
import frost.*;
import frost.crypt.*;
import frost.fcp.*;
import frost.identities.*;
import frost.messages.*;
import frost.util.*;

public class MessageDownloader {

    private static final Logger logger = Logger.getLogger(MessageDownloader.class.getName());

    /**
     * Process the downloaded file, decrypt, check sign.
     * @param tmpFile  downloaded file
     * @param results  the FcpResults
     * @param logInfo  info for log output
     * @return  null if unexpected Exception occurred, or results indicating state or error
     */
    protected static MessageDownloaderResult processDownloadedFile(final File tmpFile, final FcpResultGet results, final String logInfo) {
        try {
            if (FcpHandler.isFreenet05()) {
                return processDownloadedFile05(tmpFile, results, logInfo);
            } else if (FcpHandler.isFreenet07()) {
                return processDownloadedFile07(tmpFile, results, logInfo);
            } else {
                logger.severe("Unsupported freenet version, not 0.5 or 0.7");
                return null;
            }
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "Error processing downloaded message", t);
            final MessageDownloaderResult mdResult = new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
            return mdResult;
        }
    }

    /**
     * Tries to download the message, performs all base checkings and decryption.
     *
     * @return  null if not found, or MessageDownloaderResult if success or error
     */
    public static MessageDownloaderResult downloadMessage(final String downKey, final int targetIndex, final int maxRetries, final boolean fastDownload, final String logInfo) {
        FcpResultGet results;
        final File tmpFile = FileAccess.createTempFile("dlMsg_", "-" + targetIndex + ".xml.tmp");
        try {
            results = FcpHandler.inst().getFile(FcpHandler.TYPE_MESSAGE, downKey, null, tmpFile, false, fastDownload, FcpHandler.MAX_MESSAGE_SIZE_07, maxRetries);
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "TOFDN: Exception thrown in downloadDate part 1." + logInfo, t);
            tmpFile.delete();
            return null;
        }
        if (results == null || results.isSuccess() == false) {
            tmpFile.delete();
            if (results != null && results.getReturnCode() == 28) {
                logger.warning("TOFDN: All data not found." + logInfo);
                System.out.println("TOFDN: Contents of message key partially missing.");
                return new MessageDownloaderResult(MessageDownloaderResult.ALLDATANOTFOUND);
            } else if (results != null && results.getReturnCode() == 21) {
                logger.severe("TOFDN: Message file too big." + logInfo);
                System.out.println("TOFDN: Message file too big.");
                return new MessageDownloaderResult(MessageDownloaderResult.MSG_TOO_BIG);
            } else {
                return null;
            }
        }
        return processDownloadedFile(tmpFile, results, logInfo);
    }

    /**
     * Process the downloaded file, decrypt, check sign.
     * @param tmpFile  downloaded file
     * @param results  the FcpResults
     * @param logInfo  info for log output
     * @return  null if unexpected Exception occurred, or results indicating state or error
     */
    protected static MessageDownloaderResult processDownloadedFile05(final File tmpFile, final FcpResultGet results, final String logInfo) {
        try {
            logger.info("TOFDN: A message was downloaded." + logInfo);
            final byte[] metadata = results.getRawMetadata();
            if (tmpFile.length() == 0) {
                if (metadata != null && metadata.length > 0) {
                    logger.severe("TOFDN: Received metadata without data, maybe faked message." + logInfo);
                } else if (metadata == null || metadata.length == 0) {
                    logger.severe("TOFDN: Received neither metadata nor data, maybe a bug or a faked message." + logInfo);
                } else {
                    logger.severe("TOFDN: Received something, but bad things happened in code, maybe a bug or a faked message." + logInfo);
                }
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
            }
            if (metadata == null) {
                final byte[] unzippedXml = FileAccess.readZipFileBinary(tmpFile);
                if (unzippedXml == null) {
                    logger.log(Level.SEVERE, "TOFDN: Unzip of unsigned xml failed." + logInfo);
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
                }
                FileAccess.writeFile(unzippedXml, tmpFile);
                try {
                    final MessageXmlFile currentMsg = new MessageXmlFile(tmpFile);
                    if (currentMsg.getFromName().indexOf('@') > -1) {
                        logger.severe("TOFDN: unsigned message has an invalid fromName (contains an @: '" + currentMsg.getFromName() + "'), message dropped." + logInfo);
                        tmpFile.delete();
                        return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
                    }
                    currentMsg.setSignatureStatusOLD();
                    return new MessageDownloaderResult(currentMsg);
                } catch (final Exception ex) {
                    logger.log(Level.SEVERE, "TOFDN: Unsigned message is invalid." + logInfo, ex);
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
                }
            }
            MetaData _metaData = null;
            try {
                final Document doc = XMLTools.parseXmlContent(metadata, false);
                if (doc != null) {
                    _metaData = MetaData.getInstance(doc.getDocumentElement());
                }
            } catch (final Throwable t) {
                logger.log(Level.SEVERE, "TOFDN: Invalid metadata of signed message" + logInfo, t);
                _metaData = null;
            }
            if (_metaData == null) {
                logger.log(Level.SEVERE, "TOFDN: Metadata couldn't be read. " + "Offending file saved as badmetadata.xml - send to a dev for analysis." + logInfo);
                final File badmetadata = new File("badmetadata.xml");
                FileAccess.writeFile(metadata, badmetadata);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_METADATA);
            }
            if (_metaData.getType() != MetaData.SIGN && _metaData.getType() != MetaData.ENCRYPT) {
                logger.severe("TOFDN: Unknown type of metadata." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_METADATA);
            }
            final SignMetaData metaData = (SignMetaData) _metaData;
            final Identity owner = metaData.getPerson();
            if (!Core.getIdentities().isNewIdentityValid(owner)) {
                logger.severe("TOFDN: identity failed verification, message dropped." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
            }
            final byte[] plaintext = FileAccess.readByteArray(tmpFile);
            boolean sigIsValid = Core.getCrypto().detachedVerify(plaintext, owner.getPublicKey(), metaData.getSig());
            if (_metaData.getType() == MetaData.ENCRYPT) {
                final EncryptMetaData encMetaData = (EncryptMetaData) metaData;
                if (!Core.getIdentities().isMySelf(encMetaData.getRecipient())) {
                    logger.fine("TOFDN: Encrypted message was not for me.");
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.MSG_NOT_FOR_ME);
                }
                final LocalIdentity receiverId = Core.getIdentities().getLocalIdentity(encMetaData.getRecipient());
                final byte[] cipherText = FileAccess.readByteArray(tmpFile);
                final byte[] zipData = Core.getCrypto().decrypt(cipherText, receiverId.getPrivateKey());
                if (zipData == null) {
                    logger.severe("TOFDN: Encrypted message from " + encMetaData.getPerson().getUniqueName() + " could not be decrypted!" + logInfo);
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.DECRYPT_FAILED);
                }
                tmpFile.delete();
                FileAccess.writeFile(zipData, tmpFile);
                logger.fine("TOFDN: Decrypted an encrypted message for me, sender was " + encMetaData.getPerson().getUniqueName() + "." + logInfo);
            }
            final byte[] unzippedXml = FileAccess.readZipFileBinary(tmpFile);
            if (unzippedXml == null) {
                logger.severe("TOFDN: Unzip of signed xml failed." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
            }
            FileAccess.writeFile(unzippedXml, tmpFile);
            MessageXmlFile currentMsg = null;
            try {
                currentMsg = new MessageXmlFile(tmpFile);
            } catch (final Exception ex) {
                logger.log(Level.SEVERE, "TOFDN: Exception when creating message object" + logInfo, ex);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
            }
            if (!sigIsValid) {
                logger.severe("TOFDN: message failed verification, message dropped." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
            }
            final String metaDataHash = Mixed.makeFilename(Core.getCrypto().digest(metaData.getPerson().getPublicKey()));
            final String messageHash = Mixed.makeFilename(currentMsg.getFromName().substring(currentMsg.getFromName().indexOf("@") + 1, currentMsg.getFromName().length()));
            if (!metaDataHash.equals(messageHash)) {
                logger.severe("TOFDN: Hash in metadata doesn't match hash in message!\n" + "metadata : " + metaDataHash + " , message: " + messageHash + ". Message failed verification and was dropped." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
            }
            currentMsg.setSignatureStatusVERIFIED_V2();
            return new MessageDownloaderResult(currentMsg, owner);
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "TOFDN: Exception thrown in downloadDate part 2." + logInfo, t);
        }
        tmpFile.delete();
        return null;
    }

    /**
     * Process the downloaded file, decrypt, check sign.
     * @param tmpFile  downloaded file
     * @param results  the FcpResults
     * @param logInfo  info for log output
     * @return  null if unexpected Exception occurred, or results indicating state or error
     */
    protected static MessageDownloaderResult processDownloadedFile07(final File tmpFile, final FcpResultGet results, final String logInfo) {
        try {
            final MessageXmlFile currentMsg;
            try {
                currentMsg = new MessageXmlFile(tmpFile);
            } catch (final MessageCreationException ex) {
                final String errorMessage;
                if (ex.getMessageNo() == MessageCreationException.MSG_NOT_FOR_ME) {
                    logger.warning("Info: Encrypted message is not for me. " + logInfo);
                    errorMessage = MessageDownloaderResult.MSG_NOT_FOR_ME;
                } else if (ex.getMessageNo() == MessageCreationException.DECRYPT_FAILED) {
                    logger.log(Level.WARNING, "TOFDN: Exception catched." + logInfo, ex);
                    errorMessage = MessageDownloaderResult.DECRYPT_FAILED;
                } else if (ex.getMessageNo() == MessageCreationException.INVALID_FORMAT) {
                    logger.warning("Error: Message validation failed. " + logInfo);
                    errorMessage = MessageDownloaderResult.INVALID_MSG;
                } else {
                    logger.log(Level.WARNING, "TOFDN: Exception catched." + logInfo, ex);
                    errorMessage = MessageDownloaderResult.BROKEN_MSG;
                }
                tmpFile.delete();
                return new MessageDownloaderResult(errorMessage);
            } catch (final Throwable ex) {
                logger.log(Level.SEVERE, "TOFDN: Exception catched." + logInfo, ex);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.BROKEN_MSG);
            }
            boolean isSignedV1 = false;
            boolean isSignedV2 = false;
            if (currentMsg.getSignatureV1() != null && currentMsg.getSignatureV1().length() > 0) {
                isSignedV1 = true;
            }
            if (currentMsg.getSignatureV2() != null && currentMsg.getSignatureV2().length() > 0) {
                isSignedV2 = true;
            }
            if (!isSignedV1 && !isSignedV2) {
                if (currentMsg.getFromName().indexOf('@') > -1) {
                    logger.severe("TOFDN: unsigned message has an invalid fromName (contains an @: '" + currentMsg.getFromName() + "'), message dropped." + logInfo);
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
                }
                currentMsg.setSignatureStatusOLD();
                return new MessageDownloaderResult(currentMsg);
            } else if (isSignedV1 && !isSignedV2) {
                final boolean acceptV1 = Core.frostSettings.getBoolValue(SettingsClass.ACCEPT_SIGNATURE_FORMAT_V1);
                if (!acceptV1) {
                    logger.severe("TOFDN: message has only V1 signature which is not accepted, message dropped." + logInfo);
                    tmpFile.delete();
                    return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
                }
            }
            final Identity owner = Identity.createIdentityFromExactStrings(currentMsg.getFromName(), currentMsg.getPublicKey());
            if (!Core.getIdentities().isNewIdentityValid(owner)) {
                logger.severe("TOFDN: identity failed verification, message dropped." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
            }
            final boolean sigIsValid;
            if (isSignedV2) {
                sigIsValid = currentMsg.verifyMessageSignatureV2(owner.getPublicKey());
                logger.info("TOFDN: verification of V2 signature: " + sigIsValid + "." + logInfo);
            } else {
                sigIsValid = currentMsg.verifyMessageSignatureV1(owner.getPublicKey());
                logger.info("TOFDN: verification of V1 signature: " + sigIsValid + "." + logInfo);
            }
            if (!sigIsValid) {
                logger.severe("TOFDN: message failed verification, message dropped." + logInfo);
                tmpFile.delete();
                return new MessageDownloaderResult(MessageDownloaderResult.INVALID_MSG);
            }
            if (isSignedV2) {
                currentMsg.setSignatureStatusVERIFIED_V2();
            } else {
                currentMsg.setSignatureStatusVERIFIED_V1();
            }
            return new MessageDownloaderResult(currentMsg, owner);
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "TOFDN: Exception catched." + logInfo, t);
        }
        tmpFile.delete();
        return null;
    }
}
