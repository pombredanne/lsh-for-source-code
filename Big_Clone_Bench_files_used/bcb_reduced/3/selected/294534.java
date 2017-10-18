package org.digitall.projects.gdigitall.lib.misc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class OP_LibSQL {

    public static void GuardaImagen(File _imgFile, String _query) {
        if (_imgFile != null) try {
            FileInputStream fis = new FileInputStream(_imgFile);
            PreparedStatement ps;
            Connection conn = OP_Proced.CreateConnection();
            ps = conn.prepareStatement(_query);
            ps.setBinaryStream(1, fis, (int) _imgFile.length());
            ps.executeUpdate();
            ps.close();
            fis.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private static String toHexadecimal(byte[] datos) {
        String resultado = "";
        ByteArrayInputStream input = new ByteArrayInputStream(datos);
        String cadAux;
        int leido = input.read();
        while (leido != -1) {
            cadAux = Integer.toHexString(leido);
            if (cadAux.length() < 2) resultado += "0";
            resultado += cadAux;
            leido = input.read();
        }
        return resultado;
    }

    public static String getMD5(String _pwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(_pwd.getBytes());
            return toHexadecimal(new String(md.digest()).getBytes());
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace();
            return "";
        }
    }
}
