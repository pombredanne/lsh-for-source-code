package org.personalsmartspace.psm.dynmm.defaultEvaluators;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.personalsmartspace.psm.groupmgmt.api.pss3p.IDgmManager;
import org.personalsmartspace.psm.groupmgmt.api.pss3p.IPssGroupMembershipEvaluator;

/**
 * Abstract Evaluator class
 */
@Component(name = "Abstract Evaluator", componentAbstract = true)
@Service
public abstract class AbstractEvaluator implements IPssGroupMembershipEvaluator {

    protected String merDescription = "Empty Abstract Evaluator";

    protected String merId = null;

    protected AbstractEvaluator() {
        String inputId = UUID.randomUUID().toString();
        this.merId = inputId;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            byte[] resultingBytes = md5.digest(inputId.getBytes());
            this.merId = Base64.encodeBase64URLSafeString(resultingBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * An instance of the dynamic membership manager
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setDynMM", unbind = "unsetDynMM")
    protected IDgmManager dynMM = null;

    protected synchronized void unsetDynMM(IDgmManager dynMM) {
        this.dynMM = null;
    }

    protected synchronized void setDynMM(IDgmManager dynMM) {
        this.dynMM = dynMM;
    }

    @Override
    public void setMerDescription(String newVal) {
        this.merDescription = newVal;
    }

    @Override
    public void setMerId(String newVal) {
        this.merId = newVal;
    }

    @Override
    public String getMerId() {
        return this.merId;
    }

    @Override
    public String getMerDescription() {
        return this.merDescription;
    }

    public abstract String getFactoryId();
}
