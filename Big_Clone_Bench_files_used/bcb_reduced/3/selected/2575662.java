package szyfrator_mars;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.util.logging.*;
import javax.swing.*;
import mars_algorithm.MARS_Algorithm;

/**
 *
 * @author karol
 * @author mstrzyz
 */
public class SzyfrowaniePliku {

    private final int ROZMIAR_BLOKU = 16;

    private String plikWejsciowy;

    private String plikWynikowy;

    private int dlugoscKlucza;

    private String haslo;

    private String trybSzyfrowania;

    private byte[] iv;

    /** Creates a new instance of SzyfrowaniePliku */
    public SzyfrowaniePliku(String plikWe, String plikWy, int dlugoscKlucza, String trybSzyfrowania, String haslo) {
        this.plikWejsciowy = plikWe;
        this.plikWynikowy = plikWy;
        this.dlugoscKlucza = dlugoscKlucza / 8;
        this.trybSzyfrowania = trybSzyfrowania;
        this.haslo = haslo;
        iv = new byte[ROZMIAR_BLOKU];
    }

    /**
     * This method does the ciphering job. Depending on the value of
     * running_enc either encryption or decryption is done.
     *
     * @param way int Cipher.ENCRYPT or Cipher.DECRYPT
     * @return boolean
     */
    public void szyfruj(MarsRamka okno) {
        byte[] kluczSesyjny = null;
        FileInputStream fInput = null;
        FileOutputStream fOutput = null;
        try {
            fInput = new FileInputStream(plikWejsciowy);
            fOutput = new FileOutputStream(plikWynikowy);
            DataOutputStream daneWyjsciowe = new DataOutputStream(fOutput);
            daneWyjsciowe.writeBytes("MARS ");
            if (this.trybSzyfrowania.equalsIgnoreCase("ECB")) daneWyjsciowe.writeByte(1); else if (this.trybSzyfrowania.equalsIgnoreCase("CBC")) daneWyjsciowe.writeByte(2); else if (this.trybSzyfrowania.equalsIgnoreCase("CFB")) daneWyjsciowe.writeByte(3); else if (this.trybSzyfrowania.equalsIgnoreCase("OFB")) daneWyjsciowe.writeByte(4);
            if (!this.trybSzyfrowania.equalsIgnoreCase("ECB")) setIv(okno);
            daneWyjsciowe.write(iv);
            daneWyjsciowe.writeShort(this.dlugoscKlucza);
            kluczSesyjny = this.stworzKlucz(this.dlugoscKlucza);
            daneWyjsciowe.write(this.szyfrujKlucz(kluczSesyjny));
            this.zaszyfrujDane(fInput, fOutput, kluczSesyjny, okno);
            fInput.close();
            daneWyjsciowe.close();
            fOutput.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found! " + fnfe.getMessage());
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IOException! " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    private void zaszyfrujDane(FileInputStream daneWejsciowe, FileOutputStream daneWyjsciowe, byte[] kluczSesyjny, MarsRamka okno) {
        okno.jLabelTrescKomun.setText("Proszę czekać. Trwa szyfrowanie...");
        try {
            byte[] dane = new byte[ROZMIAR_BLOKU];
            Object klucz = MARS_Algorithm.makeKey(kluczSesyjny);
            long rozmiarPliku = daneWejsciowe.getChannel().size();
            int liczbaBlokow = (int) rozmiarPliku / ROZMIAR_BLOKU;
            int reszta = (int) rozmiarPliku % ROZMIAR_BLOKU;
            if (this.trybSzyfrowania.equalsIgnoreCase("ECB")) {
                for (int i = 0; i < liczbaBlokow; i++) {
                    daneWejsciowe.read(dane);
                    daneWyjsciowe.write(MARS_Algorithm.blockEncrypt(dane, 0, klucz));
                    Utils.uaktualnijProgressBar(okno.jProgressBarSzyfr, i, liczbaBlokow);
                }
                dane = dopelnij(daneWejsciowe, reszta);
                daneWyjsciowe.write(MARS_Algorithm.blockEncrypt(dane, 0, klucz));
            } else if (this.trybSzyfrowania.equalsIgnoreCase("CBC")) {
                byte[] danePoOperacjiXor = new byte[ROZMIAR_BLOKU];
                byte[] daneZaszyfrowane = new byte[ROZMIAR_BLOKU];
                for (int i = 0; i < liczbaBlokow; i++) {
                    daneWejsciowe.read(dane);
                    if (i == 0) {
                        danePoOperacjiXor = Utils.operacjaXor(dane, iv, ROZMIAR_BLOKU);
                    } else {
                        danePoOperacjiXor = Utils.operacjaXor(dane, daneZaszyfrowane, ROZMIAR_BLOKU);
                    }
                    daneZaszyfrowane = MARS_Algorithm.blockEncrypt(danePoOperacjiXor, 0, klucz);
                    daneWyjsciowe.write(daneZaszyfrowane);
                    Utils.uaktualnijProgressBar(okno.jProgressBarSzyfr, i, liczbaBlokow);
                }
                dane = dopelnij(daneWejsciowe, reszta);
                if (liczbaBlokow == 0) {
                    danePoOperacjiXor = Utils.operacjaXor(dane, iv, ROZMIAR_BLOKU);
                } else {
                    danePoOperacjiXor = Utils.operacjaXor(dane, daneZaszyfrowane, ROZMIAR_BLOKU);
                }
                daneZaszyfrowane = MARS_Algorithm.blockEncrypt(danePoOperacjiXor, 0, klucz);
                daneWyjsciowe.write(daneZaszyfrowane);
            } else if (this.trybSzyfrowania.equalsIgnoreCase("CFB")) {
                byte[] rejestr = new byte[ROZMIAR_BLOKU];
                byte bajtWejsciowy;
                byte bajtWyjsciowy;
                byte[] daneZaszyfrowane = new byte[ROZMIAR_BLOKU];
                rejestr = iv;
                for (int i = 0; i < rozmiarPliku; i++) {
                    daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                    bajtWejsciowy = (byte) daneWejsciowe.read();
                    bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                    daneWyjsciowe.write(bajtWyjsciowy);
                    for (int j = 1; j < ROZMIAR_BLOKU; j++) rejestr[j - 1] = rejestr[j];
                    rejestr[ROZMIAR_BLOKU - 1] = bajtWyjsciowy;
                    Utils.uaktualnijProgressBar(okno.jProgressBarSzyfr, i, rozmiarPliku);
                }
                byte ostatni = (byte) 0xAA;
                daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ ostatni);
                daneWyjsciowe.write(bajtWyjsciowy);
            } else if (this.trybSzyfrowania.equalsIgnoreCase("OFB")) {
                byte[] rejestr = iv;
                byte bajtWejsciowy;
                byte bajtWyjsciowy;
                byte[] daneZaszyfrowane = new byte[ROZMIAR_BLOKU];
                for (int i = 0; i < rozmiarPliku; i++) {
                    daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                    bajtWejsciowy = (byte) daneWejsciowe.read();
                    bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                    daneWyjsciowe.write(bajtWyjsciowy);
                    for (int j = 1; j < ROZMIAR_BLOKU; j++) rejestr[j - 1] = rejestr[j];
                    rejestr[ROZMIAR_BLOKU - 1] = daneZaszyfrowane[0];
                    Utils.uaktualnijProgressBar(okno.jProgressBarSzyfr, i, rozmiarPliku);
                }
                byte ostatni = (byte) 0xAA;
                daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ ostatni);
                daneWyjsciowe.write(bajtWyjsciowy);
            }
            okno.jLabelTrescKomun.setText("Plik został zaszyfrowany.");
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SzyfrowaniePliku.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioe) {
            System.out.println("IOException! " + ioe.getMessage());
            ioe.printStackTrace();
        } catch (Exception e) {
            okno.jLabelTrescKomun.setText("Plik nie został zaszyfrowany.");
        }
    }

    /**
     *Metoda oblicza skrót hasła i szyfruje klucz sesyjny w trybie ECB, przyjmujac za klucz skrót hasła 
     */
    private byte[] szyfrujKlucz(byte[] kluczSesyjny) {
        byte[] zaszyfrowanyKlucz = null;
        byte[] klucz = null;
        try {
            MessageDigest skrot = MessageDigest.getInstance("SHA-1");
            skrot.update(haslo.getBytes());
            byte[] skrotHasla = skrot.digest();
            Object kluczDoKlucza = MARS_Algorithm.makeKey(skrotHasla);
            int resztaKlucza = this.dlugoscKlucza % ROZMIAR_BLOKU;
            if (resztaKlucza == 0) {
                klucz = kluczSesyjny;
                zaszyfrowanyKlucz = new byte[this.dlugoscKlucza];
            } else {
                int liczbaBlokow = this.dlugoscKlucza / ROZMIAR_BLOKU + 1;
                int nowyRozmiar = liczbaBlokow * ROZMIAR_BLOKU;
                zaszyfrowanyKlucz = new byte[nowyRozmiar];
                klucz = new byte[nowyRozmiar];
                byte roznica = (byte) (ROZMIAR_BLOKU - resztaKlucza);
                System.arraycopy(kluczSesyjny, 0, klucz, 0, kluczSesyjny.length);
                for (int i = kluczSesyjny.length; i < nowyRozmiar; i++) klucz[i] = (byte) roznica;
            }
            byte[] szyfrogram = null;
            int liczbaBlokow = klucz.length / ROZMIAR_BLOKU;
            int offset = 0;
            for (offset = 0; offset < liczbaBlokow; offset++) {
                szyfrogram = MARS_Algorithm.blockEncrypt(klucz, offset * ROZMIAR_BLOKU, kluczDoKlucza);
                System.arraycopy(szyfrogram, 0, zaszyfrowanyKlucz, offset * ROZMIAR_BLOKU, szyfrogram.length);
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SzyfrowaniePliku.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return zaszyfrowanyKlucz;
    }

    /**
     * Metoda tworzy losowy klucz sesyjny.
     * @param dlugoscKlucza
     * @return
     */
    private byte[] stworzKlucz(int dlugoscKlucza) {
        byte[] kluczSesyjny = new byte[dlugoscKlucza];
        Random generator = new Random();
        generator.nextBytes(kluczSesyjny);
        return kluczSesyjny;
    }

    /**
     * Ustawia wektor inicjalizujacy
     */
    private void setIv(MarsRamka okno) {
        Wektor wektor = new Wektor(okno);
        wektor.run();
    }

    /**
     * metoda dopełniająca ostatni szyfrowany blok
     * @param daneWejsciowe
     * @param reszta
     * @return dopełniony blok
     */
    private byte[] dopelnij(FileInputStream daneWejsciowe, int reszta) throws IOException {
        byte[] dane = new byte[ROZMIAR_BLOKU];
        if (reszta == 0) {
            for (int i = 0; i < ROZMIAR_BLOKU; i++) dane[i] = (byte) ROZMIAR_BLOKU;
        } else {
            int roznica = ROZMIAR_BLOKU - reszta;
            daneWejsciowe.read(dane, 0, reszta);
            for (int i = reszta; i < ROZMIAR_BLOKU; i++) dane[i] = (byte) roznica;
        }
        return dane;
    }

    class Wektor {

        boolean stop = false;

        int x = 0, y = 0, i = 0, pos = 0;

        public Wektor(MarsRamka okno) {
            JOptionPane.showMessageDialog(okno, "Przez najbliższe 2 sekundy proszę poruszać kursorem myszy.\n Zostanie wygenerowany wektor początkowy.");
            okno.jLabelTrescKomun.setText("Proszę poruszać myszą ...");
            iv = new byte[ROZMIAR_BLOKU];
        }

        public void run() {
            while (!stop) {
                PointerInfo pI = MouseInfo.getPointerInfo();
                if ((pI.getLocation().getX() != x) || (pI.getLocation().getY() != y)) {
                    x = (int) pI.getLocation().getX();
                    y = (int) pI.getLocation().getY();
                    iv[i] = (byte) ((iv[i] << 1) + ((x & 0x01) ^ (y & 0x01)));
                    pos++;
                    if (pos == 7) {
                        pos = 0;
                        i++;
                    }
                    if (i == ROZMIAR_BLOKU) stop = true;
                }
            }
        }
    }
}
