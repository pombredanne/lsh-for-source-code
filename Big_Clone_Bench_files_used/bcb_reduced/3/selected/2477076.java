package pl.edu.agh.ssm.market;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jws.WebService;
import org.apache.log4j.Logger;
import org.mortbay.log.Log;
import pl.edu.agh.ssm.LongMethods.Hash;
import pl.edu.agh.ssm.billing.client.annotations.BillingMethod;
import pl.edu.agh.ssm.billing.client.annotations.BillingOperationName;
import pl.edu.agh.ssm.market.billing.BillingMethods;

@WebService(endpointInterface = "pl.edu.agh.ssm.market.MarketWebService", serviceName = "MarketWebService")
public class MarketWebServiceImpl implements MarketWebService {

    Hashtable<String, Long> tokens;

    Map<String, String> tokensToUser;

    static Logger logger = Logger.getLogger(MarketWebServiceImpl.class);

    static int logutTime = 5;

    protected static final byte[] Hexhars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encode(byte[] b) {
        StringBuilder s = new StringBuilder(2 * b.length);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            s.append((char) Hexhars[v >> 4]);
            s.append((char) Hexhars[v & 0xf]);
        }
        return s.toString();
    }

    private void checkToken(String token) throws IllegalAccessException {
        Long d = tokens.get(token);
        if (d == null) {
            throw new IllegalAccessException("no such");
        } else if (((new Date()).getTime() - d) > logutTime * 1000) {
            tokensToUser.remove(token);
            throw new IllegalAccessException("token expired ");
        }
        tokens.put(token, (new Date()).getTime());
    }

    @Override
    public String grandToken(String userName, String password) {
        try {
            String token = encode(MessageDigest.getInstance("MD5").digest(("userName" + "password" + Math.random()).getBytes()));
            tokens.put(token, (new Date()).getTime());
            tokensToUser.put(token, userName);
            logger.info("token Granted");
            System.out.println("token grandted");
            return token;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String[] coutries = { "Poland", "Greece", "Spain", "USA", "China", "Australia", "Iceland", "U.K.", "Slovakia", "Singapour" };

    public MarketWebServiceImpl() {
        logger.info("konstruktor");
        tokens = new Hashtable<String, Long>(100);
        tokensToUser = new ConcurrentHashMap<String, String>();
    }

    @Override
    public int[] getCountriesID(String token) throws IllegalAccessException {
        checkToken(token);
        int[] ret = new int[coutries.length];
        for (int i = 0; i < ret.length; i++) ret[i] = i;
        return ret;
    }

    @Override
    public String getCountryNameFromID(int ID, String token) throws IllegalAccessException {
        checkToken(token);
        return coutries[ID];
    }

    @Override
    public double[][] getShareForecast(int countryID, int shareID, Date timeFrom, Date timeTo, String token) throws IllegalAccessException {
        checkToken(token);
        try {
            Hash h = new Hash();
            h.longProc2(1000000);
            h.longProc1();
            h.longProc1();
            h.longProc1();
        } catch (Exception e) {
            e.printStackTrace();
        }
        double[][] ret = new double[100][2];
        return ret;
    }

    @Override
    public double getSharePrice(int countryID, int shareID, Date time, String token) throws IllegalAccessException {
        checkToken(token);
        try {
            Hash h = new Hash();
            h.longProc2(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @BillingMethod(clazz = BillingMethods.class, name = "billShares")
    @BillingOperationName("billShares")
    public int[] getShares(int countryID, String token) throws IllegalAccessException {
        checkToken(token);
        try {
            Hash h = new Hash();
            h.longProc2(100000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getSharesName(int countryID, int shareID, String token) throws IllegalAccessException {
        checkToken(token);
        return null;
    }

    /**
	 * @return the tokensToUser
	 */
    public Map<String, String> getTokensToUser() {
        return tokensToUser;
    }

    private void cleanSuspendedUsers() {
        for (String token : tokens.keySet()) {
            Long d = tokens.get(token);
            if (((new Date()).getTime() - d) > logutTime * 1000) {
                tokens.remove(token);
                cleanSuspendedUsers();
                break;
            }
        }
    }

    public void kickOffAllUsers() {
        tokens.clear();
    }

    public int getLoggedUsers() {
        cleanSuspendedUsers();
        return tokens.keySet().size();
    }
}
