package net.taylor.identity.util;

import net.taylor.inject.Locator;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.util.Base64;

@Name("net.taylor.util.encoder")
@Scope(ScopeType.EVENT)
@AutoCreate
@Install(precedence = Install.BUILT_IN)
public class Encoder {

    public String encode(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("MD5").digest(input.getBytes());
            return Base64.encodeBytes(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Encoder instance() {
        return Locator.getInstance(Encoder.class);
    }
}
