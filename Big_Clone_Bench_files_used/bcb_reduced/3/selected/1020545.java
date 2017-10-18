package Shifu.MainServer.Tools;

import Shifu.MainServer.Management.*;
import java.util.Stack;
import java.io.*;
import java.util.*;
import java.security.*;
import java.math.BigInteger;

/**
 *@class Classe per la gestione dei file
 *@authors vr069316
 */
public class FileTools implements Serializable {

    /**
	 *Splitta il file
	 *@param file file da splittare
	 *@param numfile numero di chunck da creare
	 *@return descrittore dei file splittato
	 */
    public static FileChunkedDescriptor fileSplit(File file, int numfile) {
        int i = 0;
        List listchunk = new ArrayList();
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(file.getName());
            int chunk_dim = ((int) file.length() / numfile) + 1;
            byte[] buffer = new byte[chunk_dim];
            int s = in.read(buffer);
            while (s > 0) {
                File newfile = new File(file.getName() + "-" + i);
                newfile.createNewFile();
                System.out.println("FileTools: Creato un nuovo file");
                out = new FileOutputStream(newfile.getName());
                out.write(buffer, 0, s);
                ChunkDescriptor chunk = new ChunkDescriptor(newfile.getName(), file.getName(), i, generate_MD5(newfile), newfile.length());
                listchunk.add(chunk);
                System.out.println("FileTools: Creato nuovo chunck descriptor : " + chunk.toString());
                out.close();
                buffer = new byte[chunk_dim];
                s = in.read(buffer);
                i++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("FileTools: Errore: file non trovato !\n");
        } catch (IOException e) {
            System.out.println("FileTools: Errore: di IO \n");
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("FileTools: Impossibile chiudere il file ", e);
            }
        }
        FileChunkedDescriptor filedesc = new FileChunkedDescriptor(file.getName(), file.length(), generate_MD5(file), i, listchunk);
        return filedesc;
    }

    public static void Merge_Chunk(FileChunkedDescriptor fcd) {
        FileOutputStream out = null;
        FileInputStream in = null;
        File newfile = null;
        try {
            newfile = new File(fcd.getFileName());
            newfile.createNewFile();
            System.out.println("FileTools: Inizio rigenerazione del file : " + fcd.getFileName() + " hash : " + fcd.getHash());
            out = new FileOutputStream(fcd.getFileName());
            int i = 0;
            while (i < fcd.getElenco_chunk().size()) {
                ChunkDescriptor currentchunk = (ChunkDescriptor) fcd.getElenco_chunk().get(i);
                File chunk_read = new File(currentchunk.getChunkName());
                in = new FileInputStream(currentchunk.getChunkName());
                byte[] buffer = new byte[8192];
                int s = in.read(buffer);
                System.out.println("FileTools: Lettura del chunck : " + "num. : " + currentchunk.getPoschunk() + " | " + currentchunk.getChunkName() + " | hash : " + currentchunk.getHashchunk());
                System.out.print("FileTools: Controllo correttezza file : ->     ");
                if (currentchunk.getHashchunk().equals(generate_MD5(chunk_read))) System.out.println("OK"); else {
                    System.out.println("Fallito");
                    System.out.println("Hash nel descrittore : " + currentchunk.getHashchunk() + " hash del file letto : " + generate_MD5(chunk_read));
                    System.exit(-1);
                }
                while (s > 0) {
                    out.write(buffer, 0, s);
                    buffer = new byte[8192];
                    s = in.read(buffer);
                }
                in.close();
                i++;
            }
        } catch (IOException e) {
            System.out.println("FileTools: Errore di IO in Merge_Chunck !");
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException("FileTools: Impossibile chiudere il file  ", e);
            }
        }
        System.out.print("FileTools: File ricostruito ! \nControllo integrità file ->   ");
        if (fcd.getHash().equals(generate_MD5(newfile))) System.out.println("OK"); else {
            System.out.println("Fallito: hash md5 errato !");
            System.exit(-1);
        }
    }

    /**
	 *Genera l'hash MD5 di in file in input
	 *@param f file del quale fare l'hashing
	 *@return md5 del file
	 *@throws IOException se il file non esiste o non è leggibile
	 *@throws NoSuchAlgorithmException se non viene trovato l'algoritmo di hashing
	 */
    public static String generate_MD5(File f) {
        InputStream is = null;
        String output;
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(f);
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            output = bigInt.toString(16);
        } catch (IOException e) {
            throw new RuntimeException("FileTools: Impossibile processare il file per il calcolo dell'md5 ", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("FileTools: Algoritmo di hashing non trovato ", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("FileTools: Impossibile chiudere il file per calcolo md5 ", e);
            }
        }
        return output;
    }

    /**
	 *Controlla che l'hash MD5 del file sia uguale a quello dato
	 *@param f file da controllare
	 *@param hash l'hash del file 
	 *@return true se il file corrisponde dall'hash	
	 */
    public static boolean check_MD5(File f, String hash) {
        String currentHash = generate_MD5(f);
        System.out.println("FileTools: Hash del file:" + f.getName() + ": " + currentHash + " hash del descrittore: " + hash);
        return (currentHash.equals(hash));
    }
}
