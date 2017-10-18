package tgdh.tree;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.DSAParameterSpec;
import tgdh.TgdhException;
import tgdh.TgdhUtil;
import tgdh.crypto.TgdhPrivateKey;
import tgdh.crypto.TgdhPublicKey;

public class Tree extends BasicTree {

    public Tree(LeafNode root) {
        super(root);
    }

    public Tree(TreeInfo tinfo) {
        super(tinfo);
        keyParams = tinfo.getKeyParams();
    }

    public TreeInfo join(LeafNode node, int position) throws TgdhException {
        LeafNode sponsor = super.join(node, position);
        removeKeys(sponsor);
        if (sponsor != owner) return null;
        try {
            genOwnerKeyPair();
        } catch (Exception e) {
            throw new TgdhException(e);
        }
        computeKeys(owner.getParent());
        return treeInfo(1);
    }

    public TreeInfo join(LeafNode node) throws TgdhException {
        return join(node, 1);
    }

    private void updatePublics(Node src, Node dest) throws TgdhException {
        if (dest == null) if (src == null) return; else throw new TgdhException("the source and destination are not the same.");
        if (dest.publicKey == null && src.publicKey != null) {
            updated = true;
            dest.publicKey = src.publicKey;
        }
        updatePublics(src.getLeft(), dest.getLeft());
        updatePublics(src.getRight(), dest.getRight());
    }

    public void updatePublics(Tree updatedTree) throws TgdhException {
        if (!updatedTree.toString().equals(toString())) {
            throw new TgdhException("updatedTree not same as the tree");
        } else {
            updated = false;
            updatePublics(updatedTree.root, root);
            return;
        }
    }

    public TreeInfo leave(LeafNode node) throws TgdhException {
        LeafNode sponsor = super.leave(node);
        if (sponsor == null) return null;
        removeKeys(sponsor);
        if (sponsor != owner) return null;
        try {
            genOwnerKeyPair();
        } catch (Exception e) {
            throw new TgdhException(e.getCause());
        }
        computeKeys(sponsor.getParent());
        return treeInfo(1);
    }

    public TreeInfo partition(Node nodes[]) throws TgdhException {
        LeafNode sponsors[] = super.basicPartition(nodes);
        String positionBS[] = new String[sponsors.length];
        for (int i = 0; i < positionBS.length; i++) positionBS[i] = Integer.toBinaryString(sponsors[i].getPosition());
        int rmIndex = 0;
        for (int i = 1; i < positionBS.length; i++) if (positionBS[i].compareTo(positionBS[rmIndex]) > 0) rmIndex = i;
        for (int i = 0; i < sponsors.length; i++) if (sponsors[i] != owner) removeKeys(sponsors[i]); else removeKeys(owner.getParent());
        if (!TgdhUtil.contains(sponsors, owner)) return null;
        if (owner == sponsors[rmIndex]) try {
            genOwnerKeyPair();
        } catch (Exception e) {
            throw new TgdhException(e.getCause());
        }
        computeKeys(owner.getParent());
        return treeInfo(1);
    }

    public TreeInfo merge(Tree trees2merge[]) throws TgdhException {
        throw new UnsupportedOperationException("merge more than 2 trees not supported");
    }

    public TreeInfo merge(Tree anotherTree) throws TgdhException {
        LeafNode sponsor = super.merge(anotherTree);
        removeKeys(sponsor);
        if (sponsor != owner) {
            return null;
        } else {
            genOwnerKeyPair();
            computeKeys(sponsor.getParent());
            return treeInfo(1);
        }
    }

    public TreeInfo treeInfo() {
        return treeInfo(1);
    }

    public TreeInfo treeInfo(int order) {
        TreeInfo tinfo = super.basicTreeInfo(order);
        tinfo.keyParams = keyParams;
        return tinfo;
    }

    public void removeKeys(Node start) {
        Node aNode = start;
        for (; start != null; start = start.getParent()) {
            start.privateKey = null;
            start.publicKey = null;
        }
    }

    public void genOwnerKeyPair() throws TgdhException {
        try {
            KeyPairGenerator pairgen = KeyPairGenerator.getInstance("DSA");
            DSAParameterSpec paramSpec = new DSAParameterSpec(keyParams.getP(), keyParams.getQ(), keyParams.getG());
            pairgen.initialize(paramSpec);
            KeyPair keypair = pairgen.generateKeyPair();
            DSAPrivateKey privateKey = (DSAPrivateKey) keypair.getPrivate();
            DSAPublicKey publicKey = (DSAPublicKey) keypair.getPublic();
            owner.privateKey = new TgdhPrivateKey(keyParams, privateKey.getX());
            owner.publicKey = new TgdhPublicKey(keyParams, publicKey.getY());
        } catch (NoSuchAlgorithmException e) {
            throw new TgdhException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new TgdhException(e.getMessage());
        }
    }

    public void computeKeys(Node start) {
        Node aNode = start;
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

    private boolean updated;

    private DSAParams keyParams;
}
