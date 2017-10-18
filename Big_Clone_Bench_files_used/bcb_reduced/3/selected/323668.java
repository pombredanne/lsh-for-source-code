package net.sf.katta.index.indexer.merge;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import net.sf.katta.testutil.ExtendedTestCase;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.jmock.Expectations;
import org.jmock.Mockery;

public class DfsIndexRecordReaderTest extends ExtendedTestCase {

    private File _file = createFile("_temporary/jobId");

    public void testNext() throws IOException, URISyntaxException {
        JobConf jobConf = new JobConf();
        Path out = new Path(_file.getAbsolutePath());
        FileOutputFormat.setOutputPath(jobConf, out);
        Mockery mockery = new Mockery();
        Path path = new Path("src/test/testIndexB/aIndex.zip");
        FileSystem fileSystem = FileSystem.get(jobConf);
        long len = fileSystem.getFileStatus(path).getLen();
        FileSplit fileSplit = new FileSplit(path, 0, len, (String[]) null);
        final IDocumentDuplicateInformation duplicateInformation = mockery.mock(IDocumentDuplicateInformation.class);
        DfsIndexRecordReader reader = new DfsIndexRecordReader(jobConf, fileSplit, duplicateInformation);
        mockery.checking(new Expectations() {

            {
                atLeast(1).of(duplicateInformation).getKeyField();
                will(returnValue("foo"));
                atLeast(1).of(duplicateInformation).getSortField();
                will(returnValue("foo"));
            }
        });
        Text text = reader.createKey();
        DocumentInformation information = reader.createValue();
        reader.next(text, information);
        assertEquals("bar", text.toString());
        assertEquals("bar", information.getSortValue().toString());
        assertEquals(0, information.getDocId().get());
        assertEquals(new File(out.toString(), ".indexes/" + path.getName() + "-" + MD5Hash.digest(path.toString()) + "-uncompress").getAbsolutePath(), new File(new URI(information.getIndexPath().toString())).getAbsolutePath());
        mockery.assertIsSatisfied();
    }
}
