package it.unibo.cs.ndiff.common.vdom.diffing;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author schirinz
 * 
 *         Classe per calcolare il valore hash di una stringa
 */
public class Hash {

    /**
	 * Traduce in esadecimale il contenuto di array
	 * 
	 * @param array
	 *            array da tradurre
	 * @return Stringa in esadecimale del contenuto di array
	 */
    private static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
        }
        return sb.toString();
    }

    /**
	 * Crea il valore hasj per il Nodo DOM passato in input
	 * 
	 * @param node
	 *            Nodo DOM sul quale calcolare il valore hash
	 * @return Stringa che rappresenta il valore Hash calcolato sul nodo
	 */
    public static String Hnode(Node node) {
        String ret = new String();
        switch(node.getNodeType()) {
            case Node.ELEMENT_NODE:
                ret += node.getNodeName() + ">>";
                if (node.hasAttributes()) {
                    NamedNodeMap att = node.getAttributes();
                    for (int i = 0; i < att.getLength(); i++) ret += att.item(i).getNodeName() + ":" + att.item(i).getNodeValue() + ">";
                }
                break;
            case Node.TEXT_NODE:
                ret += node.getTextContent() + ">>";
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                ret += node.getTextContent() + ">>";
                break;
            case Node.COMMENT_NODE:
                ret += node.getTextContent() + ">>";
                break;
            case Node.NOTATION_NODE:
                ret += node.getTextContent() + ">>";
                break;
            default:
        }
        if (node.hasChildNodes()) ret += ">>";
        return md5(ret);
    }

    /**
	 * Calcola il valore MD5 per la stringa passata in input
	 * 
	 * @param message
	 *            Stringa su cui calcolare il valore MD5
	 * @return Valore MD5
	 */
    public static String md5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("UTF8")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }
}
