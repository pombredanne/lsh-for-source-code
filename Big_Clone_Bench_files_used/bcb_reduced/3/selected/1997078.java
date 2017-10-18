package org.ps.seishin.crypt;

// ===========================================================================
//
// Seishin, Proyect Source 2011 (GNU GPLv3) (http://proyectsource.tk)
// @author Aitor Gonzalez Fernandez (jlebnikov)
//
// Based on:
//  GNU Crypto 2001-2006 (GNU GPL) (http://www.gnu.org/software/gnu-crypto/)
//  Jacksum 2003-2006 (GNU GPL) (http://jonelo.de/java/jacksum/)
//
// ===========================================================================

import gnu.crypto.hash.HashFactory;
import gnu.crypto.hash.IMessageDigest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.ps.seishin.utils.StringHelper;

public class HashManager {
    private static final Logger LOG = Logger.getLogger("HashManager(Log)");
    private static final Set<String> GNU_HASHES;
    
    static{
        GNU_HASHES = new HashSet<>();
        GNU_HASHES.add("MD2");
        GNU_HASHES.add("MD4");
        GNU_HASHES.add("MD5");
        GNU_HASHES.add("SHA1");
        GNU_HASHES.add("SHA-256");
        GNU_HASHES.add("SHA-384");
        GNU_HASHES.add("SHA-512");
        GNU_HASHES.add("RIPEMD-128");
        GNU_HASHES.add("RIPEMD-160");
        GNU_HASHES.add("Tiger");
        GNU_HASHES.add("Whirlpool");
    }
    
    public static ArrayList<String> getHashList() {
        ArrayList<String> list = new ArrayList<>();
        
        for (String hashname: GNU_HASHES) {
            list.add(hashname);
        }
        
        return list;
    }
    
    public static Map<String, String> getHash(File file, ArrayList<String> hashes) throws IOException {
        long temp = System.currentTimeMillis();
        
        final Map<String, IMessageDigest> mds = new HashMap<>();
        
        for (String h: hashes) {
            if (GNU_HASHES.contains(h)) {
                mds.put(h, HashFactory.getInstance(h));
            }
        }
        
        InputStream is = new FilterInputStream(new BufferedInputStream(
                new FileInputStream(file))){
                    @Override
                    public int read(byte[] b, int off, int len) throws IOException{
                        int leido = this.in.read(b, off, len);
                        if (leido != -1){
                            for (IMessageDigest md : mds.values()){
                                md.update(b, off, leido);
                            }
                        }
                        return leido;
                    }
                };
        
        byte[] buffer = new byte[65536]; // Buffer de 64Kb
        
        while (is.read(buffer) != -1){
            // No hay que hacer nada, el trabajo se hace en el FilterInputStream
        }
        
        Map<String, String> results = new HashMap<>();
        
        for (String algoritmo : hashes){
            results.put(algoritmo, StringHelper.getStringFromBytes(mds.get(algoritmo).digest()));
        }
        
        LOG.info(timeStampMessage(temp, System.currentTimeMillis(), "file (" + file.getName() + ")", hashes));
        
        return results;
    }
    
    public static Map<String, String> getHash(byte[] byteIn, ArrayList<String> hashes) {
        long temp = System.currentTimeMillis();
        
        final Map<String, IMessageDigest> mds = new HashMap<>();
        
        for (String h: hashes) {
            if (GNU_HASHES.contains(h)) {
                mds.put(h, HashFactory.getInstance(h));
            }
        }
        
        for (IMessageDigest md : mds.values()){
            for (int i = 0; i < byteIn.length; i++){
                md.update(byteIn[i]);
            }
        }
        
        Map<String, String> results = new HashMap<>();
        
        for (String algoritmo : hashes){
            results.put(algoritmo, StringHelper.getStringFromBytes(mds.get(algoritmo).digest()));
        }
        
        LOG.info(timeStampMessage(temp, System.currentTimeMillis(), "byte String", hashes));
        
        return results;
    }
    
    public static Map<String, String> getHash(String text, ArrayList<String> hashes) {
        
        byte[] textBytes = text.getBytes();
        
        return getHash(textBytes, hashes);
    }
    
    private static String timeStampMessage (long initTime, long lastTime, String origin, ArrayList<String> hashList) {
        String tsm = "Hash of " + origin + " with algorims: ";
        
        for (String s: hashList) {
            tsm += s + ", ";
        }
        
        tsm = tsm.substring(0, tsm.length() - 2);
        
        tsm += ". Completed in: " + ((initTime - lastTime) / 1000) + " seconds.";
        
        return tsm;
    }
}
