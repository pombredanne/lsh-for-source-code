package com.eip.yost.web.pages.client;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.Service;
import org.apache.tapestry5.beaneditor.Validate;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import com.eip.yost.entite.Client;
import com.eip.yost.services.interfaces.IClientManager;

public class ClientAdd {

    @Inject
    private Logger mLogger;

    @InjectComponent
    private Zone errorZone;

    @Inject
    private ComponentResources mComponentResources;

    @Inject
    @Service("clientManager")
    private IClientManager mClientManager;

    @Property
    private boolean mErrorExist;

    @Property
    private boolean mErrorMdp;

    @Property
    private String mNewMail;

    @Property
    private String mNewNom;

    @Property
    private String mNewPrenom;

    @Property
    private String mNewAdresse;

    @Property
    private String mNewMdp;

    @Property
    private String mNewMdpConfirm;

    public boolean getErrorExist() {
        return this.mErrorExist;
    }

    public boolean getErrorMdp() {
        return this.mErrorMdp;
    }

    public String getNewMail() {
        return this.mNewMail;
    }

    @Validate("required, regexp=^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$")
    public void setNewMail(String pNewMail) {
        this.mNewMail = pNewMail;
    }

    public String getNewNom() {
        return this.mNewNom;
    }

    @Validate("required")
    public void setNewNom(String pNewNom) {
        this.mNewNom = pNewNom;
    }

    public String getNewPrenom() {
        return this.mNewPrenom;
    }

    public void setNewPrenom(String pNewPrenom) {
        this.mNewPrenom = pNewPrenom;
    }

    public String getNewAdresse() {
        return this.mNewAdresse;
    }

    @Validate("required")
    public void setNewAdresse(String pNewAdresse) {
        this.mNewAdresse = pNewAdresse;
    }

    public String getNewMdp() {
        return this.mNewMdp;
    }

    @Validate("required, minlength=8, regexp=^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*\\W).{8,}$")
    public void setNewMdp(String pNewMdp) {
        this.mNewMdp = pNewMdp;
    }

    public String getNewMdpConfirm() {
        return this.mNewMdpConfirm;
    }

    @Validate("required, minlength=8, regexp=^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*\\W).{8,}$")
    public void setNewMdpConfirm(String pNewMdpConfirm) {
        this.mNewMdpConfirm = pNewMdpConfirm;
    }

    Object onSuccess() {
        this.mErrorExist = true;
        this.mErrorMdp = true;
        if (!mClientManager.exists(this.mNewMail)) {
            this.mErrorExist = false;
            if (mNewMdp.equals(mNewMdpConfirm)) {
                this.mErrorMdp = false;
                MessageDigest sha1Instance;
                try {
                    sha1Instance = MessageDigest.getInstance("SHA1");
                    sha1Instance.reset();
                    sha1Instance.update(this.mNewMdp.getBytes());
                    byte[] digest = sha1Instance.digest();
                    BigInteger bigInt = new BigInteger(1, digest);
                    String vHashPassword = bigInt.toString(16);
                    Client vClient = new Client(this.mNewNom, (this.mNewPrenom != null ? this.mNewPrenom : ""), this.mNewMail, vHashPassword, this.mNewAdresse, 1);
                    mClientManager.save(vClient);
                    mComponentResources.discardPersistentFieldChanges();
                    return "Client/List";
                } catch (NoSuchAlgorithmException e) {
                    mLogger.error(e.getMessage(), e);
                }
            }
        }
        return errorZone.getBody();
    }
}
