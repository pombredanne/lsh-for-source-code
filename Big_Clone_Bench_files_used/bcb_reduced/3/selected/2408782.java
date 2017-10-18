package org.sharefast.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.sharefast.discussion.DiscussionServlet;
import org.sharefast.textsearch.SFIndexer;
import org.sharefast.util.SMTPEngine;
import org.sharefast.util.TextUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.DC_11;

/**
 * FileUploadServlet handles requests for uploading files to the z-kms server.
 * And this class also make an appropriate entry in database.
 * This class has functionality to generate metadata for uploading resources,
 * but still not in use.
 * 
 * @author Kazuo Hiekata <hiekata@nakl.t.u-tokyo.ac.jp>
 */
public class FileUploadServlet extends BaseServlet {

    private static final long serialVersionUID = 1134934718965965373L;

    /**
	 * FileUploadServlet#doRequest handles only one file.
	 * FileUploadServlet replaces current file when parameter "uri" is specifed.
	 * FileUploadServlet creates new file if "uri" is not found in request.
	 * 
	 * @param	request
	 * @param	response
	 * @return	void
	 */
    protected void doRequest(HttpServletRequest request, HttpServletResponse response) {
        if ((!this.isDocAdmin) && (!this.isWfAdmin)) {
            ServerConsoleServlet.printSystemLog("organization= " + organization + " username=" + username, ServerConsoleServlet.LOG_INFO);
            try {
                response.sendError(500, "authentication failure.");
                return;
            } catch (IOException e) {
                ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            }
        }
        HashMap map = new HashMap();
        String destdir = null;
        String rdftype = null;
        String makemetadata = null;
        File targetfile = null;
        String resourceFileName = null;
        String newFileName = null;
        String projectid = null;
        String menujsp = null;
        long maxfilesize = Long.parseLong(ServerConsoleServlet.getConfigByTagName("MaximumFileSize"));
        try {
            DefaultFileItemFactory fif = new DefaultFileItemFactory();
            fif.setRepository(new File(System.getProperty("java.io.tmpdir")));
            DiskFileUpload dfu = new DiskFileUpload(fif);
            dfu.setHeaderEncoding("UTF-8");
            FileItem item = null;
            Iterator items = dfu.parseRequest(request).iterator();
            String tmp = null;
            while (items.hasNext()) {
                item = (FileItem) items.next();
                if (item.isFormField()) {
                    tmp = item.getFieldName();
                    ServerConsoleServlet.printSystemLog("item.getFieldName()= " + tmp, ServerConsoleServlet.LOG_DEBUG);
                    map.put(tmp, item.getString("UTF-8"));
                } else {
                    if (item.getSize() > maxfilesize) {
                        try {
                            response.sendError(500, "File Too Big");
                        } catch (IOException e) {
                            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
                        }
                        return;
                    }
                    map.put("file", item);
                }
            }
            destdir = (String) map.get("dir");
            destdir = URLDecoder.decode(destdir, "UTF-8");
            destdir = TextUtil.replaceFileSeparator(destdir);
            ServerConsoleServlet.printSystemLog("destdir= " + destdir, ServerConsoleServlet.LOG_DEBUG);
            map.remove("dir");
            rdftype = (String) map.get("rdftype");
            map.remove("rdftype");
            if ((rdftype == null) || (!(rdftype instanceof String))) {
                ServerConsoleServlet.printSystemLog("rdftype is not specified. set to default. rdftype= " + SF.Document.getURI(), ServerConsoleServlet.LOG_INFO);
                rdftype = SF.Document.getURI();
            }
            if ((!this.isDocAdmin) && (rdftype.equals(SF.Document.getURI())) || ((!this.isWfAdmin) && (rdftype.equals(SF.Workflow.getURI())))) {
                ServerConsoleServlet.printSystemLog("organization= " + organization + " username=" + username, ServerConsoleServlet.LOG_INFO);
                try {
                    response.sendError(500, "authentication failure.");
                    return;
                } catch (IOException e) {
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
                }
            }
            menujsp = (String) map.get("menujsp");
            map.remove("menujsp");
            if ((menujsp == null) || (!(menujsp instanceof String))) {
                menujsp = ServerConsoleServlet.getOrganizationConfigByTagName("JSPPath", this.getOrganizationName()) + "menu.jsp";
            }
            projectid = (String) map.get("projectid");
            map.remove("projectid");
            if ((projectid == null) || (!(projectid instanceof String))) {
                projectid = "standard";
            }
            uri = (String) map.get("uri");
            map.remove("uri");
            if ((uri == null) || (!(uri instanceof String))) {
                uri = "";
            }
            makemetadata = (String) map.get("makemetadata");
            map.remove("makemetadata");
            if ((makemetadata == null) || (!makemetadata.equals("off"))) makemetadata = "on";
            Object filedata = map.get("file");
            if (!(filedata instanceof FileItem)) {
                ServerConsoleServlet.printSystemLog("This field is not an ordinary file. item.getName()= " + item.getName(), ServerConsoleServlet.LOG_INFO);
                response.sendError(500, "This field is not an ordinary file. item.getName()= " + item.getName());
            }
            item = (FileItem) filedata;
            resourceFileName = item.getName();
            resourceFileName = URLDecoder.decode(resourceFileName, "UTF-8");
            ServerConsoleServlet.printSystemLog("decoded resourceFileName= " + resourceFileName, ServerConsoleServlet.LOG_DEBUG);
            resourceFileName = resourceFileName.substring(resourceFileName.lastIndexOf('\\') + 1);
            String extension = "";
            if (resourceFileName.lastIndexOf('.') >= 0) {
                extension = resourceFileName.substring(resourceFileName.lastIndexOf('.') + 1);
                if (extension == null) extension = "";
            }
            if (uri.length() > 0) {
                String[] isLocal = ResourceModelHolder.getValuesForSubject(this.organization, SF.isLocal.getURI(), this.uri);
                String isLock = this.uri.substring(uri.lastIndexOf(".") + 1);
                if ((isLocal != null) && (isLocal.length > 0) && (isLocal[0].equals("true"))) {
                    newFileName = this.uri.substring(uri.lastIndexOf("/") + 1);
                } else if (isLock.equals("lock")) {
                    newFileName = this.uri.substring(uri.lastIndexOf("/") + 1);
                } else {
                    response.sendError(500, "Cannot replace. This is not a local file.");
                    ServerConsoleServlet.printSystemLog("Cannot replace. This is not a local file." + targetfile.getAbsoluteFile(), ServerConsoleServlet.LOG_INFO);
                    return;
                }
            } else {
                newFileName = TextUtil.getNewFilename(extension);
            }
            targetfile = new File(ServerConsoleServlet.getRepositoryLocalDirectory() + File.separator + this.getOrganizationName() + File.separator + destdir, newFileName);
            ServerConsoleServlet.printSystemLog("targetfile= " + targetfile.getAbsoluteFile(), ServerConsoleServlet.LOG_DEBUG);
            if (TextUtil.checkExtension(targetfile, "pdf")) {
                uri = ServerConsoleServlet.getServerRootURL(request) + "sharefast/servlet/" + ServerConsoleServlet.FILEACCESSSERVLET + ".pdf" + "?organization=" + organization + "&filename=" + ServerConsoleServlet.getConfigByTagName("ResourceDirName") + "/" + newFileName;
            } else {
                uri = ServerConsoleServlet.getServerRootURL(request) + "sharefast/servlet/" + ServerConsoleServlet.FILEACCESSSERVLET + "?organization=" + organization + "&filename=" + ServerConsoleServlet.getConfigByTagName("ResourceDirName") + "/" + newFileName;
            }
            if (targetfile.exists()) FileUploadServlet.moveToObsoletePath(targetfile);
            item.write(targetfile);
            item.delete();
            if ((uri != null) && (uri.length() > 0) && (!ResourceModelHolder.containsResource(uri, this.organization))) {
                ResourceModelHolder.createResource(uri, this.organization);
            } else {
            }
            int newRev = ResourceModelHolder.incRevision(this.organization, uri);
            String[] strnewvals = new String[1];
            strnewvals[0] = resourceFileName;
            ResourceModelHolder.replaceProperty(this.uri, SF.filename.getURI(), strnewvals, this.getOrganizationName());
            strnewvals[0] = this.calcChechsum(targetfile);
            ResourceModelHolder.replaceProperty(this.uri, SF.checksum.getURI(), strnewvals, this.organization);
            strnewvals[0] = Long.toString(targetfile.length());
            ResourceModelHolder.replaceProperty(this.uri, SF.filesize.getURI(), strnewvals, this.organization);
            strnewvals[0] = "true";
            ResourceModelHolder.replaceProperty(this.uri, SF.isLocal.getURI(), strnewvals, this.organization);
            strnewvals[0] = TextUtil.getDcDate();
            ResourceModelHolder.replaceProperty(this.uri, DC_11.date.getURI(), strnewvals, this.getOrganizationName());
            Resource[] newvals = new Resource[1];
            if (!rdftype.equals("")) {
                Model tmp_model = ModelFactory.createDefaultModel();
                newvals[0] = tmp_model.createResource(rdftype);
                ResourceModelHolder.replaceProperty(this.uri, RDF.type.getURI(), newvals, this.organization);
            }
            if (!projectid.equals("")) {
                Model tmp_model = ModelFactory.createDefaultModel();
                newvals[0] = tmp_model.createResource(rdftype);
                newvals[0] = tmp_model.createResource(MetaEditServlet.convertProjectIdToProjectUri(this.organization, projectid, request));
                ResourceModelHolder.replaceProperty(this.uri, SF.belongsTo.getURI(), newvals, this.organization);
            }
            SFIndexer sfi = new SFIndexer(this.organization);
            sfi.addFileToSearchIndexAsync(targetfile, uri);
            if (newRev > 1) {
                String title = "New Revision (Rev. " + newRev + ") uploaded.";
                String message = "";
                String mail = "";
                DiscussionServlet.addNewThread(this.uri, this.username, this.organization, title, message, mail);
            } else {
                if (SF.Document.getURI().equals(ResourceModelHolder.getRdfType(this.organization, this.uri))) {
                    UserDatabase ud = ServerConsoleServlet.getUserDatabase(this.organization);
                    UserInfo[] ui = ud.getAllUserInfo();
                    for (int i = 0; i < ui.length; i++) {
                        if ((ui[i] != null) && ui[i].getMailNotification()) {
                            String body = "Hello " + ui[i].getUsername() + "!\n";
                            body += "New document arrived.\n";
                            body += "\n";
                            body += " Title: " + resourceFileName + "\n";
                            body += " Date: " + ResourceModelHolder.getDocInfo(this.organization, this.uri).getDate() + "\n";
                            body += " User: " + this.username + "\n";
                            body += "\n";
                            body += "Web Client: " + ServerConsoleServlet.getServerRootURL(request).replace("https:", "http:") + "/sharefast/jsp/en/webclient.jsp";
                            body += "?username=" + ui[i].getUsername() + "&organization=" + this.organization + "\n";
                            body += "\n";
                            body += "Thank you.\n";
                            body += "--\n";
                            body += "ShareFast http://www.sharefast.org/\n";
                            SMTPEngine.sendMail("[ShareFast] Document Updated <" + resourceFileName + ">", ui[i].getEmail(), body, organization);
                        }
                    }
                    UserLogServlet.printLogToDB("upload", resourceFileName, this.username, this.uri, this.organization, this.software);
                }
            }
            ResourceModelHolder.updateRdfFile(this.organization, uri);
            request.setAttribute("uri", uri);
            RequestDispatcher rDispatcher = request.getRequestDispatcher(menujsp);
            try {
                rDispatcher.forward(request, response);
            } catch (Exception e) {
                ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
                e.printStackTrace();
            }
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            try {
                response.sendError(500, "Error when writing file on this server.");
            } catch (IOException ie) {
                ServerConsoleServlet.printSystemLog(ie.toString() + " " + ie.getMessage(), ServerConsoleServlet.LOG_ERROR);
                e.printStackTrace();
            }
            return;
        }
        return;
    }

