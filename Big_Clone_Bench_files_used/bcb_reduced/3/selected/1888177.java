package br.com.lopes.gci.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import br.com.lopes.gci.exception.GCIException;
import sun.misc.BASE64Encoder;
import static br.com.lopes.gci.util.Constants.FINALIZANDO_METODO;
import static br.com.lopes.gci.util.Constants.INICIANDO_METODO;

public class Criptografia {

    private static final Logger LOGGER = Logger.getLogger(Criptografia.class);

    public static String encripta(String senha) throws GCIException {
        LOGGER.debug(INICIANDO_METODO + "encripta(String)");
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(senha.getBytes());
            BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.fatal(e.getMessage(), e);
            throw new GCIException(e);
        } finally {
            LOGGER.debug(FINALIZANDO_METODO + "encripta(String)");
        }
    }

    public static String descripta(String senha) throws GCIException {
        LOGGER.debug(INICIANDO_METODO + "descripta(String)");
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(senha.getBytes());
            BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.fatal(e.getMessage(), e);
            throw new GCIException(e);
        } finally {
            LOGGER.debug(FINALIZANDO_METODO + "descripta(String)");
        }
    }

    public static void main(String... strings) {
        try {
            System.out.println("a senha para o banco �: " + encripta("thiago"));
        } catch (GCIException e) {
            e.printStackTrace();
        }
    }
}
