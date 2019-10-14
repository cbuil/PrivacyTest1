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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.tdb.store.Hash;

import symjava.symbolic.Expr;

//Import log4j classes.
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunSymbolic
{

    private static Logger logger = LogManager
            .getLogger(RunSymbolic.class.getName());

    // privacy budget
    private static double EPSILON = 0.1;

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException, ExecutionException
    {

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
        String endpoint = "";
        boolean evaluation = false;

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
                // queryString = new Scanner(new File(queryFile))
                // .useDelimiter("\\Z").next();
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
                endpoint = cmd.getOptionValue("e");
            }
            else
            {
                logger.info("Missing endpoint address");
            }
            if (cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
                if (!Files.exists(Paths.get(outputFile)))
                {
                    Files.createFile(Paths.get(outputFile));
                }
            }
            else
            {
                logger.info("Missing output file");
            }
            if (cmd.hasOption("v"))
            {
                evaluation = true;
            }
        }
        catch (ParseException e1)
        {
            e1.getMessage();
            System.exit(-1);
        }

        try
        {
            HdtDataSource hdtDataSource = new HdtDataSource(dataFile);

            Path queryLocation = Paths.get(queryFile);
            if (Files.isRegularFile(queryLocation))
            {
                queryString = new Scanner(new File(queryFile))
                        .useDelimiter("\\Z").next();
                logger.info(queryString);
                runAnalysis(queryFile, queryString, hdtDataSource, outputFile,
                        evaluation, endpoint);
            }
            else if (Files.isDirectory(queryLocation))
            {
                Iterator<Path> filesPath = Files.list(Paths.get(queryFile))
                        .filter(p -> p.toString().endsWith(".rq")).iterator();
                logger.info("Running analysis to DIRECTORY: " + queryLocation);
                while (filesPath.hasNext())
                {
                    Path nextQuery = filesPath.next();
                    logger.info("Running analysis to query: "
                            + nextQuery.toString());
                    queryString = new Scanner(nextQuery).useDelimiter("\\Z")
                            .next();
                    logger.info(queryString);
                    runAnalysis(nextQuery.toString(), queryString,
                            hdtDataSource, outputFile, evaluation, endpoint);
                    logger.info("Cache stats: "
                            + HdtDataSource.mostFrequenResultCache.stats());
                }
            }
            else
            {
                if (Files.notExists(queryLocation))
                {
                    throw new FileNotFoundException("No query file");
                }
            }
        }
        catch (IOException | CloneNotSupportedException e1)
        {
            System.out.println("Exception: " + e1.getMessage());
            System.exit(-1);
        }
    }

    private static void runAnalysis(String queryFile, String queryString,
            HdtDataSource hdtDataSource, String outpuFile, boolean evaluation,
            String endpoint)
            throws IOException, CloneNotSupportedException, ExecutionException
    {
        int countQueryResult = HdtDataSource.executeCountQuery(queryString,
                endpoint);
        Query q = QueryFactory.create(queryString);

        List<List<String>> triplePatterns = new ArrayList();

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        Sensitivity smoothSensitivity = null;
        Element element = elementList.get(0);
        boolean starQuery = false;
        if (element instanceof ElementPathBlock)
        {
            Expr elasticStability = Expr.valueOf(0);

            int k = 1;

            Map<String, List<TriplePath>> starQueriesMap = Helper
                    .getStarPatterns(q);
            Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
            Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();
            for (String key : starQueriesMap.keySet())
            {
                List<String> listTriple = new ArrayList();
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
                    }
                    else
                    {
                        triple += " ?s" + i + " ";
                    }
                    triple += "<" + triplePath.getPredicate().getURI() + "> ";
                    if (triplePath.getObject().isVariable())
                    {
                        varStrings.add(triplePath.getObject().getName());
                        triple += "?" + triplePath.getObject().getName();
                    }
                    else
                    {
                        triple += " ?o" + i + " ";
                    }
                    i++;
                    listTriple.add(triple);
                }

                triplePatterns.add(listTriple);

                Set<String> listWithoutDuplicates = new LinkedHashSet<String>(
                        varStrings);
                varStrings.clear();

                varStrings.addAll(listWithoutDuplicates);

                for (String var : varStrings)
                {
                    MaxFreqQuery query = new MaxFreqQuery(
                            Helper.getStarQueryString(starQueryLeft), var);
                    if (mapMostFreqValue.containsKey(var))
                    {
                        List<Integer> mostFreqValues = mapMostFreqValue
                                .get(var);
                        List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar
                                .get(var);
                        if (!mostFreqValues.isEmpty())
                        {
                            mostFreqValues
                                    .add(HdtDataSource.mostFrequenResultCache
                                            .get(query));
                            mapMostFreqValue.put(var, mostFreqValues);

                            mostFreqValuesStar
                                    .add(new StarQuery(starQueryLeft));
                            mapMostFreqValueStar.put(var, mostFreqValuesStar);
                        }
                    }
                    else
                    {
                        List<Integer> mostFreqValues = new ArrayList<>();
                        mostFreqValues.add(HdtDataSource.mostFrequenResultCache
                                .get(query));
                        mapMostFreqValue.put(var, mostFreqValues);
                        List<StarQuery> mostFreqValuesStar = new ArrayList<>();
                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }
                }
            }
            logger.info(triplePatterns);
            Map<String, Integer> maxFreqMap = new HashMap<>();
            // for (String key : starQueriesMap.keySet())
            // {
            // int maxFreq = 0;
            // maxFreq = HdtDataSource.mostFrequenResultCache
            // .get(new MaxFreqQuery(Helper.getStarQueryString(
            // starQueriesMap.get(key)), key));
            // maxFreqMap.put(key, maxFreq);
            // }

            String construct = queryString.replaceFirst("SELECT.*WHERE",
                    "CONSTRUCT WHERE");
            logger.info("graph query: " + construct);
            Query constructQuery = QueryFactory.create(construct);
            long graphSize = HdtDataSource.graphSizeCache.get(constructQuery);

            graphSize = HdtDataSource.getGraphSizeTriples(triplePatterns,
                    endpoint);
            logger.info("graph size " + graphSize);
            // delta parameter: use 1/n^2, with n = size of the data in the
            // query
            double DELTA = 1 / (Math.pow(graphSize, 2));
            double beta = EPSILON / (2 * Math.log(2 / DELTA));
            if (Helper.isStarQuery(q))
            {
                starQuery = true;
                elasticStability = x;
                Sensitivity sensitivity = new Sensitivity(1.0,
                        elasticStability);
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivityStar(elasticStability,
                                sensitivity, beta, k, graphSize);
                logger.info("star query (smooth) sensitivity: "
                        + smoothSensitivity);
                // TriplePath tripleInQuery = ((ElementGroup)
                // q.getQueryPattern()).getElements().get(0);
                // MaxFreqQuery mfQuery = new MaxFreqQuery(, );
                // int keySize =
                // HdtDataSource.mostPopularValueCache.get(mfQuery);
            }
            else
            {
                elasticStability = GraphElasticSensitivity
                        .calculateElasticSensitivityAtK(k, starQueriesMap,
                                EPSILON, hdtDataSource, mapMostFreqValue,
                                mapMostFreqValueStar);
                // elasticStability = GraphElasticSensitivity
                // .calculateElasticSensitivityAtK(k,
                // (ElementPathBlock) element, EPSILON);

                logger.info("Elastic Stability: " + elasticStability);
                smoothSensitivity = GraphElasticSensitivity
                        .smoothElasticSensitivity(elasticStability, 0, beta, k,
                                graphSize);
                logger.info("Path Smooth Sensitivity: "
                        + smoothSensitivity.getSensitivity());
            }

            // add noise using Laplace Probability Density Function
            // 2 * sensitivity / epsilon
            double scale = 2 * smoothSensitivity.getSensitivity() / EPSILON;
            SecureRandom random = new SecureRandom();

            int times = 1;
            if (evaluation)
            {
                times = 100;
            }

            List<Double> privateResultList = new ArrayList<>();
            List<Integer> resultList = new ArrayList<>();
            for (int i = 0; i < times; i++)
            {
                double u = 0.5 - random.nextDouble();
                // val u = 0.5 - scala.util.Random.nextDouble()
                // LaplaceDistribution l = new LaplaceDistribution(u, scale);
                double noise = -Math.signum(u) * scale
                        * Math.log(1 - 2 * Math.abs(u));
                logger.info("Math.log(1 - 2 * Math.abs(u)) "
                        + Math.log(1 - 2 * Math.abs(u)));
                // -math.signum(u) * scale * math.log(1 - 2*math.abs(u))

                double finalResult1 = countQueryResult + noise;
                // double finalResult2 = countQueryResult + l.sample();

                logger.info("Original result: " + countQueryResult);
                logger.info("Noise added: " + Math.round(noise));
                logger.info("Private Result: " + Math.round(finalResult1));
                privateResultList.add(finalResult1);
                resultList.add(countQueryResult);
            }
            Result result = new Result(queryFile, EPSILON, privateResultList,
                    smoothSensitivity.getSensitivity(), resultList,
                    smoothSensitivity.getMaxK(), scale, elasticStability,
                    graphSize, starQuery, mapMostFreqValue,
                    mapMostFreqValueStar);

            StringBuffer csvLine = new StringBuffer();
            csvLine.append(result.toString().replace('\n', ' '));
            csvLine.append("\n");

            Files.write(Paths.get(outpuFile), csvLine.toString().getBytes(),
                    StandardOpenOption.APPEND);

        }
    }

}
