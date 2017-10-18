import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import net.sf.compositor.AppMac;
import net.sf.compositor.util.Env;
import net.sf.compositor.util.DirectoryChooser;
import net.sf.compositor.util.Formats;
import net.sf.compositor.util.Info;
import net.sf.compositor.util.ResourceLoader;

public class Dismatch extends AppMac {

    private static final int SZ = 10;

    private static final int SZ_SQ = SZ * SZ;

    public JFrame x_main;

    public JLabel x_main_dirLabel;

    public JTextField x_main_dir;

    public JCheckBox x_main_img;

    public JCheckBox x_main_subdirs;

    public JSpinner x_main_threshold;

    public JLabel x_main_listLabel;

    public JList x_main_list;

    public JLabel x_main_status;

    public JProgressBar x_main_memory;

    public JLabel x_showImages_left;

    public JLabel x_showImages_right;

    private DefaultListModel m_listModel = new DefaultListModel();

    private boolean m_subDirs;

    private boolean m_imgCompare;

    private int m_threshold;

    public static void main(final String[] args) {
        new Dismatch();
    }

    /**
	 * Make the left hand labels the same width so that the right hand controls
	 * line up.
	 */
    protected void beforeUIBuilt() {
        runAfterUiBuilt(new Runnable() {

            public void run() {
                final Dimension psd = x_main_dirLabel.getPreferredSize();
                final Dimension psl = x_main_listLabel.getPreferredSize();
                final int w = Math.max(psd.width, psl.width);
                x_main_dirLabel.setPreferredSize(new Dimension(w, psd.height));
                x_main_listLabel.setPreferredSize(new Dimension(w, psl.height));
            }
        });
        runAfterUiBuilt(new Runnable() {

            public void run() {
                new Thread() {

                    public void run() {
                        for (; ; ) {
                            final Runtime runtime = Runtime.getRuntime();
                            final int maxMem = (int) (runtime.maxMemory());
                            final int totMem = (int) (runtime.totalMemory());
                            final int freeMem = (int) (runtime.freeMemory());
                            EventQueue.invokeLater(new Runnable() {

                                public void run() {
                                    x_main_memory.setMaximum(maxMem);
                                    x_main_memory.setValue(totMem - freeMem);
                                    x_main_memory.setToolTipText("[" + Formats.formattedByteCount(totMem - freeMem, 1) + '/' + Formats.formattedByteCount(maxMem, 1) + ']');
                                }
                            });
                            try {
                                sleep(5000L);
                            } catch (final InterruptedException x) {
                            }
                        }
                    }
                }.start();
            }
        });
    }

    public void writeStatus(final String message) {
        x_main_status.setText(message);
    }

    public void main__onLoad() {
        x_main_dir.requestFocusInWindow();
        x_main_list.setModel(m_listModel);
        x_main.setMinimumSize(x_main.getSize());
    }

    /**
	 * Capturing the mouse press rather than the mouse click means that we can
	 * set the list selection before the component's popup menu is shown. If a
	 * popup has been shown, no click event is generated (at least, on
	 * Windows).
	 */
    public void main_list_onMousePressed(final MouseEvent e) {
        handleMainListRightClick(e);
    }

    /**
	 * Capturing a mouse release means that we can set the list selection if
	 * the user right clicks on a different row of the list when the popup is
	 * already showing (at least, on Windows). In this case, only mouseReleased
	 * events are received!
	 */
    public void main_list_onMouseReleased(final MouseEvent e) {
        handleMainListRightClick(e);
    }

    private void handleMainListRightClick(final MouseEvent e) {
        if (MouseEvent.BUTTON3 == e.getButton()) {
            x_main_list.setSelectedIndex(x_main_list.locationToIndex(new Point(e.getX(), e.getY())));
        }
    }

    public void main_img_onItemEvent() {
        x_main_threshold.setEnabled(x_main_img.isSelected());
    }

    public void doLeftCopy() {
        if (noneSelected()) return;
        copyString(((MatchListElement) x_main_list.getModel().getElementAt(x_main_list.getSelectedIndex())).m_leftFile.getAbsolutePath());
    }

