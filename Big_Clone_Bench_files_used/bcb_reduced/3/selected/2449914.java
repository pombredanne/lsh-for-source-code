package szyfrator_mars;

import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import mars_algorithm.MARS_Algorithm;

/**
 *
 * @author Karol Swider
 * @author mstrzyz
 */
public class DeszyfrowaniePliku {

    private final int ROZMIAR_BLOKU = 16;

    private String plikWejsciowy;

    private String plikWynikowy;

    private int dlugoscKlucza;

    private int dlugoscKluczaZaszyfrowanego;

    private String haslo;

    private String trybSzyfrowania;

    private byte[] iv;

    /** Creates a new instance of DeszyfrowaniePliku */
    public DeszyfrowaniePliku(String plikWe, String plikWy, String haslo) {
        this.plikWejsciowy = plikWe;
        this.plikWynikowy = plikWy;
        this.haslo = haslo;
    }

    /**
     * This method does the ciphering job. Depending on the value of
     * running_enc either encryption or decryption is done.
     *
     * @param way int Cipher.ENCRYPT or Cipher.DECRYPT
     * @return boolean
     */
    public void deszyfruj(MarsRamka okno) {
        byte[] kluczSesyjny = null;
        FileInputStream fInput = null;
        FileOutputStream fOutput = null;
        try {
            fInput = new FileInputStream(plikWejsciowy);
            fOutput = new FileOutputStream(plikWynikowy);
            DataInputStream daneWejsciowe = new DataInputStream(fInput);
            byte[] nazwa = new byte[5];
            daneWejsciowe.read(nazwa);
            int tryb = daneWejsciowe.readByte();
            switch(tryb) {
                case 1:
                    this.trybSzyfrowania = "ECB";
                    break;
                case 2:
                    this.trybSzyfrowania = "CBC";
                    break;
                case 3:
                    this.trybSzyfrowania = "CFB";
                    break;
                case 4:
                    this.trybSzyfrowania = "OFB";
                    break;
            }
            iv = new byte[ROZMIAR_BLOKU];
            daneWejsciowe.read(iv);
            this.dlugoscKlucza = daneWejsciowe.readShort();
            int reszta = dlugoscKlucza % ROZMIAR_BLOKU;
            int rozmiarKlucza = this.dlugoscKlucza;
            if (reszta != 0) rozmiarKlucza += ROZMIAR_BLOKU - reszta;
            byte[] zaszyfrowanyKlucz = new byte[rozmiarKlucza];
            daneWejsciowe.read(zaszyfrowanyKlucz);
            this.dlugoscKluczaZaszyfrowanego = zaszyfrowanyKlucz.length;
            kluczSesyjny = odszyfrujKlucz(zaszyfrowanyKlucz, rozmiarKlucza);
            this.odszyfrujDane(fInput, fOutput, kluczSesyjny, okno);
            fInput.close();
            daneWejsciowe.close();
            fOutput.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found! " + fnfe.getMessage());
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IOException! " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    private void odszyfrujDane(FileInputStream daneWejsciowe, FileOutputStream daneWyjsciowe, byte[] kluczSesyjny, MarsRamka okno) {
        okno.jLabelTrescKomun1.setText("Proszę czekać. Trwa deszyfrowanie...");
        try {
            byte[] dane = new byte[ROZMIAR_BLOKU];
            Object klucz = MARS_Algorithm.makeKey(kluczSesyjny);
            byte[] kolejnyBlok = new byte[ROZMIAR_BLOKU];
            boolean zapis = false;
            long rozmiarPliku = daneWejsciowe.getChannel().size();
            int liczbaBlokow = (int) rozmiarPliku / ROZMIAR_BLOKU;
            int i = 0;
            if (this.trybSzyfrowania.equalsIgnoreCase("ECB")) {
                while (daneWejsciowe.read(dane) != -1) {
                    if (zapis) daneWyjsciowe.write(kolejnyBlok);
                    kolejnyBlok = MARS_Algorithm.blockDecrypt(dane, 0, klucz);
                    zapis = true;
                    i++;
                    Utils.uaktualnijProgressBar(okno.jProgressBarDeszyfr, i, liczbaBlokow);
                }
            } else if (this.trybSzyfrowania.equalsIgnoreCase("CBC")) {
                byte[] poprzDane = new byte[ROZMIAR_BLOKU];
                while (daneWejsciowe.read(dane) != -1) {
                    if (zapis) daneWyjsciowe.write(kolejnyBlok);
                    kolejnyBlok = MARS_Algorithm.blockDecrypt(dane, 0, klucz);
                    if (!zapis) {
                        kolejnyBlok = Utils.operacjaXor(kolejnyBlok, iv, ROZMIAR_BLOKU);
                    } else {
                        kolejnyBlok = Utils.operacjaXor(kolejnyBlok, poprzDane, ROZMIAR_BLOKU);
                    }
                    System.arraycopy(dane, 0, poprzDane, 0, ROZMIAR_BLOKU);
                    zapis = true;
                    i++;
                    Utils.uaktualnijProgressBar(okno.jProgressBarDeszyfr, i, liczbaBlokow);
                }
            } else if (this.trybSzyfrowania.equalsIgnoreCase("CFB")) {
                byte[] rejestr = new byte[ROZMIAR_BLOKU];
                byte bajtWejsciowy;
                byte bajtWyjsciowy;
                byte[] daneZaszyfrowane = new byte[ROZMIAR_BLOKU];
                rozmiarPliku = rozmiarPliku - 24 - this.dlugoscKluczaZaszyfrowanego;
                rejestr = iv;
                for (i = 0; i < rozmiarPliku - 1; i++) {
                    daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                    bajtWejsciowy = (byte) daneWejsciowe.read();
                    bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                    daneWyjsciowe.write(bajtWyjsciowy);
                    for (int j = 1; j < ROZMIAR_BLOKU; j++) rejestr[j - 1] = rejestr[j];
                    rejestr[ROZMIAR_BLOKU - 1] = bajtWejsciowy;
                    Utils.uaktualnijProgressBar(okno.jProgressBarDeszyfr, i, rozmiarPliku);
                }
                bajtWejsciowy = (byte) daneWejsciowe.read();
                daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                if (bajtWyjsciowy != (byte) 0xAA) throw new Exception();
            } else if (this.trybSzyfrowania.equalsIgnoreCase("OFB")) {
                byte[] rejestr = iv;
                byte bajtWejsciowy;
                byte bajtWyjsciowy;
                byte[] daneZaszyfrowane = new byte[ROZMIAR_BLOKU];
                rozmiarPliku = rozmiarPliku - 24 - this.dlugoscKluczaZaszyfrowanego;
                for (i = 0; i < rozmiarPliku - 1; i++) {
                    daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                    bajtWejsciowy = (byte) daneWejsciowe.read();
                    bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                    daneWyjsciowe.write(bajtWyjsciowy);
                    for (int j = 1; j < ROZMIAR_BLOKU; j++) rejestr[j - 1] = rejestr[j];
                    rejestr[ROZMIAR_BLOKU - 1] = daneZaszyfrowane[0];
                    Utils.uaktualnijProgressBar(okno.jProgressBarDeszyfr, i, rozmiarPliku);
                }
                bajtWejsciowy = (byte) daneWejsciowe.read();
                daneZaszyfrowane = MARS_Algorithm.blockEncrypt(rejestr, 0, klucz);
                bajtWyjsciowy = (byte) (daneZaszyfrowane[0] ^ bajtWejsciowy);
                if (bajtWyjsciowy != (byte) 0xAA) throw new Exception();
            }
            if (zapis) {
                int koniec = kolejnyBlok[kolejnyBlok.length - 1];
                dane = new byte[kolejnyBlok.length - koniec];
                System.arraycopy(kolejnyBlok, 0, dane, 0, dane.length);
                daneWyjsciowe.write(dane);
            }
            okno.jLabelTrescKomun1.setText("Plik został odszyfrowany.");
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SzyfrowaniePliku.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioe) {
            System.out.println("IOException! " + ioe.getMessage());
            ioe.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(okno, "Błąd deszyfrowania!", "Błąd deszyfrowania", JOptionPane.ERROR_MESSAGE);
            okno.jLabelTrescKomun1.setText("Plik nie został odszyfrowany.");
            File plik = new File(this.plikWynikowy);
            plik.delete();
        }
    }

    /**
     *Metoda oblicza skrót hasła i szyfruje klucz sesyjny w trybie ECB, przyjmujac za klucz skrót hasła 
     */
    private byte[] odszyfrujKlucz(byte[] kluczSesyjny, int rozmiarKlucza) {
        byte[] odszyfrowanyKlucz = null;
        byte[] kluczTymczasowy = null;
        try {
            MessageDigest skrot = MessageDigest.getInstance("SHA-1");
            skrot.update(haslo.getBytes());
            byte[] skrotHasla = skrot.digest();
            Object kluczDoKlucza = MARS_Algorithm.makeKey(skrotHasla);
            byte[] tekst = null;
            kluczTymczasowy = new byte[rozmiarKlucza];
            int liczbaBlokow = rozmiarKlucza / ROZMIAR_BLOKU;
            for (int i = 0; i < liczbaBlokow; i++) {
                tekst = MARS_Algorithm.blockDecrypt(kluczSesyjny, i * ROZMIAR_BLOKU, kluczDoKlucza);
                System.arraycopy(tekst, 0, kluczTymczasowy, i * ROZMIAR_BLOKU, tekst.length);
            }
            odszyfrowanyKlucz = new byte[dlugoscKlucza];
            System.arraycopy(kluczTymczasowy, 0, odszyfrowanyKlucz, 0, dlugoscKlucza);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SzyfrowaniePliku.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return odszyfrowanyKlucz;
    }
}
