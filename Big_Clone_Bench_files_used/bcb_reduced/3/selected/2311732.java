package tgdh;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.DSAParameterSpec;

/**
 * 
 * The TreeSpecification class provides the TGDH tree representation
 * 
 * @author Gilbert, Paresh, Sanket
 * 
 */
public class TreeSpecification extends TreeRepresentation {

    @SuppressWarnings("unused")
    private boolean updated;

    private DSAParams keyParams;

    public TreeSpecification(LeafMember root) {
        super(root);
    }

    public TreeSpecification(TreeInformation tinfo) {
        super(tinfo);
        keyParams = tinfo.getKeyParams();
    }

    public TreeInformation join(LeafMember node, int position) throws TGDHException {
        LeafMember sponsor = super.join(node, position);
        removeKeys(sponsor);
        if (sponsor != owner) return null;
        try {
            genOwnerKeyPair();
        } catch (Exception e) {
            throw new TGDHException(e);
        }
        computeKeys(owner.getParent());
        return treeInfo(1);
    }

    public TreeInformation join(LeafMember node) throws TGDHException {
        return join(node, 1);
    }

    private void updatePublics(TGDHNode src, TGDHNode dest) throws TGDHException {
        if (dest == null) if (src == null) return; else throw new TGDHException("the source and destination are not the same.");
        if (dest.publicKey == null && src.publicKey != null) {
            updated = true;
            dest.publicKey = src.publicKey;
        }
        updatePublics(src.getLeft(), dest.getLeft());
        updatePublics(src.getRight(), dest.getRight());
    }

    public void updatePublics(TreeSpecification updatedTree) throws TGDHException {
        if (!updatedTree.toString().equals(toString())) {
            System.err.println("Rcvd: " + updatedTree);
            System.err.println("Mine: " + toString());
            throw new TGDHException("updatedTree not same as the tree");
        } else {
            updated = false;
            updatePublics(updatedTree.root, root);
            return;
        }
    }

    public TreeInformation merge(TreeSpecification trees2merge[]) throws TGDHException {
        throw new UnsupportedOperationException("merge more than 2 trees not supported");
    }

    public TreeInformation merge(TreeSpecification anotherTree) throws TGDHException {
        LeafMember sponsor = super.merge(anotherTree);
        removeKeys(sponsor);
        if (sponsor != owner) {
            return null;
        } else {
            genOwnerKeyPair();
            computeKeys(sponsor.getParent());
            return treeInfo(1);
        }
    }

    public TreeInformation treeInfo() {
        return treeInfo(1);
    }

    public TreeInformation treeInfo(int order) {
        TreeInformation tinfo = super.basicTreeInfo(order);
        tinfo.keyParams = keyParams;
        return tinfo;
    }

    public void removeKeys(TGDHNode start) {
        @SuppressWarnings("unused") TGDHNode aNode = start;
        for (; start != null; start = start.getParent()) {
            start.privateKey = null;
            start.publicKey = null;
        }
    }

    public void genOwnerKeyPair() throws TGDHException {
        try {
            KeyPairGenerator pairgen = KeyPairGenerator.getInstance("DSA");
            DSAParameterSpec paramSpec = new DSAParameterSpec(keyParams.getP(), keyParams.getQ(), keyParams.getG());
            pairgen.initialize(paramSpec);
            KeyPair keypair = pairgen.generateKeyPair();
            DSAPrivateKey privateKey = (DSAPrivateKey) keypair.getPrivate();
            DSAPublicKey publicKey = (DSAPublicKey) keypair.getPublic();
            owner.privateKey = new TGDHPrivateKey(keyParams, privateKey.getX());
            owner.publicKey = new TGDHPublicKey(keyParams, publicKey.getY());
        } catch (NoSuchAlgorithmException e) {
            throw new TGDHException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new TGDHException(e.getMessage());
        }
    }

    public void computeKeys(TGDHNode start) {
        TGDHNode aNode = start;
        for (boolean canProceed = true; aNode != null && canProceed; aNode = aNode.getParent()) canProceed = aNode.computeKeys();
    }

    public byte[] getSymmetricKey(int keyBits) {
        if (keyBits < 1 || keyBits > 512) throw new IllegalArgumentException("bit length not positiv");
        if (root.privateKey == null) computeKeys(owner.getParent());
        try {
            MessageDigest md = null;
            if (keyBits <= 160) md = MessageDigest.getInstance("SHA-1"); else if (keyBits <= 256) md = MessageDigest.getInstance("SHA-256"); else md = MessageDigest.getInstance("SHA-512");
            BigInteger rootKey = root.getPrivate().getX();
            byte digest[] = md.digest(rootKey.toByteArray());
            int keyBytes = (keyBits + 7) / 8;
            byte output[] = new byte[keyBytes];
            System.arraycopy(digest, 0, output, 0, output.length);
            output[0] &= 255 >>> 8 * keyBytes - keyBits;
            return output;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DSAPrivateKey getAsymmetricKey() {
        if (root.publicKey == null) return null;
        if (root.privateKey == null) computeKeys(owner.getParent());
        return root.privateKey;
    }

    public DSAParams getKeyParams() {
        return keyParams;
    }

    public void setKeyParams(DSAParams keyParams) {
        this.keyParams = keyParams;
    }
}
