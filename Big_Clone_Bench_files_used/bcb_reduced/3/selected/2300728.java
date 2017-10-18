package org.hydra.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.io.FileTransfer;
import org.hydra.beans.abstracts.APropertyLoader;
import org.hydra.messages.CommonMessage;
import org.hydra.messages.handlers.abstracts.AMessageHandler;
import org.hydra.messages.interfaces.IMessage;

public final class FileUtils {

    public static final int FILE_TYPE_UNKNOWN = 0;

    public static final int FILE_TYPE_IMAGE = 1;

    public static final int FILE_TYPE_COMPRESSED = FILE_TYPE_IMAGE << 1;

    public static final String URL4FILES_APPID_SUBFOLDER = "files/%s/%s/";

    public static final String FILE_DESCRIPTION_TEXT = "Text";

    public static final String FILE_DESCRIPTION_PUBLIC = "Public";

    private static final Log _log = LogFactory.getLog("org.hydra.utils.FileUtils");

    public static final String generalImageFormat = "png";

    public static String saveImage4(String inAppId, BufferedImage inImage) {
        String uri4Image = Utils.F("files/%s/images/%s.%s", inAppId, RandomStringUtils.random(8, true, true), generalImageFormat);
        String realPath = Utils.getRealPath(uri4Image);
        File output = new File(realPath);
        try {
            ImageIO.write(inImage, generalImageFormat, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (uri4Image);
    }

    public static void getListOfFiles4Dir(String URL, List<String> result, String ifFileNameEndWith) {
        getListOfFiles4Dir(URL, result, ifFileNameEndWith, true);
    }

    public static void getListOfFiles4Dir(String URL, List<String> result, String ifFileNameEndWith, boolean includeSubDirs) {
        String realURI = Utils.getRealPath(URL);
        if (realURI == null) return;
        File dir = new File(realURI);
        if (!dir.exists()) {
            _log.error("Directory not exist: " + realURI);
            return;
        }
        if (dir.isDirectory() && dir.list() != null) {
            for (String path2File : dir.list()) {
                File file = new File(realURI, path2File);
                if (file.isDirectory() && includeSubDirs) {
                    getListOfFiles4Dir(URL + path2File, result, ifFileNameEndWith);
                } else if (file.isFile()) {
                    if (ifFileNameEndWith == null || file.getName().endsWith(ifFileNameEndWith)) {
                        result.add(URL + path2File);
                    }
                }
            }
        }
    }

    public static String saveFile(CommonMessage inMessage) {
        if (!AMessageHandler.validateFile(inMessage)) {
            return ("ERROR: no file!");
        }
        if (!AMessageHandler.validateData(inMessage, "appid", "folder", "file_path", "file_real_path")) {
            return ("ERROR: no valid parameters!");
        }
        String appId = inMessage.getData().get("appid");
        FileTransfer file = inMessage.getFile();
        String realPath = inMessage.getData().get("file_real_path");
        String resultStr = "";
        InputStream is = null;
        FileOutputStream os = null;
        byte[] bufer = new byte[4096];
        int bytesRead = 0;
        try {
            is = file.getInputStream();
            os = new FileOutputStream(realPath);
            while ((bytesRead = is.read(bufer)) != -1) {
                _log.debug("bytesRead: " + bytesRead);
                os.write(bufer, 0, bytesRead);
            }
            os.close();
            resultStr = getFileBox(appId, inMessage.getData().get("file_path"));
        } catch (Exception e) {
            _log.error(e.toString());
            resultStr = e.toString();
        }
        return (resultStr);
    }

    public static boolean saveFileAndDescriptions(CommonMessage inMessage, StringWrapper outFilePath, String... dataDescriptionKeys) {
        FileTransfer file = inMessage.getFile();
        boolean result = false;
        String orginalFileName = sanitize(file.getFilename());
        String uri4FilePath = Utils.F(URL4FILES_APPID_SUBFOLDER, inMessage.getData().get("appid"), inMessage.getData().get("folder")) + getMD5FileName(orginalFileName) + getFileExtension(orginalFileName);
        String realPath = Utils.getRealPath(uri4FilePath);
        System.out.println("uri4FilePath: " + uri4FilePath);
        System.out.println("realPath: " + realPath);
        System.out.println("orginalFileName: " + orginalFileName);
        result = saveFile(realPath, file);
        result = saveFileDescriptions(inMessage, realPath, dataDescriptionKeys);
        if (result) outFilePath.setString(String.format("%s/%s", inMessage.getContextPath(), uri4FilePath));
        return result;
    }

    public static String getMD5FileName(String pass) {
        String result;
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
            byte[] data = pass.getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            result = String.format("%1$032X", i);
        } catch (NoSuchAlgorithmException e) {
            _log.error(e.getMessage());
            result = Utils.GetUUID();
        }
        return (result);
    }

    public static boolean saveFileDescriptions(CommonMessage inMessage, String filePath, String... dataDescriptionKeys) {
        if (dataDescriptionKeys.length == 0) return (false);
        try {
            File file = new File(filePath + APropertyLoader.SUFFIX);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), Constants._utf_8));
            for (int i = 0; i < dataDescriptionKeys.length; i++) {
                if (inMessage.getData().containsKey(dataDescriptionKeys[i])) {
                    String key = dataDescriptionKeys[i];
                    String value = inMessage.getData().get(key).trim();
                    if (key.compareToIgnoreCase("Name") == 0) value = sanitize(value); else if (!value.isEmpty()) value = value.replaceAll("\n", "\n\t");
                    if (value.length() > Constants._max_textarea_field_limit) value = value.substring(0, Constants._max_textarea_field_limit - 3) + "...";
                    if (value.length() > 0) bw.write(key + " = " + value + "\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            _log.error(e.getMessage());
            return (false);
        }
        return (true);
    }

    public static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return (null);
        return (filename.substring(lastDot));
    }

