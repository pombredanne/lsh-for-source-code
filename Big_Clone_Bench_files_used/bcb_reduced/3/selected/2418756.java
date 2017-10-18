package jwadlib;

import java.io.*;
import java.nio.channels.*;
import java.security.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The {@link Wad Wad} class is used to virtually open, manipulate 
 * and store WAD files.  It allows for the manipulation of a WAD file on the 
 * Java&#153; platform.
 * @author Samuel "insertwackynamehere" Horwitz
 * @version 1.0 Alpha 2
 * @since 1.0
 */
public class Wad {

    /**
     * The WAD file as a {@link java.io.File File}.
     * @since 1.0
     */
    private File wadfilelocation;

    /**
     * The WAD file as a {@link java.io.RandomAccessFile RandomAccessFile}.
     * @since 1.0
     */
    private RandomAccessFile wadfile;

    /**
     * The {@link java.nio.channels.FileChannel FileChannel} associated with the 
     * {@link #wadfile WAD file}.
     * @since 1.0
     */
    private FileChannel wadfilechannel;

    /**
     * A {@link LinkedList LinkedList} of the {@link Lump Lumps} in the {@link 
     * Wad Wad}.
     * @since 1.0
     */
    protected LinkedList<Lump> lumps;

    /**
     * The first four bytes of the WAD file.
     * @since 1.0
     */
    protected int identifier;

    /**
     * Creates a {@link Wad Wad} object from a wad file as specified from the filepath.  The 
     * wad file cannot be written to.
     * @param filepath the location of the wad file including the name and extension.
     * @throws java.io.FileNotFoundException if the WAD file cannot be found.
     * @throws jwadlib.UnableToBackUpWADFileException if the WAD file cannot be backed up.
     * @throws jwadlib.UnableToReadWADFileException if the WAD file cannot be read.
     * @since 1.0
     */
    public Wad(String filepath) throws FileNotFoundException, UnableToBackUpWADFileException, UnableToReadWADFileException {
        this(filepath, false);
    }

    /**
     * Creates a {@link Wad Wad} object from a wad file that has been already pointed to by 
     * a {@link java.io.File File} object. The wad file cannot be written to.
     * @param file a {@link java.io.File File} object that points to a wad file.
     * @throws java.io.FileNotFoundException if the WAD file cannot be found.
     * @throws jwadlib.UnableToBackUpWADFileException if the WAD file cannot be backed up.
     * @throws jwadlib.UnableToReadWADFileException if the WAD file cannot be read.
     * @since 1.0
     */
    public Wad(File file) throws FileNotFoundException, UnableToBackUpWADFileException, UnableToReadWADFileException {
        this(file, false);
    }

    /**
     * Creates a {@link Wad Wad} object from a wad file as specified from the filepath.
     * @param filepath the location of the wad file including the name and extension.
     * @param write whether or not the wad file can be written to.
     * @throws java.io.FileNotFoundException if the WAD file cannot be found.
     * @throws jwadlib.UnableToBackUpWADFileException if the WAD file cannot be backed up.
     * @throws jwadlib.UnableToReadWADFileException if the WAD file cannot be read.
     * @since 1.0
     */
    public Wad(String filepath, boolean write) throws FileNotFoundException, UnableToBackUpWADFileException, UnableToReadWADFileException {
        this(new File(filepath), write);
    }

