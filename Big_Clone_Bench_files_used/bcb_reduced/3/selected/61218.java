package Picasa2flickr;

import java.awt.*;
import java.awt.datatransfer.*;
import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photosets.Photoset;

/**
 * Picasa2flickr applet
 * @author gael
 */
public class Picasa2flickr extends Applet implements ClipboardOwner, ActionListener, ItemListener, Runnable {

    private static final long serialVersionUID = 1L;

    public static char[] hexaNibble = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private int nbPhotos = 0;

    private String[] photoTitles = null;

    private URL[] photoURL = null;

    private String[] photoDesc = null;

    private User currentUser = null;

    private FlickrUploader uploader = null;

    private Collection currentPhotosets = null;

    private boolean uploading = false;

    private String lastError = null;

    private Thread uploadThread = null;

    private URL authURL = null;

    private Label infoLabel = null;

    private Label statusLabel = null;

    private Label statusLabel2 = null;

    private Label newsetLabel = null;

    private Button actionButton = null;

    private Button authButton = null;

    private TextField tagsField = null;

    private TextField newsetField = null;

    private Choice photosetChoice = null;

    private Choice resizeChoice = null;

    private Choice privacyChoice = null;

    private Checkbox md5Check = null;

    private Checkbox filenameCheck = null;

    private Checkbox existCheck = null;

    private Checkbox removeExtCheck = null;

    /** 
     * Get the applet information string
     * @return Information string
     */
    public String getAppletInfo() {
        String ret = "picasa2flickr applet";
        try {
            InputStream in = getClass().getResourceAsStream("/applet.properties");
            Properties properties = new Properties();
            properties.load(in);
            ret = "picasa2flickr applet v" + properties.getProperty("applet.version") + " see " + properties.getProperty("applet.website");
            in.close();
        } catch (Exception e) {
        }
        return ret;
    }

