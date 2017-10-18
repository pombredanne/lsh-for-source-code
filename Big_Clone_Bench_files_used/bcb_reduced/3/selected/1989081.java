package hu.scytha.common;

import hu.scytha.main.Settings;
import hu.scytha.plugin.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * This class only has static methods.
 * Not to instanciate. 
 *  
 * @author Bertalan Lacza
 *
 */
public abstract class Util extends CommonModulUtil {

    public static final Image FOLDER = getImageDescriptor("folder.gif").createImage();

    private static ImageRegistry imageRegistry;

    private static Clipboard clipboard;

    private static HashMap<Integer, Color> colorMap;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    private static String[] picExtensions = new String[] { "jpg", "jpeg", "gif", "png", "xpm", "bmp", "tiff", "tif", "svg", "xcf" };

    private static String[] archivExtensions = new String[] { "zip", "bz2", "bz", "tar", "tgz", "jar", "war", "gz", "gzip", "rar" };

    private static String[] calcExtensions = new String[] { "xls", "csv", "sxc", "ods", "ott", "stc", "xlw", "xlt", "dbf" };

    private static String[] docExtensions = new String[] { "doc", "rtf", "sxw", "stw", "ppt", "pps", "odt" };

    private static String[] audioExtensions = new String[] { "mp3", "wav", "ogg", "wma", "au" };

    private static String[] mmExtensions = new String[] { "avi", "mov", "mpg", "mpeg", "wmv", "gp3", "mp4" };

    private static String[] imageFileExtensions = new String[] { "iso", "nrg", "mdf" };

    public static URL newURL(String url_name) {
        try {
            return new URL(url_name);
        } catch (MalformedURLException e) {
            MessageSystem.logException("", "Util", "newURL", null, e);
            throw new RuntimeException("Malformed URL " + url_name, e);
        }
    }

    /**
    * Gets the image registry, wich contains the program icons.
    * @return the image registry
    */
    public static ImageRegistry getImageRegistry() {
        if (imageRegistry == null) {
            imageRegistry = new ImageRegistry();
            Image iPic = getImageDescriptor("pic.gif").createImage();
            Image iExcel = getImageDescriptor("excel.gif").createImage();
            Image iDoc = getImageDescriptor("doc.gif").createImage();
            Image iMm = getImageDescriptor("mm.gif").createImage();
            Image iArchiv = getImageDescriptor("archiv.gif").createImage();
            Image iAudio = getImageDescriptor("audio.gif").createImage();
            Image iHtml = getImageDescriptor("html.gif").createImage();
            imageRegistry.put("scytha", getImageDescriptor("scytha.png").createImage());
            imageRegistry.put("root", getImageDescriptor("root.gif").createImage());
            imageRegistry.put("about", getImageDescriptor("about.gif").createImage());
            imageRegistry.put("updir", getImageDescriptor("updir.gif").createImage());
            imageRegistry.put("same_dir", getImageDescriptor("syncpanel.gif").createImage());
            imageRegistry.put("home", getImageDescriptor("home.gif").createImage());
            imageRegistry.put("folder", getImageDescriptor("folder.gif").createImage());
            imageRegistry.put("folder_bookmark", getImageDescriptor("folder_bookmark.gif").createImage());
            imageRegistry.put("folder_up", getImageDescriptor("folder_up.gif").createImage());
            imageRegistry.put("folder_link", getImageDescriptor("folder_link.gif").createImage());
            imageRegistry.put("folder_denied", getImageDescriptor("folder_denied.gif").createImage());
            imageRegistry.put("file", getImageDescriptor("file.gif").createImage());
            imageRegistry.put("file_link", getImageDescriptor("file_link.gif").createImage());
            imageRegistry.put("refresh", getImageDescriptor("refresh.gif").createImage());
            imageRegistry.put("ftp", getImageDescriptor("ftp.gif").createImage());
            imageRegistry.put("ftp_bookmark", getImageDescriptor("ftp_bkmrk.gif").createImage());
            imageRegistry.put("mounted_points", getImageDescriptor("mounted_points.gif").createImage());
            imageRegistry.put("close", getImageDescriptor("close.gif").createImage());
            imageRegistry.put("copy", getImageDescriptor("copy.gif").createImage());
            imageRegistry.put("delete", getImageDescriptor("delete.gif").createImage());
            imageRegistry.put("info", getImageDescriptor("info.gif").createImage());
            imageRegistry.put("cut", getImageDescriptor("cut.gif").createImage());
            imageRegistry.put("paste", getImageDescriptor("paste.gif").createImage());
            imageRegistry.put("newfolder", getImageDescriptor("newfolder.gif").createImage());
            imageRegistry.put("openfolder", getImageDescriptor("openfolder.gif").createImage());
            imageRegistry.put("search", getImageDescriptor("search.gif").createImage());
            imageRegistry.put("systray", getImageDescriptor("systray.png").createImage());
            imageRegistry.put("broken_link", getImageDescriptor("broken_link.gif").createImage());
            imageRegistry.put("sortasc", getImageDescriptor("sortasc.png").createImage());
            imageRegistry.put("sortdesc", getImageDescriptor("sortdesc.png").createImage());
            imageRegistry.put("terminal", getImageDescriptor("terminal.png").createImage());
            imageRegistry.put("help", getImageDescriptor("help.gif").createImage());
            imageRegistry.put("treeview", getImageDescriptor("treeview.gif").createImage());
            imageRegistry.put("detailsview", getImageDescriptor("detailsview.gif").createImage());
            if (imageRegistry.getDescriptor("html") == null) {
                imageRegistry.put("html", iHtml);
            }
            if (imageRegistry.getDescriptor("htm") == null) {
                imageRegistry.put("htm", iHtml);
            }
            if (imageRegistry.getDescriptor("xml") == null) {
                imageRegistry.put("xml", iHtml);
            }
            if (imageRegistry.getDescriptor("pdf") == null) {
                imageRegistry.put("pdf", getImageDescriptor("pdf.gif").createImage());
            }
            if (imageRegistry.getDescriptor("java") == null) {
                imageRegistry.put("java", getImageDescriptor("java.gif").createImage());
            }
            ImageData isoData = Program.findProgram("iso").getImageData();
            Image iImageFile;
            if (isoData != null) {
                iImageFile = new Image(null, isoData);
            } else {
                iImageFile = imageRegistry.get("file");
            }
            loadIcons(imageFileExtensions, iImageFile);
            loadIcons(picExtensions, iPic);
            loadIcons(archivExtensions, iArchiv);
            loadIcons(calcExtensions, iExcel);
            loadIcons(docExtensions, iDoc);
            loadIcons(mmExtensions, iMm);
            loadIcons(audioExtensions, iAudio);
            for (String ext : Program.getExtensions()) {
                Program p = Program.findProgram(ext);
                if (p != null && imageRegistry.getDescriptor(ext) == null) {
                    ImageData imgData = p.getImageData();
                    if (imgData != null) {
                        imageRegistry.put(ext, new Image(null, imgData));
                    }
                }
            }
        }
        return imageRegistry;
    }