    public void doRightCopy() {
        if (noneSelected()) return;
        copyString(((MatchListElement) x_main_list.getModel().getElementAt(x_main_list.getSelectedIndex())).m_rightFile.getAbsolutePath());
    }

    private static void deleteFile(final Component relativeComponent, final String path, final boolean isLeft) {
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(relativeComponent, "Permanently delete " + path + "?", "Delete " + (isLeft ? "left" : "right") + " hand file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) new File(path).delete();
    }

    public void doLeftDelete() {
        if (noneSelected()) return;
        deleteFile(x_main, ((MatchListElement) x_main_list.getModel().getElementAt(x_main_list.getSelectedIndex())).m_leftFile.getAbsolutePath(), true);
    }

    public void doRightDelete() {
        if (noneSelected()) return;
        deleteFile(x_main, ((MatchListElement) x_main_list.getModel().getElementAt(x_main_list.getSelectedIndex())).m_rightFile.getAbsolutePath(), false);
    }

    private boolean noneSelected() {
        if (-1 == x_main_list.getSelectedIndex()) {
            msgBox(x_main_list, "Mothing selected.");
            return true;
        }
        return false;
    }

    public void doShowImages() {
        if (noneSelected()) return;
        final MatchListElement listItem = (MatchListElement) x_main_list.getModel().getElementAt(x_main_list.getSelectedIndex());
        final String leftName = listItem.m_leftFile.getAbsolutePath();
        final String rightName = listItem.m_rightFile.getAbsolutePath();
        final ImageIcon leftIcon = new ImageIcon(leftName);
        final ImageIcon rightIcon = new ImageIcon(rightName);
        final int leftWidth = leftIcon.getIconWidth();
        final int leftHeight = leftIcon.getIconHeight();
        final int rightWidth = rightIcon.getIconWidth();
        final int rightHeight = rightIcon.getIconHeight();
        x_showImages_left.setIcon(new ImageIcon(leftIcon.getImage().getScaledInstance(leftWidth >= leftHeight ? 144 : -1, leftHeight >= leftWidth ? 144 : -1, Image.SCALE_SMOOTH)));
        x_showImages_right.setIcon(new ImageIcon(rightIcon.getImage().getScaledInstance(rightWidth >= rightHeight ? 144 : -1, rightHeight >= rightWidth ? 144 : -1, Image.SCALE_SMOOTH)));
        x_showImages_left.setToolTipText(leftName + " - " + leftWidth + 'x' + leftHeight);
        x_showImages_right.setToolTipText(rightName + " - " + rightWidth + 'x' + rightHeight);
        x_showImages_left.setName(leftName);
        x_showImages_right.setName(rightName);
        fixDialog(getDialog("showImages"), x_main);
    }

    public void showImages_ok_onPress() {
        getDialog("showImages").dispose();
    }

    public void doAbout() {
        new Info(getFrame("main"), "About " + getAppName(), new String[][] { { "What?", getAppName(), Info.NO_COLON, Info.BOLD }, { "Huh?", "Finds matching pairs of files", Info.NO_COLON }, { Info.SPACER }, { "How?", getAppName() + " finds exact copies and (optionally)", Info.NO_COLON }, { "", "images which are very similar. You can adjust the amount", Info.NO_COLON }, { "", "of image difference which still counts as a match: ", Info.NO_COLON }, { "", "bigger means more different", Info.NO_COLON } }, new Info.Extras("Showing \"About\" box...", this, null, getSysProps(), new ImageIcon(getFrame("main").getIconImage())));
    }

    public void doDotdotdot() {
        final JFileChooser chooser = new DirectoryChooser(new File(x_main_dir.getText()));
        writeStatus("Choosing directory...");
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(x_main)) {
            final File dir = chooser.getSelectedFile();
            x_main_dir.setText(dir.getAbsolutePath());
            m_listModel.removeAllElements();
            writeStatus("Chose " + dir.getName());
        } else {
            writeStatus("Choosing directory cancelled");
        }
    }

