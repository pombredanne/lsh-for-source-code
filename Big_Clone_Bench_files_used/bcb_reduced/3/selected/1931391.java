package com.sun.jini.discovery.internal;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import net.jini.core.constraint.Integrity;
import net.jini.core.constraint.InvocationConstraint;
import net.jini.core.constraint.InvocationConstraints;
import net.jini.io.UnsupportedConstraintException;

/**
 * Superclass for endpoint-based unicast discovery providers.
 */
class EndpointBasedProvider extends BaseProvider {

    /** Object providing access to non-public endpoint operations */
    protected final EndpointInternals endpointInternals;

    /**
	 * Constructs instance with the given format name and object providing
	 * access to non-public endpoint operations.
	 */
    EndpointBasedProvider(String formatName, EndpointInternals endpointInternals) {
        super(formatName);
        if (endpointInternals == null) {
            throw new NullPointerException();
        }
        this.endpointInternals = endpointInternals;
    }

    /**
	 * Returns true if the given constraints include Integrity.YES as a
	 * requirement or preference; returns false otherwise. If the required
	 * constraints include any constraint other than an Integrity constraint, an
	 * UnsupportedConstraintException is thrown.
	 */
    static boolean checkIntegrity(InvocationConstraints constraints) throws UnsupportedConstraintException {
        boolean integrity = false;
        for (Iterator i = constraints.requirements().iterator(); i.hasNext(); ) {
            InvocationConstraint c = (InvocationConstraint) i.next();
            if (c == Integrity.YES) {
                integrity = true;
            } else if (!(c instanceof Integrity)) {
                throw new UnsupportedConstraintException("cannot satisfy constraint: " + c);
            }
        }
        if (!integrity) {
            for (Iterator i = constraints.preferences().iterator(); i.hasNext(); ) {
                if (i.next() == Integrity.YES) {
                    integrity = true;
                    break;
                }
            }
        }
        return integrity;
    }

    /**
	 * Returns the SHA-1 hash of the concatenation of the given unicast
	 * discovery request and response handshake bytes.
	 */
    static byte[] calcHandshakeHash(ByteBuffer request, ByteBuffer response) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            update(md, request);
            update(md, response);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static void update(MessageDigest md, ByteBuffer buf) {
        if (buf.hasArray()) {
            md.update(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
            buf.position(buf.limit());
        } else {
            byte[] b = new byte[buf.remaining()];
            buf.get(b);
            md.update(b, 0, b.length);
        }
    }
}
