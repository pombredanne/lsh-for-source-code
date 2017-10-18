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
import com.eip.yost.commun.exceptions.ClientInexistantException;
import com.eip.yost.dto.ClientDTO;
import com.eip.yost.entite.Client;
import com.eip.yost.services.interfaces.IClientManager;

public class ClientEdit {

    @Inject
    private Logger mLogger;

    @InjectComponent
    private Zone errorZone;

    @Inject
    private ComponentResources mComponentResources;

    @Inject
    @Service("clientManager")
    private IClientManager mClientManager;

    private ClientDTO mClient;

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

    @Validate("required")
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

    @Validate("regexp=^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*\\W).{8,}$")
    public void setNewMdp(String pNewMdp) {
        this.mNewMdp = pNewMdp;
    }

    public String getNewMdpConfirm() {
        return this.mNewMdpConfirm;
    }

    @Validate("regexp=^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*\\W).{8,}$")
    public void setNewMdpConfirm(String pNewMdpConfirm) {
        this.mNewMdpConfirm = pNewMdpConfirm;
    }

    public void onActivate(Integer pId) {
        try {
            mClient = mClientManager.getClient(pId);
            this.setNewMail(mClient.getEmail());
            this.setNewPrenom(mClient.getPrenom());
            this.setNewNom(mClient.getNom());
            this.setNewAdresse(mClient.getAdresse());
        } catch (ClientInexistantException e) {
            mLogger.warn(e.getMLibelleErreur(), e);
        }
    }

    public Integer onPassivate() {
        return mClient.getIdclient();
    }

    Object onSuccess() {
        this.mErrorExist = true;
        this.mErrorMdp = true;
        if (this.mNewMail.equals(mClient.getEmail()) || !this.mNewMail.equals(mClient.getEmail()) && !mClientManager.exists(this.mNewMail)) {
            this.mErrorExist = false;
            if (mNewMdp != null && mNewMdpConfirm != null) {
                if (this.mNewMdp.equals(this.mNewMdpConfirm)) {
                    this.mErrorMdp = false;
                    MessageDigest sha1Instance;
                    try {
                        sha1Instance = MessageDigest.getInstance("SHA1");
                        sha1Instance.reset();
                        sha1Instance.update(this.mNewMdp.getBytes());
                        byte[] digest = sha1Instance.digest();
                        BigInteger bigInt = new BigInteger(1, digest);
                        String vHashPassword = bigInt.toString(16);
                        mClient.setMdp(vHashPassword);
                    } catch (NoSuchAlgorithmException e) {
                        mLogger.error(e.getMessage(), e);
                    }
                }
            } else {
                this.mErrorMdp = false;
            }
            if (!this.mErrorMdp) {
                mClient.setAdresse(this.mNewAdresse);
                mClient.setEmail(this.mNewMail);
                mClient.setNom(this.mNewNom);
                mClient.setPrenom((this.mNewPrenom != null ? this.mNewPrenom : ""));
                Client vClient = new Client(mClient);
                mClientManager.save(vClient);
                mComponentResources.discardPersistentFieldChanges();
                return "Client/List";
            }
        }
        return errorZone.getBody();
    }

    public ClientDTO getClient() {
        return mClient;
    }
}