    public void doFind() {
        getAction("find").setEnabled(false);
        m_listModel.removeAllElements();
        writeStatus("Finding matches...");
        m_subDirs = x_main_subdirs.isSelected();
        m_imgCompare = x_main_img.isSelected();
        m_threshold = (Integer) x_main_threshold.getValue();
        new Thread() {

            public void run() {
                boolean aborted = true;
                final long start = System.currentTimeMillis();
                try {
                    final String dirName = x_main_dir.getText();
                    x_main.setCursor(Env.WAIT_CURSOR);
                    if (null == dirName || 0 == dirName.length()) {
                        msgBox(x_main, "Please choose a directory", "Problem finding");
                        return;
                    }
                    final File dir = new File(dirName);
                    if (!dir.exists()) {
                        msgBox(x_main, "Could not find " + Env.NL + dir, "Problem finding", ERROR_MESSAGE);
                        return;
                    }
                    if (!dir.isDirectory()) {
                        msgBox(x_main, "Not a directory: " + Env.NL + dir, "Problem finding", ERROR_MESSAGE);
                        return;
                    }
                    compareFilesInDir(dir);
                    aborted = false;
                } catch (final Exception x) {
                    msgBox("Problem with find:" + Env.NL + x.getMessage());
                    x.printStackTrace();
                } finally {
                    x_main.setCursor(null);
                    if (aborted) {
                        invokeLater("writeStatus", "Find aborted");
                    } else {
                        invokeLater("writeStatus", "Find took " + (Formats.formattedElapsedTimeShort(System.currentTimeMillis(), start)));
                    }
                    getAction("find").setEnabled(true);
                }
            }
        }.start();
    }

    private void compareFilesInDir(final File dir) throws IOException {
        final Map<String, byte[]> digests = new HashMap<String, byte[]>();
        final Map<File, int[]> images = new HashMap<File, int[]>();
        final List<File> files = new ArrayList<File>();
        addFiles(files, dir);
        for (int i = 0, fileCount = files.size(); i < fileCount; i++) {
            final File leftFile = files.get(i);
            invokeLater("writeStatus", progressStatus(i, fileCount, leftFile));
            for (int j = i + 1; j < fileCount; j++) {
                final File rightFile = files.get(j);
                if (0 == i) invokeLater("writeStatus", progressStatus(i, fileCount, leftFile) + " v. " + j + " - " + rightFile.getName());
                if (compareTwoFiles(leftFile, rightFile, digests, images)) {
                    invokeLater("addMatch", new File[] { leftFile, rightFile });
                }
                Thread.yield();
            }
        }
    }

    private class NotDirFilter implements FileFilter {

        public boolean accept(final File f) {
            return !f.isDirectory();
        }
    }

    private class DirFilter implements FileFilter {

        public boolean accept(final File f) {
            return f.isDirectory();
        }
    }

    private void addFiles(final List<File> target, final File dir) {
        if (null == dir) return;
        for (final File f : dir.listFiles(new NotDirFilter())) target.add(f);
        if (m_subDirs) for (final File d : dir.listFiles(new DirFilter())) addFiles(target, d);
    }

    private static String progressStatus(final int i, final int count, final File file) {
        return "File " + (i + 1) + " of " + count + " - " + file.getName();
    }

    private class MatchListElement {

        private final File m_leftFile;

        private final File m_rightFile;

        private MatchListElement(final File leftFile, final File rightFile) {
            m_leftFile = leftFile;
            m_rightFile = rightFile;
        }

        public String toString() {
            return m_leftFile.getName() + " matches " + m_rightFile.getName();
        }
    }

    public void addMatch(final File leftFile, final File rightFile) {
        m_listModel.addElement(new MatchListElement(leftFile, rightFile));
    }

