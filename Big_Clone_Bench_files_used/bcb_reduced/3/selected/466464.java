package mbis.service.confirmation;

import mbis.entity.confirmation.Confirmation;
import mbis.entity.person.Person;
import mbis.test.AbstractIntegrationTest;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import javax.annotation.Resource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * User: thooomas
 * Date: 17.10.2008
 * Time: 20:45:04
 */
public class ConfirmationServiceTest extends AbstractIntegrationTest {

    @Resource
    private ConfirmationService confirmationService;

    @Resource(name = "tmichalec")
    private Person person;

    private Calendar now = Calendar.getInstance();

    private Calendar validUntil = Calendar.getInstance();

    private Confirmation confirmation;

    private final int key = 123456789;

    @BeforeClass
    public void onSetUp() {
        validUntil.setTime(now.getTime());
        validUntil.add(Calendar.MONTH, 3);
    }

    @BeforeMethod()
    public void before() throws Exception {
        confirmation = new Confirmation() {
        };
        confirmation.setCreated(now.getTime());
        confirmation.setValidUntil(validUntil.getTime());
        confirmation.setKey(key);
        confirmation.setOwner(person);
    }

    @Test(expectedExceptions = InvalidConfirmationException.class)
    public void testConfirmationVerifyWithNull() throws InvalidConfirmationException {
        confirmationService.verify(confirmation, null);
    }

    @Test()
    public void testConfirmationVerify() throws InvalidConfirmationException {
        final String verificationKey = confirmationService.confirmationDigest(confirmation);
        confirmationService.verify(confirmation, verificationKey);
        Assert.assertTrue(confirmation.isVerified());
    }

    @Test
    public void testConfirmationDigest() throws InvalidConfirmationException, NoSuchAlgorithmException {
        StringBuffer buffer = new StringBuffer();
        buffer.append(confirmation.getOwner().getEmail()).append(";");
        buffer.append(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(confirmation.getOwner().getCreated())).append(";");
        buffer.append(confirmation.getKey());
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] message = buffer.toString().getBytes();
        md.update(message);
        byte[] digest = md.digest();
        String digestAsString = new String(Hex.encodeHex(digest));
        Assert.assertEquals(digestAsString, confirmationService.confirmationDigest(confirmation));
    }

    @Test()
    public void testConfirmationConfirm() throws ExpiredConfirmationException, ConfirmationAlreadyConfirmedException {
        confirmation.setVerified(true);
        confirmationService.confirm(confirmation);
        assertNotNull(confirmation.getConfirmed());
        Date now = new Date();
        assertTrue(now.after(confirmation.getConfirmed()) || now.equals(confirmation.getConfirmed()));
    }

    @Test(expectedExceptions = ExpiredConfirmationException.class)
    public void testConfirmationConfirmExpired() throws ExpiredConfirmationException, ConfirmationAlreadyConfirmedException {
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.MONTH, -1);
        confirmation.setVerified(true);
        confirmation.setCreated(now.getTime());
        confirmation.setValidUntil(validUntil.getTime());
        confirmationService.confirm(confirmation);
    }

    @Test(expectedExceptions = InvalidConfirmationException.class)
    public void testConfirmationVerifyWithInvalidKey() throws ExpiredConfirmationException, InvalidConfirmationException {
        Calendar validUntil = Calendar.getInstance();
        validUntil.add(Calendar.MONTH, -1);
        confirmation.setVerified(true);
        confirmation.setCreated(now.getTime());
        confirmation.setValidUntil(validUntil.getTime());
        confirmationService.verify(confirmation, "invalid");
    }
}
