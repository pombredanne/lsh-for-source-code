package frost.threads;

import java.io.File;
import java.util.*;
import java.util.logging.*;
import org.w3c.dom.Element;
import frost.*;
import frost.fcp.*;
import frost.crypt.*;
import frost.gui.objects.FrostBoardObject;
import frost.identities.*;
import frost.identities.Identity;
import frost.messages.*;

/**
 * Downloads messages
 */
public class MessageDownloadThread extends BoardUpdateThreadObject implements BoardUpdateThread {

    public FrostBoardObject board;

    private int downloadHtl;

    private String keypool;

    private int maxMessageDownload;

    private String destination;

    private boolean secure;

    private String publicKey;

    private boolean flagNew;

    private static Logger logger = Logger.getLogger(MessageDownloadThread.class.getName());

    public int getThreadType() {
        if (flagNew) {
            return BoardUpdateThread.MSG_DNLOAD_TODAY;
        } else {
            return BoardUpdateThread.MSG_DNLOAD_BACK;
        }
    }

    public void run() {
        notifyThreadStarted(this);
        try {
            String tofType;
            if (flagNew) tofType = "TOF Download"; else tofType = "TOF Download Back";
            int waitTime = (int) (Math.random() * 5000);
            Mixed.wait(waitTime);
            logger.info("TOFDN: " + tofType + " Thread started for board " + board.toString());
            if (isInterrupted()) {
                notifyThreadFinished(this);
                return;
            }
            if (board.isPublicBoard() == false) {
                publicKey = board.getPublicKey();
                secure = true;
            } else {
                secure = false;
            }
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            if (this.flagNew) {
                downloadDate(cal);
            } else {
                GregorianCalendar firstDate = new GregorianCalendar();
                firstDate.setTimeZone(TimeZone.getTimeZone("GMT"));
                firstDate.set(Calendar.YEAR, 2001);
                firstDate.set(Calendar.MONTH, 5);
                firstDate.set(Calendar.DATE, 11);
                int counter = 0;
                while (!isInterrupted() && cal.after(firstDate) && counter < maxMessageDownload) {
                    counter++;
                    cal.add(Calendar.DATE, -1);
                    downloadDate(cal);
                }
            }
            logger.info("TOFDN: " + tofType + " Thread stopped for board " + board.toString());
        } catch (Throwable t) {
            logger.log(Level.SEVERE, Thread.currentThread().getName() + ": Oo. Exception in MessageDownloadThread:", t);
        }
        notifyThreadFinished(this);
    }