    /**
     * Creates a {@link Wad Wad} object from a wad file that has been already pointed to by 
     * a {@link java.io.File File} object.
     * @param file a {@link java.io.File File} object that points to a wad file.
     * @param write whether or not the wad file can be written to.
     * @throws java.io.FileNotFoundException if the WAD file cannot be found.
     * @throws jwadlib.UnableToBackUpWADFileException if the WAD file cannot be backed up.
     * @throws jwadlib.UnableToReadWADFileException if the WAD file cannot be read.
     * @since 1.0
     */
    public Wad(File file, boolean write) throws FileNotFoundException, UnableToBackUpWADFileException, UnableToReadWADFileException {
        wadfilelocation = file;
        if (write) {
            wadfile = new RandomAccessFile(file, "rw");
        } else {
            wadfile = new RandomAccessFile(file, "r");
        }
        wadfilechannel = wadfile.getChannel();
        File temp = new File(wadfilelocation.getAbsolutePath() + ".bak");
        int numericextension = 2;
        while (temp.exists()) {
            temp = new File(wadfilelocation.getAbsolutePath() + ".bak" + "." + numericextension);
            numericextension++;
        }
        RandomAccessFile tempraf = new RandomAccessFile(temp, "rw");
        try {
            tempraf.getChannel().transferFrom(wadfilechannel, 0, wadfilechannel.size());
        } catch (IOException e) {
            throw new UnableToBackUpWADFileException("WAD file could not be backed up.", e);
        }
        WadByteBuffer header = new WadByteBuffer(wadfilechannel, 12, 0);
        identifier = header.getInt(0);
        WadByteBuffer directory = new WadByteBuffer(wadfilechannel, header.getInt(4) * 16, header.getInt(8));
        lumps = new LinkedList<Lump>();
        for (int i = 0; i < header.getInt(4); i++) {
            int pointer = directory.getInt();
            int size = directory.getInt();
            String name = directory.getEightByteString();
            try {
                lumps.add(new Lump(name, size, wadfilechannel, pointer));
            } catch (UnableToInitializeLumpException e) {
                throw new UnableToReadWADFileException("A lump in the WAD file could not be intialized.", e);
            }
        }
    }

    /**
     * Returns a {@link WadByteBuffer WadByteBuffer} containing the directory.
     * @return the {@link WadByteBuffer WadByteBuffer} containing the directory.
     * @since 1.0
     */
    private WadByteBuffer buildDirectory() {
        int directorylength = 16 * lumps.size();
        int directorystart = 12;
        int lumpitr = directorystart + directorylength;
        WadByteBuffer directory = new WadByteBuffer(directorylength);
        Lump current;
        for (int i = 0; i < lumps.size(); i++) {
            current = lumps.get(i);
            directory.putInt(lumpitr);
            directory.putInt(current.getSize());
            directory.putEightByteString(current.getName());
            lumpitr += current.getSize();
        }
        return directory;
    }

    /**
     * Returns the {@link java.nio.channels.FileChannel FileChannel} of the WAD file.
     * @return the {@link java.nio.channels.FileChannel FileChannel} of the WAD file.
     * @since 1.0
     */
    public FileChannel getWadFileChannel() {
        return wadfilechannel;
    }

    /**
     * Returns a {@link RandomAccessFile RandomAccessFile} of the WAD 
     * that is represented by the {@link Wad Wad} object.
     * @return a {@link RandomAccessFile RandomAccessFile} of the WAD 
     * that is represented by the {@link Wad Wad} object.
     * @since 1.0
     */
    public RandomAccessFile getWad() {
        return wadfile;
    }

    /**
     * Returns the first four bytes of the WAD file.
     * @return the first four bytes of the WAD file.
     * @since 1.0
     */
    public int getWadIdentifier() {
        return identifier;
    }

    /**
     * Returns the number of lumps in the WAD, as specified in the header.
     * @return the number of lumps in the WAD, as specified in the header.
     * @since 1.0
     */
    public int getNumberOfLumps() {
        return lumps.size();
    }

    /**
     * Returns an {@link java.util.LinkedList LinkedList} of every 
     * {@link Lump Lump} in the WAD.
     * @return an {@link java.util.LinkedList LinkedList} of every 
     * {@link Lump Lump} in the WAD.
     * @since 1.0
     */
    public LinkedList<Lump> getAllLumps() {
        return lumps;
    }

    /**
     * Adds a {@link Lump Lump} object to the {@link Wad Wad}.
     * @param lump the {@link Lump Lump} to the {@link Wad Wad}.
     * @return true if the {@link Lump Lump} is successfully added.
     * @since 1.0
     */
    public boolean addLump(Lump lump) {
        lumps.add(lump);
        return true;
    }