    ;

    public static String sanitize(String filename) {
        if (filename == null || filename.isEmpty()) return Utils.GetUUID();
        int lastLeft = filename.lastIndexOf("\\");
        int lastRight = filename.lastIndexOf("/");
        if (lastLeft == lastRight) {
            return (filename);
        }
        return filename.substring((lastLeft < lastRight ? lastRight : lastLeft) + 1);
    }

    private static boolean saveFile(String realPathFile, FileTransfer file) {
        InputStream is = null;
        FileOutputStream os = null;
        byte[] bufer = new byte[4096];
        int bytesRead = 0;
        try {
            is = file.getInputStream();
            os = new FileOutputStream(realPathFile);
            while ((bytesRead = is.read(bufer)) != -1) {
                _log.debug("bytesRead: " + bytesRead);
                os.write(bufer, 0, bytesRead);
            }
            os.close();
            return (true);
        } catch (Exception e) {
            _log.error(e.toString());
            return (false);
        }
    }

    public static String getFileBox(String inAppID, String inFilePath) {
        if (inFilePath == null) {
            _log.warn("Trying to process null inFilePath");
            return ("");
        }
        String fileExtension = getFileExtension(inFilePath);
        if (fileExtension == null) {
            _log.warn("NULL file extension for: " + inFilePath);
            return ("");
        }
        StringBuffer content = new StringBuffer();
        String divHiddenID = Utils.sanitazeHtmlId("template_" + inFilePath);
        content.append("<div style=\"margin: 5px; padding: 5px; border: 1px solid rgb(127, 157, 185);\">");
        content.append(getDeleteLink("AdmFiles", Utils.Q(Constants._admin_app_action_div), inAppID, inFilePath) + " ");
        content.append("[<strong>" + fileExtension + "</strong>] ");
        String htmlTag = "NOT_DEFINED";
        if (ImageUtils.validate(inFilePath)) {
            htmlTag = Utils.F("<img src=\"%s\" border=\"0\">", inFilePath);
        } else {
            htmlTag = Utils.F("<a href=\"%s\" target=\"_blank\">TEXT</a>", inFilePath);
        }
        content.append(Utils.toogleLink(divHiddenID, inFilePath));
        content.append(Utils.F("<div id=\"%s\" style=\"display: none;\" class=\"edit\">%s<hr />%s</div>", divHiddenID, StringEscapeUtils.escapeHtml(htmlTag), htmlTag));
        content.append("</div>");
        return content.toString();
    }

