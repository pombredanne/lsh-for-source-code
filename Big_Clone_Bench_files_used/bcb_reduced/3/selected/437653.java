package net.sf.fileexchange.util.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;

public class ResourceBuilder {

    private final ByteArrayOutputStream buffer;

    private final PrintStream printStream;

    private String contentType;

    private final Set<Method> allowedMethods;

    public ResourceBuilder() {
        buffer = new ByteArrayOutputStream();
        printStream = new PrintStream(buffer);
        allowedMethods = EnumSet.of(Method.HEAD, Method.GET);
    }

    public PrintStream getPrintStream() {
        return printStream;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addAllowedMethod(Method method) {
        allowedMethods.add(method);
    }

    public GeneratedResource build() {
        printStream.flush();
        byte[] content = buffer.toByteArray();
        return new GeneratedResource(content, contentType, allowedMethods);
    }

    public class GeneratedResource implements Resource {

        private final byte[] content;

        private final String contentType;

        private final Set<Method> allowedMethods;

        private String id;

        private GeneratedResource(byte[] content, String contentType, Set<Method> allowedMethods) {
            this.content = content;
            this.contentType = contentType;
            this.allowedMethods = allowedMethods;
            this.id = null;
        }

        @Override
        public void writeTo(OutputStream out, long offset, long length) throws IOException {
            if (offset >= getContent().length) throw new IndexOutOfBoundsException();
            if (length <= 0 || offset < 0) throw new IllegalArgumentException();
            out.write(getContent(), (int) offset, (int) length);
        }

        @Override
        public long getLength() {
            return getContent().length;
        }

        private String determineID() {
            try {
                MessageDigest algorithm = MessageDigest.getInstance("SHA1");
                byte[] hash = algorithm.digest(getContent());
                return ByteToHex.convert(hash);
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        @Override
        public String getID() {
            if (id == null) this.id = determineID();
            return id;
        }

        @Override
        public void close() throws IOException {
        }

        private byte[] getContent() {
            return content;
        }

        @Override
        public Set<Method> getAllowedMethods() {
            return allowedMethods;
        }

        @Override
        public String getContentType() {
            return contentType;
        }
    }
}
