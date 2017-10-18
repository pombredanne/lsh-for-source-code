package jaxlib.security.digest;

import jaxlib.array.ByteArrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: WhirlpoolTest.java 2726 2009-04-09 12:31:40Z joerg_wassmer $
 */
public final class WhirlpoolTest extends Object {

    public WhirlpoolTest() {
        super();
    }

    @Test
    public final void test() throws Exception {
        final Whirlpool md = new Whirlpool();
        String digest = ByteArrays.toHexString(md.digest("The quick brown fox jumps over the lazy dog".getBytes("US-ASCII"))).toUpperCase();
        assertEquals("B97DE512E91E3828B40D2B0FDCE9CEB3C4A71F9BEA8D88E75C4FA854DF36725F" + "D2B52EB6544EDCACD6F8BEDDFEA403CB55AE31F03AD62A5EF54E42EE82C3FB35", digest);
        digest = ByteArrays.toHexString(md.digest("".getBytes("US-ASCII"))).toUpperCase();
        assertEquals("19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A7" + "3E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3", digest);
    }

    @Test
    public final void test2() {
        final Whirlpool md = new Whirlpool();
        md.update("-1".getBytes());
        md.digest();
    }
}