    public static String getDeleteLink(String inHandler, String inDest, String inAppID, String key) {
        String jsData = Utils.jsData("handler", Utils.Q(inHandler), "action", Utils.Q("delete"), "appid", Utils.Q(inAppID), "file_path", Utils.Q(key), "dest", Utils.sanitazeHtmlId(inDest));
        return (Utils.F("[%s]", Utils.createJSLinkWithConfirm("Delete", jsData, "X")));
    }

    public static String getFilePropertiesDescription(IMessage inMessage, String propertiesFilePath) {
        boolean isAdmin = Roles.roleNotLessThen(Roles.USER_ADMINISTRATOR, inMessage);
        Properties properties = parseProperties(propertiesFilePath);
        String Public = properties.getProperty(FILE_DESCRIPTION_PUBLIC);
        boolean isPublic = ((Public != null) && (Public.compareToIgnoreCase("true") == 0)) ? true : false;
        String divHiddenID = Utils.sanitazeHtmlId("template." + sanitize(propertiesFilePath));
        String Description = properties.getProperty(FILE_DESCRIPTION_TEXT);
        if (isPublic || isAdmin) {
            StringBuffer content = new StringBuffer();
            content.append("<div class=\"file_row\">");
            content.append("<span class=\"file_name\">" + properties.get("Name") + "</span>");
            content.append("<br />");
            String htmlTag = Utils.F("&nbsp;&nbsp;<a href=\"%s\" target=\"_blank\">%s</a>", stripPropertiesExtension(propertiesFilePath), "[[DB|Text|Download|locale]]");
            content.append(htmlTag);
            content.append(" ");
            if (Description != null) {
                content.append(Utils.toogleLink(divHiddenID, "[[DB|Text|Description|locale]]"));
                content.append(Utils.F("<div id=\"%s\" class=\"file_description\" >%s</div>", divHiddenID, properties.get(FILE_DESCRIPTION_TEXT)));
            }
            content.append("</div>");
            return content.toString();
        }
        return ("");
    }

    public static String stripPropertiesExtension(String propertiesFilePath) {
        return propertiesFilePath.substring(0, propertiesFilePath.length() - APropertyLoader.SUFFIX.length());
    }

    public static Properties parseProperties(String propertiesFilePath) {
        Properties properties = APropertyLoader.parsePropertyFile(Utils.getRealPath(propertiesFilePath));
        return properties;
    }

    public static String getFromHtmlFile(String inAppId, String fileName) {
        String filePath = String.format("/files/%s/html/%s.html", inAppId, fileName);
        String realPath = Utils.getRealPath(filePath);
        StringBuffer content = new StringBuffer(String.format("<!-- %s -->", fileName));
        if (realPath != null) {
            File file = new File(realPath);
            if (!file.exists()) {
                content.append(String.format("<!-- %s not found! -->", fileName));
                return (content.toString());
            }
            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, Constants._utf_8));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        content.append(line);
                    }
                }
            } catch (IOException e) {
                content.append(String.format("<!-- ERROR: %s -->", e.getMessage()));
            }
        }
        return (content.toString());
    }

    public static boolean isImage(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;
        if (filePath.toUpperCase().endsWith(".BMP") || filePath.toUpperCase().endsWith(".JPG") || filePath.toUpperCase().endsWith(".GIF") || filePath.toUpperCase().endsWith(".TIFF") || filePath.toUpperCase().endsWith(".PNG")) {
            return (true);
        }
        return (false);
    }
}
