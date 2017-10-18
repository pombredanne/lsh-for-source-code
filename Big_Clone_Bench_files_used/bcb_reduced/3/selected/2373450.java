package jsqlalchemy.sql;

import java.security.*;
import java.util.*;
import jsqlalchemy.Query;
import jsqlalchemy.Table;
import jsqlalchemy.datatypes.Column;
import error.*;

public class Builder {

    public boolean followForeignKeys;

    public Builder() {
    }

    public Builder(Table table) {
        this.table = table;
        this.colNames = new ArrayList<String>(table.columns.keySet());
    }

    public static enum JoinType {

        INNER, LEFT, RIGHT
    }

    ;

    private class JoinTable {

        JoinTable(Table joinTable, String thisColumn, String otherColumn, String as, JoinType joinType) {
            this.joinTable = joinTable;
            this.thisColumn = thisColumn;
            this.otherColumn = otherColumn;
            this.joinType = joinType;
            this.as = as;
        }

        Table joinTable;

        String thisColumn;

        String otherColumn;

        JoinType joinType;

        String as;
    }

    public ArrayList<String> colNames = new ArrayList<String>();

    public ArrayList<String> primaryKeys = new ArrayList<String>();

    public Table table;

    public String where;

    public ArrayList<String> orderBy;

    public Integer limit;

    public Integer offset;

    public ArrayList<JoinTable> joinTables = new ArrayList<JoinTable>();

    public String thisColumn;

    public String otherColumn;

    public boolean count = false;

    public boolean dropIfExist = true;

    public void clear() {
        this.colNames = new ArrayList<String>();
        this.table = null;
        this.where = null;
        this.orderBy = new ArrayList<String>();
        this.limit = null;
        joinTables = new ArrayList<JoinTable>();
        this.thisColumn = null;
        this.otherColumn = null;
        this.count = false;
    }

    /**
	 * count the elements - return number only number of elements
	 * @return select
	 */
    public void count() {
        this.count = true;
    }

    public void where(String where) {
        this.where = where;
    }

    public String compile(Query qry) throws JSQLException {
        switch(qry.type) {
            case INSERT:
                return insert();
            case UPDATE:
                return update();
            case SELECT:
                return select();
            case DELETE:
                return delete();
            case DROP:
                return drop();
            case CREATE:
                return create();
            default:
                throw new JSQLException("unknown type");
        }
    }

    /**
	 * create a SQL-Select-query
	 * @return a SQL-SELECT-query as string
	 */
    public String select() throws JSQLException {
        String cols = "";
        if (table == null) throw new JSQLException("no table name given");
        if (colNames.isEmpty()) cols = "*";
        String qry = "SELECT ";
        if (count) {
            qry += "COUNT(*)";
        } else {
            boolean first = true;
            for (String colName : colNames) {
                if (first) first = false; else cols += ", ";
                cols += table.getTableName() + "." + colName;
            }
            if (followForeignKeys) {
                for (JoinTable join : this.joinTables) {
                    for (String colName : join.joinTable.columns.keySet()) {
                        if (first) first = false; else cols += ", ";
                        if (join.as != null && !join.as.equals("")) cols += join.as + "." + colName; else cols += join.joinTable.getTableName() + "." + colName;
                    }
                }
            }
            qry += cols;
        }
        qry += " FROM " + table.getTableName();
        if (followForeignKeys) qry += join();
        if ((where != null) && (!where.equals(""))) {
            qry += " WHERE " + where;
        }
        if (orderBy != null && (!orderBy.equals(""))) {
            qry += " ORDER BY " + listToString(orderBy);
        }
        if (limit != null) {
            qry += " LIMIT " + limit.toString();
        }
        if (offset != null) {
            qry += " OFFSET " + offset.toString();
        }
        return qry;
    }

    private String listToString(ArrayList<String> list) {
        return listToString(list, ", ");
    }

    private String listToString(ArrayList<String> list, String seperator) {
        String str = "";
        boolean first = true;
        for (String value : list) {
            if (first) first = false; else str += seperator;
            str += value;
        }
        return str;
    }

    public void addLeftJoin(Table table, String otherColumn, String thisColumn, String as) {
        joinTables.add(new JoinTable(table, thisColumn, otherColumn, as, JoinType.LEFT));
    }