    private static void loadIcons(String[] extensions, Image pic) {
        for (String ext : extensions) {
            if (imageRegistry.getDescriptor(ext) == null) imageRegistry.put(ext, pic);
        }
    }

    /**
    * 
    * @return clipboard instance
    */
    public static Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = new Clipboard(Display.getCurrent());
        }
        return clipboard;
    }

    /**
    * The method gets all files and directories contains the given list of files.
    * @param pIFiles contains <code>IFile</code> objects
    * @return The <code>HashMap</code> contains two <code>ArrayList</code>s<br>
    * The labels are:<br>
    * "files": all files in the directories and subdirectories, type of <code>IFile</code> <br>
    * "dirs": all directories and subdirectories, type of <code>IFile</code>
    */
    public static void readSubDirsAndFiles(List<IFile> pIFiles, List<IFile> pDirs, List<IFile> pFiles, boolean pWithSymlinks) throws ScythaException {
        if (Settings.traceOn) {
            MessageSystem.trace(Util.class.getName(), "readSubDirsAndFiles(pIFiles, pDires, pFiles, pWithSymlinks)");
        }
        readSubDirsAndFiles(pIFiles, pDirs, pFiles, pWithSymlinks, null);
    }

    /**
    * The method gets all files and directories contains the given list of files.
    * @param pIFiles contains <code>IFile</code> objects
    * @return The <code>HashMap</code> contains two <code>ArrayList</code>s<br>
    * The labels are:<br>
    * "files": all files in the directories and subdirectories, type of <code>IFile</code> <br>
    * "dirs": all directories and subdirectories, type of <code>IFile</code>
    */
    public static void readSubDirsAndFiles(List<IFile> pIFiles, List<IFile> pDirs, List<IFile> pFiles, boolean pWithSymlinks, IFileFilter pFileFilter) throws ScythaException {
        if (Settings.traceOn) {
            MessageSystem.trace(Util.class.getName(), "readSubDirsAndFiles(pIFiles, pDires, pFiles, pWithSymlinks)");
        }
        for (IFile file : pIFiles) {
            file.readSubDirectoriesAndFiles(pDirs, pFiles, pWithSymlinks, pFileFilter);
        }
    }

    /**
    * 
    * @param expression
    * @return 
    */
    public static String replaceAllPredefSigns(String expression) {
        if (Settings.traceOn) {
            MessageSystem.trace(Util.class.getName(), "replaceAllPredefSigns(regex");
        }
        String[] signs = new String[] { " ", "$", "(", ")" };
        for (int i = 0; i < signs.length; i++) {
            StringTokenizer st = new StringTokenizer(expression, signs[i]);
            expression = "";
            if (st.countTokens() != 1) {
                while (st.hasMoreTokens()) {
                    expression += st.nextToken() + "\\" + signs[i];
                }
                expression = expression.substring(0, expression.length() - 2);
            } else {
                expression = st.nextToken();
            }
        }
        return expression;
    }

    /**
    * Creates a symbolic link named newPath 
    * which contains the string oldPath.
    * @param oldPath 
    * @param newPath
    */
    public static native void createSymlink(String oldPath, String newPath);

    /**
    * Sets an environment variable for a session.
    * @param env The name of the variable
    * @param value The value of the variable
    */
    public static native void setEnvironmentVariable(String env, String value);

    /**
    * Gets the environment variables.
    * @return The variables. The vector contains strings in format: 
    * VARIABLE_NAME=VARIABLE_VALUE 
    */
    public static native Vector getEnvironments();

    /**
    * Retrives all group names.
    * @return sorted group names
    */
    public static String[] getGroupNames() {
        return getUserOrGroupNames(LocalFile.GROUPS_PATH);
    }

    /**
    * Sets an environment variable for a session.
    * @param env The name of the variable
    * @param value The value of the variable
    */
    public static String getEnvironmentVariable(String env) {
        Vector envs = getEnvironments();
        for (Iterator iter = envs.iterator(); iter.hasNext(); ) {
            String element = (String) iter.next();
            StringTokenizer st = new StringTokenizer(element, "=");
            while (st.hasMoreTokens()) {
                if (st.nextToken().equals(env)) return st.nextToken();
            }
        }
        return null;
    }

    /**
    * Retrives all user names.
    * @return sorted user names
    */
    public static String[] getUserNames() {
        return getUserOrGroupNames(LocalFile.PASSWD_PATH);
    }

    /**
    * Reads a /etc/passwd or /etc/groups file.
    * @param etcFile the file
    * @return sorted names
    */
    private static String[] getUserOrGroupNames(String etcFile) {
        SortedSet<String> set = new TreeSet<String>();
        try {
            File file = new File(etcFile);
            LineNumberReader reader = new LineNumberReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] tags = line.split(":");
                set.add(tags[0]);
            }
        } catch (FileNotFoundException e) {
            MessageSystem.showErrorMessage(e.getMessage());
        } catch (IOException e) {
            MessageSystem.showErrorMessage(e.getMessage());
        }
        return set.toArray(new String[0]);
    }

    /**
    * 
    * @param directoryName
    */
    public static void deleteEntriesOfDirectory(String directoryName) {
        if (Settings.traceOn) {
            MessageSystem.trace(Util.class.getName(), "deleteEntriesOfDirectory(" + directoryName + ")");
        }
        File dir = new File(directoryName);
        if (dir.isDirectory()) {
            recoursiveDelete(dir);
        }
    }

    /**
    * 
    * @param directory
    */
    private static void recoursiveDelete(File directory) {
        File[] dirs = directory.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (int i = 0; i < dirs.length; i++) {
            recoursiveDelete(dirs[i]);
            dirs[i].delete();
        }
        File[] files = directory.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }

    /**
    * The color constant is specified in <code>SWT</code>.
    * @param colorId the color constant
    * @return the color
    * 
    * For color ids:
    * @see org.eclipse.swt.SWT
    */
    public static Color getColor(int colorId) {
        if (colorMap == null) {
            colorMap = new HashMap<Integer, Color>();
        }
        if (colorMap.containsKey(colorId)) {
            return colorMap.get(colorId);
        }
        Color color = Display.getDefault().getSystemColor(colorId);
        colorMap.put(colorId, color);
        return color;
    }

    /**
    * 
    * @param expression 
    * @param from
    * @param to
    * @return
    */
    public static String replace(String expression, String from, String to) {
        String retValue = "";
        StringTokenizer st = new StringTokenizer(expression, from);
        while (st.hasMoreTokens()) {
            retValue += st.nextToken();
            if (st.hasMoreTokens()) {
                retValue += to;
            }
        }
        return retValue;
    }

    /**
    * Generates MD5 hash for a given file.
    * @param filePath Full qualified file path
    * @return MD5 hash
    * @throws UnsupportedEncodingException
    * @throws FileNotFoundException
    * @throws IOException
    * @throws NoSuchAlgorithmException
    */
    public static String generateMD5Sum(String filePath) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        String retVal = "";
        FileInputStream fis = new FileInputStream(filePath);
        int available = fis.available();
        byte[] reed = new byte[available];
        fis.read(reed);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] ba = md.digest(reed);
        String hexChar = null;
        if (ba != null) {
            for (int i = 0; i < ba.length; i++) {
                if (ba[i] > 0) {
                    hexChar = Integer.toHexString(ba[i]);
                } else if (ba[i] < 0) {
                    hexChar = Integer.toHexString(ba[i]).substring(6);
                } else hexChar = "00";
                retVal += lpad(hexChar, '0', 2);
            }
        }
        return retVal;
    }

    /**
    * 
    * @param text
    * @param what
    * @param length
    * @return
    */
    public static String lpad(String text, char what, int length) {
        while (text.length() < length) {
            text = what + text;
        }
        return text;
    }

    /**
    * 
    * @param pFile
    * @return
    * @throws IOException
    */
    public static String tryToClarifyFileType(IPluginFile pFile) throws IOException {
        InputStream stream = pFile.getInputStream();
        byte[] b = new byte[2];
        stream.read(b);
        stream.close();
        String str = new String(b);
        if (str.equals("#!")) {
            return "sh";
        }
        return null;
    }

    /**
    * Shortens the given text <code>textValue</code> so that its width in
    * pixels does not exceed the width of the given control. Overrides
    * characters at the end of the original string with an ellipsis ("...")
    * if necessary. If a <code>null</code> value is given, <code>null</code>
    * is returned.
    * 
    * @param textValue
    *            the original string or <code>null</code>
    * @param control
    *            the control the string will be displayed on
    * @return the string to display, or <code>null</code> if null was passed in
    */
    public static String shortenText(String textValue, Control control, int maxWidth) {
        if (textValue == null) {
            return null;
        }
        GC gc = new GC(control);
        if (gc.textExtent(textValue).x < maxWidth) {
            gc.dispose();
            return textValue;
        }
        String s = textValue;
        int length = gc.textExtent(s).x;
        while (length > maxWidth) {
            if (s.length() <= 4) {
                return textValue;
            }
            String s1 = s.substring(0, s.length() - 4);
            s = s1 + Dialog.ELLIPSIS;
            length = gc.textExtent(s).x;
        }
        gc.dispose();
        return s;
    }

    /**
    * 
    * @param textValue
    * @param pCharLength
    * @return
    */
    public static String shortenText(String textValue, int pCharLength) {
        if (textValue == null) {
            return null;
        }
        int length = textValue.length();
        while (length > pCharLength) {
            if (length <= 4) {
                return textValue;
            }
            String s1 = textValue.substring(0, textValue.length() - 4);
            textValue = s1 + Dialog.ELLIPSIS;
            length = textValue.length();
        }
        return textValue;
    }

    public static String shortenText2(String textValue, int pCharLength) {
        if (textValue == null) {
            return null;
        }
        int length = textValue.length();
        if (length <= pCharLength) {
            return textValue;
        }
        int pivot = length / 2;
        int start = pivot;
        int end = pivot + 1;
        while (start >= 0 && end < length) {
            String s1 = textValue.substring(0, start);
            String s2 = textValue.substring(end, length);
            String s = s1 + Dialog.ELLIPSIS + s2;
            int l = s.length();
            if (l < pCharLength) {
                return s;
            }
            start--;
            end++;
        }
        return textValue;
    }

    /**
    * Get an <code>Image</code> from the provide SWT image
    * constant.
    * @param imageID the SWT image constant
    * @return image the image
    */
    public static Image getSWTImage(final int imageID) {
        final Image[] image = new Image[1];
        Display.getCurrent().syncExec(new Runnable() {

            public void run() {
                image[0] = Display.getCurrent().getSystemImage(imageID);
            }
        });
        return image[0];
    }

    public static int[] createTableSelection(int[] rows, int focus) {
        int[] ret = new int[rows.length];
        if (ret.length == 0) {
            ret = new int[1];
            ret[0] = focus;
            return ret;
        }
        ret[0] = focus;
        for (int i = 0; i < rows.length; i++) {
            try {
                if (rows[i] != focus) {
                    ret[i + 1] = rows[i];
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        return ret;
    }
}
