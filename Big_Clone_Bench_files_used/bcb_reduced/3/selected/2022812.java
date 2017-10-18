package edu.vt.middleware.crypt.digest;

import java.io.InputStream;
import edu.vt.middleware.crypt.AbstractCli;
import edu.vt.middleware.crypt.util.Base64Converter;
import edu.vt.middleware.crypt.util.Converter;
import edu.vt.middleware.crypt.util.HexConverter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Command line interface for digest operations.
 *
 * @author  Middleware Services
 * @version  $Revision: 16 $
 */
public class DigestCli extends AbstractCli {

    /** Digest algorithm option. */
    protected static final String OPT_ALG = "alg";

    /** Salt for digest initialization. */
    protected static final String OPT_SALT = "salt";

    /** Output encoding format. */
    protected static final String OPT_ENCODING = "encoding";

    /** Name of operation provided by this class. */
    private static final String COMMAND_NAME = "digest";

    /** Converts hex to bytes and vice versa. */
    private HexConverter hexConv = new HexConverter();

    /**
   * CLI entry point method.
   *
   * @param  args  Command line arguments.
   */
    public static void main(final String[] args) {
        new DigestCli().performAction(args);
    }

    /** {@inheritDoc} */
    protected void initOptions() {
        super.initOptions();
        final Option algorithm = new Option(OPT_ALG, true, "digest algorithm name");
        algorithm.setArgName("name");
        algorithm.setOptionalArg(false);
        final Option salt = new Option(OPT_SALT, true, "initialize digest with salt before hashing data");
        salt.setArgName("hex_salt");
        salt.setOptionalArg(false);
        final Option infile = new Option(OPT_INFILE, true, "file to digest; defaults to STDIN");
        infile.setArgName("filepath");
        infile.setOptionalArg(false);
        final Option encoding = new Option(OPT_ENCODING, true, "output encoding format, either hex or base64");
        encoding.setArgName("encoding");
        encoding.setOptionalArg(false);
        options.addOption(algorithm);
        options.addOption(salt);
        options.addOption(infile);
        options.addOption(encoding);
    }

    /** {@inheritDoc} */
    protected void dispatch(final CommandLine line) throws Exception {
        if (line.hasOption(OPT_ALG)) {
            digest(line);
        } else {
            printHelp();
        }
    }

    /**
   * Compute a digest of a data stream from options defined on command line.
   *
   * @param  line  Command line argument container.
   *
   * @throws  Exception  On digest errors.
   */
    protected void digest(final CommandLine line) throws Exception {
        final DigestAlgorithm digest = DigestAlgorithm.newInstance(line.getOptionValue(OPT_ALG));
        if (line.hasOption(OPT_SALT)) {
            digest.setSalt(hexConv.toBytes(line.getOptionValue(OPT_SALT)));
        }
        byte[] hash = null;
        final InputStream in = getInputStream(line);
        try {
            hash = digest.digest(in);
        } finally {
            closeStream(in);
        }
        if (line.hasOption(OPT_ENCODING)) {
            final String encName = line.getOptionValue(OPT_ENCODING);
            Converter conv = null;
            if (BASE_64_ENCODING.equals(encName)) {
                conv = new Base64Converter();
            } else if (HEX_ENCODING.equals(encName)) {
                conv = hexConv;
            } else {
                throw new IllegalArgumentException("Unknown encoding.");
            }
            System.out.println(conv.fromBytes(hash));
        } else {
            System.out.print(hash);
        }
    }

    /** {@inheritDoc} */
    protected String getCommandName() {
        return COMMAND_NAME;
    }
}
