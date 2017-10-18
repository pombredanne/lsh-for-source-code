package fairVote.agent.registrar;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import fairVote.agent.AgentCore;
import fairVote.agent.AgentFactory;
import fairVote.agent.AgentServer;
import fairVote.core.MyException;
import fairVote.data.AgentData;
import fairVote.data.Config;
import fairVote.data.Role4AgentData;
import fairVote.data.Role4VotazioneData;
import fairVote.data.RoleData;
import fairVote.data.VotazioneData;
import fairVote.mysql.MySql;
import fairVote.util.Basic;
import fairVote.util.Crypto;
import fairVote.util.FairLog;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.MDC;

public class RegistrarServer {

    private static Logger LOGGER = FairLog.getLogger(RegistrarServer.class.getName());

    private static void setLogInfo(Config conf, String IDVotazione) {
        String mdc_key = "nome_votazione";
        String nome = MySql.getVotazioneName(conf, IDVotazione);
        MDC.put(mdc_key, nome);
    }

    private static void removeLoginfo() {
        String mdc_key = "nome_votazione";
        MDC.remove(mdc_key);
    }

    public static void handleConnection(AgentServer as, String target) {
        target = target.replaceFirst("/registrar", "");
        if (target.equals("/openSession")) {
            handleOpenSession(as);
            return;
        }
        if (target.equals("/getT1")) {
            handleGetT1(as);
            return;
        }
        if (target.equals("/sendT2")) {
            handleSendT2(as);
            return;
        }
        if (target.equals("/closeSession")) {
            handleCloseSession(as);
            return;
        }
        if (target.equals("/checkVote")) {
            handleCheckVote(as);
            return;
        }
        LOGGER.warn("target " + target + "unknown");
        return;
    }

    private static void handleOpenSession(AgentServer as) {
        PrivateKey keyRegistrar = null;
        try {
            LOGGER.info("Opening a session");
            String response = "BOH";
            while (true) {
                LOGGER.trace("Load registrar key");
                keyRegistrar = AgentCore.getKey(as.af.aData.ksd);
                LOGGER.trace("Going to process a message");
                byte[] datadec = AgentCore.receiveBytes(as.s);
                byte[] msg = Crypto.decrypt(datadec, keyRegistrar);
                LOGGER.trace("load bundle");
                byte[] bundle = AgentCore.getS1FromMsg(msg);
                byte[] signBundle = AgentCore.getS2FromMsg(msg);
                LOGGER.trace("Extract data client");
                byte[] pS1 = AgentCore.getS1FromMsg(bundle);
                String S1 = Basic.byte2String(pS1);
                byte[] bundle_auth = AgentCore.getS2FromMsg(bundle);
                byte[] ppenc = AgentCore.getS3FromMsg(bundle);
                byte[] bIDVotazione = AgentCore.getS4FromMsg(bundle);
                String IDVotazione = Basic.byte2String(bIDVotazione);
                if (S1.length() != Registrar.SessionTokenSize) throw new Exception("Trama wrong");
                VotazioneData vd = as.af.votazioni.get(IDVotazione);
                if (vd == null) {
                    LOGGER.error("Votazione Unknown (" + IDVotazione + ")");
                    response = "FAIL::IDVOTE_UNKNOWN";
                    break;
                }
                setLogInfo(as.config, IDVotazione);
                Role4AgentData r = (Role4AgentData) as.af.roles.get(IDVotazione);
                if (!r.enable) {
                    LOGGER.error("Votazione Locked (" + IDVotazione + ")");
                    response = AgentCore.ERRMSG_VOTATIONLOCKED;
                    break;
                }
                Role4VotazioneData rd = vd.getAgentByRole(RoleData.C_REGISTRAR_STRING);
                AgentData ad = as.af.agents.get(rd.IDAgent);
                String scertRegistrar = Crypto.cert2String(AgentCore.getCert(as.af.aData.ksd));
                if (!scertRegistrar.equals(ad.cert)) {
                    LOGGER.error("Not My Role");
                    response = "FAIL::NOT_MY_ROLE";
                    break;
                }
                PublicKey certSeggio = null;
                byte[] certBytes = null;
                String credential_xml = "";
                String tyAuth = Basic.byte2String(AgentCore.getS1FromMsg(bundle_auth));
                if (tyAuth.equals(Registrar.C_AUTHX509)) {
                    certBytes = AgentCore.getS2FromMsg(bundle_auth);
                    certSeggio = Crypto.loadCert(certBytes).getPublicKey();
                } else if (tyAuth.equals(Registrar.C_AUTHUSERPASS)) {
                    credential_xml = Basic.byte2String(AgentCore.getS2FromMsg(bundle_auth));
                } else throw new Exception("auth unknown");
                PublicKey pp = Crypto.loadPublicKeyEncoded(ppenc);
                if (tyAuth.equals(Registrar.C_AUTHX509)) {
                    LOGGER.debug("check bundle");
                    boolean check = Crypto.verifySign(bundle, signBundle, certSeggio);
                    if (!check) {
                        LOGGER.error("Wrong signature for bundle");
                        response = "FAIL";
                        break;
                    }
                    LOGGER.trace("Sign OK");
                } else {
                    LOGGER.debug("parsing xml");
                    LOGGER.trace(credential_xml);
                }
                String user4T1 = null;
                LOGGER.info("Check user identity");
                if (tyAuth.equals(Registrar.C_AUTHX509)) {
                    LOGGER.warn("AUTHX509 is unimplemented");
                } else if (tyAuth.equals(Registrar.C_AUTHUSERPASS)) {
                    Document credential = Registrar.askForCredential(as.config, credential_xml, IDVotazione);
                    if ((credential == null) || (credential.selectSingleNode("/credential/user/username") == null)) {
                        LOGGER.error("Authentication failed");
                        response = "FAIL";
                        break;
                    }
                    try {
                        user4T1 = credential.selectSingleNode("/credential/user/username").getText();
                    } catch (Exception e) {
                        LOGGER.warn("problem with response credential");
                        user4T1 = null;
                    }
                } else {
                    LOGGER.error("Authentication failed");
                    response = "FAIL";
                    break;
                }
                if (user4T1 == null) {
                    LOGGER.info("Authentication failed");
                    response = "FAIL";
                    break;
                }
                LOGGER.info("Identity confirmed. Registering session");
                Registrar.addSession(S1, user4T1, IDVotazione, bundle_auth, pp);
                LOGGER.trace("Re-sign S1");
                byte[] signS1R = Crypto.sign(pS1, keyRegistrar);
                LOGGER.trace("Prepare ACK");
                msg = AgentCore.createMsg(pS1, signS1R);
                LOGGER.trace("encript");
                byte[] encBytes = Crypto.encrypt(msg, pp, "RSA", 50, 64);
                response = new String(Base64.encodeBase64(encBytes), "utf-8");
                break;
            }
            LOGGER.info("Send response");
            String cmd = "HTTP/1.1 200 OK\n";
            cmd += "Content-Type: text/plain\n\n";
            cmd += response;
            as.s.getOutputStream().write(cmd.getBytes());
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm unknown", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Key Error", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
        }
        removeLoginfo();
    }

