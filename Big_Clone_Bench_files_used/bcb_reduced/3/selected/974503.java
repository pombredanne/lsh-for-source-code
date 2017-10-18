package net.jxta.myjxta.plugins.filetransfer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import net.jxta.document.AdvertisementFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.myjxta.plugin.IPluginNotificationHandler;
import net.jxta.myjxta.plugin.ISelectableNode;
import net.jxta.myjxta.plugin.PluginBase;
import net.jxta.myjxta.plugin.PluginContainer;
import net.jxta.myjxta.util.Group;
import net.jxta.myjxta.util.GroupNode;
import net.jxta.myjxta.util.JxtaNode;
import net.jxta.myjxta.util.PeerNode;
import net.jxta.myjxta.util.Resources;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;
import net.sf.p2pim.BuddyListView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author levin
 * @since 2007-11-14 下午09:11:34
 * 将对话框从swing改为swt
 */
public class FileTransferPlugin extends PluginBase implements IPluginNotificationHandler, PluginContainer.IPopupProvider {

    HashMap<PeerGroup, JxtaServerSocket> groupsWithListeners = new HashMap<PeerGroup, JxtaServerSocket>();

    private static final String G_TRANSFER_ID = "TRANS";

    private ArrayList<Group> m_joinedGroups = new ArrayList<Group>();

    private HashMap<Group, FileReceiver> group2Receiver = new HashMap<Group, FileReceiver>();

    public FileTransferPlugin() throws NoSuchAlgorithmException {
        super();
        setName("File Transfer");
    }

    public void init(PluginContainer c) {
        super.init(c);
        start();
    }

    public void start() {
        super.start();
        m_container.registerPopupProvider(this);
        for (Group group : m_joinedGroups) {
            startListener(group);
        }
    }

    public void stop() {
        m_container.removePopupProvider(this);
        for (Group group : m_joinedGroups) {
            stopListener(group);
        }
        super.stop();
    }

    public void destroy() {
        if (m_running) {
            stop();
        }
        super.destroy();
    }

    private void stopListener(Group p_group) {
        FileReceiver receiver = group2Receiver.get(p_group);
        receiver.shutdownListener();
    }

    private void startListener(Group p_group) {
        try {
            PipeAdvertisement pipeAdvertisement = getAdvertisment(p_group.getPeerGroup(), p_group.getOwnPeersCommandId() + G_TRANSFER_ID);
            try {
                FileReceiver listener = new FileReceiver(p_group.getPeerGroup(), (PipeID) pipeAdvertisement.getPipeID()) {

                    File file = null;

                    protected File getTargetFile(final String fileName, final long fileSize, final String sender) {
                        Display.getDefault().syncExec(new Runnable() {

                            public void run() {
                                final FileDialog fd = new FileDialog(BuddyListView.getShell(), SWT.SAVE);
                                fd.setText("收到来自 " + sender + " 的文件，请选择保存位置");
                                fd.setFileName(fileName);
                                String openFile = fd.open();
                                if (openFile != null) file = new File(openFile);
                            }
                        });
                        return file;
                    }
                };
                group2Receiver.put(p_group, listener);
                listener.start();
            } catch (PeerGroupException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PipeAdvertisement getAdvertisment(PeerGroup pg, String generatorSeed) {
        PipeAdvertisement pa = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        try {
            pa.setPipeID(IDFactory.newPipeID(pg.getPeerGroupID(), createMD5(generatorSeed)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        pa.setName(generatorSeed);
        pa.setType(PipeAdvertisement.getAdvertisementType());
        return pa;
    }

    private static byte[] createMD5(String seed) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(seed.getBytes("UTF-8"));
        return md5.digest();
    }

    public IPluginNotificationHandler getPluginNotificationHander() {
        return this;
    }

    public void groupJoined(Group p_group) {
        if (!p_group.isVisible()) return;
        m_joinedGroups.add(p_group);
        if (isRunning()) {
            startListener(p_group);
        }
    }

    public void groupResigned(Group p_group) {
        if (!p_group.isVisible()) return;
        m_joinedGroups.remove(p_group);
        if (isRunning()) {
            stopListener(p_group);
        }
    }

    public void groupStateChanged(Group p_group) {
    }

    public void popupRequested(PluginContainer.IPopupGenerator popupGenerator, ISelectableNode[] selectedNodes, MouseEvent triggerEvent) {
        if (selectedNodes != null && selectedNodes.length >= 1) {
            JxtaNode jxtaNode = selectedNodes[0].getJxtaNode();
            if (jxtaNode instanceof PeerNode) {
                PeerNode peerNode = (PeerNode) jxtaNode;
                PluginContainer.MenuPath peerPath = new PluginContainer.MenuPath(Resources.getStrings().getString("menu.peer"), KeyEvent.VK_P);
                PipeID p_targetCommandPipeID = (PipeID) peerNode.getPeer().getPipeAdvertisement().getID();
                PeerGroup pg = ((GroupNode) peerNode.getParent()).getGroup().getPeerGroup();
                PipeAdvertisement socketAdv = getAdvertisment(pg, p_targetCommandPipeID + G_TRANSFER_ID);
                popupGenerator.addPopup(new PluginContainer.MenuPath[] { peerPath }, 9, new BandwidthTestAction("Send File", pg, (PipeID) socketAdv.getPipeID()));
            }
        }
    }

    private class BandwidthTestAction extends AbstractAction implements PropertyChangeListener {

        private final PipeID targetPipeID;

        private PeerGroup peerGroup;

        public BandwidthTestAction(String name, PeerGroup pg, PipeID p_message) {
            super(name);
            peerGroup = pg;
            targetPipeID = p_message;
        }

        private File getFile() {
            JFileChooser chooser = new JFileChooser();
            chooser.showOpenDialog((Component) m_container.getMyJxta().getView());
            return chooser.getSelectedFile();
        }

        public void actionPerformed(ActionEvent e) {
            File file = getFile();
            if (file == null || !file.exists() || !file.canRead()) return;
            FileSender tmp = null;
            try {
                tmp = new FileSender(peerGroup, targetPipeID, file);
                tmp.addPropertyChangeListener(this);
                tmp.start();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            System.out.println(evt.getPropertyName() + ":" + evt.getNewValue());
        }
    }
}
