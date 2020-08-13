package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import cl.utfsm.di.RDFDifferentialPrivacy.GraphElasticSensitivity;
import cl.utfsm.di.RDFDifferentialPrivacy.HdtDataSource;
import cl.utfsm.di.RDFDifferentialPrivacy.Sensitivity;
import cl.utfsm.di.RDFDifferentialPrivacy.StarQuery;
import cl.utfsm.di.RDFDifferentialPrivacy.utils.Helper;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import symjava.symbolic.Expr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static symjava.symbolic.Symbol.x;

public class GraphElasticSensitivityTest {
    private static final Logger logger = LogManager
            .getLogger(GraphElasticSensitivityTest.class.getName());

    private static HdtDataSource hdtDataSource;
    private static int graphSize;
    private static int beta;
    private static int k;
    private static double EPSILON;

    @Before
    public void initialize()
    {
        String hdtFile = "resources/watdiv.100M.nt.hdt";
        try
        {
            hdtDataSource = new HdtDataSource(hdtFile);
            logger.debug("loaded data file");
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void smoothElasticSensitivityTest()
    {
        Sensitivity smoothSensitivity;
        Expr elasticStability = Expr.valueOf(0);
        smoothSensitivity = GraphElasticSensitivity.smoothElasticSensitivity(
                elasticStability, 0, beta, k, graphSize);
        assertEquals(0, smoothSensitivity.getMaxK());
        assertEquals(Expr.valueOf(0), smoothSensitivity.getS());
//        assertEquals(0.0, smoothSensitivity.getSensitivity());
    }

    @Test
    public void smoothElasticSensitivityStarTest()
    {
        Sensitivity smoothSensitivity;
        Expr elasticStability = x;
        Sensitivity sensitivity = new Sensitivity(1.0, elasticStability);
        smoothSensitivity = GraphElasticSensitivity
                .smoothElasticSensitivityStar(elasticStability, sensitivity,
                        beta, k, graphSize);
        logger.info("smoothElasticSensitivityStarTest results: " + smoothSensitivity.getMaxK());
        assertEquals(smoothSensitivity.getMaxK(), 0);
    }

    @Test
    public void calculateElasticSensitivityAtKTest()
    {
        String queryString;
        
        queryString = "SELECT (COUNT(?v0) as ?count) WHERE {\n" +
                "        <http://db.uwaterloo.ca/~galuc/wsdbm/Retailer9247> <http://purl.org/goodrelations/offers> ?v0 . \n" +
                "  	?v0 <http://purl.org/goodrelations/includes> ?v1 . 		\n" +
                "  	?v0 <http://purl.org/goodrelations/price> ?v3 . 	\n" +
                "  	?v0 <http://purl.org/goodrelations/validThrough> ?v4 . 	\n" +
                "  	?v1 <http://ogp.me/ns#title> ?v5 . 	\n" +
                "  	?v1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?v6 . \n" +
                "  }\n" +
                "";

//        Triple triple = new Triple(NodeFactory.createVariable("v0"), NodeFactory.createURI("<http://purl.org/goodrelations/includes>"), NodeFactory.createVariable("v1"));
//        TriplePath tp = new TriplePath(triple);
        Query q = QueryFactory.create(queryString);
        Map<String, List<TriplePath>> starQueriesMap = Helper
                .getStarPatterns(q);
        List<StarQuery> listStars = new ArrayList<>();
        for (List<TriplePath> tp : starQueriesMap.values()) {
            listStars.add(new StarQuery(tp));
        }
        StarQuery elasticStability = null;
        try {
            elasticStability = GraphElasticSensitivity
                    .calculateSensitivity(k, listStars, EPSILON,
                            hdtDataSource);
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertTrue(elasticStability != null);

    }

}
