package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

public class HdtDataSource {

    private static Logger logger = LogManager
            .getLogger(HdtDataSource.class.getName());

    private static HDT datasource;
    private static NodeDictionary dictionary;
    private static HDTGraph graph;
    private static Model triples;
    private LoadingCache<QueryVariableCount, Integer> countResults;

    public class QueryVariableCount {
        private String query;
        private String variable;

        public int getQuerySize(){
            return query.length();
        }

        public String getQueryString(){
            return query;
        }

        public String getVariableString(){
            return variable;
        }

        // standard getters and setters
    }

    /**
     * Creates a new HdtDataSource.
     *
     * @param hdtFile     the HDT datafile
     * @throws IOException if the file cannot be loaded
     */
    public HdtDataSource(String hdtFile) throws IOException {
        datasource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
        graph = new HDTGraph(datasource);
        triples = ModelFactory.createModelForGraph(graph);
        countResults = CacheBuilder.newBuilder()
                .maximumWeight(100000)
                .weigher(new Weigher<QueryVariableCount, Integer>() {
                    public int weigh(QueryVariableCount k, Integer resultSize) {
                        return k.getQuerySize();
                    }
                })
                .build(
                        new CacheLoader<QueryVariableCount, Integer>() {
                            @Override
                            public Integer load(QueryVariableCount s) throws Exception {
                                return getCountResults(s.getQueryString(), s.getVariableString());
                            }
                        });

    }

    public static int getCountResults(String starQuery, String variableName) {

        variableName = variableName.replace("“", "").replace("”", "");
        String maxFreqQueryString = "select (count(?" + variableName
                + ") as ?count) where { " + starQuery + " " + "} GROUP BY ?"
                + variableName + " " + "ORDER BY ?" + variableName
                + " DESC (?count) LIMIT 1 ";

        Query query = QueryFactory.create(maxFreqQueryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("count");
                int res = x.asLiteral().getInt();
                logger.info("max freq value: " + res + " for variable "
                        + variableName);
                return res;
            } else {
                return 0;
            }
        }
    }

    public static int getCountResults(TriplePath triplePath,
                                      String variableName) {
        List<String> aux = Helper.triplePartExtractor(triplePath);
        String subject = aux.get(0);
        String pred = aux.get(1);
        String object = aux.get(2);

        variableName = variableName.replace("“", "").replace("”", "");
        String maxFreqQueryString = "select (count(" + variableName
                + ") as ?count) where { " + subject + " " + pred + " " + object
                + " " + "} GROUP BY " + variableName + " " + "ORDER BY "
                + variableName + " DESC (?count) LIMIT 1 ";

        Query query = QueryFactory.create(maxFreqQueryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("count");
                int res = x.asLiteral().getInt();
                System.out.println("max freq value: " + res);
                return res;
            } else {
                return 0;
            }
        }
    }

    public static ResultSet excecuteQuery(Query query) {
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples)) {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            return results;
        }
    }

    public static int getTripSize(Query query) {
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples)) {
            Iterator<Triple> results = qexec.execConstructTriples();
            int resultSize = 0;
            while (results.hasNext()) {
                results.next();
                resultSize++;
            }
            return resultSize;
        }
    }

    public static int executeCountQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        ResultSet results = HdtDataSource.excecuteQuery(query);
        QuerySolution soln = results.nextSolution();
        RDFNode x = soln.get(soln.varNames().next());
        return x.asLiteral().getInt();
    }

}
