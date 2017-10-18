package src;

import java.security.MessageDigest;

public abstract class AbstractKonto implements ISuperVisor {

    protected int nr;

    protected float saldo = 0;

    protected boolean gesperrt;

    protected Kunde kunde;

    protected String MD5pin = "";

    protected final SuperVisor Visor = new SuperVisor();

    public AbstractKonto(float startbetrag) {
        this.nr = NrProvider.getNextAccountIdNumber();
        saldo += startbetrag;
    }

    public AbstractKonto(float eroeffnungsgebuehr, float _saldo) {
        this.nr = NrProvider.getNextAccountIdNumber();
        this.saldo = _saldo;
        this.saldo -= eroeffnungsgebuehr;
    }

    public String printSV() {
        return Visor.printSV();
    }

    public String getName() {
        return Visor.getName();
    }

    public void setName(String name) {
        Visor.setName(name);
    }

    public EnumSuperVisor getKennung() {
        return Visor.getKennung();
    }

    public void setKennung(EnumSuperVisor Kennung) {
        Visor.setKennung(Kennung);
    }

    public abstract boolean writePin(String _pin);

    public abstract boolean checkPin(String _pin);

    public abstract String print();

    public float einzahlen(float betrag) throws KontoActionException {
        if (!gesperrt) {
            saldo += betrag;
        } else {
            KontoActionException kge = new KontoActionException();
            throw kge;
        }
        return saldo;
    }

    public float einzahlen(int euro, int cent) throws KontoActionException {
        float floatvar = ((float) euro) + (((float) cent) / 100);
        return einzahlen(floatvar);
    }

    public float einzahlen(int cent) throws KontoActionException {
        float floatvar = ((float) cent) / 100;
        return einzahlen(floatvar);
    }

    public boolean isGesperrt() {
        return this.gesperrt;
    }

    public void setGesperrt(boolean gesperrt) {
        this.gesperrt = gesperrt;
    }

    public float getSaldo() {
        return this.saldo;
    }

    public int getNr() {
        return this.nr;
    }

    public Kunde getKunde() {
        return kunde;
    }

    public abstract float getLimit();

    public static String makeMD5(String pin) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(pin.getBytes());
            byte[] hash = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public abstract float auszahlen(float betrag) throws KontoActionException;
}