    /**
	 * FileUploadServlet#calcChechsum calculates checksum of specified file. <br>
	 *   e.g. 6C-B2-28-FB-AC-E4-7E-12-56-2B-48-1A-46-FB-56-33
	 * 
	 * @param	f
	 * @return	void
	 */
    private String calcChechsum(File f) {
        StringBuffer csum = new StringBuffer();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            int blocksize = 4096;
            byte[] buf = new byte[blocksize];
            int readlen = -1;
            while ((readlen = bis.read(buf)) != -1) {
                md5.update(buf, 0, readlen);
            }
            char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
            byte[] digest = md5.digest();
            for (int i = 0; i < digest.length; i++) {
                csum.append(hex[(digest[i] & 0xf0) >>> 4]);
                csum.append(hex[(digest[i] & 0x0f)]);
                if (i != digest.length - 1) csum.append("-");
            }
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
        }
        return csum.toString();
    }

    /**
	 * FileUploadServlet#moveToObsoletePath moves specifed file to obsolete path. <br>
	 * File name in obsolete path is number. <br>
	 *   e.g. file1.dat will be moved to ./obsolete/file1.dat/1 or ./obsolete/file1.data/(number)
	 * 
	 * @param	f
	 * @return	void
	 */
    public static void moveToObsoletePath(File deleteFile) {
        if (!(deleteFile instanceof File)) return;
        File obsoletePath = (new File(deleteFile.getParent() + File.separator + "obsolete"));
        if (!obsoletePath.exists()) obsoletePath.mkdir();
        File atticPath = (new File(deleteFile.getParent() + File.separator + "obsolete" + File.separator + deleteFile.getName()));
        if (!atticPath.exists()) atticPath.mkdir();
        File[] flist = atticPath.listFiles();
        int revMax = 0;
        for (int i = 0; i < flist.length; i++) {
            String fname = flist[i].getName();
            try {
                int rev = Integer.parseInt(fname);
                if (rev > revMax) revMax = rev;
            } catch (Exception e) {
            }
        }
        deleteFile.renameTo(new File(atticPath, Integer.toString(revMax + 1)));
        return;
    }

    /**
	 * return local FileName of specified uri. <br>
	 * return null if this uri is not in this server.
	 * 
	 * @param uri
	 * @return
	 */
    public static String convertURItoFileName(String uri, String organization) {
        String resourceFileName = null;
        if (uri.length() > (uri.lastIndexOf("/") + 1)) {
            resourceFileName = uri.substring(uri.lastIndexOf("/") + 1);
        } else {
            return null;
        }
        File resourceFile = new File(ServerConsoleServlet.getResourceLocalDirectory(organization) + File.separator + resourceFileName);
        if (resourceFile.exists()) return resourceFile.getName(); else return null;
    }

    /**
	 * return local File of specified uri. <br>
	 * return null if this uri is not in this server.
	 * 
	 * @param uri
	 * @return
	 */
    public static File convertURItoFile(String uri, String organization) {
        String filename = FileUploadServlet.convertURItoFileName(uri, organization);
        if (filename != null) {
            return new File(ServerConsoleServlet.getResourceLocalDirectory(organization), filename);
        } else {
            return null;
        }
    }
}
