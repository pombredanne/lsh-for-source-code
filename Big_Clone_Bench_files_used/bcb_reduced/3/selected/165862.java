package de.mindcrimeilab.xsanalyzer.xsext;

import java.security.MessageDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.xs.XSTypeDefinition;
import de.mindcrimeilab.xsanalyzer.XSAnalyzerConstants;
import de.mindcrimeilab.xsanalyzer.util.XSModelHelper;

/**
 * @author Michael Engelhardt<me@mindcrime-ilab.de>
 * @author $Author: agony $
 * @version $Revision: 165 $
 * 
 */
public class AbstractTypeDescription {

    protected static final Log logger = LogFactory.getLog("xsAnalyzerApplicationLogger");

    protected static String getSignatureDigest(String signature, MessageDigest messageDigest) {
        byte[] digest = messageDigest.digest(signature.toString().getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(Integer.toHexString(b & 0xff));
        }
        return sb.toString();
    }

    /**
     * @param signature
     * @param baseType
     */
    protected static void appendBaseType(StringBuilder signature, XSTypeDefinition baseType) {
        signature.append(baseType.getNamespace()).append(":");
        signature.append(baseType.getName()).append(":");
    }

    protected static XSTypeDefinition getBaseTypeRecursive(XSTypeDefinition type) {
        if (XSAnalyzerConstants.XML_SCHEMA_NAMESPACE.equals(type.getNamespace())) {
            return type;
        } else {
            XSTypeDefinition base = XSModelHelper.getBaseType(type);
            return (null == base) ? type : AbstractTypeDescription.getBaseTypeRecursive(base);
        }
    }

    public AbstractTypeDescription() {
        super();
    }
}
