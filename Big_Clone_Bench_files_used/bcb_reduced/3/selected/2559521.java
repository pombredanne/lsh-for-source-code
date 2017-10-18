package org.infoeng.icws.documents;

import org.infoeng.icws.utils.ICWSConstants;
import org.infoeng.icws.utils.Utils;
import java.security.MessageDigest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class InformationIdentifier {

    private String urlType = "url";

    private String textType = "text";

    private boolean unmodifiedUnderlier;

    private String underlierLocator = null;

    private String digestValue = null;

    private String digestValueAlgorithm = null;

    private String underlierType = null;

    public InformationIdentifier() {
        underlierLocator = null;
        digestValue = null;
        digestValueAlgorithm = new String(ICWSConstants.sha1AlgorithmURL);
    }

    public InformationIdentifier(String ul) throws Exception {
        try {
            if (ul == null || "".equals(ul)) {
                throw new Exception("underlier must not be null or empty");
            }
            if (!ul.equals(Utils.addEntities(ul))) {
                throw new Exception("no xml special characters may be present in the underlier! underlier: " + ul + "");
            }
            underlierLocator = new String(ul);
            digestValueAlgorithm = new String(ICWSConstants.sha1AlgorithmURL);
            MessageDigest sha = MessageDigest.getInstance("SHA1");
            digestValue = new String(Utils.toHex(sha.digest(ul.getBytes())));
            underlierType = new String(textType);
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public InformationIdentifier(String ul, String dv) throws Exception {
        if (ul == null || dv == null) {
            throw new Exception("You must provide non-null variables!");
        }
        underlierLocator = new String(ul);
        digestValue = new String(dv);
        digestValueAlgorithm = new String(ICWSConstants.sha1AlgorithmURL);
        underlierType = new String("url");
    }

    public InformationIdentifier(String ul, String dv, String ulType, String algorithm) throws Exception {
        if (ul == null || dv == null) {
            throw new Exception("You must provide non-null variables!");
        }
        underlierLocator = new String(ul);
        digestValue = new String(dv);
        if (algorithm == null || "".equals(algorithm)) {
            digestValueAlgorithm = new String(ICWSConstants.sha1AlgorithmURL);
        } else {
            digestValueAlgorithm = new String(algorithm);
        }
        if (ulType == null) {
            underlierType = new String("url");
        } else {
            underlierType = new String(ulType);
        }
        if (!("url".equals(ulType) || "text".equals(ulType) || ulType == null)) {
            underlierType = "url";
        }
    }

    public InformationIdentifier(String ul, String dv, String dvAlg) {
        if (ul == null || dv == null || dvAlg == null) {
            return;
        }
        if (ul != null) {
            underlierLocator = new String(ul);
        }
        if (dv != null) {
            digestValue = new String(dv);
        }
        if (dvAlg != null) {
            digestValueAlgorithm = new String(dvAlg);
        }
        underlierType = new String(urlType);
    }

    public InformationIdentifier(Element rootElement) {
        if (rootElement == null) {
            return;
        }
        underlierLocator = null;
        digestValue = null;
        if (rootElement.getElementsByTagName("digestValue").getLength() != 0) {
            Element sidElem = (Element) rootElement.getElementsByTagName("digestValue").item(0);
            digestValueAlgorithm = sidElem.getAttribute("algorithm");
            digestValue = new String(sidElem.getTextContent());
        }
        if (rootElement.getElementsByTagName("underlierLocator").getLength() != 0) {
            Element sidElem = (Element) rootElement.getElementsByTagName("underlierLocator").item(0);
            underlierLocator = sidElem.getTextContent();
            underlierType = sidElem.getAttribute("type");
        }
    }

    public String getUnderlierType() {
        return underlierType;
    }

    public void setUnderlierType(String ulType) throws Exception {
        if (ulType == null) throw new Exception("underlier type must not be null.");
        underlierType = new String(ulType);
    }

    public String getDigestValueAlgorithm() {
        return digestValueAlgorithm;
    }

    public void setDigestValueAlgorithm(String alg) {
        if (alg != null) {
            digestValueAlgorithm = new String(alg);
        }
    }

    public String getDigestValue() {
        if (digestValue == null || "".equals(digestValue)) return null;
        return digestValue.trim();
    }

    public void setDigestValue(String dv) {
        if (dv != null) {
            digestValue = new String(dv);
        } else {
            digestValue = null;
        }
    }

    public String getUnderlierLocator() {
        if (underlierLocator == null || "".equals(underlierLocator)) return null;
        return underlierLocator.trim();
    }

    public void setUnderlierLocator(String ul) {
        if (ul == null || "".equals(ul)) {
            underlierLocator = null;
        }
        underlierLocator = new String(ul);
    }

    public void setUnmodifiedUnderlierLocator(String ul) throws Exception {
        if (ul == null || "".equals(ul.trim())) {
            throw new Exception(" ul must not be null or empty.");
        }
        underlierLocator = new String(ul);
    }

    public Element getElement(Document doc) {
        Element iiElem = (Element) doc.createElement("InformationIdentifier");
        Element ulElem = (Element) doc.createElement("underlierLocator");
        Element dvElem = (Element) doc.createElement("digestValue");
        dvElem.setAttribute("algorithm", new String(ICWSConstants.sha1AlgorithmURL));
        if (getUnderlierType() == null) {
            ulElem.setAttribute("type", "url");
        } else {
            ulElem.setAttribute("type", getUnderlierType());
        }
        dvElem.setTextContent(getDigestValue());
        ulElem.setTextContent(Utils.convertEntities(getUnderlierLocator()));
        iiElem.appendChild(doc.createTextNode("\n"));
        iiElem.appendChild(dvElem);
        iiElem.appendChild(doc.createTextNode("\n"));
        iiElem.appendChild(ulElem);
        iiElem.appendChild(doc.createTextNode("\n"));
        return iiElem;
    }

    public String toString() {
        if ((getUnderlierLocator() == null) && (getDigestValue() == null)) return null;
        String retStr = "<InformationIdentifier>\n";
        if (getUnderlierLocator() != null) {
            retStr += "<underlierLocator";
            if (getUnderlierType() != null) {
                retStr += " type=\"";
                retStr += getUnderlierType();
                retStr += "\"";
            }
            retStr += ">" + getUnderlierLocator() + "</underlierLocator>\n";
        }
        if (getDigestValue() != null) {
            retStr += "<digestValue";
            if (getDigestValueAlgorithm() != null) {
                retStr += " algorithm=\"" + getDigestValueAlgorithm() + "\"";
            }
            retStr += ">" + getDigestValue() + "</digestValue>\n";
        }
        retStr += "</InformationIdentifier>\n";
        return retStr;
    }
}
