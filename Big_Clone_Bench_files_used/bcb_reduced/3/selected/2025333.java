package br.com.algdam.hashutil;

import br.com.algdam.gui.Dialog;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Esta classe utiliza biblioteca proprietária da sun
 * com.sun.org.apache.xerces.internal.impl.dv.util.HexBin, porém os alertas
 * estão suprimidos pela annotation 
 * @SuppressWarnings(value="RetentionPolicy.SOURCE")
 * @author Tulio
 */
public class HashMD5 {

    /**
     * Gera código MD5 baseado em uma chave fornecida, o retorno do método é
     * um array de bytes (bytes[])
     * @param chave (String) chave que será convertida para MD5
     * @return (byte[]) array de bytes com o MD5
     */
    @SuppressWarnings(value = "RetentionPolicy.SOURCE")
    public static byte[] getHashMD5(String chave) {
        byte[] hashMd5 = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(chave.getBytes());
            hashMd5 = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            Dialog.erro(ex.getMessage(), null);
        }
        return (hashMd5);
    }

    /**
     * Gera código MD5 baseado em uma chave fornecida, o retorno do método é
     * em Hexadecimal retornado como Strig
     * @param chave (String) chave que será convertida para MD5
     * @return (String) string com o código em Hexadecimal
     */
    public static String getHexaHashMD5(String chave) {
        return (HexBin.encode(getHashMD5(chave)));
    }

    /**
     * Compara se dois arrays de byte (byte[]) MD5 são iguais
     * @param valor1 (byte[])
     * @param valor2 (byte[])
     * @return (boolean) true/false
     */
    public static boolean comparaMD5(byte[] valor1, byte[] valor2) {
        return (MessageDigest.isEqual(valor1, valor2));
    }

    /**
     * Compara se duas string Hexadecimal MD5 são iguais
     * @param hexa1 (String)
     * @param hexa2 (String)
     * @return (boolean) true/false
     */
    public static boolean comparaHexaMD5(String hexa1, String hexa2) {
        return (MessageDigest.isEqual(HexBin.decode(hexa1), HexBin.decode(hexa2)));
    }

    public static String converteMD5ByteToHexString(byte[] md5) {
        return (HexBin.encode(md5));
    }

    public static byte[] converteMD5HexStringToByte(String md5) {
        return (HexBin.decode(md5));
    }
}
