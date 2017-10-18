package org.pagger.data.picture.geo.nmea;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Franz Wilhelmstötter
 */
public class ChecksumTest {

    private final Map<String, Integer> _sentences = new HashMap<String, Integer>();

    public ChecksumTest() {
        _sentences.put("$GPRMC,103603,A,4644.9459,N,01025.0768,E,000.3,172.9,050908,,,A*71", Integer.parseInt("71", 16));
        _sentences.put("$GPRMC,103648,A,4644.9448,N,01025.0766,E,001.0,172.9,050908,,,A*72", Integer.parseInt("72", 16));
        _sentences.put("$GPVTG,172.9,T,,M,001.0,N,001.9,K,A*09", Integer.parseInt("09", 16));
        _sentences.put("$GPVTG,172.9,T,,M,000.1,N,000.3,K,A*02", Integer.parseInt("02", 16));
        _sentences.put("$GPGSV,3,1,09,05,20,238,00,09,61,303,52,12,39,233,52,15,52,175,49*74", Integer.parseInt("74", 16));
        _sentences.put("$GPRMC,103548,A,4644.9465,N,01025.0767,E,000.4,172.9,050908,,,A*7A", Integer.parseInt("7a", 16));
        _sentences.put("$GPGGA,103533,4644.9442,N,01025.0770,E,1,04,02.1,02199.9,M,047.7,M,,*43", Integer.parseInt("43", 16));
        _sentences.put("$GPGSV,3,2,09,17,41,078,52,18,24,270,00,22,17,308,00,26,37,159,41*75", Integer.parseInt("75", 16));
        _sentences.put("$GPGGA,103603,4644.9459,N,01025.0768,E,1,05,01.6,02200.1,M,047.7,M,,*4E", Integer.parseInt("4e", 16));
        _sentences.put("$GPVTG,172.9,T,,M,000.6,N,001.1,K,A*06", Integer.parseInt("06", 16));
    }

    @Test
    public void getNmeaChecksum() {
        for (Map.Entry<String, Integer> entry : _sentences.entrySet()) {
            String sentence = entry.getKey();
            int checksum = Checksum.getNmeaChecksum(sentence);
            Assert.assertEquals(entry.getValue(), new Integer(checksum));
        }
    }

    @Test
    public void digest() {
        for (Map.Entry<String, Integer> entry : _sentences.entrySet()) {
            String sentence = entry.getKey();
            int checksum = Checksum.digest(sentence);
            Assert.assertEquals(entry.getValue(), new Integer(checksum));
        }
    }

    @Test
    public void check() {
        for (Map.Entry<String, Integer> entry : _sentences.entrySet()) {
            Checksum.check(entry.getKey());
        }
    }
}
