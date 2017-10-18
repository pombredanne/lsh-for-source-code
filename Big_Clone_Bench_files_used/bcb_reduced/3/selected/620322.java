package com.sts.webmeet.client;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.sts.webmeet.api.DialogClosingListener;
import com.sts.webmeet.api.MessageReader;
import com.sts.webmeet.api.MessageRouter;
import com.sts.webmeet.api.StartupListener;
import com.sts.webmeet.common.AntiModeratorMessage;
import com.sts.webmeet.common.BigBundle;
import com.sts.webmeet.common.Constants;
import com.sts.webmeet.common.EjectMessage;
import com.sts.webmeet.common.EndMeetingMessage;
import com.sts.webmeet.common.IOUtil;
import com.sts.webmeet.common.JavaRuntimeInfo;
import com.sts.webmeet.common.LockUnlockMeetingMessage;
import com.sts.webmeet.common.MeetingLockedStatusMessage;
import com.sts.webmeet.common.MeetingOpenedMessage;
import com.sts.webmeet.common.ModeratorInfoMessage;
import com.sts.webmeet.common.ModeratorMessage;
import com.sts.webmeet.common.OpenMeetingMessageAccount;
import com.sts.webmeet.common.OpenMeetingMessageCC;
import com.sts.webmeet.common.OpenMeetingMessagePlayback;
import com.sts.webmeet.common.PackageUtil;
import com.sts.webmeet.common.ParticipantInfo;
import com.sts.webmeet.common.RecordingOffMessage;
import com.sts.webmeet.common.RecordingOnMessage;
import com.sts.webmeet.common.RecordingStartStopMessage;
import com.sts.webmeet.common.RosterChangedMessage;
import com.sts.webmeet.common.RosterJoinAcceptMessage;
import com.sts.webmeet.common.RosterJoinMessage;
import com.sts.webmeet.common.RosterJoinPlaybackMessage;
import com.sts.webmeet.common.RosterRequestMessage;
import com.sts.webmeet.common.RosterRosterMessage;
import com.sts.webmeet.common.ServerIDMessage;
import com.sts.webmeet.common.ViewControlMessage;
import com.sts.webmeet.common.WebmeetMessage;
import com.sts.webmeet.content.client.AbstractContent;
import com.sts.webmeet.content.client.ClientContext;
import com.sts.webmeet.content.client.Content;
import com.sts.webmeet.content.client.ControlWindowContent;
import com.sts.webmeet.content.client.MainWindowContent;
import com.sts.webmeet.content.client.StatusInfo;
import com.sts.webmeet.content.client.StatusUI;
import com.sts.webmeet.content.client.StatusWindowContent;
import com.sts.webmeet.content.common.ControlWindowActivatedMessage;
import com.sts.webmeet.content.common.MeetingScript;
import com.sts.webmeet.content.common.ScriptIndexMessage;
import com.sts.webmeet.content.common.ScriptItemImpl;
import com.sts.webmeet.content.common.ScriptItemSelectedMessage;
import com.sts.webmeet.content.common.ScriptMessage;
import com.sts.webmeet.content.common.ScriptReplyMessage;
import com.sts.webmeet.content.common.ScriptRequestMessage;

public class ContentManager implements MessageReader, ControlUIListener, ClientContext, ConnectionEventListener, TimeListener {

    public ContentManager(Applet applet, BigBundle bundle) {
        this.applet = applet;
        this.bundle = bundle;
    }

    public void sendMessage(WebmeetMessage message) {
        router.sendMessage(message);
    }

    public void sendMessageToSelf(WebmeetMessage message) {
        router.sendMessageToSelf(message);
    }

    public final String getMeetingID() {
        return getParameter("confID");
    }

    public final String getMeetingKey() {
        return getParameter("meetingKey");
    }

    public boolean isFilePlayback() {
        return router instanceof FileMessageRouter;
    }

    public java.net.URL getCodeBase() {
        return applet.getCodeBase();
    }

