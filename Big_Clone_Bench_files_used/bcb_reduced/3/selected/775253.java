package frost.transferlayer;

import java.io.*;
import java.util.logging.*;
import frost.*;
import frost.crypt.*;
import frost.fcp.*;
import frost.fileTransfer.*;
import frost.gui.objects.*;
import frost.identities.*;
import frost.messages.*;

public class IndexFileDownloader {

    private static Logger logger = Logger.getLogger(IndexFileDownloader.class.getName());

    protected static IndexFileDownloaderResult processDownloadedFile(File target, FcpResultGet fcpresults, Board board) {
        if (FcpHandler.getInitializedVersion() == FcpHandler.FREENET_05) {
            return processDownloadedFile05(target, fcpresults, board);
        } else if (FcpHandler.getInitializedVersion() == FcpHandler.FREENET_07) {
            return processDownloadedFile07(target, fcpresults, board);
        } else {
            logger.severe("Unsupported freenet version: " + FcpHandler.getInitializedVersion());
            return null;
        }
    }

    /**
     * Returns null if no file found.
     */
    public static IndexFileDownloaderResult downloadIndexFile(String downKey, Board board) {
        try {
            File tmpFile = File.createTempFile("frost-index", ".tmp", new File(Core.frostSettings.getValue("temp.dir")));
            tmpFile.deleteOnExit();
            FcpResultGet fcpresults = FcpHandler.inst().getFile(downKey, null, tmpFile, Core.frostSettings.getIntValue("keyDownloadHtl"), false);
            if (fcpresults == null || tmpFile.length() == 0) {
                tmpFile.delete();
                return null;
            }
            return processDownloadedFile(tmpFile, fcpresults, board);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error in UpdateIdThread", t);
        }
        return null;
    }

    /**
     * This method checks if the digest of sharer matches the pubkey,
     * and adds the NEW identity to list of neutrals.
     * @param _sharer
     * @param _pubkey
     * @return
     */
    protected static Identity addNewSharer(String _sharer, String _pubkey) {
        String given_digest = _sharer.substring(_sharer.indexOf("@") + 1, _sharer.length()).trim();
        String calculatedDigest = Core.getCrypto().digest(_pubkey.trim()).trim();
        calculatedDigest = Mixed.makeFilename(calculatedDigest).trim();
        if (!Mixed.makeFilename(given_digest).equals(calculatedDigest)) {
            logger.warning("Warning: public key of sharer didn't match its digest:\n" + "given digest :'" + given_digest + "'\n" + "pubkey       :'" + _pubkey.trim() + "'\n" + "calc. digest :'" + calculatedDigest + "'");
            return null;
        }
        Identity sharer = new Identity(_sharer.substring(0, _sharer.indexOf("@")), _pubkey);
        sharer.setState(FrostIdentities.NEUTRAL);
        Core.getIdentities().addIdentity(sharer);
        return sharer;
    }