    private static void handleGetT1(AgentServer as) {
        PrivateKey keyRegistrar = null;
        try {
            LOGGER.info("T1 has been requested");
            keyRegistrar = AgentCore.getKey(as.af.aData.ksd);
            byte[] keyRegistrarBytes = keyRegistrar.getEncoded();
            LOGGER.debug("Receive 2nd msg");
            byte[] datadec = AgentCore.receiveBytes(as.s);
            byte[] msg = Crypto.decrypt(datadec, keyRegistrar);
            byte[] pS1 = AgentCore.getS1FromMSGT1(msg);
            String S1 = Basic.byte2String(pS1);
            String IDVotazione = AgentCore.getIDVotazioneFromMSGT1(msg);
            LOGGER.debug("Get cert from Session");
            String user = Registrar.getSessionUser(S1);
            byte[] bundle_auth = Registrar.getSessionBundleAuth(S1);
            PublicKey pp = Registrar.getSessionPublicKey(S1);
            String user4T1 = null;
            String result = "BOH";
            String response = null;
            msg = null;
            while (true) {
                if (user == null) {
                    LOGGER.error("Session not found");
                    result = "FAIL::SessionNotOpen";
                    break;
                }
                if (!as.af.roles.containsKey(IDVotazione)) {
                    LOGGER.error("ID not found:" + IDVotazione);
                    result = "FAIL::NotMyBusinnes-1";
                    break;
                }
                setLogInfo(as.config, IDVotazione);
                Role4AgentData r = (Role4AgentData) as.af.roles.get(IDVotazione);
                if (r.role != Role4AgentData.C_REGISTRAR) {
                    LOGGER.error("REGISTRAR not my role for " + IDVotazione);
                    result = "FAIL::NotMyBusinnes-2";
                    break;
                }
                if (!r.enable) {
                    LOGGER.error("Votazione Locked");
                    LOGGER.error("IDV:" + IDVotazione);
                    response = AgentCore.ERRMSG_VOTATIONLOCKED;
                    break;
                }
                int resultCheckTime = as.af.checkTime(IDVotazione);
                if (resultCheckTime == AgentFactory.C_TimeTooEarly) {
                    LOGGER.error("TimeEarly  for " + IDVotazione);
                    result = "FAIL::TimeTooEarly";
                    break;
                }
                if (resultCheckTime == AgentFactory.C_TimeTooLate) {
                    LOGGER.error("TimeOut  for " + IDVotazione);
                    result = "FAIL::TimeTooLate";
                    break;
                }
                if (resultCheckTime != AgentFactory.C_TimeOk) {
                    LOGGER.error("Time???" + resultCheckTime + "  for " + IDVotazione);
                    result = "FAIL::TimeBoh";
                    break;
                }
                LOGGER.debug("Check eligibility");
                boolean isEligible = false;
                String tyAuth = Basic.byte2String(AgentCore.getS1FromMsg(bundle_auth));
                if (tyAuth.equals("X509")) {
                    LOGGER.debug("Eligibiliy with cert:FIXIT!");
                } else if (tyAuth.equals("USERPASS")) {
                    String credential_xml = "";
                    try {
                        credential_xml = Basic.byte2String(AgentCore.getS2FromMsg(bundle_auth));
                        if (LOGGER.isDebugEnabled()) LOGGER.debug("Credentials: " + credential_xml);
                    } catch (Exception e) {
                        LOGGER.warn("credential xml is not valid");
                        user4T1 = null;
                    }
                    Document credential = null;
                    try {
                        credential = Registrar.askForCredential(as.config, credential_xml, IDVotazione);
                        user4T1 = credential.selectSingleNode("/credential/user/username").getText();
                    } catch (Exception e) {
                        LOGGER.warn("problem with response credential", e);
                        user4T1 = null;
                    }
                    isEligible = Registrar.checkEligibility(credential, IDVotazione, as.config);
                } else {
                    result = "FAIL::AuthUnknown";
                    break;
                }
                if (!isEligible) {
                    result = AgentCore.ERRMSG_NOTELIGIBLE;
                    break;
                }
                LOGGER.info("User eligible to vote");
                if (user4T1 == null) {
                    result = "FAIL::USER4T1NULL";
                    break;
                }
                LOGGER.debug("Create T1 and register it");
                byte[] T1 = Registrar.createT1(keyRegistrarBytes, user4T1.getBytes("UTF-8"), IDVotazione);
                Registrar.setSessionT1(S1, T1);
                byte[] signT1 = Crypto.sign(T1, keyRegistrar);
                byte[] msgT1 = AgentCore.createMsg(S1.getBytes("utf-8"), T1, signT1);
                LOGGER.debug("Scramble T1");
                msg = Crypto.encrypt(msgT1, pp, "RSA", 50, 64);
                result = "OK";
                break;
            }
            if (result.equals("OK")) response = new String(Base64.encodeBase64(msg), "utf-8"); else response = result;
            LOGGER.info("Send response");
            String cmd = "HTTP/1.1 200 OK\n";
            cmd += "Content-Type: text/plain\n\n";
            cmd += response;
            as.s.getOutputStream().write(cmd.getBytes());
        } catch (MyException e) {
            LOGGER.error("MyException:", e);
        } catch (IOException e) {
            LOGGER.error("IOE:", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("NSAE:", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("IKSE:", e);
        } catch (Exception e) {
            LOGGER.error("Unknown error", e);
        }
        removeLoginfo();
    }

    private static void handleCloseSession(AgentServer as) {
        PrivateKey keyRegistrar = null;
        try {
            keyRegistrar = AgentCore.getKey(as.af.aData.ksd);
            LOGGER.info("Closing session");
            byte[] datadec = AgentCore.receiveBytes(as.s);
            byte[] pS1 = Crypto.decrypt(datadec, keyRegistrar);
            String S1 = Basic.byte2String(pS1);
            String response = null;
            int result = Registrar.removeSession(S1);
            if (result == 0) response = "OK"; else response = "FAIL::result=" + Integer.toString(result);
            LOGGER.debug("Send response");
            String cmd = "HTTP/1.1 200 OK\n";
            cmd += "Content-Type: text/plain\n\n";
            cmd += response;
            as.s.getOutputStream().write(cmd.getBytes());
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm unknown", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Invalid key", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error", e);
        }
    }

    private static void handleSendT2(AgentServer as) {
        PrivateKey keyRegistrar = null;
        try {
            LOGGER.info("Going to save T2 ...");
            byte[] datadec = AgentCore.receiveBytes(as.s);
            keyRegistrar = AgentCore.getKey(as.af.aData.ksd);
            LOGGER.trace("Decript msg");
            byte[] S1T2 = Crypto.decrypt(datadec, keyRegistrar);
            LOGGER.trace("Extract S1,T2");
            byte[] pS1 = AgentCore.getS1FromMsg(S1T2);
            String S1 = Basic.byte2String(pS1);
            byte[] T2 = AgentCore.getS2FromMsg(S1T2);
            String response = "BOH";
            while (true) {
                String user = Registrar.getSessionUser(S1);
                String IDVotazione = Registrar.getSessionIDVotazione(S1);
                if (user == null) {
                    LOGGER.error("Session not found");
                    response = "FAIL";
                    break;
                }
                setLogInfo(as.config, IDVotazione);
                Role4AgentData r = (Role4AgentData) as.af.roles.get(IDVotazione);
                if (!r.enable) {
                    LOGGER.error("Votazione (" + IDVotazione + ") is locked");
                    response = AgentCore.ERRMSG_VOTATIONLOCKED;
                    break;
                }
                LOGGER.trace("register T2 on session");
                int check = Registrar.setSessionT2(S1, T2, user, IDVotazione, as.config);
                if (check < 0) {
                    LOGGER.error("Registration fail");
                    if (check == Registrar.SETSESSION_DIFFERS) {
                        LOGGER.error("T2 differs");
                        response = AgentCore.ERRMSG_T2DIFFER;
                    } else {
                        response = "FAIL";
                    }
                    break;
                }
                LOGGER.info("... T2 saved");
                response = "OK";
                break;
            }
            LOGGER.info("Send response");
            String cmd = "HTTP/1.1 200 OK\n";
            cmd += "Content-Type: text/plain\n\n";
            cmd += response;
            as.s.getOutputStream().write(cmd.getBytes());
        } catch (IOException e) {
            LOGGER.error("IO Error", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm unknown", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Invalid Key", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error", e);
        }
        removeLoginfo();
    }

    private static void handleCheckVote(AgentServer as) {
        PrivateKey keyRegistrar = null;
        Certificate certCollector;
        try {
            LOGGER.info("Checking vote");
            keyRegistrar = AgentCore.getKey(as.af.aData.ksd);
            LOGGER.trace("Receive msg");
            byte[] datadec = AgentCore.receiveBytes(as.s);
            LOGGER.trace("Decript msg");
            byte[] msg = Crypto.decrypt(datadec, keyRegistrar);
            LOGGER.trace("Extract R, signR");
            byte[] R = AgentCore.getS1FromMsg(msg);
            byte[] signR = AgentCore.getS2FromMsg(msg);
            byte[] bIDVotazione = AgentCore.getS3FromMsg(msg);
            String IDVotazione = Basic.byte2String(bIDVotazione);
            certCollector = as.af.getCertificate(IDVotazione, Role4VotazioneData.C_COLLECTOR);
            if (certCollector == null) {
                throw new Exception("cert registrar not found");
            }
            String response = "BOH";
            LOGGER.debug("check signature registrar");
            if (!Crypto.verifySign(R, signR, certCollector.getPublicKey())) {
                LOGGER.error("Signature registrar not correct");
                response = "FAIL";
            } else {
                byte[] T1 = AgentCore.getS1FromMsg(R);
                byte[] DVotazioneSalt = AgentCore.getS2FromMsg(R);
                byte[] DVotazioneSaltT2 = AgentCore.getS3FromMsg(R);
                byte[] T2 = Registrar.getT2FromT1(T1, as.config);
                if (T2 == null) {
                    LOGGER.error("T2 not found");
                    response = "FAIL";
                } else {
                    byte[] m = AgentCore.createMsg(DVotazioneSalt, T2);
                    MessageDigest algorithm = MessageDigest.getInstance("MD5");
                    algorithm.reset();
                    algorithm.update(m);
                    byte[] MyDVotazioneSaltT2 = algorithm.digest();
                    if (Arrays.equals(MyDVotazioneSaltT2, DVotazioneSaltT2)) {
                        LOGGER.info("... vote check is successfull");
                        response = "OK";
                    } else {
                        LOGGER.error("DVotazioneSaltT2 s differs");
                        response = "FAIL";
                    }
                }
            }
            LOGGER.info("Send response");
            String cmd = "HTTP/1.1 200 OK\n";
            cmd += "Content-Type: text/plain\n\n";
            cmd += response;
            as.s.getOutputStream().write(cmd.getBytes());
        } catch (IOException e) {
            LOGGER.error("IO Error", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm error", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Invalid key", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error", e);
        }
    }
}
