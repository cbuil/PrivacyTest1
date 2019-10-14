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
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

public class HdtDataSource
{

    private static Logger logger = LogManager
            .getLogger(HdtDataSource.class.getName());

    private static HDT datasource;
    private static NodeDictionary dictionary;
    private static HDTGraph graph;
    private static Model triples;
    public static LoadingCache<MaxFreqQuery, Integer> mostFrequenResultCache;
    public static LoadingCache<Query, Long> graphSizeCache;

    /**
     * Creates a new HdtDataSource.
     *
     * @param hdtFile
     *            the HDT datafile
     * @throws IOException
     *             if the file cannot be loaded
     */
    public HdtDataSource(String hdtFile) throws IOException
    {
        datasource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
        graph = new HDTGraph(datasource);
        triples = ModelFactory.createModelForGraph(graph);
        mostFrequenResultCache = CacheBuilder.newBuilder().recordStats()
                .maximumWeight(100000)
                .weigher(new Weigher<MaxFreqQuery, Integer>()
                {
                    public int weigh(MaxFreqQuery k, Integer resultSize)
                    {
                        return k.getQuerySize();
                    }
                }).build(new CacheLoader<MaxFreqQuery, Integer>()
                {
                    @Override
                    public Integer load(MaxFreqQuery s) throws Exception
                    {
                        logger.debug(
                                "into mostPopularValueCache CacheLoader, loading: "
                                        + s.toString());
                        return getMostFrequentResult(s.getQuery(),
                                s.getVariableString());
                    }
                });
        graphSizeCache = CacheBuilder.newBuilder().recordStats()
                .maximumWeight(1000).weigher(new Weigher<Query, Long>()
                {
                    public int weigh(Query k, Long resultSize)
                    {
                        return k.toString().length();
                    }
                }).build(new CacheLoader<Query, Long>()
                {
                    @Override
                    public Long load(Query q) throws Exception
                    {
                        logger.debug(
                                "into graphSizeCache CacheLoader, loading: "
                                        + q.toString());
                        return getGraphSize(q);
                    }
                });
    }

    private static int getMostFrequentResult(String starQuery,
            String variableName)
    {

        variableName = variableName.replace("“", "").replace("”", "");
        String maxFreqQueryString = "select (count(?" + variableName
                + ") as ?count) where { " + starQuery + " " + "} GROUP BY ?"
                + variableName + " " + "ORDER BY ?" + variableName
                + " DESC (?count) LIMIT 1 ";

        Query query = QueryFactory.create(maxFreqQueryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            ResultSet results = qexec.execSelect();
            if (results.hasNext())
            {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("count");
                int res = x.asLiteral().getInt();
                logger.info("max freq value: " + res + " for variable "
                        + variableName);
                return res;
            }
            else
            {
                return 0;
            }
        }
    }

    public static ResultSet excecuteQuery(Query query)
    {
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
            return results;
        }
    }

    public static ResultSet excecuteQuery(Query query, String endpoint)
    {
        try (QueryExecution qexec = (QueryEngineHTTP) QueryExecutionFactory
                .sparqlService(endpoint, query))
        {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
            return results;
        }
    }

    private static long getGraphSize(Query query)
    {
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            Model results = qexec.execConstruct();
            long resultSize = results.size();
            qexec.close();
            return resultSize;
        }
    }

    public static int executeCountQuery(String queryString)
    {
        Query query = QueryFactory.create(queryString);
        ResultSet results = HdtDataSource.excecuteQuery(query);
        QuerySolution soln = results.nextSolution();
        RDFNode x = soln.get(soln.varNames().next());
        return x.asLiteral().getInt();
    }

    public static int executeCountQuery(String queryString, String endpoint)
    {

        Query query = QueryFactory.create(queryString);
        logger.info("count query: " + queryString);
        logger.info("query endpoint: " + endpoint);
        QueryExecution qexec = (QueryEngineHTTP) QueryExecutionFactory
                .sparqlService(endpoint, query);
        ResultSet results = qexec.execSelect();
        QuerySolution soln = results.nextSolution();
        logger.info("count query executed... ");
        qexec.close();
        RDFNode x = soln.get(soln.varNames().next());
        return x.asLiteral().getInt();
    }

    public static long getGraphSizeTriples(
            List<List<String>> triplePatternsCount, String endpoint)
    {
        long count = 0;
        for (List<String> star : triplePatternsCount)
        {
            String construct = "";
            for (String tp : star)
            {
                construct += tp + " . ";
            }
            logger.info("construct query for graph size so far: " + construct);
            count += executeCountQuery("SELECT (COUNT(*) as ?count) WHERE {" + construct + "}", endpoint);
            logger.info("graph size so far: " + count);
        }
        return count;
    }
}