    protected static IndexFileDownloaderResult processDownloadedFile05(File target, FcpResultGet fcpresults, Board board) {
        try {
            IndexFileDownloaderResult ifdResult = new IndexFileDownloaderResult();
            String digest = Core.getCrypto().digest(target);
            if (Core.getMessageHashes().contains(digest)) {
                target.delete();
                ifdResult.errorMsg = IndexFileDownloaderResult.DUPLICATE_FILE;
                return ifdResult;
            } else {
                Core.getMessageHashes().add(digest);
            }
            byte[] unzippedXml = FileAccess.readZipFileBinary(target);
            if (unzippedXml == null) {
                logger.warning("Could not extract received zip file, skipping.");
                target.delete();
                ifdResult.errorMsg = IndexFileDownloaderResult.BROKEN_DATA;
                return ifdResult;
            }
            File unzippedTarget = new File(target.getPath() + "_unzipped");
            FileAccess.writeFile(unzippedXml, unzippedTarget);
            unzippedXml = null;
            FrostIndex receivedIndex = null;
            try {
                Index idx = Index.getInstance();
                synchronized (idx) {
                    receivedIndex = idx.readKeyFile(unzippedTarget);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not parse the index file: ", ex);
            }
            if (receivedIndex == null || receivedIndex.getFilesMap().size() == 0) {
                logger.log(Level.SEVERE, "Received index file invalid or empty, skipping.");
                target.delete();
                unzippedTarget.delete();
                ifdResult.errorMsg = IndexFileDownloaderResult.INVALID_DATA;
                return ifdResult;
            }
            Identity sharer = null;
            Identity sharerInFile = receivedIndex.getSharer();
            if (fcpresults.getRawMetadata() != null) {
                SignMetaData md;
                try {
                    md = new SignMetaData(fcpresults.getRawMetadata());
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Could not read the XML metadata, skipping file index.", t);
                    target.delete();
                    unzippedTarget.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.BROKEN_METADATA;
                    return ifdResult;
                }
                if (sharerInFile == null) {
                    logger.warning("MetaData present, but file didn't contain an identity :(");
                    target.delete();
                    unzippedTarget.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.BROKEN_METADATA;
                    return ifdResult;
                }
                String _owner = null;
                String _pubkey = null;
                if (md.getPerson() != null) {
                    _owner = Mixed.makeFilename(md.getPerson().getUniqueName());
                    _pubkey = md.getPerson().getKey();
                }
                if (_owner == null || _owner.length() == 0 || _pubkey == null || _pubkey.length() == 0) {
                    logger.warning("XML metadata have missing fields, skipping file index.");
                    target.delete();
                    unzippedTarget.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.INVALID_DATA;
                    return ifdResult;
                }
                if (!_owner.equals(Mixed.makeFilename(sharerInFile.getUniqueName())) || !_pubkey.equals(sharerInFile.getKey())) {
                    logger.warning("The identity in MetaData didn't match the identity in File! :(\n" + "file owner : " + sharerInFile.getUniqueName() + "\n" + "file key : " + sharerInFile.getKey() + "\n" + "meta owner: " + _owner + "\n" + "meta key : " + _pubkey);
                    target.delete();
                    unzippedTarget.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.TAMPERED_DATA;
                    return ifdResult;
                }
                byte[] zippedXml = FileAccess.readByteArray(target);
                boolean valid = Core.getCrypto().detachedVerify(zippedXml, _pubkey, md.getSig());
                zippedXml = null;
                if (valid == false) {
                    logger.warning("Invalid signature for index file from " + _owner);
                    target.delete();
                    unzippedTarget.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.TAMPERED_DATA;
                    return ifdResult;
                }
                if (Core.getIdentities().isMySelf(_owner)) {
                    logger.info("Received index file from myself");
                    sharer = Core.getIdentities().getMyId();
                } else {
                    logger.info("Received index file from " + _owner);
                    sharer = Core.getIdentities().getIdentity(_owner);
                    if (sharer == null) {
                        sharer = addNewSharer(_owner, _pubkey);
                        if (sharer == null) {
                            logger.info("sharer was null... :(");
                            target.delete();
                            unzippedTarget.delete();
                            ifdResult.errorMsg = IndexFileDownloaderResult.TAMPERED_DATA;
                            return ifdResult;
                        }
                    } else if (sharer.getState() == FrostIdentities.ENEMY) {
                        if (Core.frostSettings.getBoolValue("hideBadFiles")) {
                            logger.info("Skipped index file from BAD user " + _owner);
                            target.delete();
                            unzippedTarget.delete();
                            ifdResult.errorMsg = IndexFileDownloaderResult.BAD_USER;
                            return ifdResult;
                        }
                    }
                    sharer.updateLastSeenTimestamp();
                }
            } else if (Core.frostSettings.getBoolValue("hideAnonFiles")) {
                target.delete();
                unzippedTarget.delete();
                ifdResult.errorMsg = IndexFileDownloaderResult.ANONYMOUS_BLOCKED;
                return ifdResult;
            }
            String sharerStr;
            if (sharer == null || sharer.getState() != FrostIdentities.FRIEND) {
                sharerStr = (sharer == null) ? "Anonymous" : sharer.getUniqueName();
                logger.info("adding only files from " + sharerStr);
            } else {
                logger.info("adding all files from " + sharer.getUniqueName());
                sharerStr = null;
            }
            Index idx = Index.getInstance();
            synchronized (idx) {
                idx.add(receivedIndex, board, sharerStr);
            }
            target.delete();
            unzippedTarget.delete();
            ifdResult.errorMsg = IndexFileDownloaderResult.SUCCESS;
            return ifdResult;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error in UpdateIdThread", t);
        }
        target.delete();
        return null;
    }

    protected static IndexFileDownloaderResult processDownloadedFile07(File target, FcpResultGet fcpresults, Board board) {
        try {
            IndexFileDownloaderResult ifdResult = new IndexFileDownloaderResult();
            FrostIndex receivedIndex = null;
            try {
                Index idx = Index.getInstance();
                synchronized (idx) {
                    receivedIndex = idx.readKeyFile(target);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not parse the index file: ", ex);
            }
            if (receivedIndex == null || receivedIndex.getFilesMap().size() == 0) {
                logger.log(Level.SEVERE, "Received index file invalid or empty, skipping.");
                target.delete();
                ifdResult.errorMsg = IndexFileDownloaderResult.INVALID_DATA;
                return ifdResult;
            }
            Identity sharer = null;
            Identity sharerInFile = receivedIndex.getSharer();
            if (sharerInFile == null || receivedIndex.getSignature() == null) {
                if (Core.frostSettings.getBoolValue("hideAnonFiles")) {
                    target.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.ANONYMOUS_BLOCKED;
                    return ifdResult;
                }
            } else {
                String _owner = Mixed.makeFilename(sharerInFile.getUniqueName());
                String _pubkey = sharerInFile.getKey();
                boolean sigIsValid = receivedIndex.verifySignature(sharerInFile);
                if (sigIsValid == false) {
                    logger.warning("Invalid signature for index file from " + _owner);
                    target.delete();
                    ifdResult.errorMsg = IndexFileDownloaderResult.TAMPERED_DATA;
                    return ifdResult;
                }
                if (_owner != null) {
                    if (Core.getIdentities().isMySelf(_owner)) {
                        logger.info("Received index file from myself");
                        target.delete();
                        ifdResult.errorMsg = IndexFileDownloaderResult.SUCCESS;
                        return ifdResult;
                    } else {
                        logger.info("Received index file from " + _owner);
                        sharer = Core.getIdentities().getIdentity(_owner);
                        if (sharer == null) {
                            sharer = addNewSharer(_owner, _pubkey);
                            if (sharer == null) {
                                logger.info("sharer was null... :(");
                                target.delete();
                                ifdResult.errorMsg = IndexFileDownloaderResult.TAMPERED_DATA;
                                return ifdResult;
                            }
                        } else if (sharer.getState() == FrostIdentities.ENEMY) {
                            if (Core.frostSettings.getBoolValue("hideBadFiles")) {
                                logger.info("Skipped index file from BAD user " + _owner);
                                target.delete();
                                ifdResult.errorMsg = IndexFileDownloaderResult.BAD_USER;
                                return ifdResult;
                            }
                        }
                        sharer.updateLastSeenTimestamp();
                    }
                }
            }
            Index idx = Index.getInstance();
            synchronized (idx) {
                idx.add(receivedIndex, board, null);
            }
            target.delete();
            ifdResult.errorMsg = IndexFileDownloaderResult.SUCCESS;
            return ifdResult;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error in UpdateIdThread", t);
        }
        target.delete();
        return null;
    }
}
