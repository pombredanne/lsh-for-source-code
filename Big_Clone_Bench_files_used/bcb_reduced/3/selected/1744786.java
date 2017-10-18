package org.wikiup.modules.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.wikiup.core.Wikiup;
import org.wikiup.core.bean.WikiupPluginManager;
import org.wikiup.core.impl.context.XPathContext;
import org.wikiup.core.impl.getter.dl.ByAttributeNameSelector;
import org.wikiup.core.inf.Document;
import org.wikiup.core.inf.Getter;
import org.wikiup.core.util.Documents;
import org.wikiup.core.util.StreamUtil;
import org.wikiup.core.util.StringUtil;
import org.wikiup.core.util.ValueUtil;

public class UpdaterUtil {

    public static void doUpdate(Document desc) {
        if (!Documents.getAttributeBooleanValue(desc, "disabled", false)) {
            WikiupPluginManager pluginManager = Wikiup.getModel(WikiupPluginManager.class);
            Document repository = desc.getChild("repository");
            Document list = Documents.loadFromURL(Documents.getDocumentValue(repository.getChild("list"), "url"));
            String downloadPattern = Documents.getDocumentValue(repository, "download");
            String target = Documents.getDocumentValue(repository, "target");
            Getter<Document> byName = new ByAttributeNameSelector(list, "name");
            for (String name : pluginManager) {
                WikiupPluginManager.Plugin plugin = pluginManager.get(name);
                File jarFile = new File(plugin.getJarFile().getName());
                Document node = byName.get(name);
                if (node != null) {
                    String md5 = md5(jarFile);
                    if (!md5.equalsIgnoreCase(Documents.getDocumentValue(node, "digest"))) {
                        Getter<?> getter = new XPathContext(node);
                        String url = StringUtil.evaluateEL(downloadPattern, getter);
                        String fileName = StringUtil.evaluateEL(target, getter);
                        updateArtifact(jarFile, new File(jarFile.getParentFile(), fileName), url);
                    }
                }
            }
        }
    }

    private static void updateArtifact(File from, File to, String urlStr) {
        if (!to.exists()) {
            OutputStream os = null;
            InputStream is = null;
            try {
                URL url = new URL(urlStr);
                is = url.openStream();
                os = new FileOutputStream(to, false);
                StreamUtil.copy(os, is);
            } catch (IOException e) {
            } finally {
                StreamUtil.close(os);
                StreamUtil.close(is);
                from.delete();
            }
        }
    }

    private static String md5(File jarFile) {
        byte[] buf = new byte[16384];
        InputStream is = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            is = new FileInputStream(jarFile);
            int len;
            while ((len = is.read(buf)) > 0) md.update(buf, 0, len);
            return ValueUtil.toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        } finally {
            StreamUtil.close(is);
        }
        return "";
    }
}
