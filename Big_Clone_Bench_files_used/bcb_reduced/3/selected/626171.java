package br.ufrj.cad.fwk.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import br.ufrj.cad.fwk.util.ResourceUtil;

public class SegurancaUtil {

    private static final String CRYPT_ALGORITHM = "MD5";

    private static Logger logger = Logger.getLogger(SegurancaUtil.class);

    public static String crypt(String senha) {
        String md5 = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(CRYPT_ALGORITHM);
            md.update(senha.getBytes());
            Hex hex = new Hex();
            md5 = new String(hex.encode(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            logger.error(ResourceUtil.getLOGMessage("_nls.mensagem.geral.log.crypt.no.such.algorithm", CRYPT_ALGORITHM));
        }
        return md5;
    }
}
