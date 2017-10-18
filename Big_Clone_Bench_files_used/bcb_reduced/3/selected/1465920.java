package org.bionote.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bionote.Configuration;
import org.bionote.Constants;
import org.bionote.file.LocalFileWrapper;
import org.bionote.om.Attachment;
import org.bionote.om.IAttachment;
import org.bionote.om.IPage;
import org.bionote.om.IUser;
import org.bionote.om.service.PageService;
import org.bionote.service.context.ContextWrapper;
import edu.iupui.cmg.image.ImageMetrics;
import edu.iupui.cmg.image.Thumbnail;
import edu.iupui.cmg.stringutil.StringUtil;

/**
 * Stores files and retrieves them from local disk storage.
 * Default location is to the WEB-INF/files directory.
 * 
 * @author mbreese
 *
 */
public class FileServiceLocalImpl implements FileService {

    private PageService pageService = null;

    private String rootPath = Configuration.getConfigValue(Constants.FILE_PATH_KEY, "WEB-INF/files");

    protected final Log logger = LogFactory.getLog(getClass());

    public void setPageService(PageService pageService) {
        this.pageService = pageService;
    }

    /**
	 * searches for the next available filename with the given base
	 * filename_0, filename_1, etc...
	 * 
	 * @param page
	 * @param name
	 * @return
	 */
    protected String getNextFileName(IPage page, String name) {
        IAttachment a = pageService.findAttachment(page, name);
        int counter = 0;
        String newName = name;
        while (a != null) {
            if (name.indexOf(".") > 0) {
                newName = name.substring(0, name.lastIndexOf(".")) + "." + counter++ + name.substring(name.lastIndexOf("."));
            } else {
                newName = name + "_" + counter++;
            }
            a = pageService.findAttachment(page, newName);
        }
        return newName;
    }

    protected String getPathName(IPage page) {
        String path = "Space_" + page.getSpace().getId() + File.separator + "Page_" + page.getId();
        return path;
    }

    protected String getFullPathName(IPage page, ContextWrapper context) {
        String path;
        if (!rootPath.startsWith(File.separator)) {
            path = context.getRealPath(rootPath);
        } else {
            path = rootPath;
        }
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        path += getPathName(page);
        return path;
    }

