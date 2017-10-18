package fr.x9c.cadmium.kernel;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import fr.x9c.cadmium.util.IO;
import fr.x9c.cadmium.util.Misc;
import fr.x9c.cadmium.util.RandomAccessInputStream;

/**
 * This class implements bytecode file loading.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
final class FileLoader {

    /** Magic for bytecode file. */
    private static final String EXEC_MAGIC = "Caml1999X008";

    /** Size of file trailer (number of sections + magic). */
    private static final int TRAILER_SIZE = 16;

    /** Identifier for MD5 digest algorithm. */
    private static final String MD5_ALGO = "MD5";

    /** Section name for shared lib path. */
    private static final String SECTION_SHARED_LIB_PATH = "DLPT";

    /** Section name for shared libs. */
    private static final String SECTION_SHARED_LIBS = "DLLS";

    /** Section name for requested primitives. */
    private static final String SECTION_REQ_PRIMS = "PRIM";

    /** Section name for code. */
    private static final String SECTION_CODE = "CODE";

    /** Section name for data. */
    private static final String SECTION_DATA = "DATA";

    /** Section name for debug infos. */
    private static final String SECTION_DEBUG = "DBUG";

    /** Length of section name in ISO-8859-1 characters. */
    private static final int SECTION_NAME_LENGTH = 4;

    /** Size of section descriptor (section name + section length). */
    private static final int SECTION_SIZE = 8;

    /** Message for invalid magic. */
    private static final String INVALID_MAGIC = "invalid magic";

    /** Message for missing section. */
    private static final String MISSING_SECTION = "missing '%s' section";

    /** Message for invalid section. */
    private static final String INVALID_SECTION = "section of more than " + Integer.MAX_VALUE + " bytes";

    /** Message for invalid code section (size being not multiple of 4). */
    private static final String INVALID_CODE_SECTION_SIZE = "code section size should be a multiple of 4";

    /** List of requested primitives. */
    private final List<String> primitives;

    /** List of shared libraries paths. */
    private final List<String> sharedLibPath;

    /** List of shared libraries. */
    private final List<String> sharedLibs;

    /** Code section, as an array of 32-bit integers. */
    private final int[] code;

    /** Value for global data. */
    private final Value globalData;

    /** Value for debug information. */
    private final Value debugInfo;

    /**
     * Loads sections from a bytecode file.
     * @param ctxt used for global data loading (custom types). <br/>
     *             Code, code digest, global data and debug informations are
     *             also set - should not be <tt>null</tt>
     * @param inputStream bytecode source - should not be <tt>null</tt>
     * @throws IOException if an error occurs during file read
     * @throws InvalidFileFormatException if the file is not a valid
     *                                    bytecode file
     * @throws NoSuchAlgorithmException if MD5 algorithm is not available
     * @throws Fatal.Exception if an unrecoverable error (such as 64-bit element) occurs
     */
    FileLoader(final Context ctxt, final RandomAccessInputStream inputStream) throws IOException, InvalidFileFormatException, NoSuchAlgorithmException, Fatal.Exception {
        assert ctxt != null : "null ctxt";
        assert inputStream != null : "null inputStream";
        final long length = inputStream.length();
        DataInput in = inputStream.dataInputFrom(length - FileLoader.TRAILER_SIZE);
        final int nbSections = Misc.ensure32s(IO.read32u(in));
        final byte[] magic = new byte[FileLoader.EXEC_MAGIC.length()];
        in.readFully(magic);
        if (!FileLoader.EXEC_MAGIC.equals(Misc.convertBytesToString(magic))) {
            throw new InvalidFileFormatException(FileLoader.INVALID_MAGIC);
        }
        long offset = 0;
        in = inputStream.dataInputFrom(length - (FileLoader.TRAILER_SIZE + FileLoader.SECTION_SIZE * nbSections));
        final Map<String, Section> sections = new LinkedHashMap<String, Section>();
        for (int i = 0; i < nbSections; i++) {
            final byte[] tmpName = new byte[FileLoader.SECTION_NAME_LENGTH];
            in.readFully(tmpName);
            final String name = Misc.convertBytesToString(tmpName);
            final long lenSection = IO.read32u(in);
            if (lenSection > Integer.MAX_VALUE) {
                throw new InvalidFileFormatException(FileLoader.INVALID_SECTION);
            }
            sections.put(name, new Section(name, offset, (int) lenSection));
            offset += lenSection;
        }
        for (Section s : sections.values()) {
            s.offset = length - (offset - s.offset + FileLoader.TRAILER_SIZE + FileLoader.SECTION_SIZE * nbSections);
        }
        final Section codeSection = sections.get(FileLoader.SECTION_CODE);
        if (codeSection == null) {
            throw new InvalidFileFormatException(String.format(FileLoader.MISSING_SECTION, FileLoader.SECTION_CODE));
        }
        final int codeSize = codeSection.size;
        if ((codeSize % 4) != 0) {
            throw new InvalidFileFormatException(FileLoader.INVALID_CODE_SECTION_SIZE);
        }
        final byte[] codeData = new byte[codeSize];
        in = inputStream.dataInputFrom(codeSection.offset);
        in.readFully(codeData);
        final MessageDigest md5 = MessageDigest.getInstance(FileLoader.MD5_ALGO);
        final byte[] codeDigest = md5.digest(codeData);
        ctxt.setCodeDigest(codeDigest);
        int ptr = 0;
        while (ptr < codeSize) {
            byte tmp = codeData[ptr];
            codeData[ptr] = codeData[ptr + 3];
            codeData[ptr + 3] = tmp;
            tmp = codeData[ptr + 1];
            codeData[ptr + 1] = codeData[ptr + 2];
            codeData[ptr + 2] = tmp;
            ptr += 4;
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(codeData);
        final DataInputStream dis = new DataInputStream(bais);
        final int lenCode = codeSize / 4;
        this.code = new int[lenCode];
        for (int i = 0; i < lenCode; i++) {
            this.code[i] = dis.readInt();
        }
        ctxt.appendCode(this.code);
        ctxt.setupCallbackTail();
        this.sharedLibPath = Collections.unmodifiableList(readSection(inputStream, FileLoader.SECTION_SHARED_LIB_PATH, sections, false));
        this.sharedLibs = Collections.unmodifiableList(readSection(inputStream, FileLoader.SECTION_SHARED_LIBS, sections, false));
        final List<String> primList = readSection(inputStream, FileLoader.SECTION_REQ_PRIMS, sections, true);
        if ((primList == null) || (primList.size() == 0)) {
            throw new InvalidFileFormatException(String.format(FileLoader.MISSING_SECTION, FileLoader.SECTION_REQ_PRIMS));
        }
        this.primitives = Collections.unmodifiableList(primList);
        final Section data = sections.get(FileLoader.SECTION_DATA);
        if (data == null) {
            throw new InvalidFileFormatException(String.format(FileLoader.MISSING_SECTION, FileLoader.SECTION_DATA));
        }
        in = inputStream.dataInputFrom(data.offset);
        try {
            this.globalData = Intern.inputVal(ctxt, in, true);
        } catch (final Fail.Exception fe) {
            throw new InvalidFileFormatException(fe.getMessage());
        }
        ctxt.setGlobalData(this.globalData);
        final Section debug = sections.get(FileLoader.SECTION_DEBUG);
        if (debug != null) {
            in = inputStream.dataInputFrom(debug.offset);
            final int numEvents = Misc.ensure32s(IO.read32u(in));
            final Block events = Block.createBlock(numEvents, 0);
            for (int i = 0; i < numEvents; i++) {
                final int orig = Misc.ensure32s(IO.read32u(in));
                final Value evl;
                try {
                    evl = Intern.inputVal(ctxt, in, true);
                } catch (final Fail.Exception fe) {
                    throw new InvalidFileFormatException(fe.toString());
                }
                Value l = evl;
                while (l != Value.EMPTY_LIST) {
                    final Block b = l.asBlock();
                    final Block blk = b.get(0).asBlock();
                    blk.set(BackTrace.EV_POS, Value.createFromLong(blk.get(BackTrace.EV_POS).asLong() + orig));
                    l = b.get(1);
                }
                events.set(i, evl);
            }
            this.debugInfo = Value.createFromBlock(events);
        } else {
            this.debugInfo = Value.FALSE;
        }
        ctxt.setDebugInfo(this.debugInfo);
    }

    /**
     * Returns the list of the shared library paths. <br/>
     * The returned list is unmodifiable.
     * @return the list of the shared library paths
     */
    List<String> getSharedLibPath() {
        return this.sharedLibPath;
    }

    /**
     * Returns the list of the shared libraries. <br/>
     * The returned list is unmodifiable.
     * @return the list of the shared libraries
     */
    List<String> getSharedLibs() {
        return this.sharedLibs;
    }

    /**
     * Returns the list of requested primitives. <br/>
     * The returned list is unmodifiable.
     * @return the list of requested primitives
     */
    List<String> getPrimitives() {
        return this.primitives;
    }

    /**
     * Returns the code to interpret.
     * @return the code to interpret
     */
    int[] getCode() {
        return this.code;
    }

    /**
     * Returns the value representing the global data.
     * @return the value representing the global data
     */
    Value getGlobalData() {
        return this.globalData;
    }

    /**
     * Returns the debug information, <i>false</i> if none.
     * @return the debug information, <i>false</i> if none
     */
    Value getDebugInfo() {
        return this.debugInfo;
    }

    /**
     * Reads a section of strings from a given file
     * (and also fills the data field of the section).
     * @param in stream to read section from - should not be <tt>null</tt>
     * @param name name of the section to read - should not be <tt>null</tt>
     * @param sections map containing sections data
     *                 - should not be <tt>null</tt>
     * @param mandatory whether the section is mandatory
     * @return the list a strings from the section <br/>
     *         an empty list is returned if the section does not exist and is
     *         not mandatory <br/>
     *         <tt>null</tt> is returned if the section does not exist and is
     *         mandatory
     * @throws IOException if an error occurs while reading section
     */
    private static List<String> readSection(final RandomAccessInputStream in, final String name, final Map<String, Section> sections, final boolean mandatory) throws IOException {
        assert in != null : "null in";
        assert name != null : "null name";
        assert sections != null : "null sections";
        final List<String> res = new LinkedList<String>();
        final Section section = sections.get(name);
        if (section != null) {
            final int len = section.size;
            final byte[] data = new byte[len];
            in.dataInputFrom(section.offset).readFully(data);
            int idx = 0;
            while ((idx < len) && (data[idx] != 0)) {
                final int start = idx;
                while ((idx < len) && (data[idx] != 0)) {
                    idx++;
                }
                res.add(Misc.convertBytesToString(data, start, idx - start));
                idx++;
            }
            return res;
        } else {
            return mandatory ? null : res;
        }
    }

    /**
     * This class describes a section of a bytecode file.
     */
    private static final class Section {

        /** Name of the section. */
        private final String name;

        /** Offset of the section. */
        private long offset;

        /** Size of the section in bytes. */
        private final int size;

        /**
         * Constructs a section from name, offset and data.
         * @param n name - should not be <tt>null</tt>
         * @param ofs offset - should be >= 0
         * @param s size - should be >= 0
         */
        private Section(final String n, final long ofs, final int s) {
            assert n != null : "null n";
            assert ofs >= 0 : "ofs should be >= 0";
            assert s >= 0 : "s should be >= 0";
            this.name = n;
            this.offset = ofs;
            this.size = s;
        }
    }
}