    /**
     * Writes the {@link Wad Wad} to the WAD file that loaded the {@link Wad Wad} 
     * object.
     * @return true if successfully completed.
     * @throws jwadlib.UnableToWriteWADFileException if the WAD file cannot be
     * written to.
     * @since 1.0
     */
    public boolean writeToFile() throws UnableToWriteWADFileException {
        return writeToFile(wadfilechannel);
    }

    /**
     * Writes the {@link Wad Wad} to a WAD file in the current WAD's directory.
     * @param filename the file name as a {@link java.lang.String String} to write the WAD file to. 
     * Do not include ".wad" extension, it is automatically added.
     * @return true if successfully completed.
     * @throws jwadlib.UnableToWriteWADFileException if the WAD file cannot be
     * written to.
     * @since 1.0
     */
    public boolean writeToFile(String filename) throws UnableToWriteWADFileException {
        try {
            File temp = new File(wadfilelocation.getParentFile().getAbsoluteFile(), filename + ".wad");
            return writeToFile(new RandomAccessFile(new File(temp.getAbsolutePath()), "rw").getChannel());
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * Writes the {@link Wad Wad} to a WAD file.
     * @param file the file and path as a {@link java.io.File File} to write the WAD file to. The 
     * {@link java.io.File File's} name should not have an extension; ".wad" will automatically be added.
     * @return true if successfully completed.
     * @throws jwadlib.UnableToWriteWADFileException if the WAD file cannot be
     * written to.
     * @since 1.0
     */
    public boolean writeToFile(File file) throws UnableToWriteWADFileException {
        try {
            return writeToFile(new RandomAccessFile(file, "rw").getChannel());
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * Writes the {@link Wad Wad} to a WAD file.
     * @param filechannel the {@link java.nio.channels.FileChannel FileChannel} to write the WAD file to.
     * @return true if successfully completed.
     * @throws jwadlib.UnableToWriteWADFileException if the WAD file cannot be
     * written to.
     * @since 1.0
     */
    public boolean writeToFile(FileChannel filechannel) throws UnableToWriteWADFileException {
        WadByteBuffer header = new WadByteBuffer(12);
        header.putInt(identifier);
        header.putInt(lumps.size());
        header.putInt(12);
        header.setPosition(0);
        WadByteBuffer directory = buildDirectory();
        try {
            header.writeToFile(filechannel, 0);
            directory.writeToFile(filechannel, 12);
        } catch (IOException e) {
            throw new UnableToWriteWADFileException("WAD file cannot be written to. May be read only.", e);
        }
        int numbytes = directory.getLength() + 12;
        for (int i = 0; i < lumps.size(); i++) {
            try {
                if (lumps.get(i).getRawLumpData() != null) {
                    lumps.get(i).writeToFile(filechannel, numbytes);
                    numbytes = numbytes + lumps.get(i).getSize();
                }
            } catch (UnableToWriteLumpFileException e) {
                throw new UnableToWriteWADFileException("WAD file cannot be written to. May be read only.", e);
            }
        }
        return true;
    }

    public boolean equals(Wad wad) {
        if (identifier != wad.getWadIdentifier() || getNumberOfLumps() != wad.getNumberOfLumps()) {
            return false;
        }
        MessageDigest messagedigest;
        ArrayList<Integer> checked = new ArrayList<Integer>();
        byte[] thisdigest, comparedigest;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        for (int i = 0; i < getNumberOfLumps(); i++) {
            messagedigest.update(getAllLumps().get(i).getRawLumpData().getByteBuffer());
            thisdigest = messagedigest.digest();
            for (int j = 0; j < wad.getNumberOfLumps(); j++) {
                if (!checked.contains(new Integer(j)) && getAllLumps().get(i).getName().equals(wad.getAllLumps().get(j).getName()) && getAllLumps().get(i).getSize() == wad.getAllLumps().get(j).getSize()) {
                    messagedigest.update(wad.getAllLumps().get(j).getRawLumpData().getByteBuffer());
                    comparedigest = messagedigest.digest();
                    if (MessageDigest.isEqual(thisdigest, comparedigest)) {
                        checked.add(j);
                        break;
                    }
                }
                if (j == wad.getNumberOfLumps() - 1) {
                    return false;
                }
            }
        }
        return true;
    }
}
