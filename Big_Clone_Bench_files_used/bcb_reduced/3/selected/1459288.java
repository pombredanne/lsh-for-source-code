package org.apache.nutch.indexer;

import java.util.Random;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MD5Hash;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.nutch.analysis.NutchDocumentAnalyzer;
import org.apache.nutch.util.NutchConfiguration;
import junit.framework.TestCase;

public class TestDeleteDuplicates extends TestCase {

    Configuration conf;

    FileSystem fs;

    Path root;

    Path index1;

    Path index2;

    Path index3;

    public void setUp() throws Exception {
        conf = NutchConfiguration.create();
        conf.set("fs.default.name", "local");
        fs = FileSystem.get(conf);
        root = new Path("build/test/dedup2-test-" + new Random().nextInt());
        index1 = createIndex("index1", true, 1.0f, 10L, false);
        index2 = createIndex("index2", false, 2.0f, 20L, true);
        index3 = createIndex("index3", true, 1.0f, 10L, true);
    }

    private Path createIndex(String name, boolean hashDup, float inc, long time, boolean incFirst) throws Exception {
        Path idx = new Path(root, name);
        Path sub = new Path(idx, "part-0000");
        Directory dir = FSDirectory.getDirectory(sub.toString());
        IndexWriter writer = new IndexWriter(dir, new NutchDocumentAnalyzer(conf), true);
        Document doc = makeDoc(name, MD5Hash.digest("1").toString(), "http://www.example.com/1", 1.0f + (incFirst ? inc : 0.0f), time);
        writer.addDocument(doc);
        if (hashDup) {
            doc = makeDoc(name, MD5Hash.digest("1").toString(), "http://www.example.com/2", 1.0f + (!incFirst ? inc : 0.0f), time + 1);
        } else {
            doc = makeDoc(name, MD5Hash.digest("2").toString(), "http://www.example.com/1", 1.0f + (!incFirst ? inc : 0.0f), time + 1);
        }
        writer.addDocument(doc);
        writer.close();
        return idx;
    }

    private Document makeDoc(String segment, String digest, String url, float boost, long time) {
        Document doc = new Document();
        doc.add(new Field("segment", segment, Field.Store.YES, Field.Index.NO));
        doc.add(new Field("digest", digest, Field.Store.YES, Field.Index.NO));
        doc.add(new Field("url", url, Field.Store.YES, Field.Index.TOKENIZED));
        doc.setBoost(boost);
        doc.add(new Field("boost", "" + boost, Field.Store.YES, Field.Index.NO));
        doc.add(new Field("tstamp", DateTools.timeToString(time, Resolution.MILLISECOND), Field.Store.YES, Field.Index.NO));
        return doc;
    }

    public void tearDown() throws Exception {
        fs.delete(root);
    }

    private void hashDuplicatesHelper(Path index, String url) throws Exception {
        DeleteDuplicates dedup = new DeleteDuplicates(conf);
        dedup.dedup(new Path[] { index });
        FsDirectory dir = new FsDirectory(fs, new Path(index, "part-0000"), false, conf);
        IndexReader reader = IndexReader.open(dir);
        assertEquals("only one doc left", reader.numDocs(), 1);
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) {
                System.out.println("-doc " + i + " deleted");
                continue;
            }
            Document doc = reader.document(i);
            assertEquals("check url", url, doc.get("url"));
            System.out.println(doc);
        }
        reader.close();
    }

    public void testHashDuplicates() throws Exception {
        hashDuplicatesHelper(index1, "http://www.example.com/2");
        hashDuplicatesHelper(index3, "http://www.example.com/1");
    }

    public void testUrlDuplicates() throws Exception {
        DeleteDuplicates dedup = new DeleteDuplicates(conf);
        dedup.dedup(new Path[] { index2 });
        FsDirectory dir = new FsDirectory(fs, new Path(index2, "part-0000"), false, conf);
        IndexReader reader = IndexReader.open(dir);
        assertEquals("only one doc left", reader.numDocs(), 1);
        MD5Hash hash = MD5Hash.digest("2");
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) {
                System.out.println("-doc " + i + " deleted");
                continue;
            }
            Document doc = reader.document(i);
            assertEquals("check hash", hash.toString(), doc.get("digest"));
            System.out.println(doc);
        }
        reader.close();
    }

    public void testMixedDuplicates() throws Exception {
        DeleteDuplicates dedup = new DeleteDuplicates(conf);
        dedup.dedup(new Path[] { index1, index2 });
        FsDirectory dir = new FsDirectory(fs, new Path(index1, "part-0000"), false, conf);
        IndexReader reader = IndexReader.open(dir);
        assertEquals("only one doc left", reader.numDocs(), 1);
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) {
                System.out.println("-doc " + i + " deleted");
                continue;
            }
            Document doc = reader.document(i);
            assertEquals("check url", "http://www.example.com/2", doc.get("url"));
            System.out.println(doc);
        }
        reader.close();
        dir = new FsDirectory(fs, new Path(index2, "part-0000"), false, conf);
        reader = IndexReader.open(dir);
        assertEquals("only one doc left", reader.numDocs(), 1);
        MD5Hash hash = MD5Hash.digest("2");
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) {
                System.out.println("-doc " + i + " deleted");
                continue;
            }
            Document doc = reader.document(i);
            assertEquals("check hash", hash.toString(), doc.get("digest"));
            System.out.println(doc);
        }
        reader.close();
    }
}