    public void browseURL(URL url) {
        applet.getAppletContext().showDocument(url, "" + url);
    }

    public void start() {
        this.simpleLog.info("SimpleLog test message [info]");
        this.simpleLog.debug("SimpleLog test message [debug]");
        bIsPlayback = (null != getParameter("playback") && "true".equalsIgnoreCase(getParameter("playback")));
        pi = new ParticipantInfo();
        pi.setEmail(getParameter("email"));
        if (null != getParameter("participant_name") && getParameter("participant_name").trim().length() > 0) {
            pi.setName(getParameter("participant_name"));
        }
        pi.setConfID(getParameter("confID"));
        pi.setIsLeader(getParameter("role").equalsIgnoreCase("moderator"));
        pi.setRole(getParameter("role"));
        bModerator = getParameter("role").equalsIgnoreCase("moderator");
        String strRemote = applet.getDocumentBase().toString();
        int iLastSlash = strRemote.lastIndexOf('/');
        strAppletDir = strRemote.substring(0, iLastSlash);
        if (System.getProperty("java.vendor").indexOf("Microsoft") > -1) {
            try {
                if (Class.forName("com.ms.security.PolicyEngine") != null) {
                    com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.SYSTEM);
                    System.getProperty("com.ms.sysdir");
                    getJNIFiles();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String strServletString = strAppletDir + "/" + getParameter("control_servlet") + "?confID=" + getParameter("confID") + "&participantID=" + getParameter("participantID") + "&meetingKey=" + getParameter("meetingKey");
        if (null != applet.getParameter("stream_file")) {
            try {
                router = new FileMessageRouter(new URL(applet.getDocumentBase(), applet.getParameter("stream_file")));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            router = new HttpMessageRouter(strServletString);
        }
        if (this.bIsPlayback) {
            router.setTimeListener(this);
        }
        subscribe(router);
        router.setConnectionEventListener(this);
        addUserSpecificButtons(new ControlList(aciCommands), this);
        addMeetingControls(new ControlList(getMeetingCommands()), this);
        try {
            setContentTypes(getParameter("contenttypes"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        initStatus();
        router.start();
    }

    public void stop() {
        if (null != contents) {
            for (int i = 0; i < contents.length; i++) {
                try {
                    contents[i].destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (null != router) {
            router.stop();
        }
    }

    public void setContentUI(ContentUI contentUI) {
        this.contentUI = contentUI;
    }

    public void setControlUI(ControlUI controlUI) {
        this.controlUI = controlUI;
        this.controlUI.setControlUIListener(this);
    }

    public void setStatusUI(StatusUI statusUI) {
        this.statusUI = statusUI;
    }

    public void setSelectedContentItem(int index) {
        controlUI.setSelectedContentItem(index);
    }

    public ParticipantInfo getSelectedUser() {
        ParticipantInfo pi = null;
        String strSelected = controlUI.getSelectedRosterLabel();
        if (null != strSelected) {
            pi = (ParticipantInfo) hashParticipants.get(strSelected);
        }
        return pi;
    }

    public void timeUpdate(long lElapsedTime) {
        this.led.setElapsedTime(lElapsedTime);
    }

    public void refl_makeMod() {
        if (null != getSelectedUser()) {
            router.sendMessage(new ModeratorMessage(getSelectedUser()));
        }
    }

    public void refl_invitePeople() {
        this.showInvitePeopleUI();
    }

    public void refl_endMeeting() {
        router.sendMessage(new EndMeetingMessage());
    }

    public void refl_startStopRecording() {
        router.sendMessage(new RecordingStartStopMessage());
    }

    public void refl_eject() {
        if (null != getSelectedUser() && (!this.pi.equals(getSelectedUser()))) {
            router.sendMessage(new EjectMessage(getSelectedUser()));
        }
    }

    public void refl_lockMeeting() {
        router.sendMessage(new LockUnlockMeetingMessage());
    }

    public void listItemSelected(int index) {
        router.sendMessage(new ScriptItemSelectedMessage(index));
    }

    public void controlWindowTabSelected(String strWindow) {
        ControlWindowContent cwc = (ControlWindowContent) hashControlWindowContents.get(strWindow);
        cwc.activated();
        if (bModerator) {
            router.sendMessage(new ControlWindowActivatedMessage(strWindow));
        }
    }

    public void meetingControlAction(String strCommand, Object objHandler) {
        if ("endMeeting".equals(strCommand)) {
            router.sendMessage(new EndMeetingMessage());
        }
    }

    public void subscribe(MessageRouter router) {
        this.router = router;
        router.subscribe(this);
    }

    public void readMessage(WebmeetMessage message) {
        if (message instanceof ServerIDMessage) {
            handleServerIDAssignment(((ServerIDMessage) message).getServerID());
        } else if (message instanceof MeetingOpenedMessage) {
            logon();
        } else if (message instanceof RosterJoinAcceptMessage) {
            handleJoinAccept();
        } else if (message instanceof ViewControlMessage) {
            showMainContent((ViewControlMessage) message);
        } else if (message instanceof RosterChangedMessage) {
            router.sendMessage(new RosterRequestMessage());
        } else if (message instanceof RosterRosterMessage) {
            updateRoster(((RosterRosterMessage) message).getRoster());
        } else if (message instanceof ModeratorMessage) {
            makeModerator(true);
        } else if (message instanceof AntiModeratorMessage) {
            makeModerator(false);
        } else if (message instanceof ScriptMessage) {
            handleScriptMessage((ScriptMessage) message);
        } else if (message instanceof ControlWindowActivatedMessage) {
            activateControlWindow((ControlWindowActivatedMessage) message);
        } else if (message instanceof ModeratorInfoMessage) {
            handleModeratorInfo((ModeratorInfoMessage) message);
        } else if (message instanceof RecordingOnMessage) {
            handleRecordingOn();
        } else if (message instanceof RecordingOffMessage) {
            handleRecordingOff();
        } else if (message instanceof EndMeetingMessage) {
            exitMeeting((EndMeetingMessage) message);
        } else if (message instanceof EjectMessage) {
            handleEject();
        } else if (message instanceof MeetingLockedStatusMessage) {
            handleMeetingLockStatusMessage((MeetingLockedStatusMessage) message);
        }
    }

    public void connected(boolean bSecure) {
        statusUI.setStatusActive(STATUS_INFO_CONNECTION);
        if (bSecure) {
            statusUI.setStatusActive(STATUS_INFO_SECURITY);
        } else {
            statusUI.setStatusInactive(STATUS_INFO_SECURITY);
        }
    }

    public void disconnected() {
        statusUI.setStatusInactive(STATUS_INFO_CONNECTION);
        statusUI.setStatusInactive(STATUS_INFO_SECURITY);
        if (!this.isMeetingOver() && !this.bIsPlayback && this.iReconnectCount++ < MAX_RECONNECTS) {
            System.out.println("about to reconnect...");
            bReconnecting = true;
            router.stop();
            router.start();
            System.out.println("...back from reconnecting.");
        }
    }

    public boolean isMeetingOver() {
        return this.bMeetingOver;
    }

    private void handleJoinAccept() {
        pi.setServerID(applet.getParameter("participantID"));
        if (bModerator) {
            router.sendMessage(new ModeratorMessage(pi));
        } else {
        }
        router.sendMessage(new ScriptRequestMessage());
    }

    private void handleScriptMessage(ScriptMessage mess) {
        if (mess instanceof ScriptReplyMessage) {
            script = ((ScriptReplyMessage) mess).getScript();
            controlUI.clearContentList();
            ScriptItemImpl[] items = script.getItems();
            for (int i = 0; i < items.length; i++) {
                controlUI.addToContentList(items[i].getName(), ImageUtil.getImage(items[i].getIconPath(), applet), i + "");
            }
        } else if (mess instanceof ScriptIndexMessage) {
            setSelectedContentItem(((ScriptIndexMessage) mess).getIndex());
        }
    }

    private void activateControlWindow(ControlWindowActivatedMessage message) {
        controlUI.activateControlWindow(message.getWindowName());
    }

    private void handleModeratorInfo(ModeratorInfoMessage mess) {
        piModerator = mess.getModerator();
        if (this.meetingHostEmail == null) {
            this.meetingHostEmail = piModerator.getEmail();
            this.meetingPassword = mess.getMeetingPassword();
        }
        updateRoster(apiRoster);
    }

    private void handleRecordingOn() {
        statusUI.setStatusActive(STATUS_INFO_RECORDED);
    }

    private void handleRecordingOff() {
        statusUI.setStatusInactive(STATUS_INFO_RECORDED);
    }

    private void handleEject() {
        if (!this.bOwner) {
            EndMeetingMessage emm = new EndMeetingMessage();
            emm.setDonePage("eject.html");
            this.exitMeeting(emm);
        }
    }

    private void handleMeetingLockStatusMessage(MeetingLockedStatusMessage mess) {
        if (mess.isLocked()) {
            statusUI.setStatusActive(STATUS_INFO_LOCK);
        } else {
            statusUI.setStatusInactive(STATUS_INFO_LOCK);
        }
    }

    private void makeModerator(boolean bModerator) {
        this.bModerator = bModerator;
        controlUI.enableViewButtons(bModerator);
        controlUI.enableContentList(bModerator);
        controlUI.enableContentControls(bModerator);
        controlUI.enableUserSpecificControls(bOwner ? true : bModerator);
        controlUI.enableMeetingControls(bOwner ? true : bModerator);
        for (int i = 0; i < contents.length; i++) {
            contents[i].onModerator(bModerator);
        }
        statusUI.setStatus(STATUS_INFO_MODERATOR, bModerator);
    }

    private void updateRoster(ParticipantInfo[] api) {
        if (null == api) {
            return;
        }
        apiRoster = api;
        String strSelected = controlUI.getSelectedRosterLabel();
        controlUI.clearRoster();
        hashParticipants.clear();
        int iSelectedIndex = 0;
        Image icon = null;
        for (int i = 0; i < api.length; i++) {
            ParticipantInfo piTemp = api[i];
            hashParticipants.put(piTemp.getLabel(), piTemp);
            if (null != piModerator && piTemp.getServerID().equals(piModerator.getServerID())) {
                icon = ImageUtil.getImage(MODERATOR_ICON, applet);
            } else {
                icon = null;
            }
            controlUI.addToRoster(piTemp.getLabel(), icon);
            if (null != strSelected && piTemp.getLabel().equals(strSelected)) {
                iSelectedIndex = i;
            }
        }
        controlUI.setSelectedRoster(iSelectedIndex);
        if (this.bOwner && (!this.invitePeopleUIShown) && (!this.bIsPlayback)) {
            this.invitePeopleUIShown = true;
            showInvitePeopleUI();
        }
        if (!startupListenersCalled) {
            startupListenersCalled = true;
            for (int i = 0; i < this.contents.length; i++) {
                if (contents[i] instanceof StartupListener) {
                    ((StartupListener) contents[i]).startedUp();
                }
            }
        }
    }

    public void showNonModalDialog(final Component content, String title, final DialogClosingListener dialogClosingListener) {
        showDialog(content, title, dialogClosingListener, false);
    }

    public void showModalDialog(final Component content, String title, final DialogClosingListener dialogClosingListener) {
        showDialog(content, title, dialogClosingListener, true);
    }

    private void showDialog(final Component content, String title, final DialogClosingListener dialogClosingListener, boolean modal) {
        Component comp = this.applet;
        while (comp.getParent() != null) {
            comp = comp.getParent();
        }
        try {
            Dialog dialog = new Dialog((Frame) comp, title, modal);
            dialog.add(content);
            dialog.pack();
            centerWindowOn(comp, dialog);
            if (dialogClosingListener != null) {
                dialog.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e) {
                        dialogClosingListener.dialogClosing(content);
                    }
                });
            }
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInvitePeopleUI() {
        try {
            URL mailto = buildMailtoInviteUrl();
            InvitePeopleUI ui = new InvitePeopleUI(this.applet, mailto, buildInviteLink(false));
            ui.init();
            showNonModalDialog(ui, "Invite People", ui);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setContentTypes(String strTypes) throws Exception {
        StringTokenizer strTok = new StringTokenizer(strTypes, ",");
        int iTypeCount = strTok.countTokens();
        contents = new Content[iTypeCount];
        ControlList[] aControlLists = null;
        ControlList[] aMeetingControlLists = null;
        for (int i = 0; i < iTypeCount; i++) {
            String strClass = strTok.nextToken() + ".Content";
            String strResources = PackageUtil.getParent(strClass) + ".Resources";
            try {
                ((BigBundle) bundle).addBundle(strResources);
            } catch (Exception e) {
                e.printStackTrace();
            }
            contents[i] = (Content) Class.forName(strClass).newInstance();
            if (contents[i] instanceof AbstractContent) {
                ((AbstractContent) contents[i]).setResourceBundle(bundle);
            }
            if (contents[i] instanceof MainWindowContent) {
                MainWindowContent mw = (MainWindowContent) contents[i];
                contentUI.addContentObject(mw.getAWTComponent(), mw.getClass().getName());
                controlUI.addViewButton(mw.getViewButtonInfo(), mw);
            }
            if (contents[i] instanceof StatusWindowContent) {
                ((StatusWindowContent) contents[i]).setStatusUI(statusUI);
            }
            if (contents[i] instanceof ControlWindowContent) {
                ControlWindowContent cwc = (ControlWindowContent) contents[i];
                cwc.setThinletHelper(controlUI.getThinletHelper());
                String strThinletUI = cwc.getThinletGUI();
                controlUI.addThinletControlWindowContent(cwc.getContentLabel(), cwc.getClass().getName(), strThinletUI, cwc);
                hashControlWindowContents.put(cwc.getClass().getName(), cwc);
            }
            aMeetingControlLists = contents[i].getMeetingControls();
            for (int j = 0; j < aMeetingControlLists.length; j++) {
                addMeetingControls(aMeetingControlLists[j], contents[i]);
            }
            aControlLists = contents[i].getUserSpecificControls();
            addUserSpecificButtons(aControlLists, contents[i]);
            contents[i].setClientContext(this);
            contents[i].init();
            if (contents[i] instanceof MessageReader) {
                ((MessageReader) contents[i]).subscribe(router);
            }
        }
    }

    private void showMainContent(ViewControlMessage vcm) {
        String strClass = vcm.getViewClassName();
        if (null != currentMainWindow) {
            currentMainWindow.contentActivated(false);
        }
        contentUI.showContentObject(strClass);
        int contentIndex = -1;
        for (int i = 0; i < contents.length; i++) {
            if (strClass.equals(contents[i].getClass().getName())) {
                contentIndex = i;
                break;
            }
        }
        controlUI.addContentControls(((MainWindowContent) contents[contentIndex]).getContentControls(), contents[contentIndex]);
        currentMainWindow = (MainWindowContent) contents[contentIndex];
        currentMainWindow.contentActivated(true);
        controlUI.enableContentControls(bModerator);
        controlUI.setSelectedViewButton(strClass);
    }

    private void handleServerIDAssignment(String strServerID) {
        pi.setServerID(strServerID);
        if (bIsPlayback) {
            pi.setConfID(pi.getConfID() + "_" + strServerID);
        }
        router.setParticipantInfo(pi);
        if (("moderator".equals(pi.getRole()) || bIsPlayback) && bReconnecting != true) {
            openMeeting();
            bOwner = true;
        } else {
            logon();
            bReconnecting = false;
        }
    }

    private void openMeeting() {
        if (bIsPlayback) {
            router.sendMessage(new OpenMeetingMessagePlayback(getParameter("customerID"), pi.getConfID(), getParameter("meetingKey")));
        } else if (null != getParameter("hasAccount") && (!("null".equals(getParameter("hasAccount")))) && ("true".equals(getParameter("hasAccount")))) {
            router.sendMessage(new OpenMeetingMessageAccount(getParameter("customerID"), pi.getConfID(), getParameter("meetingKey")));
        } else {
            router.sendMessage(new OpenMeetingMessageCC(getParameter("customerID"), pi.getConfID(), getParameter("meetingKey"), getParameter("cc_number"), getParameter("cc_expiration")));
        }
    }

    private void logon() {
        if (bIsPlayback) {
            router.sendMessage(new RosterJoinPlaybackMessage(getParameter("participantID"), buildJavaRuntimeInfo()));
        } else {
            router.sendMessage(new RosterJoinMessage(getParameter("participantID"), buildJavaRuntimeInfo()));
        }
    }

    private JavaRuntimeInfo buildJavaRuntimeInfo() {
        return new JavaRuntimeInfo(System.getProperty("java.vendor"), System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
    }

    private String getParameter(String str) {
        return applet.getParameter(str);
    }

    private void initStatus() {
        if (this.bIsPlayback) {
            System.out.println("adding LED");
            led = new Led(Color.black, new Color(181, 189, 165), new Color(241, 255, 231), new Font("Dialog", Font.PLAIN, 10));
            led.setSize(10);
            this.lTotalTime = getRecordedSessionLength();
            led.setElapsedTime(0);
            led.setTotalTime(this.lTotalTime);
            statusUI.addStatusPanel(STATUS_TIME, led, 0);
        }
        StatusInfo[] asi = getStatusInfos();
        for (int i = 0; i < asi.length; i++) {
            statusUI.addStatusInfo(asi[i]);
        }
        ((Component) statusUI).repaint();
    }

    private long getRecordedSessionLength() {
        long lRet = -1;
        String strLength = this.applet.getParameter(Constants.PLAYBACK_MEETING_LENGTH_PARAM);
        if (null != strLength) {
            lRet = (new Long(strLength)).longValue();
        } else {
            Properties recProps = new Properties();
            try {
                URL urlProps = new URL(applet.getDocumentBase(), Constants.RECORDED_SESSION_INFO_PROPERTIES);
                recProps.load(urlProps.openStream());
                lRet = (new Long(recProps.getProperty(Constants.PLAYBACK_MEETING_LENGTH_PARAM))).longValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return lRet;
    }

    private void addUserSpecificButtons(ControlList[] controlLists, Object objHandler) {
        for (int i = 0; i < controlLists.length; i++) {
            addUserSpecificButtons(controlLists[i], objHandler);
        }
    }

    private void addMeetingControls(ControlList controlList, Object objHandler) {
        controlUI.addMeetingControls(controlList, objHandler);
    }

    private void addUserSpecificButtons(ControlList controlList, Object objHandler) {
        controlUI.addUserSpecificControls(controlList, objHandler);
    }

    private void exitMeeting(EndMeetingMessage mess) {
        this.bMeetingOver = true;
        this.stop();
        String strDonePage = mess.getDonePage();
        if (bIsPlayback) {
            strDonePage = "donepage.html";
        }
        try {
            applet.getAppletContext().showDocument(new URL(strDonePage));
        } catch (MalformedURLException mue) {
            System.out.println("Could not navigate to: " + strDonePage + ". Trying as relative link...");
        }
        try {
            applet.getAppletContext().showDocument(new URL(applet.getDocumentBase(), strDonePage));
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        }
    }

    public void exitMeeting() {
        this.exitMeeting(new EndMeetingMessage());
    }

    private void browseAway() {
        this.bMeetingOver = true;
        this.stop();
    }

    private static void centerWindowOn(Component compRef, Window window) {
        Point pointOrigin = compRef.getLocationOnScreen();
        Point pointCenter = new Point(((compRef.getBounds().width) / 2) + pointOrigin.x, ((compRef.getBounds().height) / 2) + pointOrigin.y);
        Point pointNew = new Point(Math.max(0, pointCenter.x - ((window.getBounds().width) / 2)), Math.max(0, pointCenter.y - ((window.getBounds().height) / 2)));
        window.setLocation(pointNew);
    }

    private final void getJNIFiles() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DigestInputStream dis = new DigestInputStream(new URL(strAppletDir + "/" + JNI_ARCHIVE).openStream(), MessageDigest.getInstance("MD5"));
        IOUtil.copyStream(dis, baos);
        byte[] baResult = dis.getMessageDigest().digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < baResult.length; i++) {
            if ((0xff & baResult[i]) < 0x10) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(0xff & baResult[i]));
        }
        if (!sb.toString().equals(this.JNI_ARCHIVE_MD5)) {
            throw new Exception("Checksum failed on jni.");
        }
        String strLocalPath = System.getProperty("com.ms.sysdir");
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
        ZipEntry ze = null;
        while (null != (ze = zis.getNextEntry())) {
            FileOutputStream fos = new FileOutputStream(strLocalPath + "\\" + ze.getName());
            IOUtil.copyStream(zis, fos);
        }
    }

    private ControlInfo[] getMeetingCommands() {
        ControlInfo[] ci = null;
        boolean bRecorded = this.getParameter("recorded") != null && this.getParameter("recorded").equalsIgnoreCase("true");
        if (bRecorded) {
            ci = new ControlInfo[this.aciMeetingCommands.length + this.aciRecordingCommands.length];
        } else {
            ci = new ControlInfo[this.aciMeetingCommands.length];
        }
        System.arraycopy(this.aciMeetingCommands, 0, ci, 0, this.aciMeetingCommands.length);
        if (bRecorded) {
            System.arraycopy(this.aciRecordingCommands, 0, ci, this.aciMeetingCommands.length, this.aciRecordingCommands.length);
        }
        return ci;
    }

    private URL buildMailtoInviteUrl() throws MalformedURLException, URISyntaxException {
        String body = "Click%20here%20to%20join%20the%20meeting:%20%0d%0a" + buildInviteLink(true);
        String query = "mailto:&subject=Join%20me%20for%20an%20online%20meeting&body=" + body;
        return new URL(query);
    }

    private String buildInviteLink(boolean mailtoSafe) {
        return this.strAppletDir + "/j.do" + (mailtoSafe ? "%3f" : "?") + "hem=" + getHostEmail() + (mailtoSafe ? "%26" : "&") + "pw=" + getMeetingPassword();
    }

    private StatusInfo[] getStatusInfos() {
        StatusInfo[] asi = null;
        if (this.bIsPlayback) {
            asi = this.asiAll;
        } else {
            asi = new StatusInfo[this.asiAll.length + this.asiLiveOnly.length];
            System.arraycopy(this.asiLiveOnly, 0, asi, 0, this.asiLiveOnly.length);
            System.arraycopy(this.asiAll, 0, asi, this.asiLiveOnly.length, this.asiAll.length);
        }
        return asi;
    }

    private String getHostEmail() {
        return this.meetingHostEmail;
    }

    private String getMeetingPassword() {
        return this.meetingPassword;
    }

    private static final ControlInfo[] aciCommands = { new ButtonInfo("/images/make_mod.gif", "refl_makeMod", "i18n.makeMod.tt"), new ButtonInfo("/images/invite_people.gif", "refl_invitePeople", "i18n.invitePeople.tt"), new ButtonInfo("/images/eject.gif", "refl_eject", "i18n.eject.tt") };

    private static final ControlInfo[] aciMeetingCommands = { new ButtonInfo("/images/end_meeting.gif", "refl_endMeeting", "i18n.endMeeting.tt"), new ButtonInfo("/images/lock_meeting.gif", "refl_lockMeeting", "i18n.lockMeeting.tt") };

    private static final ControlInfo[] aciRecordingCommands = { new ButtonInfo("/images/record.gif", "refl_startStopRecording", "i18n.startStopRecording.tt") };

    private static final String JNI_ARCHIVE = "jni.zip";

    private static final String JNI_ARCHIVE_MD5 = "@jni.zip.md5@";

    private static final String MODERATOR_ICON = "/images/make_mod.gif";

    private static final String STATUS_MODERATOR = "mod";

    private static final String STATUS_RECORDED = "rec";

    private static final String STATUS_CONNECTION = "con";

    private static final String STATUS_SECURITY = "sec";

    private static final String STATUS_TIME = "time";

    private static final String STATUS_LOCKED = "lock";

    private static final String MODERATOR_ACTIVE = "/images/moderator_active.gif";

    private static final String MODERATOR_INACTIVE = "/images/moderator_inactive.gif";

    private static final String RECORDED_ACTIVE = "/images/rec_on.gif";

    private static final String RECORDED_INACTIVE = "/images/rec_off.gif";

    private static final String CONNECTION_ACTIVE = "/images/connection_active.gif";

    private static final String CONNECTION_INACTIVE = "/images/connection_inactive.gif";

    private static final String SECURITY_ACTIVE = "/images/security_active.gif";

    private static final String SECURITY_INACTIVE = "/images/security_inactive.gif";

    private static final String MEETING_LOCK_ACTIVE = "/images/lock_active.gif";

    private static final String MEETING_LOCK_INACTIVE = "/images/lock_inactive.gif";

    private static final StatusInfo STATUS_INFO_MODERATOR = new StatusInfo(STATUS_MODERATOR, MODERATOR_ACTIVE, MODERATOR_INACTIVE);

    private static final StatusInfo STATUS_INFO_RECORDED = new StatusInfo(STATUS_RECORDED, RECORDED_ACTIVE, RECORDED_INACTIVE);

    private static final StatusInfo STATUS_INFO_CONNECTION = new StatusInfo(STATUS_CONNECTION, CONNECTION_ACTIVE, CONNECTION_INACTIVE);

    private static final StatusInfo STATUS_INFO_SECURITY = new StatusInfo(STATUS_SECURITY, SECURITY_ACTIVE, SECURITY_INACTIVE);

    private static final StatusInfo STATUS_INFO_LOCK = new StatusInfo(STATUS_LOCKED, MEETING_LOCK_ACTIVE, MEETING_LOCK_INACTIVE);

    private static final StatusInfo[] asiLiveOnly = { STATUS_INFO_MODERATOR, STATUS_INFO_RECORDED, STATUS_INFO_LOCK };

    private static final StatusInfo[] asiAll = { STATUS_INFO_CONNECTION, STATUS_INFO_SECURITY };

    private boolean bReconnecting;

    MeetingScript script;

    private Content[] contents;

    private ControlUI controlUI;

    private StatusUI statusUI;

    private ContentUI contentUI;

    private MessageRouter router;

    private Applet applet;

    private ParticipantInfo pi;

    private String strAppletDir;

    private boolean bIsPlayback;

    private int iReconnectCount;

    private static final int MAX_RECONNECTS = 50;

    private MainWindowContent currentMainWindow;

    private boolean bModerator;

    private boolean bOwner;

    private ParticipantInfo piModerator;

    private ParticipantInfo[] apiRoster;

    private Hashtable hashParticipants = new Hashtable();

    private Hashtable hashControlWindowContents = new Hashtable();

    private ResourceBundle bundle;

    private Led led;

    private boolean bMeetingOver = false;

    private long lTotalTime = 0L;

    private SimpleLog simpleLog = new SimpleLog(getClass().getName());

    private boolean invitePeopleUIShown;

    private String meetingHostEmail;

    private String meetingPassword;

    private boolean startupListenersCalled;
}
