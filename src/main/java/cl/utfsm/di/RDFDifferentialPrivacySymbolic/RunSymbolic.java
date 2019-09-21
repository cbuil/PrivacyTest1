package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import static symjava.symbolic.Symbol.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

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

public class RunSymbolic {

    private static Logger logger = LogManager
            .getLogger(RunSymbolic.class.getName());

    // privacy budget
    private static double EPSILON = 0.1;

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException, ExecutionException {

        // create Options object
        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");
        options.addOption("o", "dir", true, "output file");
        options.addOption("v", "evaluation", true, "evaluation");
        String queryString = "";
        String queryFile = "";
        String queryDir = "";
        String dataFile = "";
        String outputFile = "";
        boolean evaluation = false;

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q")) {
                queryString = cmd.getOptionValue("q");
            } else {
                logger.info("Missing SPARQL query ");
            }
            if (cmd.hasOption("f")) {
                queryFile = cmd.getOptionValue("f");
                // queryString = new Scanner(new File(queryFile))
                // .useDelimiter("\\Z").next();
                // transform into Jena Query object
            } else {
                logger.info("Missing SPARQL query file");
            }
            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
            } else {
                logger.info("Missing data file");
            }
            if (cmd.hasOption("e")) {
                queryDir = cmd.getOptionValue("e");
            } else {
                logger.info("Missing query directory");
            }
            if (cmd.hasOption("o")) {
                outputFile = cmd.getOptionValue("o");
                if (!Files.exists(Paths.get(outputFile))) {
                    Files.createFile(Paths.get(outputFile));
                }
            } else {
                logger.info("Missing output file");
            }
            if (cmd.hasOption("v")) {
                evaluation = true;
            }
        } catch (ParseException e1) {
            e1.getMessage();
            System.exit(-1);
        }

        try {
            HdtDataSource hdtDataSource = new HdtDataSource(dataFile);

            Path queryLocation = Paths.get(queryFile);
            if (Files.isRegularFile(queryLocation)) {
                queryString = new Scanner(new File(queryFile))
                        .useDelimiter("\\Z").next();
                logger.info(queryString);
                runAnalysis(queryFile, queryString, hdtDataSource, outputFile, evaluation);
            } else if (Files.isDirectory(queryLocation)) {
                Iterator<Path> filesPath = Files.list(Paths.get(queryFile))
                        .filter(p -> p.toString().endsWith(".rq")).iterator();
                logger.info("Running analysis to DIRECTORY: "
                        + queryLocation);
                while (filesPath.hasNext()) {
                    Path nextQuery = filesPath.next();
                    logger.info("Running analysis to query: "
                            + nextQuery.toString());
                    queryString = new Scanner(nextQuery).useDelimiter("\\Z")
                            .next();
                    logger.info(queryString);
                    runAnalysis(nextQuery.toString(), queryString,
                            hdtDataSource, outputFile, evaluation);
                    logger.info("Cache stats: " + HdtDataSource.mostPopularValueCache.stats());
                }
            } else {
                if (Files.notExists(queryLocation)) {
                    throw new FileNotFoundException("No query file");
                }
            }
        } catch (IOException | CloneNotSupportedException e1) {
            System.out.println("Exception: " + e1.getMessage());
            System.exit(-1);
        }
    }

    private static void runAnalysis(String queryFile, String queryString,
                                    HdtDataSource hdtDataSource, String outpuFile, boolean evaluation)
            throws IOException, CloneNotSupportedException, ExecutionException {
        Query q = QueryFactory.create(queryString);

        String construct = queryString.replaceFirst("SELECT.*WHERE",
                "CONSTRUCT WHERE");
        Query constructQuery = QueryFactory.create(construct);
        int graphSize = HdtDataSource.graphSizeCache.get(constructQuery);
        // tripSize = 1000000000;
        // delta parameter: use 1/n^2, with n = size of the data in the
        // query
        double DELTA = 1 / (Math.pow(graphSize, 2));
        double beta = EPSILON / (2 * Math.log(2 / DELTA));

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        double smoothSensitivity = 0.0;
        Element element = elementList.get(0);
        boolean starQuery = false;
        if (element instanceof ElementPathBlock) {
            Expr elasticStability = Expr.valueOf(0);

            int k = 1;

            Map<String, List<TriplePath>> starQueriesMap = Helper
                    .getStarPatterns(q);

            if (Helper.isStarQuery(q)) {
                starQuery = true;
                elasticStability = x;
                double sensitivity = k;
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivityStar(elasticStability,
                                sensitivity, beta, k, graphSize);
                logger.info("star query (smooth) sensitivity: "
                        + smoothSensitivity);
            } else {
                elasticStability = GraphElasticSensitivity
                        .calculateElasticSensitivityAtK(k, starQueriesMap,
                                EPSILON, hdtDataSource);
                // elasticStability = GraphElasticSensitivity
                // .calculateElasticSensitivityAtK(k,
                // (ElementPathBlock) element, EPSILON);

                logger.info("Elastic Stability: " + elasticStability);
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivity(elasticStability, 0, beta,
                                k, graphSize);
                logger.info(
                        "Path Smooth Sensitivity: " + smoothSensitivity);
            }

            // add noise using Laplace Probability Density Function
//                2 * sensitivity / epsilon
            double scale = 2 * smoothSensitivity / EPSILON;
            SecureRandom random = new SecureRandom();

            int times = 1;
            if (evaluation) {
                times = 10;
            }
            for (int i = 0; i < times; i++) {
                double u = 0.5 - random.nextDouble();
//              val u = 0.5 - scala.util.Random.nextDouble()
                // LaplaceDistribution l = new LaplaceDistribution(u, scale);
                double noise = -Math.signum(u) * scale
                        * Math.log(1 - 2 * Math.abs(u));
//                -math.signum(u) * scale * math.log(1 - 2*math.abs(u))

                int countQueryResult = HdtDataSource
                        .executeCountQuery(queryString);

                double finalResult1 = countQueryResult + noise;
                // double finalResult2 = countQueryResult + l.sample();

                logger.info("Original result: " + countQueryResult);
                logger.info("Noise added: " + Math.round(noise));
                logger.info("Private Result: " + Math.round(finalResult1));

                StringBuffer csvLine = new StringBuffer();
                csvLine.append(queryFile);
                csvLine.append(",");
                csvLine.append(countQueryResult);
                csvLine.append(",");
                csvLine.append(finalResult1);
                csvLine.append(",");
                int queryTriples = ((ElementPathBlock) element).getPattern()
                        .size();
                csvLine.append(queryTriples);
                csvLine.append(",");
                if (starQuery) {
                    csvLine.append("starQuery");
                } else {
                    csvLine.append("NOstarQuery");
                }
                csvLine.append(",");
                csvLine.append(EPSILON);
                csvLine.append(",");
                csvLine.append(scale);
                csvLine.append(",");
                csvLine.append(elasticStability);
                csvLine.append(",");
                csvLine.append(k);
                csvLine.append(",");
                csvLine.append(graphSize);
                csvLine.append("\n");

                Files.write(Paths.get(outpuFile), csvLine.toString().getBytes(),
                        StandardOpenOption.APPEND);
            }

        }
    }

}
