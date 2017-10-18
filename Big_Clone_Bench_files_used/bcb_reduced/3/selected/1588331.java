package com.jigen.msi;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public abstract class AbstractResourceDescriptor {

    static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static int resourceCounter = 0;

    private final String resourceID = "Resource" + (resourceCounter++);

    private final String name;

    private final LinkedList<ShortcutDescriptor> shortcuts = new LinkedList<ShortcutDescriptor>();

    private final LinkedList<ResourceDescriptor> relatedResources = new LinkedList<ResourceDescriptor>();

    public AbstractResourceDescriptor(String name) {
        this.name = name;
    }

    protected void addRelatedResources(ResourceDescriptor... resources) {
        this.relatedResources.addAll(Arrays.asList(resources));
    }

    protected void addShortcuts(ShortcutDescriptor... shortcuts) {
        this.shortcuts.addAll(Arrays.asList(shortcuts));
    }

    public String getName() {
        return name;
    }

    public Collection<ResourceDescriptor> getRelatedResources() {
        return relatedResources;
    }

    public Collection<ShortcutDescriptor> getShortcuts() {
        return shortcuts;
    }

    public String getResourceID() {
        return resourceID;
    }

    public String getEncryptedResourceID() {
        messageDigest.update(resourceID.getBytes(), 0, resourceID.length());
        return new BigInteger(1, messageDigest.digest()).toString(16);
    }
}
