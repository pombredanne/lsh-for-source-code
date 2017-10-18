package fairVote.agent;

import java.net.ConnectException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import fairVote.core.MyException;
import fairVote.util.Crypto;
import fairVote.util.FairLog;
import fairVote.votazione.Votazione;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

public class AgentClient {

    private static Logger LOGGER = FairLog.getLogger(AgentClient.class.getName());

    private String agentUrl = null;

    private Certificate agentCert = null;

    private PrivateKey editorKey = null;

    private Certificate editorCert = null;

    public String rt = "\n";

    public AgentClient(String agentUrl, Certificate agentCert, PrivateKey editorKey, Certificate editorCert, String rt) {
        this.agentUrl = agentUrl;
        this.agentCert = agentCert;
        this.editorCert = editorCert;
        this.editorKey = editorKey;
        this.rt = rt;
    }

    public AgentClient(String agentUrl, Certificate agentCert, PrivateKey editorKey, Certificate editorCert) {
        this.agentUrl = agentUrl;
        this.agentCert = agentCert;
        this.editorCert = editorCert;
        this.editorKey = editorKey;
        this.rt = "\n";
    }

    public String sendCmd(String cmd) {
        try {
            LOGGER.trace("create envelope");
            byte[] signCmd = Crypto.sign(cmd.getBytes(), editorKey);
            LOGGER.trace("create message");
            byte[] msg = AgentCore.createMsg(cmd.getBytes(), editorCert.getEncoded(), signCmd);
            LOGGER.trace("crypt for agent");
            byte[] encBytes = Crypto.encrypt(msg, agentCert.getPublicKey());
            LOGGER.trace("Send FEnv to forward");
            String sAgentServer = agentUrl + "/agent/sendCommand";
            String response = AgentCore.sendBytes(encBytes, sAgentServer, rt);
            if (LOGGER.isTraceEnabled()) LOGGER.trace("response:" + response);
            return response;
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
            return "FAIL";
        }
    }

    public String sendCmdVotazione(String command, Votazione votazione) {
        String result = "OK";
        try {
            LOGGER.trace("create msg");
            byte[] cmdBundle = AgentCore.createMsg(command.getBytes(), votazione.getRawXml().getBytes());
            byte[] signCmd = Crypto.sign(cmdBundle, editorKey);
            byte[] msg = AgentCore.createMsg(cmdBundle, editorCert.getEncoded(), signCmd);
            LOGGER.trace("cript for agent");
            byte[] encBytes = Crypto.encrypt(msg, agentCert.getPublicKey());
            LOGGER.trace("Send FEnv to agent");
            String sAgentServer = agentUrl + "/agent/sendCommandArg1";
            String response = AgentCore.sendBytes(encBytes, sAgentServer, rt);
            if (LOGGER.isTraceEnabled()) LOGGER.trace("response: " + response);
            if (response.startsWith("FAIL")) {
                throw new MyException(MyException.ERROR_GENERIC, response);
            }
        } catch (MyException e) {
            LOGGER.error("An error has occurred", e);
            result = e.getErrMsg();
        } catch (ConnectException e) {
            result = AgentCore.ERRMSG_CONNECT;
        } catch (Exception e) {
            LOGGER.error("Unexpected", e);
            result = "FAIL";
        }
        if (LOGGER.isTraceEnabled()) LOGGER.trace("result: " + result);
        return result;
    }

    public String sendCmdID(String command, String ID) {
        String result = "OK";
        try {
            LOGGER.trace("create msg");
            byte[] cmdBundle = AgentCore.createMsg(command.getBytes(), ID.getBytes());
            byte[] signCmd = Crypto.sign(cmdBundle, editorKey);
            byte[] msg = AgentCore.createMsg(cmdBundle, editorCert.getEncoded(), signCmd);
            LOGGER.trace("cript MSG for agent");
            byte[] encBytes = Crypto.encrypt(msg, agentCert.getPublicKey());
            LOGGER.trace("Send bytes to agent");
            String sAgentServer = agentUrl + "/agent/sendCommandArg1";
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(encBytes);
            byte[] tmpDigest = algorithm.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < tmpDigest.length; i++) {
                sb.append(Integer.toHexString((tmpDigest[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
            }
            String response = AgentCore.sendBytes(encBytes, sAgentServer, rt);
            if (response.startsWith("FAIL")) {
                throw new MyException(MyException.ERROR_GENERIC, response);
            }
            return response;
        } catch (MyException e) {
            LOGGER.error("An error has occurred", e);
            result = e.getErrMsg();
        } catch (ConnectException e) {
            result = AgentCore.ERRMSG_CONNECT;
        } catch (Exception e) {
            LOGGER.error("Unexpected error", e);
            result = "FAIL";
        }
        if (LOGGER.isTraceEnabled()) LOGGER.trace("result: " + result);
        return result;
    }
}
