package ciif;

import java.awt.image.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import javax.imageio.stream.ImageInputStream;
import java.util.prefs.*;
import java.security.*;
import java.math.*;
import java.beans.*;

/**
 *
 * @author dhart
 */
public class CIIFImportLocalTask extends SwingWorker<Void, Void> {

    private CIIFView main;

    private int progress;

    private int maxProgress;

    private File spiderRoot;

    private List<File> fileList;

    private Preferences prefs;

    private JProgressBar pBar;

    private Boolean recursive;

    private String importGroup;

    private Boolean wasCanceled = false;

    private CIIFImportLocalTaskListener listener;

    private CIIFDatabase db;

    public CIIFImportLocalTask(CIIFView w, CIIFImportLocalTaskListener l, String root, javax.swing.JProgressBar bar, Boolean recurse, String ig, CIIFDatabase d) {
        main = w;
        pBar = bar;
        progress = 0;
        recursive = recurse;
        importGroup = ig;
        listener = l;
        db = d;
        fileList = new ArrayList<File>();
        spiderRoot = new File(root);
        prefs = Preferences.userNodeForPackage(this.getClass());
        return;
    }

    @Override
    public Void doInBackground() {
        recurseFilesystem(spiderRoot);
        maxProgress = fileList.size();
        processImages();
        if (wasCanceled) {
            main.updateImportLocalMessages("Import was canceled by user.");
            System.out.println("Task was canceled by user.");
        }
        main.finishLocalImport(pBar);
        return null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int lProgress = (Integer) evt.getNewValue();
            if (progress > 0) {
                pBar.setIndeterminate(false);
                pBar.setValue(lProgress);
                pBar.setString(lProgress + "%");
            } else {
                pBar.setIndeterminate(true);
            }
        }
    }

    private Void processImages() {
        progress = 0;
        for (File file : fileList) {
            if (!isCancelled()) {
                processFile(file);
                float percent = ((float) ++progress / maxProgress) * 100;
                setProgress(Math.round(percent));
            } else {
                wasCanceled = true;
            }
        }
        return null;
    }

    private static boolean isSymlink(File file) throws IOException {
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    private Void recurseFilesystem(File start) {
        File[] rawFiles = start.listFiles();
        List<File> files = Arrays.asList(rawFiles);
        pBar.setString("Indexing " + start.getPath() + "...");
        for (File file : files) {
            if (!isCancelled()) {
                if (file.canRead() && file.isDirectory()) {
                    try {
                        if (!isSymlink(file)) if (recursive) {
                            recurseFilesystem(file);
                        }
                    } catch (IOException e) {
                        return null;
                    }
                } else if (file.canRead()) {
                    fileList.add(file);
                }
            } else {
                wasCanceled = true;
            }
        }
        return null;
    }

    private String md5(String txt) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(txt.getBytes(), 0, txt.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (Exception e) {
            return "BAD MD5";
        }
    }

    private Void processFile(File file) {
        BufferedImage img;
        String fileHash = md5(file.getPath());
        File tempFolder = new File(prefs.get("TEMPFOLDER", "") + File.separator + "CI-IF" + File.separator + fileHash.substring(0, 3));
        String thumbFileName = tempFolder.getPath() + File.separator + fileHash + ".png";
        File thumbFile = new File(thumbFileName);
        Boolean valid = false;
        if (file.length() < 2000) {
            return null;
        }
        if (!thumbFile.exists()) {
            try {
                img = ImageIO.read(file);
                valid = true;
            } catch (Exception e) {
                System.out.println(e.toString() + file.getName());
                return null;
            } finally {
                if (!valid) {
                    main.updateImportLocalMessages("Invalid image: " + file.getPath());
                    return null;
                }
            }
            ImageInputStream iis = null;
            try {
                FileInputStream imageStream = new FileInputStream(file);
                iis = ImageIO.createImageInputStream(imageStream);
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
            Iterator readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                int width = img.getWidth();
                int height = img.getHeight();
                double nWidth;
                double nHeight;
                if (height > width) {
                    double ratio = (double) width / (double) height;
                    nHeight = (double) 200 / height;
                    nWidth = (200 * ratio) / width;
                } else {
                    double ratio = (double) height / (double) width;
                    nWidth = (double) 200 / width;
                    nHeight = (200 * ratio) / height;
                }
                AffineTransform tx = new AffineTransform();
                tx.scale(nWidth, nHeight);
                AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                BufferedImage thumbImage = op.filter(img, null);
                if (!tempFolder.isDirectory()) {
                    tempFolder.mkdir();
                }
                try {
                    ImageIO.write(thumbImage, "png", thumbFile);
                } catch (IOException e) {
                    return null;
                }
                if (!db.recordExists(fileHash)) {
                    db.doInsert(fileHash, file.getPath(), thumbFile.getPath(), 0, importGroup);
                    main.updateImportImageDisplay(thumbImage, thumbFile.getName(), true);
                } else {
                    main.updateImportImageDisplay(thumbImage, thumbFile.getName(), false);
                }
            }
        } else {
            BufferedImage thumbImage;
            try {
                thumbImage = ImageIO.read(thumbFile);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (!db.recordExists(fileHash)) {
                db.doInsert(fileHash, file.getPath(), thumbFile.getPath(), 0, importGroup);
                main.updateImportImageDisplay(thumbImage, thumbFile.getName(), true);
            } else {
                main.updateImportImageDisplay(thumbImage, thumbFile.getName(), false);
            }
        }
        return null;
    }
}
