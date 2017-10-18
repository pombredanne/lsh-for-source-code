package amint.amintd;

import amint.util.*;
import java.util.*;
import java.util.logging.Logger;
import java.math.*;
import java.net.URL;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.sql.*;
import javax.sql.rowset.serial.SerialBlob;

public class MintInfo {

    /** static fields*/
    static final int ALGO_RSA = 1;

    private java.util.Date asOf;

    private java.util.Date updated;

    private boolean mintOwner;

    private int mintId;

    private int coinFormat;

    private int coinSize;

    private Conn conn;

    private String currencyName;

    private int currentSeries;

    private String exportName;

    private int keyAlgorithm;

    private byte[] keyHash;

    private int keySize;

    private Logger logger;

    private int lowestValidSeries;

    private String msgKeyGrip;

    private String nick;

    private PublicKey publicKey;

    private int[] denoms;

    private MintServices mintServices;

    public MintInfoHolder holder;

    public MintInfo() {
        mintId = -1;
        keySize = -1;
        coinSize = -1;
        keyAlgorithm = -1;
        coinFormat = -1;
        lowestValidSeries = 1;
        currentSeries = 0;
        mintOwner = false;
        denoms = new int[0];
        holder = new MintInfoHolder(this);
    }

    public static Vector<MintInfo> allMintInfos(Conn conn) {
        try {
            Vector<MintInfo> v = new Vector<MintInfo>();
            int j1 = 1;
            String query = "select mint_id from mints order by mint_id asc";
            PreparedStatement pstmt = conn.conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                MintInfo info = MintInfo.fetchById(conn, rs.getInt(1));
                v.add(info);
            }
            return v;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MintInfo fetchByHash(Conn conn, byte[] keyHash) {
        MintInfo info = new MintInfo();
        info.setConn(conn);
        info.setLogger(conn.logger);
        if (info.fetchInfo("keyHash", keyHash)) return info;
        conn.logger.severe("MintInfo not found keyHash=" + keyHash.toString());
        conn.addError("MintInfo not found keyHash=" + keyHash.toString());
        return null;
    }

    public static MintInfo fetchById(Conn conn, int mintId) {
        MintInfo info = new MintInfo();
        info.setConn(conn);
        info.setLogger(conn.logger);
        Integer id = new Integer(mintId);
        if (info.fetchInfo("mintId", id)) return info;
        conn.logger.severe("MintInfo not found mintid=" + mintId);
        conn.addError("MintInfo not found mintid=" + mintId);
        return null;
    }

    public static MintInfo fetchByNick(Conn conn, String nick) {
        MintInfo info = new MintInfo();
        info.setConn(conn);
        info.setLogger(conn.logger);
        if (info.fetchInfo("nick", nick)) return info;
        conn.logger.severe("MintInfo not found nick=" + nick);
        conn.addError("MintInfo not found nick=" + nick);
        return null;
    }

    private boolean fetchDenoms() {
        try {
            String query = "select denom from coin_keys where mint_id = ? and series = ? order by denom asc";
            PreparedStatement pstmt = conn.conn.prepareStatement(query);
            pstmt.setInt(1, mintId);
            pstmt.setInt(2, currentSeries);
            ResultSet rs = pstmt.executeQuery();
            Vector<Integer> vDenoms = new Vector<Integer>();
            while (rs.next()) {
                Integer i = new Integer(rs.getInt(1));
                vDenoms.add(i);
            }
            this.denoms = new int[vDenoms.size()];
            for (int j2 = 0; j2 < denoms.length; j2++) {
                denoms[j2] = vDenoms.elementAt(j2).intValue();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            conn.addError(e.toString());
            return false;
        }
    }

    private boolean fetchInfo(String option, Object o) {
        try {
            String query = "select mint_id,key_hash,key_size,key_algorithm,coin_size," + "coin_format,currency_name,export_name,nick,current_series," + "lowest_valid_series,public_key,as_of,owner_id,updated " + "from mints where ";
            if (option.equals("mintId")) {
                Integer mintId = (Integer) o;
                query = query + "mint_id = ?";
            } else if (option.equals("nick")) {
                query = query + " nick = ?";
            } else if (option.equals("keyHash")) {
                query = query + "key_hash = ?";
            } else {
                logger.severe("MintInfo fetchInfo unrecognized option" + option);
                return false;
            }
            PreparedStatement pstmt = conn.conn.prepareStatement(query);
            if (option.equals("mintId")) {
                Integer mintId = (Integer) o;
                pstmt.setInt(1, mintId.intValue());
            } else if (option.equals("nick")) {
                String nick = (String) o;
                pstmt.setString(1, nick);
            } else if (option.equals("keyHash")) {
                byte[] keyHash = (byte[]) o;
                pstmt.setBlob(1, new SerialBlob(keyHash));
            }
            ResultSet rs = pstmt.executeQuery();
            if (!rs.first()) {
                rs.close();
                return false;
            }
            Blob blob;
            int j1 = 1;
            mintId = rs.getInt(j1++);
            blob = rs.getBlob(j1++);
            keyHash = blob.getBytes((long) 1, (int) blob.length());
            keySize = rs.getInt(j1++);
            keyAlgorithm = rs.getInt(j1++);
            coinSize = rs.getInt(j1++);
            coinFormat = rs.getInt(j1++);
            currencyName = rs.getString(j1++);
            exportName = rs.getString(j1++);
            nick = rs.getString(j1++);
            currentSeries = rs.getInt(j1++);
            lowestValidSeries = rs.getInt(j1++);
            blob = rs.getBlob(j1++);
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(blob.getBytes((long) 1, (int) blob.length()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(pubKeySpec);
            asOf = rs.getDate(j1++);
            if (rs.getInt(j1++) == conn.getOwnerId()) mintOwner = true;
            updated = rs.getTimestamp(j1++);
            if (!fetchDenoms()) return false;
            mintServices = new MintServices(this);
            logger.fine("retrieved mint info");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error fetching MintInfo by " + option);
            logger.severe(e.toString());
            return false;
        }
    }

    public java.util.Date getAsOf() {
        return asOf;
    }

    public int getCoinFormat() {
        return coinFormat;
    }

    public PublicKey getCoinKey(int series, int denom) {
        try {
            PreparedStatement pstmt = conn.conn.prepareStatement("select public_key from coin_keys where mint_id = ? " + " and series = ? and denom = ?");
            int j1 = 1;
            pstmt.setInt(j1++, mintId);
            pstmt.setInt(j1++, series);
            pstmt.setInt(j1++, denom);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.first()) {
                logger.severe("Coin key not found mint_id = " + mintId + " series = " + series + " denom= " + denom);
                return null;
            }
            Blob blob = rs.getBlob(1);
            rs.close();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(blob.getBytes((long) 1, (int) blob.length()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(pubKeySpec);
            logger.fine("fetched coin key");
            return publicKey;
        } catch (Exception e) {
            logger.severe(e.toString());
            return null;
        }
    }

    public Vector<CoinKey> getCoinKeys() {
        Vector<CoinKey> v = new Vector<CoinKey>();
        try {
            int j1 = 1;
            PreparedStatement pstmt = conn.conn.prepareStatement("select series,denom,public_key from coin_keys " + "where mint_id = ? and series >= ? order by series,denom");
            pstmt.setInt(j1++, mintId);
            pstmt.setInt(j1++, lowestValidSeries);
            ResultSet rs = pstmt.executeQuery();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            while (rs.next()) {
                int series = rs.getInt(1);
                int denom = rs.getInt(2);
                Blob blob = rs.getBlob(3);
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(blob.getBytes((long) 1, (int) blob.length()));
                Key publicKey = keyFactory.generatePublic(pubKeySpec);
                CoinKey ck = new CoinKey(publicKey, series, denom, true);
                v.add(ck);
            }
            return v;
        } catch (Exception e) {
            logger.severe("Exception in MintInfo.getCoinKeys");
            logger.severe(e.toString());
            return null;
        }
    }

    public int getCoinSize() {
        return coinSize;
    }

    public Conn getConn() {
        return conn;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    int[] getCurrentDenoms() {
        return denoms;
    }

    public int getCurrentSeries() {
        return currentSeries;
    }

    public String getExportName() {
        return exportName;
    }

    public int getKeySize() {
        return keySize;
    }

    public int getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public byte[] getKeyHash() {
        return keyHash;
    }

    public Logger getLogger() {
        return logger;
    }

    public int getLowestValidSeries() {
        return lowestValidSeries;
    }

    public int getMintId() {
        return mintId;
    }

    public String getMsgKeyGrip() {
        return msgKeyGrip;
    }

    public String getNick() {
        return nick;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public MintServices getMintServices() {
        return mintServices;
    }

    public boolean insert() {
        if (!readyToCreate(true)) return false;
        MintInfo info2 = MintInfo.fetchByHash(conn, keyHash);
        if (info2 != null) {
            this.mintId = info2.getMintId();
            if (!this.asOf.after(info2.getAsOf())) return true;
            return this.update();
        }
        try {
            int j1 = 1;
            PreparedStatement pstmt = conn.conn.prepareStatement("insert into mints (owner_id,internal,key_hash,key_size," + "key_algorithm,coin_size,coin_format,currency_name,export_name,nick," + "current_series,lowest_valid_series,public_key,as_of) " + "values (?,0,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(j1++, conn.getRootId());
            pstmt.setBlob(j1++, new SerialBlob(keyHash));
            pstmt.setInt(j1++, keySize);
            pstmt.setInt(j1++, keyAlgorithm);
            pstmt.setInt(j1++, coinSize);
            pstmt.setInt(j1++, coinFormat);
            pstmt.setString(j1++, currencyName);
            pstmt.setString(j1++, exportName);
            pstmt.setString(j1++, nick);
            pstmt.setInt(j1++, currentSeries);
            pstmt.setInt(j1++, lowestValidSeries);
            pstmt.setBlob(j1++, new SerialBlob(publicKey.getEncoded()));
            pstmt.setDate(j1++, new java.sql.Date(asOf.getTime()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (!rs.next()) {
                logger.severe("unable to create new owner");
                conn.addError("unable to create new owner");
                return false;
            }
            mintId = rs.getInt(1);
            if (!update2()) return false;
            if (!fetchDenoms()) return false;
            logger.info("created new MintInfo id =" + mintId);
            return true;
        } catch (Exception e) {
            logger.severe("Error in Mint insert");
            logger.severe(e.toString());
            return false;
        }
    }

    private boolean insertCoinKeys(List<CoinKey> coinKeys) {
        try {
            int numKeys = coinKeys.size();
            PreparedStatement pstmt = conn.conn.prepareStatement("insert ignore into coin_keys (mint_id,series,denom,public_key) " + "values (?,?,?,?)");
            pstmt.setInt(1, mintId);
            for (CoinKey ck : coinKeys) {
                pstmt.setInt(2, ck.getSeries());
                pstmt.setInt(3, ck.getDenom());
                pstmt.setBlob(4, new SerialBlob(ck.getKey().getEncoded()));
                pstmt.executeUpdate();
                logger.info("inserted a coin key mint_id=" + mintId + " series = " + ck.getSeries() + " denom = " + ck.getDenom());
            }
            return true;
        } catch (Exception e) {
            logger.severe("Error in Mint insert");
            logger.severe(e.toString());
            return false;
        }
    }

    public boolean isMintOwner() {
        return mintOwner;
    }

    public boolean readyToCreate(boolean warn) {
        if (asOf == null) {
            if (warn) logger.warning("MintInfo missing as of date");
            return false;
        }
        if (coinSize <= 0) {
            if (warn) logger.warning("mintInfo missing coin size");
            return false;
        }
        if (coinFormat < 0) {
            if (warn) logger.warning("MintInfo missing coin format");
            return false;
        }
        if (currencyName == null) {
            if (warn) logger.warning("MintInfo missing currency name");
            return false;
        }
        if (exportName == null) {
            if (warn) logger.warning("MintInfo missing export name");
            return false;
        }
        if (nick == null) {
            if (warn) logger.warning("MintInfo missing nick");
            return false;
        }
        if (keyAlgorithm == 0) {
            if (warn) logger.warning("MintInfo keyAlgorithm not set");
            return false;
        }
        if (keySize <= 0) {
            if (warn) logger.warning("mintInfo missing key size");
            return false;
        }
        if (publicKey == null) {
            if (warn) logger.warning("MintInfo missing public key");
            return false;
        }
        return true;
    }

    public static int keyAlgorithmFromString(String algoName) {
        if (algoName.equals("RSA")) return MintInfo.ALGO_RSA;
        return -1;
    }

    public String keyAlgorithmToString() {
        if (this.keyAlgorithm == MintInfo.ALGO_RSA) return "RSA";
        return null;
    }

    public static String keyAlgorithmToString(int keyAlgorithm) {
        if (keyAlgorithm == MintInfo.ALGO_RSA) return "RSA";
        return null;
    }

    public String coinFormatToString() {
        return Coin.formatToString(coinFormat);
    }

    public void setAsOf(java.util.Date asOf) {
        this.asOf = asOf;
    }

    public void setCoinFormat(int coinFormat) {
        this.coinFormat = coinFormat;
    }

    public void setCoinFormat(String formatName) throws Exception {
        int coinFormat = Coin.formatFromString(formatName);
        if (coinFormat < 0) {
            Exception e = new Exception("Unknown coin format " + formatName);
            throw e;
        }
        this.coinFormat = coinFormat;
    }

    public void setCoinSize(int coinSize) {
        this.coinSize = coinSize;
    }

    public void setConn(Conn conn) {
        this.conn = conn;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public void setCurrentSeries(int currentSeries) {
        this.currentSeries = currentSeries;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public void setKeyAlgorithm(int keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public void setKeyAlgorithm(String algoName) throws Exception {
        int keyAlgorithm = keyAlgorithmFromString(algoName);
        if (keyAlgorithm < 0) {
            Exception e = new Exception("Unknown key algorithm " + algoName);
            throw e;
        }
        this.keyAlgorithm = keyAlgorithm;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setLowestValidSeries(int lowestValidSeries) {
        this.lowestValidSeries = lowestValidSeries;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setPublicKey(PublicKey pub) {
        this.publicKey = pub;
        try {
            RSAKey rsa = (RSAKey) pub;
            BigInteger modulus = rsa.getModulus();
            byte[] modbytes = modulus.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(modbytes);
            keyHash = md.digest();
            logger.info("set public key");
        } catch (Exception e) {
            logger.severe("Error setting MintInfo public key");
            logger.severe(e.toString());
        }
    }

    public boolean singleSig() {
        if (this.coinFormat == Coin.FMT_RANDOM) return true;
        return false;
    }

    public String[] toStringArray() {
        String[] s = new String[8];
        int j1 = 0;
        s[j1++] = "" + mintId;
        s[j1++] = nick;
        s[j1++] = exportName;
        if (mintOwner) {
            s[j1++] = "1";
        } else {
            s[j1++] = "0";
        }
        s[j1++] = Coin.formatToString(coinFormat);
        s[j1++] = currencyName;
        s[j1++] = Base64.encodeBytes(keyHash);
        String sDenoms = "";
        if (denoms.length > 0) {
            sDenoms = sDenoms + denoms[0];
        } else {
            sDenoms = "0";
        }
        for (int j2 = 1; j2 < denoms.length; j2++) {
            sDenoms = sDenoms + "," + denoms[j2];
        }
        s[j1++] = sDenoms;
        return s;
    }

    public static StringTable vectorToStringTable(Vector<MintInfo> v) {
        try {
            String[] headers = { "mint_id", "nick", "export_name", "owner", "coin_format", "currency_name", "key_hash", "denoms" };
            StringTable st = new StringTable(headers);
            for (MintInfo info : v) {
                st.addRow(info.toStringArray());
            }
            return st;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean update() {
        try {
            String query = "update mints set updated = now(), as_of = ?," + "current_series = ?,lowest_valid_series = ? where mint_id = ?";
            PreparedStatement pstmt = conn.conn.prepareStatement(query);
            int j1 = 1;
            pstmt.setDate(j1++, new java.sql.Date(asOf.getTime()));
            pstmt.setInt(j1++, currentSeries);
            pstmt.setInt(j1++, lowestValidSeries);
            pstmt.setInt(j1++, mintId);
            pstmt.executeUpdate();
            return update2();
        } catch (Exception e) {
            e.printStackTrace();
            conn.addError(e.toString());
            return false;
        }
    }

    private boolean update2() {
        try {
            insertCoinKeys(holder.getCoinKeys());
            mintServices = new MintServices(this);
            mintServices.reset();
            Set<Map.Entry<String, URL>> services = holder.getMintServices().entrySet();
            for (Map.Entry<String, URL> service : services) {
                mintServices.setUrl(service.getKey(), service.getValue());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            conn.addError(e.toString());
            return false;
        }
    }

    public boolean validateKeyHash(String keyHashString) {
        try {
            RSAKey rsa = (RSAKey) publicKey;
            BigInteger modulus = rsa.getModulus();
            byte[] modbytes = modulus.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(modbytes);
            byte[] keyHash = md.digest();
            if (keyHashString.equals(Base64.encodeBytes(keyHash))) return true;
        } catch (Exception e) {
            logger.severe("Error validating keyhash");
            logger.severe(e.toString());
            logger.severe(Util.getStackTop(e));
        }
        return false;
    }
}