    /**Returns true if message is duplicate*/
    private boolean exists(File file) {
        File[] fileList = (file.getParentFile()).listFiles();
        String one = null;
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                if (!fileList[i].equals(file) && fileList[i].getName().endsWith(".sig") == false && fileList[i].getName().indexOf(board.getBoardFilename()) != -1 && file.getName().indexOf(board.getBoardFilename()) != -1) {
                    if (one == null) {
                        one = FileAccess.readFile(file);
                    }
                    String two = FileAccess.readFile(fileList[i]);
                    if (one.equals(two)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void downloadDate(GregorianCalendar calDL) {
        VerifyableMessageObject currentMsg = null;
        String dirdate = DateFun.getDateOfCalendar(calDL);
        String fileSeparator = System.getProperty("file.separator");
        destination = new StringBuffer().append(keypool).append(board.getBoardFilename()).append(fileSeparator).append(dirdate).append(fileSeparator).toString();
        File makedir = new File(destination);
        if (!makedir.exists()) {
            makedir.mkdirs();
        }
        File checkLockfile = new File(destination + "locked.lck");
        int index = 0;
        int failures = 0;
        int maxFailures;
        if (flagNew) {
            maxFailures = 3;
        } else {
            maxFailures = 2;
        }
        while (failures < maxFailures && (flagNew || !checkLockfile.exists())) {
            byte[] metadata = null;
            try {
                String val = new StringBuffer().append(destination).append(System.currentTimeMillis()).append(".xml.msg").toString();
                File testMe = new File(val);
                val = new StringBuffer().append(destination).append(dirdate).append("-").append(board.getBoardFilename()).append("-").append(index).append(".xml").toString();
                File testMe2 = new File(val);
                if (testMe2.length() > 0) {
                    index++;
                    failures = 0;
                } else {
                    String downKey = null;
                    if (secure) {
                        downKey = new StringBuffer().append(publicKey).append("/").append(board.getBoardFilename()).append("/").append(dirdate).append("-").append(index).append(".xml").toString();
                    } else {
                        downKey = new StringBuffer().append("KSK@frost/message/").append(MainFrame.frostSettings.getValue("messageBase")).append("/").append(dirdate).append("-").append(board.getBoardFilename()).append("-").append(index).append(".xml").toString();
                    }
                    try {
                        boolean fastDownload = !flagNew;
                        FcpResults res = FcpRequest.getFile(downKey, null, testMe, downloadHtl, false, fastDownload);
                        if (res == null) metadata = null; else metadata = res.getRawMetadata();
                        Mixed.wait(111);
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, "Exception thrown in downloadDate(GregorianCalendar calDL)", t);
                    }
                    if (testMe.length() > 0) {
                        testMe.renameTo(testMe2);
                        testMe = testMe2;
                        String messageId = Core.getCrypto().digest(testMe);
                        if (!exists(testMe) && !Core.getMessageSet().contains(messageId)) {
                            Core.getMessageSet().add(messageId);
                            if (metadata == null) {
                                byte[] unzippedXml = FileAccess.readZipFileBinary(testMe);
                                FileAccess.writeByteArray(unzippedXml, testMe);
                                try {
                                    currentMsg = new VerifyableMessageObject(testMe);
                                } catch (Exception ex) {
                                    logger.log(Level.SEVERE, "Exception thrown in downloadDate(GregorianCalendar calDL)", ex);
                                    index++;
                                    continue;
                                }
                                currentMsg.setStatus(VerifyableMessageObject.OLD);
                                addMessageToGui(currentMsg, testMe, true);
                                index++;
                                continue;
                            }
                            byte[] plaintext = FileAccess.readByteArray(testMe);
                            MetaData _metaData = null;
                            try {
                                File tempMeta = new File("tempMeta");
                                FileAccess.writeByteArray(metadata, tempMeta);
                                Element el = XMLTools.parseXmlFile(tempMeta, false).getDocumentElement();
                                tempMeta.delete();
                                _metaData = MetaData.getInstance(plaintext, el);
                            } catch (Throwable t) {
                                logger.log(Level.SEVERE, "metadata couldn't be read. " + "Offending file saved as badmetadata.xml - send to a dev for analysis", t);
                                File badmetadata = new File("badmetadata.xml");
                                FileAccess.writeByteArray(metadata, badmetadata);
                                index++;
                                failures = 0;
                                continue;
                            }
                            assert _metaData.getType() == MetaData.SIGN || _metaData.getType() == MetaData.ENCRYPT : "unknown type of metadata";
                            if (_metaData.getType() == MetaData.SIGN) {
                                SignMetaData metaData = null;
                                metaData = (SignMetaData) _metaData;
                                String _owner = metaData.getPerson().getUniqueName();
                                Identity owner;
                                owner = identities.getFriends().get(_owner);
                                if (owner == null) owner = identities.getNeutrals().get(_owner);
                                if (owner == null) owner = identities.getEnemies().get(_owner);
                                if (owner == null) {
                                    owner = metaData.getPerson();
                                    owner.noFiles = 0;
                                    owner.noMessages = 1;
                                    identities.getNeutrals().add(owner);
                                }
                                boolean valid = Core.getCrypto().detachedVerify(plaintext, owner.getKey(), metaData.getSig());
                                byte[] unzippedXml = FileAccess.readZipFileBinary(testMe);
                                FileAccess.writeByteArray(unzippedXml, testMe);
                                try {
                                    currentMsg = new VerifyableMessageObject(testMe);
                                } catch (Exception ex) {
                                    logger.log(Level.SEVERE, "Exception thrown in downloadDate(GregorianCalendar calDL)", ex);
                                    index++;
                                    continue;
                                }
                                if (!valid) {
                                    currentMsg.setStatus(VerifyableMessageObject.TAMPERED);
                                    logger.warning("TOFDN: message failed verification");
                                    addMessageToGui(currentMsg, testMe, false);
                                    index++;
                                    continue;
                                }
                                String metaDataHash = Mixed.makeFilename(Core.getCrypto().digest(metaData.getPerson().getKey()));
                                String messageHash = Mixed.makeFilename(currentMsg.getFrom().substring(currentMsg.getFrom().indexOf("@") + 1, currentMsg.getFrom().length()));
                                if (!metaDataHash.equals(messageHash)) {
                                    logger.warning("hash in metadata doesn't match hash in message!\n" + "metadata : " + metaDataHash + " , message: " + messageHash);
                                    currentMsg.setStatus(VerifyableMessageObject.TAMPERED);
                                    addMessageToGui(currentMsg, testMe, false);
                                    index++;
                                    continue;
                                }
                                if (identities.getFriends().containsKey(_owner)) currentMsg.setStatus(VerifyableMessageObject.VERIFIED); else if (identities.getEnemies().containsKey(_owner)) currentMsg.setStatus(VerifyableMessageObject.FAILED); else currentMsg.setStatus(VerifyableMessageObject.PENDING);
                                if (currentMsg.isValidFormat(calDL) == false) {
                                }
                                addMessageToGui(currentMsg, testMe, true);
                            } else if (_metaData.getType() == MetaData.ENCRYPT) {
                                if (!_metaData.getPerson().getUniqueName().equals(identities.getMyId().getUniqueName())) {
                                    logger.fine("encrypted message was for " + _metaData.getPerson().getUniqueName());
                                    index++;
                                    failures = 0;
                                    continue;
                                }
                                byte[] cipherText = FileAccess.readByteArray(testMe);
                                byte[] plainText = Core.getCrypto().decrypt(cipherText, identities.getMyId().getPrivKey());
                            }
                        } else {
                            logger.info(Thread.currentThread().getName() + ": TOFDN: ****** Duplicate Message : " + testMe.getName() + " *****");
                            FileAccess.writeFile("Empty", testMe);
                        }
                        index++;
                        failures = 0;
                    } else {
                        failures++;
                        index++;
                    }
                }
                if (isInterrupted()) return;
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Exception thrown in downloadDate(GregorianCalendar calDL)", t);
                index++;
            }
        }
    }

    private void addMessageToGui(VerifyableMessageObject currentMsg, File testMe, boolean markAsNew) {
        if (currentMsg.isValid()) {
            if (TOF.blocked(currentMsg, board) && testMe.length() > 0) {
                board.incBlocked();
                logger.info("TOFDN: ########### blocked message for board '" + board.toString() + "' #########\n");
            } else {
                if (markAsNew) {
                    FileAccess.writeFile("This message is new!", testMe.getPath() + ".lck");
                }
                TOF.addNewMessageToTable(testMe, board, markAsNew);
                Iterator it = currentMsg.getAttachmentList().getAllOfType(Attachment.FILE).iterator();
                while (it.hasNext()) {
                    SharedFileObject current = ((FileAttachment) it.next()).getFileObj();
                    if (current.getOwner() != null) Index.add(current, board);
                }
                Core.addNewKnownBoards(currentMsg.getAttachmentList().getAllOfType(Attachment.BOARD));
            }
        } else {
            FileAccess.writeFile("Empty", testMe);
        }
    }

    public MessageDownloadThread(boolean fn, FrostBoardObject boa, int dlHtl, String kpool, String maxmsg, FrostIdentities newIdentities) {
        super(boa, newIdentities);
        this.flagNew = fn;
        this.board = boa;
        this.downloadHtl = dlHtl;
        this.keypool = kpool;
        this.maxMessageDownload = Integer.parseInt(maxmsg);
    }
}
