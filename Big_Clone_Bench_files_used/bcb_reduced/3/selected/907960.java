package org.psepr.PsEPRServer.Accounting;

import java.io.Reader;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.HashMap;
import org.psepr.PsEPRServer.ConnectionReader;
import org.psepr.PsEPRServer.DebugLogger;
import org.psepr.PsEPRServer.Global;
import org.psepr.PsEPRServer.PsEPRServerException;
import org.psepr.PsEPRServer.ParamServer;
import org.psepr.PsEPRServer.Processor;
import org.psepr.PsEPRServer.ServerEvent;
import org.psepr.PsEPRServer.ServerEventChannel;
import org.psepr.PsEPRServer.Utilities;
import org.psepr.services.service.ChannelUseDescription;
import org.psepr.services.service.ChannelUseDescriptionCollection;
import org.psepr.services.service.EventDescription;
import org.psepr.services.service.EventDescriptionCollection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.mxp1.MXParser;

/**
 * Receive accouting information and verify certificates from various sources
 * <p>
 * One channel has information on the accounts that can use this server.
 * This routine receives that information, stores it and then ConnectionManagers can
 * call us to see if the account is good.
 * </p>
 * <p>
 * The basic account type is simply and account/password pair.  Someday there
 * will be a fancy ticket system in place.
 * </p>
 */
public class Accounting extends Processor {

    DebugLogger log;

    public static final String ACCOUNT_NAMESPACE = "http://dsmt.org/schema/psepr/payload/PsEPRServer/accounting-1.0";

    /**
	 * Internal calls to hold accounts specified by name/password.
	 * The password stored here is usually MD5 coded.
	 * @author radams1
	 */
    private class passwordAccount {

        public passwordAccount() {
            name = null;
            password = null;
            expiration = 0L;
        }

        private String name;

        private String password;

