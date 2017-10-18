package cn.edu.dutir.test.unit.corpus.cwt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import cn.edu.dutir.utility.Constants;
import cn.edu.dutir.utility.MD5;

public class TWURL2DocnoMapper {

    public static int DEFALUT_RECORD_COUNT = 800000;

    private Map<String, String> mUrl2DocnoMap;

    private int mBlockNumber = 0;

    private TWURL2DocnoMapper() {
        mBlockNumber = 0;
        mUrl2DocnoMap = new HashMap<String, String>(DEFALUT_RECORD_COUNT);
    }

    public void collectFiles(File in, List<File> fileList) {
        if (in.isDirectory()) {
            File files[] = in.listFiles();
            for (File file : files) {
                collectFiles(file, fileList);
            }
        } else {
            fileList.add(in);
        }
    }

    public void dispatcher(File mapFile, File srcDir, File dstDir) throws IOException {
        List<File> files = new ArrayList<File>();
        collectFiles(mapFile, files);
        BufferedReader reader = null;
        boolean flag = true;
        for (int i = 0, len = files.size(); i < len; i++) {
            reader = new BufferedReader(new FileReader(files.get(i)));
            System.out.println("Processing " + files.get(i).getCanonicalPath());
            while (true) {
                boolean finished = fetchFixedBlock(reader, DEFALUT_RECORD_COUNT - mUrl2DocnoMap.size());
                if (finished && (i < (len - 1) || mUrl2DocnoMap.size() == 0)) {
                    break;
                }
                if (flag) {
                    transform(srcDir.getAbsolutePath(), dstDir.getAbsolutePath(), Constants.DEFAULT_CHARSET);
                } else {
                    transform(dstDir.getAbsolutePath(), srcDir.getAbsolutePath(), Constants.DEFAULT_CHARSET);
                }
                flag = !flag;
                mUrl2DocnoMap.clear();
            }
            reader.close();
        }
    }

    public boolean fetchFixedBlock(BufferedReader reader) {
        return fetchFixedBlock(reader, DEFALUT_RECORD_COUNT);
    }

    public boolean fetchFixedBlock(BufferedReader reader, int blockSize) {
        Date start = new Date();
        try {
            String text = null;
            if (mUrl2DocnoMap.size() == 0) {
                mBlockNumber++;
            }
            int number = 0;
            System.out.println("Begin reading block " + mBlockNumber + " ...");
            while (number++ < blockSize && (text = reader.readLine()) != null) {
                String fields[] = text.split("\t");
                if (fields == null || fields.length != 2) {
                    String msg = "Invliad format with text =  " + text;
                    System.err.println(msg);
                } else {
                    mUrl2DocnoMap.put(MD5.digest(fields[0]), fields[1]);
                }
            }
            Date end = new Date();
            System.out.println("Finished reading block " + mBlockNumber + ", with block size = " + mUrl2DocnoMap.size() + " and time spent: " + (end.getTime() - start.getTime()) / 1000 + " ms");
            if (text != null) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getDocno(String md5URL) {
        return mUrl2DocnoMap.get(md5URL);
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
        if (fields == null || (fields.length != 6 && fields.length != 4)) {
            String msg = "Invalid trec format : " + line;
            throw new IllegalArgumentException(msg);
        }
        if (fields.length == 4) {
            return line;
        }
        if (fields.length == 6) {
            String tmp = getDocno(MD5.digest(fields[2].trim()));
            if (tmp == null) {
                return line;
            }
            fields[2] = tmp;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(fields[0] + "\t");
        sb.append(fields[3] + "\t");
        sb.append(fields[4] + "\t");
        sb.append(fields[2]);
        return sb.toString();
    }

    public void tranformOrdinaralFile(File inFile, File outFile) {
        tranformOrdinaralFile(inFile, outFile, Constants.DEFAULT_CHARSET);
    }

    public void tranformOrdinaralFile(File inFile, File outFile, String charset) {
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
            System.out.println("Converting " + inputfiles.get(i) + " to " + outputfiles.get(i));
            try {
                File inFile = new File((String) inputfiles.get(i));
                File outFile = new File((String) outputfiles.get(i));
                tranformOrdinaralFile(inFile, outFile, charset);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected static void dohelp() {
        System.out.println("Usage: TWURL2DocnoMapper <mapfile_dir> <input_dir> <swap_dir>");
        System.exit(1);
    }

    public static void main(String args[]) throws Exception {
        String mapFile = "E:/Corpus/CWT200g/cwt200g_url_no";
        String srcFile = "D:/Topic/test";
        String dstFile = "D:/Topic/swap";
        Date start = new Date();
        File inFile = new File(srcFile);
        File outFile = new File(dstFile);
        File refFile = new File(mapFile);
        TWURL2DocnoMapper mapper = new TWURL2DocnoMapper();
        System.out.println("Begin transfroming...");
        mapper.dispatcher(refFile, inFile, outFile);
        Date end = new Date();
        System.out.println("Time spend: " + (end.getTime() - start.getTime()) / 1000);
    }
}
