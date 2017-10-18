package net.jxta.myjxta.util;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.MessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEConfig;
import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.myjxta.MyJXTA;
import net.jxta.myjxta.View;
import net.jxta.peergroup.PeerGroup;
import net.sf.p2pim.BuddyListView;
import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import org.eclipse.jface.dialogs.MessageDialog;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.cert.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author james todd [gonzo at jxta dot org]
 * @version $Id: CredentialUtil.java,v 1.10 2007/06/10 21:15:12 nano Exp $
 */
public class CredentialUtil {

    private static final String NEW_LINE = "\n";

    private static final String COLON = ": ";

    private static final String ALGORITHM = "MD5";

    private static final char[] CHAR_MAP = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static final String TIME_STAMP = "EEE MMM dd hh:mm:ss z yyyy";

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_STAMP);

    private static final ResourceBundle STRINGS = Resources.getStrings();

    private static final String SUBJECT = STRINGS.getString("label.certificate.subject");

    private static final String FINGER_PRINT = STRINGS.getString("label.certificate.fingerPrint");

    private static final String VALIDITY = STRINGS.getString("label.certificate.validity");

    private static final String START_DATE = STRINGS.getString("label.certificate.startDate");

    private static final String END_DATE = STRINGS.getString("label.certificate.endDate");

    private static final String DATE = STRINGS.getString("label.certificate.date");

    private static final Logger LOG = Logger.getLogger(CredentialUtil.class.getName());

    public static StructuredDocument getCredential(View v, Group g) {
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("getCredential");
        }
        return getCredential(v, g != null ? g.getPeerGroup() : null);
    }

    public static StructuredDocument getCredential(View v, PeerGroup pg) {
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("get credential");
        }
        StructuredDocument d = null;
        PeerGroup cpg = AuthenticationUtil.getTLSPeerGroup(pg);
        if (!AuthenticationUtil.isAuthenticated(cpg)) {
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("authenticating");
            }
            AuthenticationUtil.authenticate(v, cpg);
        }
        if (AuthenticationUtil.isAuthenticated(cpg)) {
            try {
                d = cpg.getMembershipService().getDefaultCredential().getDocument(MimeMediaType.XMLUTF8);
            } catch (PeerGroupException pge) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "no default credential", pge);
                }
            } catch (Exception e) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "no default credential", e);
                }
            }
        } else {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("not authorized");
            }
        }
        return d;
    }

    public static boolean importCredential(MessageElement me, Group g, MyJXTA myjxta) {
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("importCredential");
        }
        boolean exists = false;
        boolean imported = false;
        PeerGroup cpg = AuthenticationUtil.getTLSPeerGroup(g);
        if (!AuthenticationUtil.isAuthenticated(cpg)) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("authenticating");
            }
            AuthenticationUtil.authenticate(myjxta.getView(), cpg);
        }
        if (AuthenticationUtil.isAuthenticated(cpg)) {
            MembershipService ms = cpg != null ? cpg.getMembershipService() : null;
            PSEMembershipService pse = ms != null && ms instanceof PSEMembershipService ? (PSEMembershipService) ms : null;
            PSEConfig pc = pse != null ? pse.getPSEConfig() : null;
            StructuredDocument sd = null;
            if (me != null) {
                try {
                    sd = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, me.getStream());
                } catch (IOException ioe) {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "can\'t read document", ioe);
                    }
                }
            } else {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("me IS NULL");
                }
            }
            PSECredential psec = pse != null && sd != null ? (PSECredential) pse.makeCredential(sd) : null;
            ID pid = psec != null ? psec.getPeerID() : null;
            if (pc != null && pid != null) {
                if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                    LOG.info("checking for certificate: " + pid);
                }
                try {
                    exists = pc.getTrustedCertificate(pid) != null;
                } catch (KeyStoreException kse) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "can\'t get certificate: " + pid, kse);
                    }
                } catch (IOException ioe) {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "can\'t get certificate: " + pid, ioe);
                    }
                }
                if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                    LOG.info("certificate exists: " + exists);
                }
            }
            if (!exists) {
                X509Certificate[] x509s = psec != null ? psec.getCertificateChain() : null;
                if (pc != null && pid != null && x509s != null && x509s.length > 0) {
                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                        LOG.info("importing certificates: " + pid);
                    }
                    X509Certificate x509;
                    ID cid;
                    boolean isTrusted = false;
                    boolean first = true;
                    boolean bail = false;
                    String s;
                    String v;
                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                        LOG.info("processing certificates: " + x509s.length);
                    }
                    for (int i = 0; i < x509s.length && !isTrusted && !bail; i++) {
                        x509 = x509s[i];
                        v = null;
                        try {
                            x509.checkValidity();
                        } catch (CertificateExpiredException cee) {
                            v = STRINGS.getString("error.certificate.expired");
                        } catch (CertificateNotYetValidException cnvye) {
                            v = STRINGS.getString("error.certificate.notYetValid");
                        }
                        s = SUBJECT + COLON + getSubject(x509) + NEW_LINE + FINGER_PRINT + COLON + getFingerPrint(x509) + (v != null ? NEW_LINE + VALIDITY + COLON + v + NEW_LINE + START_DATE + COLON + getStartDate(x509) + NEW_LINE + END_DATE + COLON + getEndDate(x509) + NEW_LINE + DATE + COLON + getDate() : "");
                        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                            LOG.info("certificate[" + i + "]: " + s);
                        }
                        if (true || MessageDialog.openConfirm(BuddyListView.getShell(), STRINGS.getString("label.certificate.validate"), s)) {
                            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                                LOG.info("import accepted");
                            }
                            try {
                                isTrusted = pc.getTrustedCertificateID(x509) != null;
                                if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                                    LOG.info("certificate is trusted: " + isTrusted);
                                }
                                if (!isTrusted) {
                                    cid = first ? pid : IDFactory.newCodatID(cpg.getPeerGroupID(), new ByteArrayInputStream(x509.getEncoded()));
                                    first = false;
                                    pc.erase(cid);
                                    pc.setTrustedCertificate(cid, x509);
                                    s = STRINGS.getString("status.peer.1to1.certificate.imported") + ": " + cid;
                                    myjxta.setStatus(s);
                                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                                        LOG.info(s);
                                    }
                                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("certificate imported: " + cid);
                                    }
                                }
                                imported = true;
                            } catch (KeyStoreException ke) {
                                imported = false;
                                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                                    LOG.log(Level.SEVERE, "keystore error", ke);
                                }
                            } catch (CertificateEncodingException cee) {
                                imported = false;
                                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                                    LOG.log(Level.SEVERE, "certificate error", cee);
                                }
                            } catch (IOException ioe) {
                                imported = false;
                                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                                    LOG.log(Level.SEVERE, "can\'t read certificate", ioe);
                                }
                            }
                        } else {
                            bail = true;
                            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                                LOG.info("import revoked");
                            }
                        }
                    }
                } else {
                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                        LOG.info("can\'t process certificates");
                    }
                }
            }
        } else {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("not authorized");
            }
        }
        return exists || imported;
    }

    private static X500Principal getSubject(X509Certificate c) {
        return c.getSubjectX500Principal();
    }

    private static String getFingerPrint(Certificate c) {
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("getFingerPrint");
        }
        StringBuffer sb = null;
        byte[] ba = null;
        try {
            ba = MessageDigest.getInstance(ALGORITHM).digest(c.getEncoded());
        } catch (Exception e) {
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "can\'t get messsage digest", e);
            }
        }
        if (ba != null) {
            sb = new StringBuffer();
            byte b;
            for (int i = 0; i < ba.length; i++) {
                b = ba[i];
                sb.append(CHAR_MAP[(b & 0xf0) >> 4]);
                sb.append(CHAR_MAP[b & 0xf]);
                if (i < ba.length - 1) {
                    sb.append(":");
                }
            }
        }
        String fp = sb != null ? sb.toString() : null;
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("fingerPrint: " + fp);
        }
        return fp;
    }

    private static Date getStartDate(X509Certificate c) {
        return c.getNotBefore();
    }

    private static Date getEndDate(X509Certificate c) {
        return c.getNotAfter();
    }

    private static String getDate() {
        return DATE_FORMATTER.format(new Date(System.currentTimeMillis()));
    }
}
