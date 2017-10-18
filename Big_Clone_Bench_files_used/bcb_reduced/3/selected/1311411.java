package net.sourceforge.jpalm.mobiledb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.sourceforge.jpalm.BaseTestFromResource;
import net.sourceforge.jpalm.Utilities;
import net.sourceforge.jpalm.mobiledb.DatabaseInfo;
import net.sourceforge.jpalm.mobiledb.MobileDB;
import net.sourceforge.jpalm.mobiledb.field.definition.DateDefinition;
import net.sourceforge.jpalm.mobiledb.field.definition.Definition;
import net.sourceforge.jpalm.mobiledb.field.definition.ListDefinition;
import net.sourceforge.jpalm.mobiledb.field.definition.NumberDefinition;
import net.sourceforge.jpalm.mobiledb.field.definition.SequenceDefinition;
import net.sourceforge.jpalm.mobiledb.field.definition.TextDefinition;
import net.sourceforge.jpalm.mobiledb.field.definition.TimeDefinition;
import net.sourceforge.jpalm.mobiledb.field.type.Date;
import net.sourceforge.jpalm.mobiledb.field.type.ListOption;
import net.sourceforge.jpalm.mobiledb.field.type.Number;
import net.sourceforge.jpalm.mobiledb.field.type.Text;
import net.sourceforge.jpalm.mobiledb.field.type.Time;
import net.sourceforge.jpalm.mobiledb.field.type.Type;
import net.sourceforge.jpalm.mobiledb.record.DataRecord;
import net.sourceforge.jpalm.mobiledb.record.FieldDefinitionsRecord;
import net.sourceforge.jpalm.mobiledb.record.FieldDisplaySizesRecord;
import net.sourceforge.jpalm.mobiledb.record.FieldLabelsRecord;
import net.sourceforge.jpalm.mobiledb.record.FilteredDataRecord;
import net.sourceforge.jpalm.mobiledb.record.PreferencesRecord;
import net.sourceforge.jpalm.palmdb.ApplicationInfo;
import net.sourceforge.jpalm.palmdb.Header;
import net.sourceforge.jpalm.palmdb.PalmWriter;
import net.sourceforge.jpalm.palmdb.Record;
import net.sourceforge.jpalm.palmdb.SortInfo;
import net.sourceforge.juint.UInt32;
import net.sourceforge.juint.UInt8;

public class TestMobileDB extends BaseTestFromResource {

    private static final String FILE = "tests/net/sourceforge/jpalm/palmdb/resources/workout.pdb";

    private static final String MD5SUM = "756a240abfc1ed65d7b85ebd587cfdfb";

    private Map<String, Definition> fields;

    private MobileDB database;

    private SimpleDateFormat dateFormat;

