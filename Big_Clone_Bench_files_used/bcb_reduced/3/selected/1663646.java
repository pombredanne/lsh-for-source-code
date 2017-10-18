package net.jxta.impl.shell.bin.pse;

import net.jxta.id.ID;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.membership.MembershipService;
import net.jxta.protocol.ModuleImplAdvertisement;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;

/**
 * PSE.keys
 */
public class certs extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        boolean showChains = false;
        boolean showCerts = false;
        GetOpt options = new GetOpt(argv, 0, "lc");
        while (true) {
            int option;
            try {
                option = options.getNextOption();
            } catch (IllegalArgumentException badopt) {
                consoleMessage("Illegal argument :" + badopt);
                return syntaxError();
            }
            if (-1 == option) {
                break;
            }
            switch(option) {
                case 'c':
                    showChains = true;
                    break;
                case 'l':
                    showCerts = true;
                    break;
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }
        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }
        MembershipService membership = getGroup().getMembershipService();
        if (!(membership instanceof PSEMembershipService)) {
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement) membership.getImplAdvertisement();
            consoleMessage("Group membership service is not PSE. (" + mia.getDescription() + ")");
            return ShellApp.appMiscError;
        }
        PSEMembershipService pse = (PSEMembershipService) membership;
        if (null == pse.getDefaultCredential()) {
            consoleMessage("KeyStore has not been opened.");
            return ShellApp.appMiscError;
        }
        try {
            Iterator eachCert = Arrays.asList(pse.getPSEConfig().getTrustedCertsList()).iterator();
            if (eachCert.hasNext()) {
                consoleMessage("PSE Trusted Certs for " + getGroup().getPeerGroupName() + " [" + getGroup().getPeerGroupID() + "]");
            }
            while (eachCert.hasNext()) {
                ID aCert = (ID) eachCert.next();
                X509Certificate cert = pse.getPSEConfig().getTrustedCertificate(aCert);
                println(aCert.toString() + "\t[ " + cert.getSubjectX500Principal().getName() + " ]");
                if (showChains) {
                    X509Certificate certs[] = pse.getPSEConfig().getTrustedCertificateChain(aCert);
                    if (null != certs) {
                        for (X509Certificate aChainCert : Arrays.asList(certs)) {
                            if (showCerts) {
                                StringBuilder indent = new StringBuilder("\n" + aChainCert.toString().trim());
                                int from = indent.length();
                                while (from > 0) {
                                    int returnAt = indent.lastIndexOf("\n", from);
                                    from = returnAt - 1;
                                    if ((returnAt >= 0) && (returnAt != indent.length())) {
                                        indent.insert(returnAt + 1, "\t");
                                    }
                                }
                                println(indent.toString());
                                println(" ");
                            } else {
                                println("\t[ " + cert.getSubjectX500Principal().getName() + " ] Fingerprint (SHA1) : " + calcCertFingerPrint(aChainCert));
                            }
                        }
                    }
                } else {
                    if (showCerts) {
                        StringBuilder indent = new StringBuilder("\n" + cert.toString().trim());
                        int from = indent.length();
                        while (from > 0) {
                            int returnAt = indent.lastIndexOf("\n", from);
                            from = returnAt - 1;
                            if ((returnAt >= 0) && (returnAt != indent.length())) {
                                indent.insert(returnAt + 1, "\t\t");
                            }
                        }
                        println(indent.toString());
                        println(" ");
                    } else {
                        println("\t\tFingerprint (SHA1) : " + calcCertFingerPrint(cert));
                    }
                }
            }
        } catch (KeyStoreException failure) {
            printStackTrace("KeyStore failure while printing keys", failure);
            return ShellApp.appMiscError;
        } catch (IOException failure) {
            printStackTrace("IO failure while printing keys", failure);
            return ShellApp.appMiscError;
        } catch (Exception failure) {
            printStackTrace("Failure while printing keys", failure);
            return ShellApp.appMiscError;
        }
        return ShellApp.appNoError;
    }

    /**
     * Gets the finger print of the certificate.
     */
    private String calcCertFingerPrint(Certificate cert) throws Exception {
        byte[] derCert = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] digest = md.digest(derCert);
        StringBuilder hexes = new StringBuilder();
        for (int eachByte = 0; eachByte < digest.length; eachByte++) {
            hexes.append(toHexDigits(digest[eachByte]));
            if (eachByte + 1 != digest.length) {
                hexes.append(':');
            }
        }
        return hexes.toString();
    }

    /**
     * Private replacement for toHexString since we need the leading 0 digits.
     * Returns a String containing byte value encoded as 2 hex characters.
     *
     * @param theByte a byte containing the value to be encoded.
     * @return String containing byte value encoded as 2 hex characters.
     */
    private static String toHexDigits(byte theByte) {
        final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuilder result = new StringBuilder(2);
        result.append(HEXDIGITS[(theByte >>> 4) & 15]);
        result.append(HEXDIGITS[theByte & 15]);
        return result.toString();
    }

    private int syntaxError() {
        consoleMessage("Usage: pse.certs [-l] [-c] ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display the certificates contained in the current group's PSE Membership";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     pse.certs  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     pse.certs [-l] [-c] ");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("      [-c]     Print the certificate chain for each certificate.");
        println("      [-l]     Print the complete certificates.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("     Prints the list of certificates contained in the current PSE.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pse.certs");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("     pse.keys");
    }
}