    /** 
     * Applet initialization
     */
    public void init() {
        System.out.println(this.getAppletInfo());
        this.uploading = false;
        try {
            if (getParameter("nbPhotos") != null) this.nbPhotos = Integer.parseInt(getParameter("nbPhotos"));
            if (this.nbPhotos > 0) {
                this.photoTitles = new String[nbPhotos];
                this.photoURL = new URL[nbPhotos];
                this.photoDesc = new String[nbPhotos];
                for (int i = 0; i < nbPhotos; i++) {
                    this.photoTitles[i] = getParameter("photoTitle_" + i);
                    this.photoURL[i] = new URL(getParameter("photoURL_" + i));
                    this.photoDesc[i] = getParameter("photoDesc_" + i);
                }
            } else {
                this.nbPhotos = 2;
                this.photoTitles = new String[2];
                this.photoURL = new URL[2];
                this.photoDesc = new String[2];
                this.photoTitles[0] = "view from clavel";
                this.photoURL[0] = new URL("http://picasa2flickr.4now.net/test_pic/ClavelBalcon_1024px.jpg");
                this.photoDesc[0] = "depuis le balcon";
                this.photoTitles[1] = "Mont St-Michel";
                this.photoURL[1] = new URL("http://picasa2flickr.4now.net/test_pic/Mont_St-Michel_1024px.jpg");
                this.photoDesc[1] = "yes this is the mont !";
            }
            this.setLayout(null);
            this.setFont(new Font("Helvetica", Font.PLAIN, 12));
            this.infoLabel = new Label("", Label.LEFT);
            this.infoLabel.setSize(450, 20);
            this.add(this.infoLabel);
            this.authButton = new Button("Authorize");
            this.authButton.addActionListener(this);
            this.authButton.setBounds(55, 25, 85, 25);
            this.add(this.authButton);
            this.actionButton = new Button("Continue");
            this.actionButton.addActionListener(this);
            this.actionButton.setBounds(155, 25, 80, 25);
            this.add(this.actionButton);
            this.statusLabel = new Label("", Label.LEFT);
            this.statusLabel.setForeground(Color.red);
            this.statusLabel.setBounds(0, 215, 400, 20);
            this.add(this.statusLabel);
            this.statusLabel2 = new Label("", Label.LEFT);
            this.statusLabel2.setForeground(Color.red);
            this.statusLabel2.setBounds(0, 235, 400, 20);
            this.add(this.statusLabel2);
            Label tagsLabel = new Label("Tags:");
            tagsLabel.setBounds(0, 65, 50, 20);
            tagsLabel.setForeground(Color.blue);
            this.add(tagsLabel);
            this.tagsField = new TextField("", 80);
            this.tagsField.setBounds(55, 65, 200, 20);
            this.add(this.tagsField);
            this.md5Check = new Checkbox("MD5");
            this.md5Check.setBounds(260, 65, 40, 20);
            this.md5Check.setForeground(Color.blue);
            this.md5Check.addItemListener(this);
            this.md5Check.setFont(new Font("Helvetica", Font.PLAIN, 10));
            this.add(this.md5Check);
            this.filenameCheck = new Checkbox("File");
            this.filenameCheck.setBounds(305, 65, 40, 20);
            this.filenameCheck.setForeground(Color.blue);
            this.filenameCheck.setFont(new Font("Helvetica", Font.PLAIN, 10));
            this.add(this.filenameCheck);
            this.existCheck = new Checkbox("Skip (beta)");
            this.existCheck.setBounds(344, 65, 140, 20);
            this.existCheck.setForeground(Color.blue);
            this.existCheck.setEnabled(this.md5Check.getState());
            this.existCheck.setState(false);
            this.existCheck.setFont(new Font("Helvetica", Font.PLAIN, 10));
            this.add(this.existCheck);
            this.removeExtCheck = new Checkbox("Remove file extension");
            this.removeExtCheck.setBounds(260, 95, 140, 20);
            this.removeExtCheck.setForeground(Color.blue);
            this.removeExtCheck.setState(true);
            this.removeExtCheck.setFont(new Font("Helvetica", Font.PLAIN, 10));
            this.add(this.removeExtCheck);
            Label albumLabel = new Label("Set:");
            albumLabel.setBounds(0, 95, 50, 20);
            albumLabel.setForeground(Color.blue);
            this.add(albumLabel);
            this.photosetChoice = new Choice();
            this.photosetChoice.addItem("---");
            this.photosetChoice.addItem("New Set");
            this.photosetChoice.setBounds(55, 95, 200, 20);
            this.photosetChoice.addItemListener(this);
            this.add(this.photosetChoice);
            this.newsetLabel = new Label("New Set:");
            this.newsetLabel.setBounds(0, 125, 50, 20);
            this.newsetLabel.setForeground(Color.gray);
            this.add(newsetLabel);
            this.newsetField = new TextField("", 80);
            this.newsetField.setBounds(55, 125, 250, 20);
            this.newsetField.setEnabled(false);
            this.add(this.newsetField);
            Label privacyLabel = new Label("Privacy:");
            privacyLabel.setBounds(0, 155, 50, 20);
            privacyLabel.setForeground(Color.blue);
            this.add(privacyLabel);
            this.privacyChoice = new Choice();
            this.privacyChoice.addItem("Anyone (Public)");
            this.privacyChoice.addItem("Your Friends");
            this.privacyChoice.addItem("Your Family");
            this.privacyChoice.addItem("Your Friends & Family");
            this.privacyChoice.addItem("Only You");
            this.privacyChoice.setBounds(55, 155, 150, 20);
            this.add(this.privacyChoice);
            Label resizeLabel = new Label("Resize:");
            resizeLabel.setBounds(0, 185, 50, 20);
            resizeLabel.setForeground(Color.blue);
            this.add(resizeLabel);
            this.resizeChoice = new Choice();
            this.resizeChoice.addItem("No resize");
            this.resizeChoice.addItem("640 pixels");
            this.resizeChoice.addItem("800 pixels");
            this.resizeChoice.addItem("1024 pixels");
            this.resizeChoice.addItem("1200 pixels");
            this.resizeChoice.addItem("1600 pixels");
            this.resizeChoice.setBounds(55, 185, 90, 20);
            this.add(this.resizeChoice);
            Label verLabel = new Label(this.getAppletInfo());
            verLabel.setBounds(0, 255, 400, 20);
            verLabel.setForeground(Color.gray);
            this.add(verLabel);
            File authFile = new File(System.getProperty("user.home") + File.separatorChar + ".picasa2flickrAuth");
            this.uploader = new FlickrUploader("b1162fd4a86afde3c29123cd6757f695", "aad19e582679f323", authFile);
            this.currentUser = uploader.getDefaultAuthenticatedUser();
            this.updateStatus();
            if (this.currentUser == null) {
                this.authURL = this.uploader.getAuthURL();
                System.out.println(this.authURL.toString());
                Clipboard clipboard = getToolkit().getSystemClipboard();
                StringSelection fieldContent = new StringSelection(this.authURL.toString());
                clipboard.setContents(fieldContent, Picasa2flickr.this);
                this.getAppletContext().showDocument(this.authURL, "_blank");
            }
        } catch (Exception e) {
            this.lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
    }

    /**
   * Lost Clipboard ownership
   */
    public void lostOwnership(Clipboard parClipboard, Transferable parTransferable) {
    }

    /** 
   * Paint method
   */
    public void paint(Graphics screen) {
    }

    /**
   * Enable, or not, gui controls
   * @param enable 
   */
    public void enableControl(boolean enable) {
        this.tagsField.setEnabled(enable);
        this.newsetField.setEnabled(enable);
        this.photosetChoice.setEnabled(enable);
        this.privacyChoice.setEnabled(enable);
        this.resizeChoice.setEnabled(enable);
        this.md5Check.setEnabled(enable);
        this.filenameCheck.setEnabled(enable);
        this.removeExtCheck.setEnabled(enable);
        this.authButton.setEnabled(enable);
        this.actionButton.setEnabled(enable);
    }

    /**
     * Update status & control according to the current state
     */
    public void updateStatus() {
        try {
            if (this.currentUser != null) {
                if (!this.uploading && this.nbPhotos > 0) {
                    this.infoLabel.setText("Ready to upload " + this.nbPhotos + " photo(s) to '" + this.currentUser.getUsername() + "' account");
                    this.actionButton.setLabel("Start Upload");
                    this.authButton.setLabel("Change user");
                    this.enableControl(true);
                    this.photosetChoice.removeAll();
                    this.photosetChoice.addItem("---");
                    this.photosetChoice.addItem("New Set");
                    this.currentPhotosets = this.uploader.retrivePhotosetsList(this.currentUser);
                    for (Iterator it = this.currentPhotosets.iterator(); it.hasNext(); ) {
                        Object element = it.next();
                        this.photosetChoice.addItem(((Photoset) element).getTitle());
                    }
                } else {
                    this.enableControl(false);
                    if (this.nbPhotos > 0) {
                        this.infoLabel.setText("Uploading ....");
                    } else {
                    }
                }
            } else {
                this.infoLabel.setText("Once you've authorized picasa2flickr, click on 'Continue'");
                this.statusLabel.setText("If no pop-up appears you can manually past the URL into your browser");
                this.statusLabel.setForeground(Color.red);
                this.statusLabel2.setText("NOTE: URL have already been copied into the clipboard");
                this.statusLabel2.setForeground(Color.red);
                this.authButton.setLabel("Authorize");
                this.actionButton.setLabel("Continue");
                this.enableControl(false);
                this.actionButton.setEnabled(true);
                this.authButton.setEnabled(true);
            }
        } catch (Exception e) {
            this.lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
        if (this.lastError != null && this.lastError.length() > 0) {
            this.statusLabel.setForeground(Color.red);
            this.statusLabel.setText("Error: " + this.lastError);
        } else if (this.currentUser != null) {
            int bw_used = 0;
            float bw_limit = this.currentUser.getBandwidthMax();
            if (bw_limit > 0) bw_used = (int) (0.5 + (float) this.currentUser.getBandwidthUsed() / bw_limit);
            this.statusLabel.setForeground(Color.gray);
            MessageFormat form = new MessageFormat("You''ve uploaded {0}% of your limit");
            Object[] args = { new Integer(bw_used) };
            this.statusLabel.setText(form.format(args));
            this.statusLabel2.setText("");
        }
    }

    /**
     * Handle choice changes
     */
    public void itemStateChanged(ItemEvent e) {
        if (this.photosetChoice.getSelectedIndex() == 1) {
            this.newsetField.setEnabled(true);
            this.newsetLabel.setForeground(Color.blue);
        } else {
            this.newsetField.setEnabled(false);
            this.newsetLabel.setForeground(Color.gray);
        }
    }

    /**
     * Handle button clicks
     */
    public void actionPerformed(ActionEvent act) {
        this.lastError = null;
        try {
            if (act.getSource() == this.actionButton) {
                if (this.nbPhotos < 1) {
                } else if (this.currentUser == null) {
                    this.currentUser = uploader.getDefaultAuthenticatedUser();
                    if (this.currentUser == null) {
                        this.uploader.storeToken();
                        this.currentUser = uploader.getDefaultAuthenticatedUser();
                    }
                } else {
                    if (!this.uploading) {
                        this.uploading = true;
                        this.uploadThread = new Thread(this);
                        this.uploadThread.start();
                    }
                }
            } else if (act.getSource() == this.authButton) {
                this.currentUser = null;
                this.uploader.clearAuthStore();
                this.authURL = this.uploader.getAuthURL();
                System.out.println(this.authURL.toString());
                Clipboard clipboard = getToolkit().getSystemClipboard();
                StringSelection fieldContent = new StringSelection(this.authURL.toString());
                clipboard.setContents(fieldContent, Picasa2flickr.this);
                this.getAppletContext().showDocument(this.authURL, "_blank");
            }
        } catch (Exception e) {
            this.lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
        this.updateStatus();
        this.repaint();
    }

    /**
     * Uploading thread
     */
    public void run() {
        this.lastError = null;
        try {
            String photosetID = null;
            String newset_name = null;
            boolean publicFlag, familyFlag, friendFlag;
            int idx = this.privacyChoice.getSelectedIndex();
            switch(idx) {
                case 1:
                    publicFlag = familyFlag = false;
                    friendFlag = true;
                    break;
                case 2:
                    publicFlag = friendFlag = false;
                    familyFlag = true;
                    break;
                case 3:
                    publicFlag = false;
                    familyFlag = friendFlag = true;
                    break;
                case 4:
                    publicFlag = familyFlag = friendFlag = false;
                    break;
                default:
                    publicFlag = familyFlag = friendFlag = true;
                    break;
            }
            idx = this.photosetChoice.getSelectedIndex();
            if (idx > 1) {
                idx--;
                for (Iterator it = this.currentPhotosets.iterator(); idx > 0 && it.hasNext(); ) {
                    Object element = it.next();
                    idx--;
                    if (idx == 0) photosetID = ((Photoset) element).getId();
                }
            } else if (idx == 1) {
                newset_name = this.newsetField.getText().trim();
                if (newset_name.length() > 0) {
                    photosetID = "CREATEME";
                }
            }
            String url_suffix = "";
            switch(this.resizeChoice.getSelectedIndex()) {
                case 1:
                    url_suffix = "?size=640";
                    break;
                case 2:
                    url_suffix = "?size=800";
                    break;
                case 3:
                    url_suffix = "?size=1024";
                    break;
                case 4:
                    url_suffix = "?size=1200";
                    break;
                case 5:
                    url_suffix = "?size=1600";
                    break;
                default:
                    url_suffix = "?size=0";
            }
            List tags = new ArrayList();
            Pattern pattern = Pattern.compile("(?:([^\\s\"]\\S*)|(?:\"((?:i|[^i])*?)(?:(?:\"\\s)|(?:\"$)|$)))");
            Matcher matcher = pattern.matcher(this.tagsField.getText());
            while (matcher.find()) {
                tags.add('"' + matcher.group(matcher.group(1) == null ? 2 : 1) + '"');
            }
            int md5Idx = tags.size();
            int fnIdx = md5Idx;
            byte[] buf = new byte[1024];
            MessageFormat messageForm = new MessageFormat("Uploading photo #{0} of {1} {6,choice,0#|0<({6} skipped)}... {2}% {3,choice,0#|0<(eta: {4} min {5} sec)}");
            Object[] messageArgs = { new Integer(1), new Integer(this.nbPhotos), new Integer(0), new Long(0), new Integer(0), new Integer(0), new Integer(0) };
            boolean computeMD5 = this.md5Check.getState();
            boolean existMD5 = this.existCheck.getState();
            boolean saveFilename = this.filenameCheck.getState();
            if (computeMD5) {
                tags.add("picasa2flickr:md5=none");
                fnIdx++;
            }
            if (saveFilename) tags.add("picasa2flickr:filename=none");
            long eta = 0;
            long uploadTotal = 0;
            long uploadCurrent = 0;
            if (this.resizeChoice.getSelectedIndex() == 0) {
                for (int i = 0; i < this.nbPhotos; i++) {
                    URL photoURL = new URL(this.photoURL[i].toString() + url_suffix);
                    URLConnection urlConn = photoURL.openConnection();
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);
                    DataInputStream dis = new DataInputStream(urlConn.getInputStream());
                    uploadTotal += urlConn.getContentLength();
                    dis.close();
                }
            }
            long startTime = System.currentTimeMillis();
            int skipedUpload = 0;
            for (int i = 0; i < this.nbPhotos; i++) {
                URL photoURL = new URL(this.photoURL[i].toString() + url_suffix);
                URLConnection urlConn = photoURL.openConnection();
                boolean skip = false;
                urlConn.setDoInput(true);
                urlConn.setUseCaches(false);
                DataInputStream dis = new DataInputStream(urlConn.getInputStream());
                if (computeMD5) {
                    MessageDigest hash = MessageDigest.getInstance("MD5");
                    int nbRead = 0;
                    while ((nbRead = dis.read(buf)) != -1) {
                        hash.update(buf, 0, nbRead);
                    }
                    dis.close();
                    String md5Str = "picasa2flickr:md5=" + toHexString(hash.digest(), 0, 0);
                    tags.set(md5Idx, md5Str);
                    urlConn = photoURL.openConnection();
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);
                    dis = new DataInputStream(urlConn.getInputStream());
                    if (existMD5) {
                        if (this.uploader.isPhotoExist(md5Str, this.currentUser)) {
                            System.out.println("Photo " + this.photoTitles[i] + " skipped");
                            skip = true;
                            skipedUpload += 1;
                        }
                    }
                }
                if (saveFilename) {
                    tags.set(fnIdx, "picasa2flickr:filename=\"" + this.photoTitles[i] + "\"");
                }
                messageArgs[0] = new Integer(i + 1);
                messageArgs[2] = new Integer((int) (0.5 + 100. * (float) i / this.nbPhotos));
                messageArgs[3] = new Long(eta);
                messageArgs[4] = new Integer((int) (eta / 60));
                messageArgs[5] = new Integer((int) (eta % 60));
                messageArgs[6] = new Integer(skipedUpload);
                this.infoLabel.setText(messageForm.format(messageArgs));
                this.infoLabel.repaint();
                Thread.yield();
                String photoID = null;
                if (!skip) {
                    uploadCurrent += urlConn.getContentLength();
                    if (this.removeExtCheck.getState() && this.photoTitles[i].lastIndexOf('.') != -1) {
                        this.photoTitles[i] = this.photoTitles[i].substring(0, this.photoTitles[i].lastIndexOf('.'));
                    }
                    photoID = this.uploader.uploadPhoto(this.currentUser, dis, this.photoTitles[i], this.photoDesc[i], tags, publicFlag, familyFlag, friendFlag);
                } else {
                    uploadTotal -= urlConn.getContentLength();
                }
                long duration = System.currentTimeMillis() - startTime;
                if (uploadTotal > 0 && uploadCurrent > 0) {
                    eta = ((uploadTotal - uploadCurrent) * duration) / (1000 * uploadCurrent);
                }
                dis.close();
                if (photoID != null && photosetID != null) {
                    if (photosetID == "CREATEME") {
                        photosetID = null;
                        try {
                            photosetID = this.uploader.createPhotoset(this.currentUser, newset_name, photoID);
                        } catch (Exception e) {
                            Collection sets = this.uploader.retrivePhotosetsList(this.currentUser);
                            if (!sets.isEmpty()) {
                                Photoset pset = (Photoset) (sets.iterator().next());
                                if (pset.getTitle().equalsIgnoreCase(newset_name)) {
                                    photosetID = pset.getId();
                                }
                            }
                        }
                    } else {
                        this.uploader.addPhotoToPhotoset(this.currentUser, photoID, photosetID);
                    }
                }
            }
            this.infoLabel.setText((this.nbPhotos - skipedUpload) + " photo(s) uploaded (" + skipedUpload + " skipped). You can close the window using the link below.");
            this.nbPhotos = 0;
        } catch (Exception e) {
            this.lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
        this.uploading = false;
        this.updateStatus();
    }

    /** 
     * Converts a byte array to an hexa string
     * @param block input block
     * @param ofs offset
     * @param len length
     * @return hexa string
     */
    private String toHexString(byte[] block, int ofs, int len) {
        StringBuffer buf = new StringBuffer();
        if (len < 1) len = block.length;
        for (int i = ofs; i < ofs + len; i++) {
            buf.append(hexaNibble[(block[i] >>> 4) & 15]);
            buf.append(hexaNibble[block[i] & 15]);
        }
        return buf.toString();
    }
}
