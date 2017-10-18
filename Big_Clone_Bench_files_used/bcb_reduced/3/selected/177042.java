package org.poset.server.persistence.mysql;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.antlr.runtime.RecognitionException;
import org.poset.exception.ConcurrencyException;
import org.poset.exception.ParserException;
import org.poset.exception.SystemException;
import org.poset.model.DomainResponse;
import org.poset.model.Group;
import org.poset.model.Link;
import org.poset.model.Node;
import org.poset.model.Pair;
import org.poset.model.Topic;
import org.poset.model.Tuple;
import org.poset.model.TupleSet;
import org.poset.model.mapper.NodeMapper;
import org.poset.server.Registry;
import org.poset.server.parser.QueryObject;
import org.poset.server.parser.QueryParser;
import org.poset.server.persistence.SQL;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.jdbc.object.StoredProcedure;

public enum NodeMapperImpl implements NodeMapper {

    INSTANCE;

    private final NodeRowMapper rowMapper = new NodeRowMapper();

    private static Map<Integer, Node> idToNode = new ConcurrentHashMap<Integer, Node>();

    private static Map<String, Node> uidShortToNode = new ConcurrentHashMap<String, Node>();

    private static Map<String, TupleSet> uidShortToTupleSet = new ConcurrentHashMap<String, TupleSet>();

    private static final Map<String, Object> emptyMap = new HashMap<String, Object>();

    private ThreadLocal<QueryParser> parserContainer = new ThreadLocal<QueryParser>();

    private final SQLImpl sqlObject = new SQLImpl();

    private JdbcTemplate jdbcTemplate;

    private QueryByUidShort findNodeByUidShort;

    private QueryBySurrogateId findNodeById;

    private QueryTuplesByUidShort findTuplesByUidShort;

    private ProcedureGetGroupPaths callGetGroupPaths;

    private ProcedureGetPath callGetPath;

    private UpdateNode updateNode;

    private class NodeRowMapper implements RowMapper {

