package com.levigo.jbig2;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import javax.imageio.stream.ImageInputStream;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.levigo.jbig2.io.DefaultInputStreamFactory;

@RunWith(Parameterized.class)
public class ChecksumTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { "/images/042_1.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_2.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_3.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_4.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_5.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_6.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_7.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_8.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_9.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_10.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_11.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_12.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_15.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_16.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_17.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_18.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_19.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_20.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_21.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_22.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_23.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_24.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/042_25.jb2", "69-26-6629-1793-107941058147-58-79-37-31-79" }, { "/images/amb_1.jb2", "58311272494-318210035-125100-344625-126-79" }, { "/images/amb_2.jb2", "58311272494-318210035-125100-344625-126-79" }, { "/images/002.jb2", "-12713-4587-92-651657111-57121-1582564895" }, { "/images/003.jb2", "-37-108-89-33-78-5019-966-96-124-9675-1-108-24" }, { "/images/004.jb2", "-10709436-24-59-48-217114-37-85-3126-24" }, { "/images/005.jb2", "712610586-1224021396100112-102-77-1177851" }, { "/images/006.jb2", "-8719-116-83-83-35-3425-64-528667602154-25" }, { "/images/007.jb2", "6171-125-109-20-128-71925295955793-127-41-122" }, { "/images/sampledata_page1.jb2", "104-68-555325117-4757-48527676-9775-8432" }, { "/images/sampledata_page2.jb2", "104-68-555325117-4757-48527676-9775-8432" }, { "/images/sampledata_page3.jb2", "-7825-56-41-30-19-719536-3678580-61-2586" } });
    }

    private final String filepath;

    private final String checksum;

    public ChecksumTest(String filepath, String cksum) {
        this.filepath = filepath;
        this.checksum = cksum;
    }

    @Test
    public void compareChecksum() throws Throwable {
        int imageIndex = 1;
        InputStream is = getClass().getResourceAsStream(filepath);
        System.out.println("####################################");
        System.out.println("File: " + filepath);
        DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
        ImageInputStream iis = disf.getInputStream(is);
        JBIG2Document doc = new JBIG2Document(iis);
        long time = System.currentTimeMillis();
        Bitmap b = doc.getPage(imageIndex).getBitmap();
        long duration = System.currentTimeMillis() - time;
        byte[] digest = MessageDigest.getInstance("MD5").digest(b.getByteArray());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte toAppend : digest) {
            stringBuilder.append(toAppend);
        }
        System.out.println("Completed decoding in " + duration + " ms");
        System.out.println("####################################\n");
        Assert.assertEquals(checksum, stringBuilder.toString());
    }
}