    public IAttachment storeFile(LocalFileWrapper wrapper, String caption, IPage page, IUser user, ContextWrapper context) throws IOException {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.getFileName().equals("") || wrapper.getSize() == 0) {
            return null;
        }
        String safeFileName = StringUtil.makeSafeFileName(wrapper.getFileName());
        safeFileName = getNextFileName(page, safeFileName);
        IAttachment attachment = new Attachment();
        attachment.setCaption(caption);
        attachment.setCreatedDate(new Date());
        attachment.setCreatedUser(user);
        attachment.setFilename(safeFileName);
        attachment.setMimeType(wrapper.getContentType());
        attachment.setPage(page);
        attachment.setSize(wrapper.getSize());
        String fullpath = getFullPathName(page, context);
        logger.debug("writing to path:" + fullpath);
        File path = new File(fullpath);
        path.mkdirs();
        String filename = safeFileName;
        File file = new File(path, filename);
        int counter = 0;
        while (file.exists()) {
            filename = safeFileName + "_" + counter;
            file = new File(path, filename);
        }
        logger.debug("writing to file:" + file.getAbsolutePath());
        file.createNewFile();
        String md5 = writeFile(file, wrapper.getInputStream());
        String local = getPathName(page) + File.separator + filename;
        attachment.setLocalFilename(local);
        attachment.setMd5(md5);
        if (ImageMetrics.isImageMimeType(wrapper.getContentType())) {
            logger.debug("Determining image metrics");
            InputStream is = new FileInputStream(file);
            ImageMetrics fileMetrics = ImageMetrics.dimensions(is);
            try {
                is.close();
            } catch (IOException e1) {
            }
            attachment.setHeight(fileMetrics.getHeight());
            attachment.setWidth(fileMetrics.getWidth());
            String sizeStr = Configuration.getConfigValue(Constants.THUMBNAIL_SIZE_KEY, "200");
            int maxThumbSize = 200;
            try {
                maxThumbSize = Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                logger.warn("invalid thumbnail size:" + sizeStr + " using default (200)");
            }
            if (ImageMetrics.isImageThumbnailableMimeType(wrapper.getContentType()) && (fileMetrics.getHeight() > maxThumbSize || fileMetrics.getWidth() > maxThumbSize)) {
                logger.debug("Creating thumbnail");
                String thumbLocal = safeFileName + "_t.jpg";
                byte[] thumbBytes = Thumbnail.createJPEGMaxDimension(fileMetrics.getImage(), maxThumbSize);
                ImageMetrics thumbMetrics = ImageMetrics.dimensions(thumbBytes);
                File thumbnailFile = new File(path, thumbLocal);
                String thumbLocalFileName = thumbLocal;
                counter = 0;
                while (thumbnailFile.exists()) {
                    thumbLocalFileName = thumbLocal + "_" + counter;
                    thumbnailFile = new File(path, thumbLocalFileName);
                }
                thumbnailFile.createNewFile();
                writeFile(thumbnailFile, new ByteArrayInputStream(thumbBytes));
                attachment.setThumbnailLocalFilename(getPathName(page) + File.separator + thumbLocalFileName);
                attachment.setThumbnailHeight(thumbMetrics.getHeight());
                attachment.setThumbnailWidth(thumbMetrics.getWidth());
                attachment.setThumbnailSize(thumbBytes.length);
            }
            fileMetrics = null;
        }
        pageService.updateAttachment(attachment);
        return attachment;
    }

    /**
	 * 
	 * @param file
	 * @param in
	 * @return MD5 sum of the bytes written
	 * @throws IOException
	 */
    protected String writeFile(File file, InputStream in) throws IOException {
        OutputStream out = null;
        out = new FileOutputStream(file);
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }
        out = new BufferedOutputStream(out);
        in = new BufferedInputStream(in);
        byte[] buffer = new byte[4096];
        int bytes = 0;
        long total = 0;
        try {
            bytes = in.read(buffer);
            while (bytes > -1) {
                total += bytes;
                out.write(buffer, 0, bytes);
                if (md5 != null) {
                    md5.update(buffer, 0, bytes);
                }
                bytes = in.read(buffer);
            }
        } catch (IOException e1) {
            logger.error(e1);
            throw e1;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        if (md5 != null) {
            return new String(Hex.encodeHex(md5.digest()));
        } else {
            return null;
        }
    }

    public IAttachment[] storeFiles(LocalFileWrapper[] files, String[] captions, IPage page, IUser user, ContextWrapper context) throws IOException {
        IAttachment[] as = new IAttachment[files.length];
        for (int i = 0; i < files.length; i++) {
            if (files[i] != null) {
                as[i] = storeFile(files[i], captions[i], page, user, context);
            }
        }
        return as;
    }

    public InputStream retrieveFile(IAttachment attachment, ContextWrapper context) throws FileNotFoundException {
        return retrieveFile(attachment, context, false);
    }

    public InputStream retrieveFile(IAttachment attachment, ContextWrapper context, boolean thumbnail) throws FileNotFoundException {
        if (attachment != null) {
            String filename = "";
            if (thumbnail && attachment.getThumbnailLocalFilename() != null && !attachment.getThumbnailLocalFilename().equals("")) {
                filename = attachment.getThumbnailLocalFilename();
            } else {
                filename = attachment.getLocalFilename();
            }
            logger.debug(filename);
            String rootPath = this.rootPath;
            if (!rootPath.endsWith(File.separator) || !filename.startsWith(File.separator)) {
                filename = rootPath + File.separator + filename;
            } else {
                filename = rootPath + filename;
            }
            logger.debug(filename);
            if (!rootPath.startsWith(File.separator)) {
                filename = context.getRealPath(filename);
            }
            logger.debug(filename);
            try {
                return new FileInputStream(new File(filename));
            } catch (FileNotFoundException e) {
                logger.error(e);
                throw e;
            }
        }
        return null;
    }

    public InputStream retrieveFile(long id, ContextWrapper context) throws FileNotFoundException {
        return retrieveFile(id, context, false);
    }

    public InputStream retrieveFile(long id, ContextWrapper context, boolean thumbnail) throws FileNotFoundException {
        return retrieveFile(pageService.findAttachment(id), context, thumbnail);
    }
}
