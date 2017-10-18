package com.browseengine.bobo.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.Version;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.config.FieldConfiguration;
import com.browseengine.bobo.config.impl.XMLFieldConfigurationBuilder;
import com.browseengine.bobo.index.digest.DataDigester;

public class BoboIndexer {

    private Directory _index;

    private DataDigester _digester;

    private IndexWriter _writer;

    private Analyzer _analyzer;

    private static class MyDataHandler implements DataDigester.DataHandler {

        private IndexWriter _writer;

        MyDataHandler(IndexWriter writer) {
            _writer = writer;
        }

        public void handleDocument(Document doc) throws IOException {
            _writer.addDocument(doc);
        }
    }

    public void setAnalyzer(Analyzer analyzer) {
        _analyzer = analyzer;
    }

    private Analyzer getAnalyzer() {
        return _analyzer == null ? new StandardAnalyzer(Version.LUCENE_CURRENT) : _analyzer;
    }

    public BoboIndexer(DataDigester digester, Directory index) {
        super();
        _index = index;
        _digester = digester;
    }

    public void index() throws IOException {
        _writer = null;
        try {
            _writer = new IndexWriter(_index, getAnalyzer(), MaxFieldLength.UNLIMITED);
            MyDataHandler handler = new MyDataHandler(_writer);
            _digester.digest(handler);
            _writer.optimize();
        } finally {
            if (_writer != null) {
                _writer.close();
            }
        }
    }
}