        private long expiration;

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public void setExpiration(String expirationStr) {
            try {
                expiration = Long.decode(expirationStr);
            } catch (Exception e) {
                expiration = 0L;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    private HashMap<String, passwordAccount> accountsWithPasswords;

    private String accountsWithPasswordsCoding;

    private String accountsWithPasswordsSeed;

    MessageDigest MD5Digester;

    public Accounting() {
        super();
        this.init();
    }

    private void init() {
        log = new DebugLogger("Accounting");
        accountsWithPasswords = null;
        accountsWithPasswordsCoding = null;
        accountsWithPasswordsSeed = null;
        MD5Digester = null;
        Global.accounting = this;
        if (Global.serviceStatus != null) {
            ChannelUseDescriptionCollection cudc = Global.serviceStatus.getServiceDescription().getChannelUses();
            cudc.add(new ChannelUseDescription(Global.params().getParamString(ParamServer.ACCOUNTING_CHANNEL), "reception of accounting passwords and keys", 0L, 0L, null, new EventDescriptionCollection(new EventDescription(ACCOUNT_NAMESPACE, "account information"))));
        }
    }

    public void start() {
        Global.routeTable.addRoute("Accounting", Global.params().getParamString(ParamServer.ACCOUNTING_CHANNEL), ACCOUNT_NAMESPACE, ConnectionReader.TYPE_CLIENT, this);
        return;
    }

    public void stop() {
        Global.routeTable.removeRoute(this);
        return;
    }

    /**
	 * If asked, say we're a client. This makes sure the above routerTable entry
	 * is transmitted to the other peers.
	 */
    public int getConnectionType() {
        return ConnectionReader.TYPE_CLIENT;
    }

    /**
	 * Check to see if the account/password pair is an account we know of.
	 * The password is plain text when it gets here and it is coded according
	 * to the specification that came with the accounts.
	 * @param accountName account name
	 * @param accountPassword plain text password
	 * @return
	 */
    public boolean verifyAccountPassword(String accountName, String accountPassword) {
        boolean ret = false;
        if (accountsWithPasswords != null) {
            if (accountsWithPasswordsCoding == null || accountsWithPasswordsCoding.equals("MD5")) {
                if (MD5Digester == null) {
                    try {
                        MD5Digester = MessageDigest.getInstance("MD5");
                    } catch (Exception e) {
                        ret = false;
                        MD5Digester = null;
                    }
                }
                if (MD5Digester != null) {
                    String expandedPassword;
                    if (accountsWithPasswordsSeed == null) {
                        expandedPassword = accountPassword;
                    } else {
                        expandedPassword = accountsWithPasswordsSeed + accountPassword;
                    }
                    MD5Digester.reset();
                    String codedPassword = MD5Digester.digest(expandedPassword.getBytes()).toString();
                    if (accountsWithPasswords.containsKey(accountName)) {
                        String fromManager = accountsWithPasswords.get(accountName).getPassword();
                        if (fromManager.equals("ANY") || fromManager.equals(codedPassword)) {
                            ret = true;
                        }
                    }
                }
            } else {
                ret = false;
            }
        }
        ret = true;
        return ret;
    }

    /**
	 * The routing table sends account namespace events on the accounting channel
	 * to this routine.  Here verify that this message is from a service we can
	 * rely on and then extract the accounting information.  These are the accounts
	 * that can log into us.  There are several different types of authentication
	 * and these are put in different tables.
	 */
    public void send(ServerEvent se) {
        if (!(se instanceof ServerEventChannel)) {
            log.log(log.SERVICES, "Received server event of unknown type: " + se.getClass().getName());
            return;
        }
        ServerEventChannel sec = (ServerEventChannel) se;
        if (sec.getToChannel() == null || sec.getPayload() == null) {
            log.log(log.SERVICES, "Received server command message for no channel or no payload");
            return;
        }
        if (!sec.getFromService().equalsIgnoreCase(Global.params().getParamString(ParamServer.ACCOUNTING_SERVICE))) {
            log.log(log.SERVICES, "Received command from odd service:" + sec.getFromService());
            return;
        }
        try {
            XmlPullParser parser = null;
            Reader rdr = new StringReader(sec.getPayload());
            try {
                parser = new MXParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                parser.setInput(rdr);
            } catch (Exception e) {
                log.log(log.BADERROR, "Exception getting parser for account event: " + e.toString());
                throw new PsEPRServerException("Excpetion getting parser for account event:" + e.toString());
            }
            boolean done = false;
            int eventType = parser.getEventType();
            while (!done && eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String elementName = parser.getName();
                    if (elementName.equals("passwordAccounts")) {
                        log.log(log.IODETAIL, "passwordAccount data");
                        readPasswordAccounts(parser, "passwordAccounts");
                    } else if (elementName.equals("passwordTicket")) {
                        log.log(log.IODETAIL, "passwordTicket data");
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("payload")) {
                        done = true;
                    }
                }
                if (!done) {
                    eventType = parser.next();
                }
            }
        } catch (Exception e) {
            log.log(log.BADERROR, "Excpetion parsing account event: " + e.toString());
            throw new PsEPRServerException("Could not parse command payload:" + e.toString());
        }
    }

    /**
	 * Parse a block of accounts with passwords and create the array of same.
	 * <p>
	 * We're looking for a block that looks like:
	 * <pre>
	 * &lt;passwordCoding&gt;passwordCodingSpecification&lt;/passwordCoding&gt;
	 * &lt;passwordSeed&gt;passwordSeed&lt;/passwordSeed&gt;
	 * &lt;account&gt;
	 *   &lt;name&gt;accountName&lt;/name&gt;
	 *   &lt;password&gt;accountPassword&lt;/password&gt;
	 * &lt;/account&gt;
	 * ... many more 'account' elements
	 * &lt;/endElement&gt;
	 * </pre>
	 * Notice that the previous parser has swollowed the beginning tag.
	 * </p>
	 * @param parser
	 * @param endElement
	 */
    private void readPasswordAccounts(XmlPullParser parser, String endElement) {
        HashMap<String, passwordAccount> accts = new HashMap<String, passwordAccount>();
        passwordAccount thisAcct = null;
        String passwordCoding = null;
        String passwordSeed = null;
        try {
            boolean done = false;
            int eventType = parser.getEventType();
            while (!done && eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String elementName = parser.getName();
                    if (elementName.equals("passwordSeed")) {
                        passwordSeed = Utilities.cleanXMLText(parser.nextText());
                    } else if (elementName.equals("passwordCoding")) {
                        passwordCoding = Utilities.cleanXMLText(parser.nextText());
                    } else if (elementName.equals("account")) {
                        thisAcct = new passwordAccount();
                    } else if (elementName.equals("name")) {
                        if (thisAcct != null) {
                            thisAcct.setName(Utilities.cleanXMLText(parser.nextText()));
                        }
                    } else if (elementName.equals("password")) {
                        if (thisAcct != null) {
                            thisAcct.setPassword(Utilities.cleanXMLText(parser.nextText()));
                        }
                    } else if (elementName.equals("expiration")) {
                        if (thisAcct != null) {
                            thisAcct.setExpiration(Utilities.cleanXMLText(parser.nextText()));
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("account")) {
                        if (thisAcct != null) {
                            accts.put(thisAcct.name, thisAcct);
                            thisAcct = null;
                        }
                    } else if (parser.getName().equals(endElement)) {
                        done = true;
                    }
                }
                if (!done) {
                    eventType = parser.next();
                }
            }
        } catch (Exception e) {
            log.log(log.BADERROR, "Excpetion parsing account event: " + e.toString());
            throw new PsEPRServerException("Could not parse command payload:" + e.toString());
        }
        if (accts != null) {
            accountsWithPasswords = accts;
            accountsWithPasswordsCoding = passwordCoding;
            accountsWithPasswordsSeed = passwordSeed;
        }
        return;
    }
}
