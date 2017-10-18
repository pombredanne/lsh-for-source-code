package mbis.service.confirmation;

import mbis.entity.confirmation.Confirmation;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Component
public class ConfirmationServiceImpl implements ConfirmationService {

    private String hashAlgorithm = "SHA";

    private Random randomNumberGenerator = new Random(System.currentTimeMillis());

    public String confirmationDigest(Confirmation confirmation) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(confirmation.getOwner().getEmail()).append(";");
        buffer.append(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(confirmation.getOwner().getCreated())).append(";");
        buffer.append(confirmation.getKey());
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest;
        byte[] message = buffer.toString().getBytes();
        md.update(message);
        digest = md.digest();
        return new String(Hex.encodeHex(digest));
    }

    public void verify(Confirmation confirmation, String verificationKey) throws InvalidConfirmationException {
        if (!confirmationDigest(confirmation).equals(verificationKey)) {
            throw new InvalidConfirmationException();
        }
        confirmation.setVerified(true);
    }

    public void confirm(Confirmation confirmation) throws ExpiredConfirmationException, ConfirmationAlreadyConfirmedException {
        if (confirmation.getConfirmed() == null) {
            Date now = new Date();
            if (!confirmation.isVerified()) {
                throw new ConfirmationNotVerifiedException();
            }
            if (now.after(confirmation.getValidUntil())) {
                throw new ExpiredConfirmationException();
            }
            confirmation.setConfirmed(new Date());
        } else {
            throw new ConfirmationAlreadyConfirmedException();
        }
    }

    public void generateNewKey(Confirmation confirmation) {
        confirmation.setKey(randomNumberGenerator.nextInt());
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
