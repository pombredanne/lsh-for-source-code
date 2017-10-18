package eu.planets_project.ifr.core.services.fixity.javadigest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;
import com.sun.xml.ws.developer.StreamingAttachment;
import eu.planets_project.ifr.core.services.fixity.javadigest.utils.JavaDigestDescription;
import eu.planets_project.ifr.core.services.fixity.javadigest.utils.JavaDigestUtils;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.fixity.Fixity;
import eu.planets_project.services.fixity.FixityResult;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * JavaDigest Fixity service.
 * First pass simply creates an MD5 checksum, will implement other supported algorithms
 * via a parameter
 * 
 * @author <a href="mailto:carl.wilson@bl.uk">Carl Wilson</a>
 */
@Stateless
@WebService(name = JavaDigest.NAME, serviceName = Fixity.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.fixity.Fixity")
@MTOM
@StreamingAttachment(parseEagerly = true, memoryThreshold = ServiceUtils.JAXWS_SIZE_THRESHOLD)
public final class JavaDigest implements Fixity, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -8087686018249395167L;

    private static Logger log = Logger.getLogger(JavaDigest.class.getName());

    private static final String NO_DATA_MESSAGE = "No data associated with Digital Object";

    private static final String NO_ALG_MESSAGE = "The MessageDigest function does not implement the algorithm ";

    private static final String SUCCESS_MESSAGE = "Digest calculated successfully";

    private static final int DEFAULT_CHUNK_SIZE = 1024;

    /** The name of the service / class */
    public static final String NAME = "JavaDigest";

    /**
	 * @see eu.planets_project.services.fixity.Fixity#calculateChecksum(DigitalObject, List)
	 */
    public FixityResult calculateChecksum(DigitalObject digitalObject, List<Parameter> parameters) {
        FixityResult retResult = null;
        ServiceReport retReport = null;
        try {
            URI requestedAlgId = this.getDigestIdFromParameters(parameters);
            MessageDigest messDigest = MessageDigest.getInstance(JavaDigestUtils.getJavaAlgorithmName(requestedAlgId));
            InputStream inStream = digitalObject.getContent().getInputStream();
            if (this.addStreamBytesToDigest(messDigest, inStream, JavaDigest.DEFAULT_CHUNK_SIZE) < 1) {
                JavaDigest.log.severe(JavaDigest.NO_DATA_MESSAGE);
                retResult = this.createErrorResult(ServiceReport.Status.TOOL_ERROR, JavaDigest.NO_DATA_MESSAGE);
                return retResult;
            }
            retReport = new ServiceReport(ServiceReport.Type.INFO, ServiceReport.Status.SUCCESS, JavaDigest.SUCCESS_MESSAGE);
            retResult = new FixityResult(JavaDigestUtils.getDefaultAlgorithmId().toString(), messDigest.getProvider().getName(), messDigest.digest(), null, retReport);
        } catch (NoSuchAlgorithmException e) {
            retResult = this.createErrorResult(ServiceReport.Status.TOOL_ERROR, e.getMessage() + " for algorithm " + JavaDigestUtils.getDefaultAlgorithmId() + ".");
        } catch (IOException e) {
            retResult = this.createErrorResult(ServiceReport.Status.TOOL_ERROR, e.getMessage());
        } catch (URISyntaxException e) {
            retResult = this.createErrorResult(ServiceReport.Status.TOOL_ERROR, e.getMessage());
        }
        return retResult;
    }

    /**
	 * @see eu.planets_project.services.PlanetsService#describe()
	 */
    public ServiceDescription describe() {
        return JavaDigestDescription.getDescription();
    }

    /**
	 * Feeds an input stream to the digest algorithm in chunks of the requested size
	 * 
	 * @param messDigest the java.security.MessageDigest checksum algorithm
	 * @param inStream the java.io.InputStream containing the byte sequence to be added to the digest
	 * @param chunkSize the size of the chunks to be fed to the digest algorithm in bytes, i.e. 1024 = 1KB chunks
	 * @return the total number of bytes in the stream
	 * @throws IOException when there's a problem reading from the InputStream inStream
	 */
    private int addStreamBytesToDigest(MessageDigest messDigest, InputStream inStream, int chunkSize) throws IOException {
        int totalBytes = 0;
        byte[] dataBytes = new byte[chunkSize];
        int numRead = inStream.read(dataBytes);
        while (numRead > 0) {
            messDigest.update(dataBytes, 0, numRead);
            totalBytes += numRead;
            numRead = inStream.read(dataBytes);
        }
        return totalBytes;
    }

    /**
	 * Creates an empty FixityResult containing a ServiceReport that contains error information
	 * for a failed invocation
	 * @param status The ServiceReport status to use
	 * @param message The String message for the ServiceReport
	 * @return a FixityResult that wraps the ServiceReport for return
	 */
    private FixityResult createErrorResult(ServiceReport.Status status, String message) {
        ServiceReport retReport = new ServiceReport(ServiceReport.Type.ERROR, status, message);
        return new FixityResult(retReport);
    }

    private URI getDigestIdFromParameters(List<Parameter> params) throws NoSuchAlgorithmException, URISyntaxException {
        URI retVal = JavaDigestUtils.getDefaultAlgorithmId();
        if (params != null) {
            for (Parameter param : params) {
                if (param.getName().equals(JavaDigestDescription.ALG_PARAM_NAME)) {
                    try {
                        if (JavaDigestUtils.hasAlgorithmById(URI.create(param.getValue()))) return URI.create(param.getValue());
                        throw new NoSuchAlgorithmException(NO_ALG_MESSAGE + param.getValue());
                    } catch (IllegalArgumentException e) {
                        throw (URISyntaxException) e.getCause();
                    }
                }
            }
        }
        return retVal;
    }
}
