package cn.edu.dutir.test.unit.corpus.cwt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Vector;
import cn.edu.dutir.utility.Constants;
import cn.edu.dutir.utility.MD5;
import lemurproject.indri.ParsedDocument;
import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.ScoredExtentResult;

public class TWURL2DocnoTransformer {

    private QueryEnvironment mEnv;

    private String mIndexPath;

    private TWURL2DocnoTransformer(String indexPath) {
        mIndexPath = indexPath;
        try {
            mEnv = new QueryEnvironment();
            mEnv.addIndex(mIndexPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String url2Docno(String mdURl) {
        ScoredExtentResult[] rs;
        try {
            rs = mEnv.runQuery(mdURl, 1);
            ParsedDocument[] ds = mEnv.documents(rs);
            if (ds == null || ds.length != 1) {
                String msg = "No docno found or not unique docno with url =  " + mdURl;
                throw new IllegalArgumentException(msg);
            }
            return docno(ds[0].text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String docno(String docText) {
        int startIndex = docText.indexOf("<DOCNO>");
        int endIndex = docText.indexOf("</DOCNO>");
        if (startIndex < 0 || endIndex < 0) {
            String msg = "Invalid document found, with document text = \n" + docText;
            throw new IllegalArgumentException(msg);
        }
        return docText.substring(startIndex + "<DOCNO>".length(), endIndex);
    }

    /**
	 * ���룺Indri�������6�и�ʽ����topic-id Q0 document-id rank similarity run-id.
	 * �����д˴���document-dΪURL,����Ҫת�������Ӧ����ʵdocument-id,��ת����SEWM2009Ҫ��
	 * ��4�и�ʽ��topic-id rank similarity docno.
	 * 
	 * @param line
	 * @return
	 */
    public String transformString(String line) {
        String fields[] = line.split("\\s+");
        if (fields == null || fields.length != 6) {
            String msg = "Invalid Trec Format : " + line;
            throw new IllegalArgumentException(msg);
        }
        fields[2] = url2Docno(MD5.digest(fields[2].trim()));
        StringBuffer sb = new StringBuffer();
        sb.append(fields[0] + "\t");
        sb.append(fields[3] + "\t");
        sb.append(fields[4] + "\t");
        sb.append(fields[2]);
        return sb.toString();
    }

    public void transformOrdinaralFile(File inFile, File outFile) {
        transformOrdinaralFile(inFile, outFile, Constants.DEFAULT_CHARSET);
    }

    public void transformOrdinaralFile(File inFile, File outFile, String charset) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), charset));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), charset));
            String text = null;
            while ((text = reader.readLine()) != null) {
                writer.write(transformString(text));
                writer.newLine();
            }
            reader.close();
            writer.flush();
            writer.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void transform(File srcDir, File dstDir) throws IOException {
        transform(srcDir, dstDir, Constants.DEFAULT_CHARSET);
    }

    public void transform(File srcDir, File dstDir, String charset) throws IOException {
        transform(srcDir.getCanonicalPath(), dstDir.getCanonicalPath(), charset);
    }

    public void transform(String srcDir, String dstDir) throws IOException {
        transform(srcDir, dstDir, Constants.DEFAULT_CHARSET);
    }

    public void transform(String srcDir, String dstDir, String charset) throws IOException {
        BufferedReader srcbuffer;
        BufferedWriter outbuffer;
        String dataline;
        Vector inputfiles = new Vector();
        Vector outputfiles = new Vector();
        inputfiles.add(srcDir);
        outputfiles.add(dstDir);
        int i, j;
        File tmpfile, tmpout;
        String dirfiles[];
        for (i = 0; i < inputfiles.size(); i++) {
            System.out.println((String) inputfiles.get(i));
            tmpfile = new File((String) inputfiles.get(i));
            if (tmpfile.exists() == false) {
                System.out.println("ERROR: Source file " + (String) inputfiles.get(i) + " does not exist./n");
                continue;
            }
            if (tmpfile.isDirectory() == true) {
                tmpout = new File((String) outputfiles.get(i));
                if (tmpout.exists() == false) {
                    tmpout.mkdir();
                }
                dirfiles = tmpfile.list();
                if (dirfiles != null) {
                    for (j = 0; j < dirfiles.length; j++) {
                        inputfiles.add((String) inputfiles.get(i) + File.separator + dirfiles[j]);
                        outputfiles.add((String) outputfiles.get(i) + File.separator + dirfiles[j]);
                    }
                }
                continue;
            }
            System.err.println("Converting " + inputfiles.get(i) + " to " + outputfiles.get(i));
            try {
                File inFile = new File((String) inputfiles.get(i));
                File outFile = new File((String) outputfiles.get(i));
                transformOrdinaralFile(inFile, outFile, charset);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void dohelp() {
        System.out.println("Usage: TWURL2DocnoTransformer <index_dir> " + "<input_dir> <swap_dir>");
        System.exit(1);
    }

    public void commandLineInterface() {
        String query = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(System.in, Constants.DEFAULT_CHARSET));
            System.out.print("URL2Docno >");
            while ((query = reader.readLine()) != null) {
                if (query.equals("")) {
                    continue;
                }
                if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println("***************************************");
                System.out.println(url2Docno(MD5.digest(query)));
                System.out.println("***************************************");
                System.out.print("URL2Docno >");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
            dohelp();
        }
        String indexDir = "D:/cwt200g_url_index";
        String srcDir = "D:/Topic/correct";
        String dstDir = "D:/Topic/swap";
        indexDir = args[0];
        srcDir = args[1];
        dstDir = args[2];
        Date start = new Date();
        TWURL2DocnoTransformer mTransformer = new TWURL2DocnoTransformer(indexDir);
        System.out.println("Begin transfroming...");
        mTransformer.transform(srcDir, dstDir, "gbk");
        Date end = new Date();
        System.out.println("Time spend: " + (end.getTime() - start.getTime()) / 1000);
    }
}
