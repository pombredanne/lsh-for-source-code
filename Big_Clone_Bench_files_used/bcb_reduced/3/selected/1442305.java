package de.intarsys.pdf.cos;

import java.security.MessageDigest;
import java.util.Iterator;
import de.intarsys.tools.locator.ILocator;
import de.intarsys.tools.randomaccess.IRandomAccess;
import de.intarsys.tools.randomaccess.RandomAccessByteArray;
import de.intarsys.tools.string.StringTools;

/**
 * The document trailer.
 */
public class COSTrailer extends COSBasedObject {

    /**
	 * The meta class implementation
	 */
    public static class MetaClass extends COSBasedObject.MetaClass {

        protected MetaClass(Class instanceClass) {
            super(instanceClass);
        }
    }

    /** The well known attribute names */
    public static final COSName DK_Info = COSName.constant("Info");

    public static final COSName DK_Prev = COSName.constant("Prev");

    public static final COSName DK_Root = COSName.constant("Root");

    public static final COSName DK_Size = COSName.constant("Size");

    public static final COSName DK_Encrypt = COSName.constant("Encrypt");

    public static final COSName DK_ID = COSName.constant("ID");

    public static final COSName DK_XRefStm = COSName.constant("XRefStm");

    /** The meta class instance */
    public static final MetaClass META = new MetaClass(MetaClass.class.getDeclaringClass());

    /** The cached catalog object */
    private COSCatalog cachedCatalog;

    protected COSTrailer(COSObject object) {
        super(object);
    }

    /**
	 * The /Encrypt field of the trailer.
	 * 
	 * @return The /Encrypt field of the trailer.
	 */
    public COSDictionary cosGetEncryption() {
        return cosGetField(DK_Encrypt).asDictionary();
    }

    /**
	 * The /ID field of the trailer.
	 * 
	 * @return The /ID field of the trailer.
	 */
    public COSArray cosGetID() {
        return cosGetField(DK_ID).asArray();
    }

    /**
	 * Set the /Encrypt field of the trailer.
	 * 
	 * @param encryption
	 *            The new encryption dictionary
	 */
    public void cosSetEncryption(COSDictionary encryption) {
        cosSetField(DK_Encrypt, encryption);
    }

    /**
	 * <code> 
	 * - include time 
	 * - file location 
	 * - size 
	 * - document information dictionary 
	 * </code>
	 * 
	 * @return a byte array with the created ID
	 */
    protected byte[] createFileID() {
        try {
            COSDocument cosDoc = cosGetDoc();
            if (cosDoc == null) {
                return null;
            }
            ILocator locator = cosDoc.getLocator();
            if (locator == null) {
                return null;
            }
            IRandomAccess ra = cosDoc.stGetDoc().getRandomAccess();
            if (ra == null) {
                ra = new RandomAccessByteArray(StringTools.toByteArray("DummyValue"));
            }
            MessageDigest digest = MessageDigest.getInstance("MD5");
            long time = System.currentTimeMillis();
            digest.update(String.valueOf(time).getBytes());
            digest.update(locator.getFullName().getBytes());
            digest.update(String.valueOf(ra.getLength()).getBytes());
            COSInfoDict infoDict = getInfoDict();
            if (infoDict != null) {
                for (Iterator it = infoDict.cosGetDict().iterator(); it.hasNext(); ) {
                    COSObject object = (COSObject) it.next();
                    digest.update(object.stringValue().getBytes());
                }
            }
            return digest.digest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
	 * The {@link COSInfoDict} containing metadata.
	 * 
	 * @return The {@link COSInfoDict} containing metadata.
	 */
    public COSInfoDict getInfoDict() {
        return (COSInfoDict) COSInfoDict.META.createFromCos(cosGetField(DK_Info));
    }

    /**
	 * 
	 * @return Offset of previous trailer dict or -1 if none exists
	 */
    public int getPrev() {
        return getFieldInt(DK_Prev, -1);
    }

    /**
	 * Get the root object (the catalog) for the document.
	 * 
	 * @return The root object (the catalog) for the document.
	 */
    public COSCatalog getRoot() {
        if (cachedCatalog == null) {
            cachedCatalog = (COSCatalog) COSCatalog.META.createFromCos(cosGetField(DK_Root));
        }
        return cachedCatalog;
    }

    /**
	 * @return Total number of indirect objects in the document
	 */
    public int getSize() {
        return getFieldInt(DK_Size, -1);
    }

    @Override
    protected void initializeFromScratch() {
        super.initializeFromScratch();
        setRoot((COSCatalog) COSCatalog.META.createNew());
        setInfoDict((COSInfoDict) COSInfoDict.META.createNew());
    }

    @Override
    public void invalidateCaches() {
        super.invalidateCaches();
        cachedCatalog = null;
    }

    /**
	 * Set the info dictionary containing metadata.
	 * 
	 * @param infoDict
	 *            The info dictionary containing metadata.
	 */
    public void setInfoDict(COSInfoDict infoDict) {
        setFieldObject(DK_Info, infoDict);
    }

    /**
	 * Set the catalog.
	 * 
	 * @param root
	 *            The document catalog
	 */
    public void setRoot(COSCatalog root) {
        setFieldObject(DK_Root, root);
    }

    /**
	 * The current file id or null
	 */
    public COSArray cosGetFileID() {
        return cosGetField(DK_ID).asArray();
    }

    /**
	 * The permanent file id part.
	 */
    public COSString cosGetPermanentFileID() {
        COSArray fileID = cosGetField(DK_ID).asArray();
        if ((fileID == null) || (fileID.size() == 0)) {
            return null;
        } else {
            return (COSString) fileID.get(0);
        }
    }

    /**
	 * The dynamic file id part.
	 */
    public COSString cosGetDynamicFileID() {
        COSArray fileID = cosGetField(DK_ID).asArray();
        if ((fileID == null) || (fileID.size() < 2)) {
            return null;
        } else {
            return (COSString) fileID.get(1);
        }
    }

    /**
	 * Generates a unique file ID array (10.3).
	 */
    public void updateFileID() {
        COSArray fileID = cosGetField(DK_ID).asArray();
        if ((fileID == null) || (fileID.size() == 0)) {
            fileID = COSArray.create();
            cosSetField(DK_ID, fileID);
            byte[] id = createFileID();
            COSString permanentID = COSString.create(id);
            fileID.add(permanentID);
            fileID.add(permanentID);
        } else {
            byte[] id = createFileID();
            COSString changingID = COSString.create(id);
            if (fileID.size() < 2) {
                fileID.add(changingID);
            } else {
                fileID.set(1, changingID);
            }
        }
    }
}