    public TestMobileDB() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    }

    public void setUp() {
        fields = new LinkedHashMap<String, Definition>();
        database = new MobileDB(fields);
    }

    public void testMobileDB() {
        fields.put("field1", new TextDefinition());
        database = new MobileDB(fields);
        assertEquals("field1", database.getFieldLabels().get(0));
        assertEquals(TextDefinition.class, database.getFieldDefinitions().get(0).getClass());
    }

    public void testGetSetDatabaseInfo() {
        DatabaseInfo info = new DatabaseInfo();
        info.setApplicationData(new byte[] { 0x01, 0x02 });
        database.setDatabaseInfo(info);
        assertEquals(info, database.getDatabaseInfo());
    }

    public void testGetSetUserData() throws Exception {
        database.setUserData("hi, there");
        assertEquals("hi, there", database.getUserData());
    }

    public void testGetSetUserDataAsByteArray() {
        byte[] data = { 0x02, 0x04, 0x06 };
        database.setUserDataAsByteArray(data);
        assertTrue(Utilities.isEqual(data, database.getUserDataAsByteArray()));
    }

    public void testGetSetName() {
        database.setName("name");
        assertEquals("name", database.getName());
    }

    public void testGetSetNote() {
        database.setNote("note");
        assertEquals("note", database.getNote());
    }

    public void testGetSetFieldDisplaySizes() {
        database.setFieldDisplaySizes(Arrays.asList(new Integer(1), new Integer(2)));
        assertEquals(new Integer(1), database.getFieldDisplaySizes().get(0));
        assertEquals(new Integer(2), database.getFieldDisplaySizes().get(1));
    }

    public void testGetSetFieldLabels() {
        database.setFieldLabels(Arrays.asList("field1"));
        assertEquals("field1", database.getFieldLabels().get(0));
    }

    public void testGetSetFieldDefinitions() {
        database.setFieldDefinitions(Arrays.asList((Definition) new TextDefinition()));
        assertTrue(TextDefinition.class.isInstance(database.getFieldDefinitions().get(0)));
    }

    public void testGetSetDataRecords() {
        DataRecord record = new DataRecord();
        record.setFields(Arrays.asList((Type) new Number(1.1)));
        database.setDataRecords(Arrays.asList(record));
        assertEquals(1, database.getDataRecords().size());
        assertEquals(new Number(1.1), database.getDataRecords().get(0).getFields(Arrays.asList((Definition) new NumberDefinition())).get(0));
    }

    public void testGetSetFilteredDataRecords() {
        FilteredDataRecord record = new FilteredDataRecord();
        record.setFields(Arrays.asList((Type) new Number(1.1)));
        database.setFilteredDataRecords(Arrays.asList(record));
        assertEquals(1, database.getFilteredDataRecords().size());
        assertEquals(new Number(1.1), database.getFilteredDataRecords().get(0).getFields(Arrays.asList((Definition) new NumberDefinition())).get(0));
    }

    public void testGetSetCreationDate() throws Exception {
        database.setCreationDate(new net.sourceforge.jpalm.palmdb.Date(new UInt32(3240144649L)).getDate());
        java.util.Date expected = dateFormat.parse("2006-09-03 10:10:49 Central Standard Time");
        assertEquals(expected, database.getCreationDate());
    }

    public void testGetSetModificationDate() throws Exception {
        database.setModificationDate(new net.sourceforge.jpalm.palmdb.Date(new UInt32(3240145402L)).getDate());
        java.util.Date expected = dateFormat.parse("2006-09-03 10:23:22 Central Standard Time");
        assertEquals(expected, database.getModificationDate());
    }

    public void testGetRecords() {
        ArrayList<FilteredDataRecord> filteredDataRecords = new ArrayList<FilteredDataRecord>();
        filteredDataRecords.add(new FilteredDataRecord());
        filteredDataRecords.add(new FilteredDataRecord());
        database.setFilteredDataRecords(filteredDataRecords);
        ArrayList<DataRecord> dataRecords = new ArrayList<DataRecord>();
        dataRecords.add(new DataRecord());
        dataRecords.add(new DataRecord());
        database.setDataRecords(dataRecords);
        database.setFieldDefinitionsRecord(new FieldDefinitionsRecord());
        database.setFieldLabelsRecord(new FieldLabelsRecord());
        database.setFieldDisplaySizesRecord(new FieldDisplaySizesRecord());
        database.setPreferencesRecord(new PreferencesRecord());
        java.util.List<Record> records = database.getRecords();
        assertEquals(8, records.size());
        assertTrue(records.contains(new FilteredDataRecord()));
        assertTrue(records.contains(new DataRecord()));
        assertTrue(records.contains(new FieldDefinitionsRecord()));
        assertTrue(records.contains(new FieldLabelsRecord()));
        assertTrue(records.contains(new FieldDisplaySizesRecord()));
        assertTrue(records.contains(new PreferencesRecord()));
    }

    public void testSetRecords() {
        ArrayList<Record> records = new ArrayList<Record>();
        records.add(new FilteredDataRecord());
        records.add(new FilteredDataRecord());
        records.add(new DataRecord());
        records.add(new DataRecord());
        records.add(new FieldDefinitionsRecord());
        records.add(new FieldLabelsRecord());
        records.add(new FieldDisplaySizesRecord());
        records.add(new PreferencesRecord());
        database.setRecords(records);
        assertEquals(2, database.getFilteredDataRecords().size());
        assertEquals(2, database.getDataRecords().size());
        assertEquals(new FieldDefinitionsRecord(), database.getFieldDefinitionsRecord());
        assertEquals(new FieldLabelsRecord(), database.getFieldLabelsRecord());
        assertEquals(new FieldDisplaySizesRecord(), database.getFieldDisplaySizesRecord());
        assertEquals(new PreferencesRecord(), database.getPreferencesRecord());
    }

    public void testGetSetHeader() {
        Header header = new Header();
        database.setHeader(header);
        assertEquals(header, database.getHeader());
    }

    public void testGetSetSortInfo() {
        SortInfo info = new SortInfo(new byte[0]);
        database.setSortInfo(info);
        assertEquals(info, database.getSortInfo());
    }

    public void testGetSetApplicationInfo() {
        ApplicationInfo info = new ApplicationInfo();
        UInt8 id = new UInt8(100);
        info.setLastUniqueId(id);
        assertEquals(new UInt8(15), database.getApplicationInfo().getLastUniqueId());
        database.setApplicationInfo(info);
        assertEquals(id, database.getApplicationInfo().getLastUniqueId());
    }

    public void testSave() throws Exception {
        MobileDB database = new MobileDB();
        database.setName("workout");
        database.setModificationDate(Timestamp.valueOf("2006-12-08 00:00:00"));
        database.setCreationDate(Timestamp.valueOf("2006-12-08 00:00:00"));
        PreferencesRecord preferencesRecord = new PreferencesRecord();
        preferencesRecord.setNote("Workout database.");
        database.setPreferencesRecord(preferencesRecord);
        FieldDisplaySizesRecord fieldDisplaySizesRecord = new FieldDisplaySizesRecord();
        database.setFieldDisplaySizesRecord(fieldDisplaySizesRecord);
        FieldLabelsRecord fieldLabelsRecord = new FieldLabelsRecord();
        ArrayList<String> fieldLabels = new ArrayList<String>();
        fieldLabels.add("Sequence");
        fieldLabels.add("Exercise");
        fieldLabels.add("Weight");
        fieldLabels.add("Repetitions");
        fieldLabels.add("Notes");
        fieldLabels.add("Date");
        fieldLabels.add("Time");
        fieldLabelsRecord.setFieldLabels(fieldLabels);
        database.setFieldLabelsRecord(fieldLabelsRecord);
        FieldDefinitionsRecord fieldDefinitionsRecord = new FieldDefinitionsRecord();
        ArrayList<Definition> definitions = new ArrayList<Definition>();
        SequenceDefinition sequenceDefinition = new SequenceDefinition();
        sequenceDefinition.setIncrement(1d);
        sequenceDefinition.setInitialValue(2d);
        definitions.add(sequenceDefinition);
        ListDefinition exercises = new ListDefinition();
        ArrayList<ListOption> options = new ArrayList<ListOption>();
        options.add(new ListOption("Barbell Bench Press"));
        options.add(new ListOption("Weighted Chest Dip"));
        options.add(new ListOption("Dumbbell Fly"));
        options.add(new ListOption("Lever Pec Deck Fly"));
        options.add(new ListOption("New"));
        exercises.setOptions(options);
        definitions.add(exercises);
        definitions.add(new NumberDefinition());
        definitions.add(new NumberDefinition());
        definitions.add(new TextDefinition());
        DateDefinition dateDefinition = new DateDefinition();
        dateDefinition.setDefaultToCurrentDate(true);
        definitions.add(dateDefinition);
        TimeDefinition timeDefinition = new TimeDefinition();
        timeDefinition.setDefaultToCurrentTime(true);
        definitions.add(timeDefinition);
        fieldDefinitionsRecord.setFieldDefinitions(definitions);
        database.setFieldDefinitionsRecord(fieldDefinitionsRecord);
        DataRecord dataRecord = new DataRecord();
        ArrayList<Type> dataFields = new ArrayList<Type>();
        dataFields.add(new Number(1));
        dataFields.add(new Text("Barbell Bench Press"));
        dataFields.add(new Number(165));
        dataFields.add(new Number(10));
        dataFields.add(new Text(""));
        dataFields.add(new Date(Timestamp.valueOf("2006-12-08 00:00:00")));
        dataFields.add(new Time(53408));
        dataRecord.setFields(dataFields);
        database.setDataRecords(Arrays.asList(dataRecord));
        PalmWriter writer = new PalmWriter();
        writer.save(new FileOutputStream(FILE), database);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream stream = new FileInputStream(new File(FILE));
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        String hash = Utilities.byteArrayToHexString(digest.digest());
        assertEquals(MD5SUM, hash);
    }
}
