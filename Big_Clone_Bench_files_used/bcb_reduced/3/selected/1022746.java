package chatteroll;

import java.math.BigInteger;
import java.security.MessageDigest;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

/**
 *
 * @author NeoSkye
 */
public class Utility {

    public static String MD5Hash(String to_hash) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        md5.digest(to_hash.getBytes());
        BigInteger hash = new BigInteger(1, md5.digest(to_hash.getBytes()));
        return hash.toString(16);
    }

    public static InternetGatewayDevice OpenPort(int portnum) throws java.io.IOException {
        InternetGatewayDevice[] devices = InternetGatewayDevice.getDevices(5000);
        if (devices.length > 0) {
            java.net.InetAddress host = java.net.InetAddress.getLocalHost();
            byte[] ipaddress = host.getAddress();
            String str_ipaddress = "" + ipaddress[0] + ipaddress[1];
            str_ipaddress += "" + ipaddress[2] + ipaddress[3];
            try {
                boolean map_ok = devices[0].addPortMapping(null, null, portnum, portnum, str_ipaddress, 0, "TCP");
                if (!map_ok) {
                    throw new java.io.IOException("Port is already mapped.");
                }
                return devices[0];
            } catch (UPNPResponseException e) {
                System.out.println("Error with UPNP mapping: " + e.toString());
            }
        }
        return null;
    }
}