        public Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return mapRowNode(rs, rowNumber);
        }
    }

    private class ProcedureGetPath extends StoredProcedure {

        private static final String SQL = "get_path2";

        private Set<String> termCovs = new HashSet<String>();

        public ProcedureGetPath(DataSource dataSource) {
            setDataSource(dataSource);
            setFunction(false);
            setSql(SQL);
            declareParameter(new SqlParameter("inTermId", Types.INTEGER));
            declareParameter(new SqlParameter("inCov", Types.VARCHAR));
            compile();
        }

        public Map<String, Object> execute(Integer termId, Pair.Coverage cov) {
            String cov2 = cov.toString().toLowerCase();
            String cacheKey = String.valueOf(termId) + "-" + cov2;
            if (termCovs.contains(cacheKey)) return emptyMap;
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("inTermId", termId);
            params.put("inCov", cov2);
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) execute(params);
            termCovs.add(cacheKey);
            return map;
        }
    }

    private class ProcedureGetGroupPaths extends StoredProcedure {

        private static final String SQL = "get_group_paths";

        private boolean initialized = false;

        public ProcedureGetGroupPaths(DataSource dataSource) {
            setDataSource(dataSource);
            setFunction(false);
            setSql(SQL);
            declareParameter(new SqlParameter("inAccountId", Types.INTEGER));
            compile();
        }

        public Map<String, Object> execute() {
            if (initialized) return emptyMap;
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) execute(Collections.singletonMap("inAccountId", sqlObject.getAccountId()));
            initialized = true;
            return map;
        }
    }

    private class QueryByUidShort extends MappingSqlQuery {

        public QueryByUidShort(DataSource dataSource) {
            super(dataSource, sqlObject.findNodeByUidShort());
            declareParameter(new SqlParameter("accountId", Types.INTEGER));
            declareParameter(new SqlParameter("uidShort", Types.VARCHAR));
            compile();
        }

        public Node find(String uidShort) {
            if (uidShortToNode.containsKey(uidShort)) return uidShortToNode.get(uidShort);
            Object[] params = new Object[] { sqlObject.getAccountId(), uidShort };
            @SuppressWarnings("unchecked") Node node = extractNode((List<Object>) execute(params), uidShort);
            return node;
        }

        public Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return mapRowNode(rs, rowNumber);
        }
    }

    private class QueryBySurrogateId extends MappingSqlQuery {

        public QueryBySurrogateId(DataSource dataSource) {
            super(dataSource, sqlObject.findNodeBySurrogateId());
            declareParameter(new SqlParameter("accountId", Types.INTEGER));
            declareParameter(new SqlParameter("surrogateId", Types.INTEGER));
            compile();
        }

        public Node find(int surrogateId) {
            if (idToNode.containsKey(surrogateId)) return idToNode.get(surrogateId);
            Object[] params = new Object[] { sqlObject.getAccountId(), surrogateId };
            @SuppressWarnings("unchecked") Node node = extractNode((List<Object>) execute(params), String.valueOf(surrogateId));
            return node;
        }

        public Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return mapRowNode(rs, rowNumber);
        }
    }

    private class QueryTuplesByUidShort extends MappingSqlQuery {

        public QueryTuplesByUidShort(DataSource dataSource) {
            super(dataSource, sqlObject.findTuplesByUidShort());
            declareParameter(new SqlParameter("accountId", Types.INTEGER));
            declareParameter(new SqlParameter("uidShort", Types.VARCHAR));
            compile();
        }

        public List<Tuple> find(String uidShort) {
            Object[] params = new Object[] { sqlObject.getAccountId(), uidShort };
            @SuppressWarnings("unchecked") List<Tuple> list = (List<Tuple>) execute(params);
            return list;
        }

        public Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
            Integer id = rs.getInt("tuple_id");
            Node node = extractNodeFromResultSet(rs, "n_");
            Node group = extractNodeFromResultSet(rs, "gn_");
            assert group instanceof Group;
            Node topic = extractNodeFromResultSet(rs, "tn_");
            assert topic instanceof Topic;
            return new Tuple(id, node, (Group) group, (Topic) topic);
        }
    }

    private class UpdateNode extends SqlUpdate {

        public UpdateNode(DataSource dataSource) {
            setDataSource(dataSource);
            setSql(SQLImpl.updateNode());
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.INTEGER));
            declareParameter(new SqlParameter(Types.INTEGER));
            compile();
        }

        /**
         * @param id for the Customer to be updated
         * @param rating the new value for credit rating
         * @return number of rows updated
         */
        public Node update(Node node) {
            if (node.isTransient()) {
                throw new IllegalArgumentException("Not implemented yet");
            }
            int version = node.getVersion();
            Object[] params = new Object[] { version + 1, node.getUIDShort(), getTypeDbEnum(node), node.getReference(), node.getSurrogateId(), version };
            int count = update(params);
            if (count < 1) throw new ConcurrencyException("Node " + node.getSurrogateId() + "(v. " + version + ") has been updated");
            node.setVersion(version + 1);
            return node;
        }
    }

    static Node extractNodeFromResultSet(ResultSet rs, String prefix) throws SQLException {
        Integer surrogateId = rs.getInt(prefix + "id");
        int version = rs.getInt(prefix + "version");
        if (idToNode.containsKey(surrogateId)) {
            Node old = (Node) idToNode.get(surrogateId);
            if (old.getVersion() > version) throw new ConcurrencyException("Id " + surrogateId + ", version: " + version + ", old version: " + old.getVersion());
            return old;
        }
        String type = rs.getString(prefix + "node_type");
        String uidShort = rs.getString(prefix + "uid_short");
        String reference = rs.getString(prefix + "reference");
        String uri = rs.getString(prefix + "uri");
        String desc = rs.getString(prefix + "description");
        Node node = null;
        if (type.equals("group")) {
            node = new Group(surrogateId, version, uidShort, reference);
        } else if (type.equals("topic")) {
            node = new Topic(surrogateId, version, uidShort, reference);
        } else if (type.equals("link")) {
            node = new Link(surrogateId, version, uidShort, reference, uri, desc);
        } else {
            assert false;
        }
        idToNode.put(surrogateId, node);
        uidShortToNode.put(uidShort, node);
        return node;
    }

    private static String getTypeDbEnum(Node node) {
        if (node instanceof Group) return "group";
        if (node instanceof Topic) return "topic";
        if (node instanceof Link) return "link";
        throw new IllegalArgumentException("The type '" + node.getClass() + "' is not recognized");
    }

    private static Node mapRowNode(ResultSet rs, int rowNumber) throws SQLException {
        return extractNodeFromResultSet(rs, "");
    }

    public DomainResponse<Node> findNodesByQueryString(String queryString) {
        if (queryString.isEmpty()) throw new IllegalArgumentException("queryString cannot be empty");
        QueryObject qo = null;
        try {
            qo = getParser().parse(queryString);
        } catch (RecognitionException e) {
            throw new ParserException(queryString, e);
        } catch (IOException e) {
            throw new SystemException("Unable to read queryString", e);
        }
        SQL so = SQLImpl.fromQueryObject(qo);
        for (Integer termId : so.getIdeals()) {
            callGetPath.execute(termId, Pair.Coverage.IDEAL);
        }
        for (Integer termId : so.getFilters()) {
            callGetPath.execute(termId, Pair.Coverage.FILTER);
        }
        List<Node> nodes = findAdHoc(so.getCacheable());
        DomainResponse<Node> response = new DomainResponse<Node>();
        setNodes(response, nodes);
        return response;
    }

    public TupleSet findTuplesByUidShort(String uidShort) {
        if (uidShortToTupleSet.containsKey(uidShort)) return uidShortToTupleSet.get(uidShort);
        callGetGroupPaths.execute();
        List<Tuple> list = (List<Tuple>) findTuplesByUidShort.find(uidShort);
        TupleSet tuples = new TupleSet(list);
        uidShortToTupleSet.put(uidShort, tuples);
        return tuples;
    }

    QueryParser getParser() {
        QueryParser parser = parserContainer.get();
        if (parser == null) {
            parser = new QueryParser();
            parserContainer.set(parser);
        }
        return parser;
    }

    Node extractNode(List<Object> rows, String id) {
        int size = rows.size();
        if (size < 1) throw new IllegalArgumentException("Id " + id + " not recognized");
        if (size > 1) throw new IllegalStateException("More than one object returned for id " + id);
        return (Node) rows.get(0);
    }

    void setNodes(DomainResponse<Node> response, Collection<Node> nodes) {
        response.setResults(nodes);
        String md5Hash = Registry.md5Digest().digest(nodes.toString().getBytes()).toString();
        response.setMD5Hash(md5Hash);
    }

    DomainResponse<Node> saveOrUpdateNodes(List<Node> nodes) {
        DomainResponse<Node> response = new DomainResponse<Node>();
        for (Node node : nodes) {
            updateNode.update(node);
        }
        setNodes(response, nodes);
        return response;
    }

    public Node findNodeByUidShort(String uidShort) {
        callGetGroupPaths.execute();
        return findNodeByUidShort.find(uidShort);
    }

    List<Node> findAdHoc(String sql) {
        callGetGroupPaths.execute();
        @SuppressWarnings("unchecked") List<Node> nodes = (List<Node>) jdbcTemplate.query(sql, new Object[] { sqlObject.getAccountId() }, rowMapper);
        return nodes;
    }

    public Node findBySurrogateId(int surrogateId) {
        return findNodeById.find(surrogateId);
    }

    public Node saveOrUpdate(Node node) {
        return updateNode.update(node);
    }

    static NodeMapperImpl getInstance() {
        return INSTANCE;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        findNodeByUidShort = new QueryByUidShort(dataSource);
        findNodeById = new QueryBySurrogateId(dataSource);
        callGetGroupPaths = new ProcedureGetGroupPaths(dataSource);
        callGetPath = new ProcedureGetPath(dataSource);
        updateNode = new UpdateNode(dataSource);
        findTuplesByUidShort = new QueryTuplesByUidShort(dataSource);
    }
}
