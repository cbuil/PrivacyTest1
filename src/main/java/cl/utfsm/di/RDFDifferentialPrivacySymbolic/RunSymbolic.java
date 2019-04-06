package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import static symjava.symbolic.Symbol.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import symjava.symbolic.Expr;

//Import log4j classes.
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunSymbolic
{

    private static Logger logger = LogManager
            .getLogger(RunSymbolic.class.getName());

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException
    {

        // privacy budget
        double EPSILON = 0.1;

        // create Options object
        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");
        String queryString = "";
        String queryFile = "";
        String queryDir = "";
        String dataFile = "";

        CommandLineParser parser = new DefaultParser();
        try
        {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q"))
            {
                queryString = cmd.getOptionValue("q");
            }
            else
            {
                logger.info("Missing SPARQL query ");
            }
            if (cmd.hasOption("f"))
            {
                queryFile = cmd.getOptionValue("f");
                queryString = new Scanner(new File(queryFile))
                        .useDelimiter("\\Z").next();
                // transform into Jena Query object
            }
            else
            {
                logger.info("Missing SPARQL query file");
            }
            if (cmd.hasOption("d"))
            {
                dataFile = cmd.getOptionValue("d");
            }
            else
            {
                logger.info("Missing data file");
            }
            if (cmd.hasOption("e"))
            {
                queryDir = cmd.getOptionValue("e");
            }
            else
            {
                logger.info("Missing query directory");
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
        Query q = QueryFactory.create(queryString);

        String construct = queryString.replaceFirst("SELECT.*WHERE",
                "CONSTRUCT WHERE");
        Query constructQuery = QueryFactory.create(construct);
        double tripSize = HdtDataSource.getTripSize(constructQuery);
        // tripSize = 1000000000;
        // delta parameter: use 1/n^2, with n = size of the data in the query
        double DELTA = 1 / (Math.pow(tripSize, 2));
        double beta = EPSILON / (2 * Math.log(2 / DELTA));

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        double smoothSensitivity = 0.0;
        Element element = elementList.get(0);
        if (element instanceof ElementPathBlock)
        {
            Expr elasticStability = Expr.valueOf(0);

            int k = 1;

            Map<String, List<TriplePath>> starQueriesMap = Helper
                    .getStarPatterns(q);

            if (Helper.isStarQuery(q))
            {
                elasticStability = x;
                double sensitivity = k;
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivity(elasticStability, sensitivity,
                                beta, k);
                logger.info("star query (smooth) sensitivity: "
                        + smoothSensitivity);
            }
            else
            {
                elasticStability = GraphElasticSensitivity
                        .calculateElasticSensitivityAtK(k, starQueriesMap,
                                EPSILON, hdtDataSource);
                // elasticStability = GraphElasticSensitivity
                // .calculateElasticSensitivityAtK(k,
                // (ElementPathBlock) element, EPSILON);

                logger.info("Elastic Stability: " + elasticStability);
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivity(elasticStability, 0, beta, 0);
                logger.info("Path Smooth Sensitivity: " + smoothSensitivity);
            }

            // add noise using Laplace Probability Density Function
            double scale = 2 * smoothSensitivity / EPSILON;
            Random random = new Random();
            double u = 0.5 - random.nextDouble();
            // LaplaceDistribution l = new LaplaceDistribution(u, scale);
            double noise = -Math.signum(u) * scale
                    * Math.log(1 - 2 * Math.abs(u));

            int countQueryResult = HdtDataSource.executeCountQuery(queryString);

            double finalResult1 = countQueryResult + noise;
            // double finalResult2 = countQueryResult + l.sample();

            logger.info("Original result: " + countQueryResult);
            logger.info("Noise added: " + Math.round(noise));
            logger.info("Private Result: " + Math.round(finalResult1));

        }
    }

}