    private boolean compareTwoFiles(final File leftFile, final File rightFile, final Map<String, byte[]> digests, final Map<File, int[]> images) throws IOException {
        boolean result = true;
        final byte[] leftDigest = getDigest(leftFile, digests);
        final byte[] rightDigest = getDigest(rightFile, digests);
        if (leftDigest.length != rightDigest.length) {
            throw new RuntimeException("Mismatched digest lengths " + leftDigest.length + " and " + rightDigest.length + " for " + leftFile.getName() + " and " + rightFile.getName());
        }
        for (int k = 0; k < leftDigest.length; k++) {
            if (leftDigest[k] != rightDigest[k]) {
                result = false;
                break;
            }
        }
        Thread.yield();
        if (!result && m_imgCompare && isPicture(leftFile) && isPicture(rightFile)) {
            final int[] leftImagePixels = getSmallImage(leftFile, images);
            final int[] rightImagePixels = getSmallImage(rightFile, images);
            final int[] diffs = new int[leftImagePixels.length];
            result = true;
            for (int i = 0, len = diffs.length; i < len; i++) {
                final int leftImagePixel = leftImagePixels[i];
                final int rightImagePixel = rightImagePixels[i];
                if ((Math.abs(((leftImagePixel >> 24) & 0xff) - ((rightImagePixel >> 24) & 0xff)) > m_threshold) || (Math.abs(((leftImagePixel >> 16) & 0xff) - ((rightImagePixel >> 16) & 0xff)) > m_threshold) || (Math.abs(((leftImagePixel >> 8) & 0xff) - ((rightImagePixel >> 8) & 0xff)) > m_threshold) || (Math.abs((leftImagePixel & 0xff) - (rightImagePixel & 0xff)) > m_threshold)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    private static int[] getSmallImage(final File file, final Map<File, int[]> images) throws IOException {
        int[] result = images.get(file);
        if (null == result) {
            final Image bigImage = new ImageIcon(file.getAbsolutePath()).getImage();
            final Image smallImage = bigImage.getScaledInstance(SZ, SZ, Image.SCALE_AREA_AVERAGING);
            final PixelGrabber pg = new PixelGrabber(smallImage, 0, 0, SZ, SZ, result = new int[SZ_SQ], 0, SZ);
            try {
                pg.grabPixels();
            } catch (final InterruptedException x) {
                throw new IOException("Interrupted whilst waiting for pixels for image " + file + '.');
            }
            if (0 != (pg.getStatus() & ImageObserver.ABORT)) {
                s_log.error("Pixel grab aborted or errored for image " + file + '.');
            }
            images.put(file, result);
        }
        return result;
    }

    private static boolean isPicture(final File file) {
        final String name = file.getName().toLowerCase();
        final int dotPos = name.indexOf('.');
        final String ext = -1 == dotPos ? null : name.substring(dotPos + 1);
        return -1 != dotPos && ("jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "png".equals(ext) || "bmp".equals(ext));
    }

    private static byte[] getDigest(final File file, final Map<String, byte[]> digests) {
        final String fileName = file.getAbsolutePath();
        if (!digests.containsKey(fileName)) {
            DigestInputStream in = null;
            final MessageDigest digester;
            final byte[] buf = new byte[8192];
            try {
                digester = MessageDigest.getInstance("MD5");
            } catch (final NoSuchAlgorithmException x) {
                throw new RuntimeException("Could not create message digester: " + x, x);
            }
            try {
                in = new DigestInputStream(new FileInputStream(file), digester);
                for (int len; -1 != (in.read(buf, 0, buf.length)); ) ;
                digests.put(fileName, digester.digest());
            } catch (final IOException x) {
                s_log.error("Could not read file: " + x, x);
                return digester.digest();
            } finally {
                if (null != in) try {
                    in.close();
                } catch (final IOException x2) {
                    s_log.error("Could not close file: " + x2, x2);
                }
            }
        }
        return digests.get(fileName);
    }

    public void showImages_left_onClick() {
        deleteFile(x_showImages_left, x_showImages_left.getName(), true);
    }

    public void showImages_right_onClick() {
        deleteFile(x_showImages_right, x_showImages_right.getName(), false);
    }
}
