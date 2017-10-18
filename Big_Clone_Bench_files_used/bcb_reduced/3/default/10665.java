import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import javax.xml.stream.XMLStreamReader;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.xpath.AXIOMXPath;

/**
 * TEST QUE MUESTRA COMO VERIFICAR QUE LOS DATOS QUE APARECEN EN UNA FIRMA XADES EXPLICITO SON 
 * IGUALES QUE UN DOCUMENTO: 
 * 
 * Verifica firma /pluginsCIM/moduls/ESBClient/test/files/firma.xml correspondiente a la firma del fic: /pluginsCIM/moduls/ESBClient/test/files/datos.txt
 * 
 * @author rsanz
 *
 */
public class VerificarDatosXADES {

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream("moduls/ESBClient/test/files/firma.xml");
        XMLStreamReader reader = StAXUtils.createXMLStreamReader(in);
        StAXOMBuilder builder = new StAXOMBuilder(reader);
        OMElement documentElement = builder.getDocumentElement();
        AXIOMXPath xpath = null;
        OMElement node = null;
        xpath = new AXIOMXPath("/AFIRMA/CONTENT");
        node = (OMElement) xpath.selectSingleNode(documentElement);
        String hashB64Xades = node.getText();
        System.out.println(" ------------ HASH B64 EN XADES: " + hashB64Xades);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        copy(new FileInputStream("moduls/ESBClient/test/files/datos.txt"), bos);
        byte[] datosDoc = bos.toByteArray();
        MessageDigest dig = MessageDigest.getInstance("SHA1");
        byte[] hash = dig.digest(datosDoc);
        String hashB64Doc = new String(org.apache.commons.codec.binary.Base64.encodeBase64(hash));
        System.out.println(" ------------ HASH B64 DOC:      " + hashB64Doc);
        System.out.println(" ------------ IGUALES?: " + hashB64Doc.equals(hashB64Xades));
    }

    private static int copy(InputStream input, OutputStream output) throws IOException {
        byte buffer[] = new byte[4096];
        int count = 0;
        for (int n = 0; -1 != (n = input.read(buffer)); ) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
