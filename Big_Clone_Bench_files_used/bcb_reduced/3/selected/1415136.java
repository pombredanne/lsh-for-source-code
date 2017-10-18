package frost.threads;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import frost.*;
import frost.fcp.*;
import frost.crypt.SignMetaData;
import frost.gui.objects.FrostBoardObject;
import frost.identities.*;
import frost.identities.Identity;
import frost.messages.FrostIndex;

public class UpdateIdThread extends BoardUpdateThreadObject implements BoardUpdateThread {

    private static int maxFailures = 4;

    private static Logger logger = Logger.getLogger(UpdateIdThread.class.getName());

    private static final int MAX_TRIES = 2;

    private Vector indices;

    private File indicesFile;

    private int maxKeys;

    private String date;

    private String currentDate;

    private String oldDate;

    private int requestHtl;

    private int insertHtl;

    private String keypool;

    private FrostBoardObject board;

    private String publicKey;

    private String privateKey;

    private String requestKey;

    private String insertKey;

    private String boardState;

    private static final String fileSeparator = System.getProperty("file.separator");

    public int getThreadType() {
        return BoardUpdateThread.BOARD_FILE_DNLOAD;
    }

    /**
	 * Generates a new index file containing keys to upload.
	 * @return true if index file was created, else false.
	 */
    private void loadIndex(String date) {
        indicesFile = new File(MainFrame.keypool + board.getBoardFilename() + fileSeparator + "indices-" + date);
        try {
            if (indicesFile.exists()) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(indicesFile));
                indices = (Vector) in.readObject();
                in.close();
            } else {
                indices = new Vector(100);
                for (int i = 0; i < 100; i++) indices.add(new Integer(0));
            }
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Exception thrown in loadIndex(String date) - Date: '" + date + "' - Board name: '" + board.getBoardFilename() + "'", exception);
        } catch (ClassNotFoundException exception) {
            logger.log(Level.SEVERE, "Exception thrown in loadIndex(String date) - Date: '" + date + "' - Board name: '" + board.getBoardFilename() + "'", exception);
        }
    }

    private void commit() {
        try {
            indicesFile.delete();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indicesFile));
            out.writeObject(indices);
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception thrown in commit()", e);
        }
    }

    /**
     * resets all indices that were tried MAX_TRIES times to 0
     * for the next run of the thread
     */
    private void resetIndices() {
        for (int i = 0; i < indices.size(); i++) {
            Integer current = (Integer) indices.elementAt(i);
            if (current.intValue() >= MAX_TRIES) indices.setElementAt(new Integer(0), i);
        }
    }

    private int findFreeUploadIndex() {
        for (int i = 0; i < indices.size(); i++) {
            Integer current = (Integer) indices.elementAt(i);
            if (current.intValue() > -1) return i;
        }
        return -1;
    }

    private int findFreeUploadIndex(int exclude) {
        for (int i = 0; i < indices.size(); i++) {
            if (i == exclude) continue;
            Integer current = (Integer) indices.elementAt(i);
            if (current.intValue() > -1) return i;
        }
        return -1;
    }

    private int findFreeDownloadIndex() {
        for (int i = 0; i < indices.size(); i++) {
            Integer current = (Integer) indices.elementAt(i);
            if (current.intValue() > -1 && current.intValue() < MAX_TRIES) return i;
        }
        return -1;
    }

    private int findFreeDownloadIndex(int exclude) {
        for (int i = 0; i < indices.size(); i++) {
            if (i == exclude) continue;
            Integer current = (Integer) indices.elementAt(i);
            if (current.intValue() > -1 && current.intValue() < MAX_TRIES) return i;
        }
        return -1;
    }

    private void setIndexFailed(int i) {
        int current = ((Integer) indices.elementAt(i)).intValue();
        if (current == -1 || current > MAX_TRIES) {
            logger.severe("WARNING - index sequence screwed in setFailed. report to a dev");
            return;
        }
        indices.setElementAt(new Integer(current++), i);
        commit();
    }

    private void setIndexSuccessfull(int i) {
        int current = ((Integer) indices.elementAt(i)).intValue();
        if (current == -1 || current > MAX_TRIES) {
            logger.severe("WARNING - index sequence screwed in setSuccesful. report to a dev");
            return;
        }
        indices.setElementAt(new Integer(-1), i);
        commit();
    }

    private FrostIndex makeIndexFile() {
        logger.info("FILEDN: UpdateIdThread.makeIndexFile for " + board.toString());
        Map files = Index.getUploadKeys(board.getBoardFilename());
        if (files == null) return null;
        return new FrostIndex(files);
    }

    private void uploadIndexFile(FrostIndex idx) throws Throwable {
        loadIndex(currentDate);
        File indexFile = new File(keypool + board.getBoardFilename() + "_upload.zip");
        XMLTools.writeXmlFile(XMLTools.getXMLDocument(idx), indexFile.getPath());
        boolean success = false;
        int tries = 0;
        String[] result = { "Error", "Error" };
        if (indexFile.length() > 0 && indexFile.isFile()) {
            boolean signUpload = MainFrame.frostSettings.getBoolValue("signUploads");
            byte[] metadata = null;
            FileAccess.writeZipFile(FileAccess.readByteArray(indexFile), "entry", indexFile);
            if (signUpload) {
                byte[] zipped = FileAccess.readByteArray(indexFile);
                SignMetaData md = new SignMetaData(zipped, identities.getMyId());
                metadata = XMLTools.getRawXMLDocument(md);
            }
            int index = findFreeUploadIndex();
            while (!success && tries <= MAX_TRIES) {
                result = FcpInsert.putFile(insertKey + index + ".idx.sha3.zip", indexFile, metadata, insertHtl, false);
                if (result[0].equals("Success")) {
                    success = true;
                    setIndexSuccessfull(index);
                    logger.info("FILEDN:***** Index file successfully uploaded *****");
                } else {
                    if (result[0].equals("KeyCollision")) {
                        index = findFreeUploadIndex(index);
                        tries = 0;
                        logger.info("FILEDN:***** Index file collided, increasing index. *****");
                    } else {
                        String tv = result[0];
                        if (tv == null) tv = "";
                        logger.info("FILEDN:***** Unknown upload error (#" + tries + ", '" + tv + "'), retrying. *****");
                    }
                }
                tries++;
            }
        }
    }

    private void adjustMaxAge(int count) {
    }

    public void run() {
        notifyThreadStarted(this);
        try {
            int waitTime = (int) (Math.random() * 5000);
            Mixed.wait(waitTime);
            int index = findFreeDownloadIndex();
            int failures = 0;
            while (failures < maxFailures) {
                if (index == -1) {
                    notifyThreadFinished(this);
                    return;
                }
                File target = File.createTempFile("frost-index-" + index, board.getBoardFilename(), new File(MainFrame.frostSettings.getValue("temp.dir")));
                logger.info("FILEDN: Requesting index " + index + " for board " + board.getBoardName() + " for date " + date);
                FcpResults fcpresults = FcpRequest.getFile(requestKey + index + ".idx.sha3.zip", null, target, requestHtl + ((Integer) indices.elementAt(index)).intValue(), true);
                if (fcpresults != null && target.length() > 0) {
                    setIndexSuccessfull(index);
                    failures = 0;
                    String digest = Core.getCrypto().digest(target);
                    if (Core.getMessageSet().contains(digest)) {
                        target.delete();
                        index = findFreeDownloadIndex();
                        continue;
                    }
                    Core.getMessageSet().add(digest);
                    try {
                        byte[] zippedXml = FileAccess.readByteArray(target);
                        byte[] unzippedXml = FileAccess.readZipFileBinary(target);
                        if (unzippedXml == null) {
                            logger.warning("Could not extract received zip file, skipping.");
                            target.delete();
                            index = findFreeDownloadIndex();
                            continue;
                        }
                        File unzippedTarget = new File(target.getPath() + "_unzipped");
                        FileAccess.writeByteArray(unzippedXml, unzippedTarget);
                        FrostIndex receivedIndex = null;
                        try {
                            receivedIndex = new FrostIndex(XMLTools.parseXmlFile(unzippedTarget, false).getDocumentElement());
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Could not parse the index file, skipping.", ex);
                            target.delete();
                            unzippedTarget.delete();
                            index = findFreeDownloadIndex();
                            continue;
                        }
                        Identity sharer = null;
                        Identity sharerInFile = receivedIndex.getSharer();
                        if (fcpresults.getRawMetadata() != null) {
                            SignMetaData md;
                            try {
                                md = new SignMetaData(zippedXml, fcpresults.getRawMetadata());
                            } catch (Throwable t) {
                                logger.log(Level.SEVERE, "Could not read the XML metadata, skipping file index.", t);
                                target.delete();
                                index = findFreeDownloadIndex();
                                continue;
                            }
                            if (sharerInFile == null) {
                                logger.warning("MetaData present, but file didn't contain an identity :(");
                                unzippedTarget.delete();
                                target.delete();
                                index = findFreeDownloadIndex();
                                continue;
                            }
                            String _owner = null;
                            String _pubkey = null;
                            if (md.getPerson() != null) {
                                _owner = Mixed.makeFilename(md.getPerson().getUniqueName());
                                _pubkey = md.getPerson().getKey();
                            }
                            if (_owner == null || _owner.length() == 0 || _pubkey == null || _pubkey.length() == 0) {
                                logger.warning("XML metadata have missing fields, skipping file index.");
                                unzippedTarget.delete();
                                target.delete();
                                index = findFreeDownloadIndex();
                                continue;
                            }
                            if (!_owner.equals(Mixed.makeFilename(sharerInFile.getUniqueName())) || !_pubkey.equals(sharerInFile.getKey())) {
                                logger.warning("The identity in MetaData didn't match the identity in File! :(\n" + "file owner : " + sharerInFile.getUniqueName() + "\n" + "file key : " + sharerInFile.getKey() + "\n" + "meta owner: " + _owner + "\n" + "meta key : " + _pubkey);
                                unzippedTarget.delete();
                                target.delete();
                                index = findFreeDownloadIndex();
                                continue;
                            }
                            boolean valid = Core.getCrypto().detachedVerify(zippedXml, _pubkey, md.getSig());
                            if (valid == false) {
                                logger.warning("Invalid sign for index file from " + _owner);
                                unzippedTarget.delete();
                                target.delete();
                                index = findFreeDownloadIndex();
                                continue;
                            }
                            if (identities.getMyId().getUniqueName().trim().equals(_owner)) {
                                logger.info("Received index file from myself");
                                sharer = identities.getMyId();
                            } else {
                                String message = "Received index file from " + _owner;
                                if (identities.getFriends().containsKey(_owner)) {
                                    sharer = identities.getFriends().get(_owner);
                                    logger.info(message + ", a friend");
                                } else if (identities.getNeutrals().containsKey(_owner)) {
                                    sharer = identities.getNeutrals().get(_owner);
                                    logger.info(message + ", a neutral");
                                } else if (identities.getEnemies().containsKey(_owner)) {
                                    if (MainFrame.frostSettings.getBoolValue("hideBadFiles")) {
                                        logger.info("Skipped index file from BAD user " + _owner);
                                        target.delete();
                                        unzippedTarget.delete();
                                        index = findFreeDownloadIndex();
                                        continue;
                                    }
                                    sharer = identities.getEnemies().get(_owner);
                                    logger.info(message + ", an enemy");
                                } else {
                                    logger.info(message + ", a new contact");
                                    sharer = addNewSharer(_owner, _pubkey);
                                    if (sharer == null) {
                                        logger.info("sharer was null... :(");
                                        unzippedTarget.delete();
                                        target.delete();
                                        index = findFreeDownloadIndex();
                                        continue;
                                    }
                                }
                            }
                        } else if (MainFrame.frostSettings.getBoolValue("hideAnonFiles")) {
                            unzippedTarget.delete();
                            target.delete();
                            index = findFreeDownloadIndex();
                            continue;
                        }
                        if (sharer == null || identities.getFriends().containsKey(sharer.getUniqueName()) == false) {
                            String _sharer = sharer == null ? "Anonymous" : sharer.getUniqueName();
                            logger.info("adding only files from " + _sharer);
                            Index.add(receivedIndex, board, _sharer);
                        } else {
                            logger.info("adding all files from " + sharer.getUniqueName());
                            Index.add(unzippedTarget, board, sharer);
                        }
                        target.delete();
                        unzippedTarget.delete();
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, "Error in UpdateIdThread", t);
                    }
                    index = findFreeDownloadIndex(index);
                    failures = 0;
                } else {
                    target.delete();
                    setIndexFailed(index);
                    failures++;
                    index = findFreeDownloadIndex(index);
                }
            }
            if (isInterrupted()) {
                notifyThreadFinished(this);
                return;
            }
            FrostIndex frostIndex = makeIndexFile();
            if (frostIndex != null) {
                logger.info("FILEDN: Starting upload of index file to board '" + board.toString());
                uploadIndexFile(frostIndex);
            } else {
                logger.info("FILEDN: No keys to upload, stopping UpdateIdThread for " + board.toString());
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Oo. EXCEPTION in UpdateIdThread", t);
        }
        notifyThreadFinished(this);
        resetIndices();
        commit();
    }

    /**
     * This method checks if the digest of sharer matches the pubkey,
     * and adds the NEW identity to list of neutrals.   
     * @param _sharer
     * @param _pubkey
     * @return
     */
    private Identity addNewSharer(String _sharer, String _pubkey) {
        Identity sharer = null;
        String given_digest = _sharer.substring(_sharer.indexOf("@") + 1, _sharer.length()).trim();
        String calculatedDigest = Core.getCrypto().digest(_pubkey.trim()).trim();
        calculatedDigest = Mixed.makeFilename(calculatedDigest).trim();
        if (!Mixed.makeFilename(given_digest).equals(calculatedDigest)) {
            logger.warning("Warning: public key of sharer didn't match its digest:\n" + "given digest :'" + given_digest + "'\n" + "pubkey       :'" + _pubkey.trim() + "'\n" + "calc. digest :'" + calculatedDigest + "'");
            return null;
        }
        sharer = new Identity(_sharer.substring(0, _sharer.indexOf("@")), _pubkey);
        identities.getNeutrals().add(sharer);
        return sharer;
    }

    /**Constructor*/
    public UpdateIdThread(FrostBoardObject board, String date, FrostIdentities newIdentities) {
        super(board, newIdentities);
        this.board = board;
        this.date = date;
        currentDate = DateFun.getDate();
        requestHtl = MainFrame.frostSettings.getIntValue("keyDownloadHtl");
        insertHtl = MainFrame.frostSettings.getIntValue("keyUploadHtl");
        keypool = MainFrame.frostSettings.getValue("keypool.dir");
        maxKeys = MainFrame.frostSettings.getIntValue("maxKeys");
        loadIndex(date);
        publicKey = board.getPublicKey();
        privateKey = board.getPrivateKey();
        if (board.isPublicBoard() == false && publicKey != null) {
            requestKey = new StringBuffer().append(publicKey).append("/").append(date).append("/").toString();
        } else {
            requestKey = new StringBuffer().append("KSK@frost/index/").append(board.getBoardFilename()).append("/").append(date).append("/").toString();
        }
        if (board.isPublicBoard() == false && privateKey != null) insertKey = new StringBuffer().append(privateKey).append("/").append(currentDate).append("/").toString(); else insertKey = new StringBuffer().append("KSK@frost/index/").append(board.getBoardFilename()).append("/").append(currentDate).append("/").toString();
    }
}
