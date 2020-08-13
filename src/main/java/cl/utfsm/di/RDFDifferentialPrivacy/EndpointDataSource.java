/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.utfsm.di.RDFDifferentialPrivacy;

import cl.utfsm.di.RDFDifferentialPrivacy.utils.Helper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * @author cbuil
 */
public class EndpointDataSource implements DataSource {

    private static final Logger logger = LogManager
            .getLogger(HdtDataSource.class.getName());

    private final String datasource;

    private final LoadingCache<MaxFreqQuery, Integer> mostFrequenResultCache;

    private final LoadingCache<Query, Long> graphSizeCache;

    private final static Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();

    private final static Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

    public EndpointDataSource(String endpoint) {
        datasource = endpoint;
        mostFrequenResultCache = CacheBuilder.newBuilder().recordStats()
                .maximumWeight(100000)
                .weigher(new Weigher<MaxFreqQuery, Integer>() {
                    public int weigh(MaxFreqQuery k, Integer resultSize) {
                        return k.getQuerySize();
                    }
                }).build(new CacheLoader<MaxFreqQuery, Integer>() {
                    @Override
                    public Integer load(MaxFreqQuery s) throws Exception {
                        logger.debug(
                                "into mostPopularValueCache CacheLoader, loading: "
                                        + s.toString());
                        return getMostFrequentResult(s.getQuery(),
                                s.getVariableString());
                    }
                });
        graphSizeCache = CacheBuilder.newBuilder().recordStats()
                .maximumWeight(1000).weigher(new Weigher<Query, Long>() {
                    public int weigh(Query k, Long resultSize) {
                        return k.toString().length();
                    }
                }).build(new CacheLoader<Query, Long>() {
                    @Override
                    public Long load(Query q) {
                        logger.debug(
                                "into graphSizeCache CacheLoader, loading: "
                                        + q.toString());
                        return getGraphSize(q);
                    }
                });
    }

    @Override
    public ResultSet excecuteQuery(Query query) {
        try (QueryExecution qexec = QueryExecutionFactory
                .sparqlService(datasource, query)) {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
            return results;
        }
    }

    @Override
    public long getGraphSize(Query query) {
        try (QueryExecution qexec = QueryExecutionFactory
                .sparqlService(datasource, query)) {
            Model results = qexec.execConstruct();
            long resultSize = results.size();
            qexec.close();
            return resultSize;
        }
    }

    @Override
    public int executeCountQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        logger.info("count query: " + queryString);
        logger.info("query endpoint: " + datasource);
        if (queryString.contains("http://www.wikidata.org/prop/direct/P31") && (queryString.lastIndexOf('?') != queryString.indexOf('?'))) {
            return 85869721;
        }
        QueryExecution qexec = QueryExecutionFactory
                .sparqlService(datasource, query);
        ResultSet results = qexec.execSelect();
        QuerySolution soln = results.nextSolution();
        logger.info("count query executed... ");
        qexec.close();
        RDFNode x = soln.get(soln.varNames().next());
        int countResult = x.asLiteral().getInt();
        logger.info("count query result (endpoint): " + countResult);
        return countResult;
    }

    @Override
    public long getGraphSizeTriples(List<List<String>> triplePatternsCount)
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
            count += executeCountQuery(
                    "SELECT (COUNT(*) as ?count) WHERE { " + construct + "} ");
            logger.info("graph size so far: " + count);
        }
        return count;
    }

    @Override
    public int mostFrequenResult(MaxFreqQuery maxFreqQuery)
    {
        try
        {
            return this.mostFrequenResultCache
                    .get(maxFreqQuery);
        } catch (ExecutionException ex)
        {
            java.util.logging.Logger.getLogger(HdtDataSource.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    @Override
    public void setMostFreqValueMaps(Map<String, List<TriplePath>> starQueriesMap, List<List<String>> triplePatterns) throws ExecutionException
    {
        Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
        Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();
        for (String key : starQueriesMap.keySet())
        {
            List<String> listTriple = new ArrayList<>();
            List<TriplePath> starQueryLeft = starQueriesMap.get(key);
            List<String> varStrings = new ArrayList<>();
            int i = 0;
            for (TriplePath triplePath : starQueryLeft)
            {
                String triple = "";
                if (triplePath.getSubject().isVariable())
                {
                    varStrings.add(triplePath.getSubject().getName());
                    triple += "?" + triplePath.getSubject().getName();
                } else
                {
                    triple += " ?s" + i + " ";
                }
                triple += "<" + triplePath.getPredicate().getURI() + "> ";
                if (triplePath.getObject().isVariable())
                {
                    varStrings.add(triplePath.getObject().getName());
                    triple += "?" + triplePath.getObject().getName();
                } else
                {
                    triple += " ?o" + i + " ";
                }
                i++;
                listTriple.add(triple);
            }

            triplePatterns.add(listTriple);

            Set<String> listWithoutDuplicates = new LinkedHashSet<>(
                    varStrings);
            varStrings.clear();

            varStrings.addAll(listWithoutDuplicates);

            for (String var : varStrings)
            {
                MaxFreqQuery query = new MaxFreqQuery(
                        Helper.getStarQueryString(starQueryLeft), var);
                if (mapMostFreqValue.containsKey(var))
                {
                    List<Integer> mostFreqValues = mapMostFreqValue.get(var);
                    List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar
                            .get(var);
                    if (!mostFreqValues.isEmpty())
                    {
                        mostFreqValues.add(this.mostFrequenResultCache
                                .get(query));
                        mapMostFreqValue.put(var, mostFreqValues);

                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }
                } else
                {
                    List<Integer> mostFreqValues = new ArrayList<>();
                    logger.info("query: " + query.getQuery());
                    logger.info("variable: " + query.getVariableString());
                    mostFreqValues.add(
                            this.mostFrequenResultCache.get(query));
                    mapMostFreqValue.put(var, mostFreqValues);
                    List<StarQuery> mostFreqValuesStar = new ArrayList<>();
                    mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                    mapMostFreqValueStar.put(var, mostFreqValuesStar);
                }
            }
        }
    }

    @Override
    public Map<String, List<StarQuery>> getMapMostFreqValueStar()
    {
        return mapMostFreqValueStar;
    }

    @Override
    public Map<String, List<Integer>> getMapMostFreqValue()
    {
        return mapMostFreqValue;
    }

    private int getMostFrequentResult(String starQuery,
            String variableName) {

        variableName = variableName.replace("“", "").replace("”", "");
        String maxFreqQueryString = "select (count(?" + variableName
                + ") as ?count) where { select ?" + variableName + " WHERE { " + starQuery + "} LIMIT 1000000 " + "} GROUP BY ?"
                + variableName + " " + "ORDER BY ?" + variableName
                + " DESC (?count) LIMIT 1 ";

        logger.info("query at getMostFrequentResult: " + maxFreqQueryString);
        Query query = QueryFactory.create(maxFreqQueryString);
        try (QueryExecution qexec = QueryExecutionFactory
                .sparqlService(datasource, query)) {
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

}