    public void addRightJoin(Table table, String otherColumn, String thisColumn, String as) {
        joinTables.add(new JoinTable(table, thisColumn, otherColumn, as, JoinType.RIGHT));
    }

    public void addInnerJoin(Table table, String otherColumn, String thisColumn, String as) {
        joinTables.add(new JoinTable(table, thisColumn, otherColumn, as, JoinType.INNER));
    }

    /**
	 * add a table to the list of joins 
	 * @param tableName
	 * @param otherColumn
	 * @param thisColumn
	 */
    public void addJoinTable(Table table, String otherColumn, String thisColumn, String as, JoinType joinType) {
        joinTables.add(new JoinTable(table, thisColumn, otherColumn, as, joinType));
    }

    /**
	 * create the INNER JOIN-part of an SELECT-query
	 * @return LEFT/RIGHT/INNER JOIN "tableName" ON ...
	 */
    public String join() {
        String joinStr = "";
        for (JoinTable joinTable : joinTables) {
            String as = "";
            String joinTableName = joinTable.joinTable.getTableName();
            if (joinTable.as != null && !joinTable.equals("")) {
                as = " AS " + joinTable.as;
                joinTableName = joinTable.as;
            }
            joinStr += " " + joinTable.joinType.toString() + " JOIN " + joinTable.joinTable.getTableName() + as + " ON " + table.getTableName() + "." + joinTable.thisColumn + " = " + joinTableName + "." + joinTable.otherColumn;
        }
        return joinStr;
    }

    /**
	 * create an update-query
	 * @return an update-query
	 * @throws JSQLException 
	 */
    public String update() throws JSQLException {
        return update(table.getTableName(), table.getColumnNameAndData(true, false, false), where);
    }

    private String update(String tableName, String data, String where) {
        String str = "UPDATE " + tableName + " SET " + data;
        if (where != null && where != "") str += " WHERE " + where;
        return str + ";";
    }

    public String insert() throws JSQLException {
        return insert(table.getTableName(), table.getAllColumnNames(), table.getAllColumnData());
    }

    private String insert(String tableName, String columns, String data) {
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + data + ");";
    }

    public String delete() throws JSQLException {
        return delete(table.getTableName(), where);
    }

    public String delete(String tableName, String where) throws JSQLException {
        if (where == null || where.equals("")) throw new JSQLException("WARNING: You surely do not want to delete the whole table! please set a where-clause");
        return "DELETE FROM " + tableName + " WHERE " + where + ";";
    }

    public String create() throws JSQLException {
        return create(table.getTableName(), createCREATETABLEQuery(table));
    }

    public String create(String tableName, String data) {
        return "CREATE TABLE " + tableName + " (" + data + ");";
    }

    private String createCREATETABLEQuery(Table table) throws JSQLException {
        String qry = "";
        primaryKeys = new ArrayList<String>();
        boolean first = true;
        for (String colName : table.columns.keySet()) {
            Column col = table.columns.get(colName);
            if (!(col.getType().equals("NONE"))) {
                if (first) first = false; else qry += ", ";
                qry += colName + " " + col.getType();
                if (col.isPrimaryKey()) primaryKeys.add(colName);
            }
        }
        if (primaryKeys.size() > 0) {
            qry += ", PRIMARY KEY(";
            first = true;
            for (String key : primaryKeys) {
                if (first) first = false; else qry += ", ";
                qry += key;
            }
            qry += ")";
        }
        return qry;
    }

    public String drop() {
        return drop(table.getTableName(), dropIfExist);
    }

    public String drop(String tableName, boolean ifExist) {
        String qry = "DROP TABLE ";
        if (ifExist) qry += "IF EXISTS ";
        qry += tableName;
        return qry + ";";
    }

    /**
	 * create an MD5-Hash of the import
	 * @param in - input
	 * @return result - MD5-Hash
	 * copied from
	 * http://blog.gotchi.at/index.php?/archives/81-MD5-SHA1-Hashes-erzeugen-mit-JAVA.html
	 */
    public static String getMD5Hash(String in) {
        StringBuffer result = new StringBuffer(32);
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(in.getBytes());
            Formatter f = new Formatter(result);
            for (byte b : md5.digest()) {
                f.format("%02x", b);
            }
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }
}
