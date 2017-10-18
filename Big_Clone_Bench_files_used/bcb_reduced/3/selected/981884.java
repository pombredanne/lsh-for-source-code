package net.sourceforge.ubcdcreator.plugin;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import net.sourceforge.ubcdcreator.BrowserLaunch;
import net.sourceforge.ubcdcreator.FileUtil;
import net.sourceforge.ubcdcreator.HttpGet;
import net.sourceforge.ubcdcreator.SettingsPanel;
import net.sourceforge.ubcdcreator.plugin.Plugin;

public class PluginInstaller {

    public void prepare(Plugin plugin, Plugin.File file, Component panel) {
        int i;
        File tmpdir = SettingsPanel.getInstance().getTempDir();
        File cachedir = SettingsPanel.getInstance().getCacheDir();
        tmpdir.mkdirs();
        cachedir.mkdirs();
        if (file.currentloc == null) file.currentloc = file.geturl;
        if (file.gettype.equals(Plugin.GetType.WEB)) {
            if (file.getautomatic) {
                File downloaddir = new File(cachedir.getAbsolutePath() + "/" + plugin.id);
                downloaddir.mkdirs();
                int pos = file.geturl.lastIndexOf(File.pathSeparator);
                File downloadfile = new File(downloaddir.getAbsolutePath() + "/" + file.geturl.substring(pos));
                file.currentloc = downloadfile.getAbsolutePath();
                if (!downloadfile.exists()) {
                    HttpGet httpGet = new HttpGet(file.geturl, downloadfile);
                    httpGet.get();
                }
            } else {
                BrowserLaunch.openURL(file.geturl);
                JOptionPane.showMessageDialog(panel, "Please download the file \"" + file.name + "\" from the following url: " + file.geturl, "Download File", JOptionPane.PLAIN_MESSAGE);
            }
        }
        if (file.gettype.equals(Plugin.GetType.FILE) || file.gettype.equals(Plugin.GetType.DIR) || (file.gettype.equals(Plugin.GetType.WEB) && !file.getautomatic)) {
            File fileloc = new File(file.currentloc);
            if (!fileloc.exists()) {
                String filestr = "";
                if (!(file.gettype.equals(Plugin.GetType.WEB) && !file.getautomatic)) filestr = file.geturl;
                fileloc = new File(filestr);
                JFileChooser chooser = new JFileChooser(fileloc);
                if (file.gettype.equals(Plugin.GetType.DIR)) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); else chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                    fileloc = chooser.getSelectedFile();
                    file.currentloc = fileloc.getAbsolutePath();
                }
            }
        }
        if (file.gettype.equals(Plugin.GetType.FILE) || file.gettype.equals(Plugin.GetType.WEB)) {
            if (checkFile(new File(file.currentloc), file.hash) == false) {
                System.out.println("Error!!");
                file.currentloc = null;
            }
        }
    }

    public void install(Plugin plugin, Component panel) {
        try {
            File tmpdir = SettingsPanel.getInstance().getTempDir();
            tmpdir.mkdirs();
            File pluginDir = new File(tmpdir.getAbsolutePath() + "/" + plugin.id);
            pluginDir.mkdirs();
            File scriptfile = new File(pluginDir.getAbsolutePath() + "/" + plugin.id + ".sh");
            FileOutputStream fos = new FileOutputStream(scriptfile);
            fos.write(plugin.script.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error!");
        }
    }

    public static boolean checkFile(File file, String hash) {
        try {
            int size;
            byte[] bytes = new byte[4096];
            FileInputStream input = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            while (input.available() > 0) {
                size = input.read(bytes);
                digest.update(bytes, 0, size);
            }
            byte[] result = digest.digest();
            return hash.equalsIgnoreCase(FileUtil.toString(result));
        } catch (Exception e) {
            return false;
        }
    }
}
