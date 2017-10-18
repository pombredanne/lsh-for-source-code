package cn.edu.dutir.corpus.cwt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import cn.edu.dutir.utility.Constants;
import cn.edu.dutir.utility.MD5;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.util.Streams;

public class TWURL2DocnoHandler implements ObjectHandler<String> {

    private Writer mWriter;

    public TWURL2DocnoHandler() {
        mWriter = null;
    }

    public TWURL2DocnoHandler(File outFile) throws IOException {
        this(new BufferedWriter(new FileWriter(outFile)));
    }

    public TWURL2DocnoHandler(Writer writer) {
        mWriter = writer;
    }

    public void setWriter(Writer writer) {
        Streams.closeWriter(mWriter);
        mWriter = writer;
    }

    public void setWriter(File outFile) {
        try {
            setWriter(new BufferedWriter(new FileWriter(outFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(String e) {
        try {
            mWriter.write(toTrecFormat(e));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public String toTrecFormat(String text) {
        String fields[] = text.split("\t");
        if (fields == null || fields.length != 2) {
            String msg = "Invalid format:" + text;
            throw new IllegalArgumentException(msg);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<DOC>" + Constants.NEWLINE);
        sb.append("<DOCNO>" + fields[1] + "</DOCNO>" + Constants.NEWLINE);
        sb.append("<URL>" + MD5.digest(fields[0]) + "</URL>" + Constants.NEWLINE);
        sb.append("</DOC>" + Constants.NEWLINE);
        sb.append(Constants.NEWLINE);
        return sb.toString();
    }

    public void close() {
        Streams.closeWriter(mWriter);
    }
}
