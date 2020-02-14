package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import cl.utfsm.di.RDFDifferentialPrivacy.HdtDataSource;
import cl.utfsm.di.RDFDifferentialPrivacy.MaxFreqQuery;

import static org.junit.Assert.assertEquals;

public class HdtDataSourceTest
{

    private final Logger logger = LogManager
            .getLogger(HdtDataSourceTest.class.getName());

    private final String queryString = "SELECT (COUNT(?v0) as ?count) WHERE {\n"
            + "        ?v0 <http://purl.org/goodrelations/includes> ?v1 .      \n"
            + "        ?v0 <http://purl.org/goodrelations/price> ?v3 . \n"
            + "        ?v0 <http://purl.org/goodrelations/validThrough> ?v4 .  \n"
            + "        ?v1 <http://ogp.me/ns#title> ?v5 .      \n"
            + "        ?v1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?v6 . \n"
            + "        FILTER (?v3 > \"30\")\n" + "  }\n" + "";
    private final String endpoint = "http://localhost:3030/watdiv/sparql";
    private final String hdtFile = "resources/watdiv.100M.nt.hdt";
    private static HdtDataSource hdtDataSource;
    private MaxFreqQuery maxFreqQuery;
    private Query query;
    private Query constructQuery;

    /**
     * Creates a new HdtDataSource.
     *
     * @throws IOException if the file cannot be loaded
     */
    @Before
    public void ConfigureHdtDataSourceTest() throws IOException
    {
        hdtDataSource = new HdtDataSource(hdtFile);
        String tp = "        ?v0 <http://purl.org/goodrelations/includes> ?v1 .      \n"
                + "        ?v0 <http://purl.org/goodrelations/price> ?v3 . \n"
                + "        ?v0 <http://purl.org/goodrelations/validThrough> ?v4 .  \n"
                + "        ?v1 <http://ogp.me/ns#title> ?v5 .      \n"
                + "        ?v1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?v6 . \n";
        maxFreqQuery = new MaxFreqQuery(tp, "v0");
        query = QueryFactory.create(queryString);
        constructQuery = QueryFactory.create(
                queryString.replaceFirst("SELECT.*WHERE", "CONSTRUCT WHERE")
                        .replaceFirst("FILTER.*\\)", ""));
    }

    @Test
    public void excecuteQueryTest()
    {
        ResultSet rs = hdtDataSource.excecuteQuery(query);
        QuerySolution qs = rs.next();
        RDFNode sol = qs.get("?count");
        int results = sol.asLiteral().getInt();
        logger.info("excecuteQueryTest result test: " + results);
        assertEquals(279123, results);
    }

    @Test
    public void excecuteQueryEndpointTest()
    {
        ResultSet rs = hdtDataSource.excecuteQuery(query);
        QuerySolution qs = rs.next();
        RDFNode sol = qs.get("?count");
        int results = sol.asLiteral().getInt();
        logger.info("excecuteQueryEndpointTest result test: " + results);
        assertEquals(279123, results);
    }

    @Test
    public void getGraphSize()
    {
        long results = 0;
        Query q = QueryFactory.create(constructQuery);
// TODO Auto-generated catch block
//            e.printStackTrace();
        results = hdtDataSource.getGraphSize(q);
        logger.info("getGraphSize result test: " + results);
        assertEquals(1460762, results);
    }

    @Test
    public void getMostFrequentResult()
    {
        long results = 0;
        logger.info("getMostFrequentResult test query: "
                + maxFreqQuery.getQuery());
// TODO Auto-generated catch block
//            e.printStackTrace();
        results = hdtDataSource.mostFrequenResult(maxFreqQuery);
        logger.info("getMostFrequentResult result test: " + results);
        assertEquals(1, results);
    }

    @Test
    public void executeCountQuery()
    {
        int results = hdtDataSource.executeCountQuery(queryString);
        logger.info("executeCountQuery result test: " + results);
        assertEquals(279123, results);
    }

    @Test
    public void executeCountQueryEndpoint()
    {
        int results = hdtDataSource.executeCountQuery(queryString);
        assertEquals(279123, results);
    }

    @Test
    public void getGraphSizeTriples()
    {
        List<List<String>> triplePatternsCount = new ArrayList<>();
        List<String> triplePatterns = new ArrayList<>();
        triplePatterns.add("?v0 <http://purl.org/goodrelations/includes> ?v1 ");
        triplePatterns.add("?v0 <http://purl.org/goodrelations/price> ?v3 ");
        triplePatterns.add("?v0 <http://purl.org/goodrelations/validThrough> ?v4 ");
        triplePatterns.add("?v1 <http://ogp.me/ns#title> ?v5 ");
        triplePatterns.add("?v1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?v6 ");
        triplePatternsCount.add(triplePatterns);
        long results = hdtDataSource.getGraphSizeTriples(triplePatternsCount);
        logger.info("getGraphSizeTriples result test: " + results);
        assertEquals(359672, results);
    }
}
