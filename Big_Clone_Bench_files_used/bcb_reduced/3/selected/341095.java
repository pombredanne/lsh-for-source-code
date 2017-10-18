package edu.mit.lcs.haystack.eclipse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.javaByteCode.JavaByteCodeCompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author Dennis Quan
 */
public class SetupAgent extends GenericService {

    static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SetupAgent.class);

    protected Resource m_defaultPackage;

    public void init(String basePath, ServiceManager manager, Resource res) throws ServiceException {
        super.init(basePath, manager, res);
        IRDFContainer source = m_serviceManager.getRootRDFContainer();
        ICompiler rdfCompiler = new RDFCodeCompiler(source);
        try {
            InputStream is = CoreLoader.getResourceAsStream("/bootstrap/packages.ad");
            rdfCompiler.compile(null, new InputStreamReader(is), null, null, null);
            is.close();
            s_logger.info("Compiled /bootstrap/packages.ad");
        } catch (Exception e) {
            s_logger.error("Failed to compile /bootstrap/packages.ad.", e);
        }
        URL[] boot = CoreLoader.getBootstrapURLs();
        for (int i = 0; i < boot.length; i++) {
            try {
                InputStream is = boot[i].openStream();
                rdfCompiler.compile(null, new InputStreamReader(is), null, null, null);
                is.close();
                s_logger.info("Compiled Adenine file " + boot[i]);
            } catch (Exception e) {
                s_logger.info("Failed to compile Adenine file " + boot[i], e);
            }
        }
        Vector packages = new Vector();
        packages.addElement(new Resource(SystemProperties.s_packageSet));
        for (int i = 0; i < boot.length; i++) {
            String urlString = boot[i].toString();
            String fileName = urlString.substring(urlString.indexOf("/bootstrap"), urlString.length() - 3);
            packages.addElement(new Resource("http://haystack.lcs.mit.edu" + fileName));
        }
        PackageFilterRDFContainer packageFilterRDFC = new PackageFilterRDFContainer(source, null);
        IRDFContainer authoringRDFC = null;
        if (source.supportsAuthoring()) authoringRDFC = (IRDFContainer) source;
        PackageInstallDisplay pid = new PackageInstallDisplay(m_serviceManager.getResource(), m_serviceManager.getIdentityManager().getUnauthenticatedIdentity(m_userResource), authoringRDFC, packages, packageFilterRDFC, source, new JavaByteCodeCompiler(packageFilterRDFC), m_serviceManager);
        try {
            new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, pid);
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        Resource[] data = Utilities.getResourceSubjects(Constants.s_rdf_type, Constants.s_config_OntologyData, source);
        for (int j = 0; j < data.length; j++) {
            String resourcePath = Utilities.getLiteralProperty(data[j], Constants.s_content_path, source);
            try {
                source.add(new Statement(data[j], Constants.s_rdf_type, Constants.s_content_JavaClasspathContent));
                source.add(new Statement(data[j], Constants.s_content_path, new Literal(resourcePath)));
            } catch (RDFException e) {
                s_logger.error("RDF exception: ", e);
            }
        }
    }

    public static String computeMD5(InputStream is) {
        if (is == null) return null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str;
            StringBuffer sb = new StringBuffer();
            while ((str = reader.readLine()) != null) sb.append(str + "\n");
            String byteChars = "0123456789abcdef";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(sb.toString().getBytes());
            sb = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                int loNibble = bytes[i] & 0xf;
                int hiNibble = (bytes[i] >> 4) & 0xf;
                sb.append(byteChars.charAt(hiNibble));
                sb.append(byteChars.charAt(loNibble));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("Could not find MD5 digest algorithm.");
        } catch (IOException e) {
            s_logger.error("Exception: ", e);
        }
        return null;
    }
}
