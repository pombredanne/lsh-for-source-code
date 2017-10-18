package net.sf.selfim.core.info;

import java.io.Serializable;
import java.util.Arrays;
import raiser.util.Objects;
import net.sf.selfim.core.Engine;
import net.sf.selfim.core.kb.KnowledgeBase;
import net.sf.selfim.core.kb.MemoryKB;

/**
 * Store a knowledge base location.
 * The digest is the password digest obtained after encription.
 * @author: Costin Emilian GRIGORE
 */
public class KBLocation extends Information implements Serializable {

    private KBLocation(String url, String password, byte[] digest, boolean isEncrypted, boolean isPasswordStored) {
        this.url = url;
        this.digest = digest;
        this.isEncrypted = isEncrypted;
        this.isPasswordStored = isPasswordStored;
        setPassword(password);
    }

    public KBLocation(String url) {
        this(url, null, null, false, false);
    }

    public KBLocation(String url, String password) {
        this(url, password, Engine.digest(password, url), true, false);
    }

    public KBLocation(String url, String password, boolean isPasswordStored) {
        this(url, password, Engine.digest(password, url), true, isPasswordStored);
    }

    public KBLocation(String url, byte[] digest) {
        this(url, null, digest, true, false);
    }

    public KBLocation() {
        this(null, null, null, false, false);
    }

    private String url;

    private byte[] digest;

    private String password;

    private boolean isEncrypted;

    private boolean isPasswordStored;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        recomputeDigest();
    }

    public String toString() {
        StringBuffer result = new StringBuffer(this.getClass().getName());
        result.append("[");
        result.append("url=");
        result.append(url);
        result.append(", isEncrypted=");
        result.append(isEncrypted());
        result.append(", isPasswordStored=");
        result.append(isPasswordStored());
        result.append(", super=");
        result.append(super.toString());
        result.append("]");
        return result.toString();
    }

    public KnowledgeBase createKnowledgeBase(String password) {
        return new MemoryKB();
    }

    public final KnowledgeBase createKnowledgeBase(String password, boolean createIfNotExist, boolean automaticLoad) throws Exception {
        String currentPassword;
        if (isEncrypted()) {
            currentPassword = password;
        } else {
            currentPassword = null;
        }
        if (!isValidPassword(currentPassword)) {
            throw new Exception("The given password is invalid. The digest obtained from your password is different from original digest. Can't create knowledge base");
        }
        KnowledgeBase kb = createKnowledgeBase(currentPassword);
        if (!kb.isAccessible()) {
            throw new Exception("The knowledge base is inaccessible. Can't create knowledge base.");
        }
        if (createIfNotExist) {
            if (!kb.isCreated()) {
                kb.commit();
                automaticLoad = false;
            }
        }
        if (automaticLoad && kb.isCreated()) {
            kb.rollback();
        }
        return kb;
    }

    /**
     * @param password2
     * @return
     */
    private boolean isValidPassword(String password) {
        if (isEncrypted()) {
            return Arrays.equals(digest, Engine.digest(password, getUrl()));
        }
        return true;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public void setPassword(String password) {
        this.password = password;
        recomputeDigest();
    }

    public void setPasswordStored(boolean isPasswordStored) {
        this.isPasswordStored = isPasswordStored;
    }

    /**
     * @return Returns the isPasswordStored.
     */
    public boolean isPasswordStored() {
        return isPasswordStored;
    }

    /**
     * @return Returns the password only if password is setted as stored.
     */
    public String getPassword() {
        return isPasswordStored() ? password : null;
    }

    /**
     * @return Returns the password.
     */
    public String getRuntimePassword() {
        return password;
    }

    public boolean haveAPassword() {
        return password != null;
    }

    /**
     * @param isEncrypted The isEncrypted to set.
     */
    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
        recomputeDigest();
    }

    /**
     * 
     */
    private void recomputeDigest() {
        if (isEncrypted) {
            if ((this.password != null) && (getUrl() != null)) {
                digest = Engine.digest(password, getUrl());
            }
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof KBLocation)) {
            return false;
        }
        KBLocation kblocation = (KBLocation) obj;
        boolean result = super.equals(kblocation);
        result &= Objects.equals(url, kblocation.getUrl());
        result &= isEncrypted() == kblocation.isEncrypted();
        result &= isPasswordStored() == kblocation.isPasswordStored();
        result &= Arrays.equals(getDigest(), kblocation.getDigest());
        return result;
    }
}
