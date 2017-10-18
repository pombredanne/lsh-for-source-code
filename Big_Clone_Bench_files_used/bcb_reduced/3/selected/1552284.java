package org.crypthing.things.validator.pkibr;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.crypthing.things.cert.DERUtil;
import org.crypthing.things.config.Bundle;
import org.crypthing.things.validator.ActionOutput;
import org.crypthing.things.validator.OutputFactory;
import org.crypthing.things.validator.Rule;
import org.crypthing.things.validator.RuleOutput;
import org.crypthing.things.validator.ValidationInput;

/**
 * Authority Key Identifier extension validator
 * @author yorickflannagan
 * @version 1.0
 *
 */
public class AuthorityKeyIdentifierValidator implements Rule {

    private static final long serialVersionUID = -4355156794038454084L;

    private static String PKIBR_RULE_NAME_STR;

    private static String PKIBR_RULE_ACTION_STR;

    private static String PKIBR_RULE_MSG_STR;

    static {
        ResourceBundle resources = Bundle.getInstance().getBundle(new AuthorityKeyIdentifierValidator());
        PKIBR_RULE_NAME_STR = resources.getString("PKIBR_RULE_NAME_AKI_STR");
        PKIBR_RULE_ACTION_STR = resources.getString("PKIBR_RULE_ACTION_AKI_STR");
        PKIBR_RULE_MSG_STR = resources.getString("PKIBR_RULE_MSG_STR");
    }

    @Override
    public RuleOutput execute(ValidationInput input, OutputFactory outputEngine) {
        X509Certificate[] chain = (X509Certificate[]) input.getRuleInput("CertificateChain");
        if (chain == null) throw new InvalidInputParamException(Bundle.getInstance().getResourceString(this, "PKIBR_CHAIN_MISSING_ERROR"));
        RuleOutput output = outputEngine.getRuleOutput();
        output.setRuleName(PKIBR_RULE_NAME_STR);
        for (int i = 0; i < chain.length; i++) {
            ActionOutput out = outputEngine.getActionOutput();
            out.setRuleAction(PKIBR_RULE_ACTION_STR.replace("[DN]", chain[i].getSubjectX500Principal().getName()));
            if ((checkExtension(chain[i], out)) && (checkIssuer(chain[i], i + 1 == chain.length ? chain[i] : chain[i + 1], out)) && (checkAKI(chain[i], i + 1 == chain.length ? chain[i] : chain[i + 1], out))) {
                out.setSuccess(true);
                out.setLevel(PKIBRWarningLevel.WARNING_LEVEL_SUCCESS);
                out.setMessage(PKIBR_RULE_MSG_STR);
            }
            output.addActionOutput(out);
        }
        return output;
    }

    private boolean checkExtension(X509Certificate cert, ActionOutput output) {
        boolean retVal = true;
        if (cert.getExtensionValue(X509Extensions.AuthorityKeyIdentifier.getId()) == null) {
            output.setSuccess(false);
            output.setLevel(PKIBRWarningLevel.WARNING_LEVEL_NON_COMPLIANCE);
            output.setMessage(Bundle.getInstance().getResourceString(this, "PKIBR_RULE_MISSING_AKI_WARN"));
            retVal = false;
        }
        return retVal;
    }

    private boolean checkIssuer(X509Certificate current, X509Certificate next, ActionOutput output) {
        boolean retVal = true;
        if (!current.getIssuerX500Principal().equals(next.getSubjectX500Principal())) {
            output.setSuccess(false);
            output.setLevel(PKIBRWarningLevel.WARNING_LEVEL_ERROR);
            output.setMessage(Bundle.getInstance().getResourceString(this, "PKIBR_RULE_INVALID_CHAIN_WARN"));
            retVal = false;
        }
        return retVal;
    }

    private boolean checkAKI(X509Certificate subject, X509Certificate issuer, ActionOutput output) {
        boolean retVal = true;
        byte[] publicKey = issuer.getPublicKey().getEncoded();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(publicKey);
            try {
                ASN1OctetString string = (ASN1OctetString) DERUtil.getDERObject(subject.getExtensionValue(X509Extensions.AuthorityKeyIdentifier.getId()));
                AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier((ASN1Sequence) DERUtil.getDERObject(string.getOctets()));
                if (!MessageDigest.isEqual(digest.digest(), aki.getKeyIdentifier())) {
                    output.setSuccess(false);
                    output.setLevel(PKIBRWarningLevel.WARNING_LEVEL_CRITICAL_ERROR);
                    output.setMessage(Bundle.getInstance().getResourceString(this, "PKIBR_RULE_INVALID_AKI_WARN"));
                    retVal = false;
                }
            } catch (Exception e) {
                output.setSuccess(false);
                output.setLevel(PKIBRWarningLevel.WARNING_LEVEL_ERROR);
                output.setMessage(Bundle.getInstance().getResourceString(this, "PKIBR_RULE_INVALID_CHAIN_WARN"));
                retVal = false;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new PKIBRValidatorException(Bundle.getInstance().getResourceString(this, "PKIBR_CRYPTO_PROVIDER_ERROR"), e);
        }
        return retVal;
    }
}
