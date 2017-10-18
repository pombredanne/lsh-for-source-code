package it.unibo.cs.csamegame;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Un SurfaceParser è un parser di mappe CSameGame, che le carica/salva da/su file
 * @author Statuto Riccardo <icymars@users.sourceforge.net>
 * @author Paolino Carmine <earcar@users.sourceforge.net>
 */
public class SurfaceParser {

    /**
     * Costruisce un SurfaceParser
     * @param aFile il file sul quale scrivere (dal quale leggere)
     */
    public SurfaceParser(String aFile) {
        file = aFile;
    }

    /**
     * Parsa il file e restituisce il terreno
     * @return il terreno di gioco
     * @throws IOException se qualcosa va storto nella lettura del file
     * @throws NoSuchAlgorithmException se non trova l'algoritmo di hash <B>MD5</B>
     */
    public Piece[][] parseFile() throws IOException, NoSuchAlgorithmException {
        BufferedReader LineReader = null;
        try {
            FileReader mappa = new FileReader(file);
            LineReader = new BufferedReader(mappa);
        } catch (FileNotFoundException e) {
            URL map = getClass().getResource(file);
            LineReader = new BufferedReader(new InputStreamReader(map.openStream()));
        }
        String[] linea;
        String line;
        while ((line = LineReader.readLine()) != null) {
            if ((linea = line.split(": "))[0].equals("Head")) {
                String[] info = linea[1].split(",");
                punteggio = Integer.parseInt(info[0]);
                hashcode = info[1];
                mode = Integer.parseInt(info[2]);
                checkPunteggio(punteggio, hashcode);
            } else {
                h++;
                l = line.length();
            }
        }
        LineReader.close();
        terreno = new Piece[l][h];
        loadMap();
        Piece[][] ground = terreno;
        return ground;
    }

    /**
     * @param punteggio2
     * @param hashcode2
     * @throws NoSuchAlgorithmException
     */
    private void checkPunteggio(int punteggio2, String hashcode2) throws NoSuchAlgorithmException {
        if (!getHash(punteggio2).equals(hashcode2)) {
            throw new SecurityException(java.util.ResourceBundle.getBundle("it/unibo/cs/csamegame/csamegame").getString("Hey,_non_bariamo_modificando_il_punteggio_nel_file_eh?!"));
        }
    }

    /**
     * @param punteggio3
     * @return blah
     * @throws NoSuchAlgorithmException
     */
    private String getHash(int punteggio3) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        byte[] points = Integer.toString(punteggio3).getBytes();
        md.update(points);
        byte[] hashish = md.digest();
        byte[] hash = new byte[16 + points.length];
        for (int i = 0; i < hashish.length; i++) {
            hash[i] = hashish[i];
        }
        hash[16] = '-';
        for (int i = 0; i < points.length; i++) {
            hash[i + 16] = points[i];
        }
        md.reset();
        md.update(hash);
        byte[] hashish_final = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (byte element : hashish_final) {
            String hex = Integer.toHexString(0xFF & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String hash_final = hexString.toString();
        return hash_final;
    }

    private void loadMap() throws IOException {
        BufferedReader LineReader = null;
        try {
            FileReader mappa = new FileReader(file);
            LineReader = new BufferedReader(mappa);
        } catch (FileNotFoundException e) {
            URL map = getClass().getResource(file);
            LineReader = new BufferedReader(new InputStreamReader(map.openStream()));
        }
        String line;
        for (int i = h - 1; i >= 0; i--) {
            line = LineReader.readLine();
            for (int j = 0; j < l; j++) {
                char type = line.charAt(j);
                if (type != '.') if (isNotIn(type)) {
                    n++;
                    listaElem.add(type + "");
                }
                Piece pezzo = new Piece(type);
                terreno[j][i] = pezzo;
            }
        }
        LineReader.close();
    }

    /**
     * Salva la mappa su file
     * @param path il path completo al file
     * @param ground Il terreno di gioco
     * @param score il punteggio
     * @param style lo stile di gioco
     * @throws IOException se qualcosa va storto nella scrittura del file
     * @throws NoSuchAlgorithmException se non trova l'algoritmo di hash <B>MD5</B>
     */
    public void saveMap(String path, Surface ground, int score, int style) throws IOException, NoSuchAlgorithmException {
        int l = ground.getL();
        int h = ground.getH();
        if (!path.endsWith(".txt")) {
            path += ".txt";
        }
        Piece[][] terreno = ground.getSurface();
        FileWriter fw = new FileWriter(path);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter outFile = new PrintWriter(bw);
        for (int i = h - 1; i >= 0; i--) {
            String line = "";
            for (int j = 0; j < l; j++) {
                line += terreno[j][i].getType();
            }
            outFile.println(line);
        }
        outFile.println("Head: " + score + "," + getHash(score) + "," + style);
        outFile.close();
    }

    private boolean isNotIn(char type) {
        for (int k = 0; k < listaElem.size(); k++) {
            if (type == listaElem.get(k).charAt(0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Restituisce l'altezza del terreno di gioco
     * @return l'altezza del terreno di gioco
     */
    public int getH() {
        return h;
    }

    /**
     * Restituisce la larghezza del terreno di gioco
     * @return la larghezza del terreno di gioco
     */
    public int getL() {
        return l;
    }

    /**
     * Restituisce il punteggio
     * @return il punteggio
     */
    public int getPunteggio() {
        return punteggio;
    }

    /**
     * Restituisce il numero di tipi di pedine
     * @return il numero di tipi di pedine
     */
    public int getN() {
        return n;
    }

    /**
     * Restituisce lo stile di gioco
     * @return lo stile di gioco
     */
    public int getStyle() {
        return mode;
    }

    private String hashcode;

    private int h;

    private int l;

    private int n;

    private int mode;

    private Piece[][] terreno;

    private String file;

    private int punteggio = 0;

    private ArrayList<String> listaElem = new ArrayList<String>();
}
