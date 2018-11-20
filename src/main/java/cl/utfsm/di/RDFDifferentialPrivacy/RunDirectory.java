package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import cl.utfsm.di.RDFDifferentialPrivacy.symbolic.SmoothResult;
import symjava.symbolic.Expr;

public class RunDirectory
{
    static HashMap<TriplePath, Integer> countTriplesCache;

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException
    {
        countTriplesCache = new HashMap<TriplePath, Integer>();
        // create Options object
        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");
        options.addOption("o", "output", true, "output file");

        if (!Files.exists(Paths.get("results_dir.csv")))
        {
            Files.createFile(Paths.get("results_dir.csv"));
        }

        CommandLineParser parser = new DefaultParser();
        try
        {
            String queryString = "";
            String queryFile = "";
            String dataFile = "";
            String outpuFile = "";

            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q"))
            {
                queryString = cmd.getOptionValue("q");
            }
            else
            {
                System.out.println("Missing SPARQL query ");
            }
            if (cmd.hasOption("d"))
            {
                dataFile = cmd.getOptionValue("d");
            }
            else
            {
                System.out.println("Missing data file");
            }
            if (cmd.hasOption("o"))
            {
                outpuFile = cmd.getOptionValue("o");
                if (!Files.exists(Paths.get(outpuFile)))
                {
                    Files.createFile(Paths.get(outpuFile));
                }
            }
            else
            {
                System.out.println("Missing output file");
            }

            if (cmd.hasOption("f"))
            {
                queryFile = cmd.getOptionValue("f");
                Path queryLocation = Paths.get(queryFile);
                if (Files.isRegularFile(queryLocation))
                {
                    queryString = new Scanner(new File(queryFile))
                            .useDelimiter("\\Z").next();
                }
                else if (Files.isDirectory(queryLocation))
                {
                    Iterator<Path> filesPath = Files.list(Paths.get(queryFile))
                            .filter(p -> p.toString().endsWith(".rq"))
                            .iterator();
                    while (filesPath.hasNext())
                    {
                        queryString = new Scanner(filesPath.next())
                                .useDelimiter("\\Z").next();
                        System.out.println(queryString);
                        runAnalysis(queryString, dataFile, outpuFile);
                    }
                }
                // transform into Jena Query object
            }
            else
            {
                System.out.println("Missing SPARQL query file");
            }

        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private static void runAnalysis(String queryString, String dataFile,
            String outpuFile) throws CloneNotSupportedException, IOException
    {
        int queryTriples = 0;
        HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
        Query q = QueryFactory.create(queryString);
        // String construct = queryString.replaceFirst("SELECT.*WHERE",
        // "CONSTRUCT WHERE");
        // Query constructQuery = QueryFactory.create(construct);
        // double tripSize = HdtDataSource.getTripSize(constructQuery);
        double tripSize = 0.0;

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        for (Element element : elementList)
        {
            if (element instanceof ElementPathBlock)
            {
                ElementPathBlock bgpBlock = (ElementPathBlock) element;
                PathBlock pb = bgpBlock.getPattern();
                Iterator bgpIt = pb.getList().iterator();
                while (bgpIt.hasNext())
                {
                    TriplePath triple = (TriplePath) bgpIt.next();
                    if (countTriplesCache.containsKey(triple))
                    {
                        tripSize += countTriplesCache.get(triple);
                    }
                    else
                    {
                        tripSize += HdtDataSource.getCountQuery(triple);
                    }
                }
                System.out.println("trip size = " + tripSize);
                // distance
                int k = 1;
                queryTriples += ((ElementPathBlock) element).getPattern()
                        .size();
                double DELTA = 1 / (Math.pow(tripSize, 2));

                // privacy budget
                double EPSILON = 0.1;

                Expr elasticStability = GraphElasticSensitivity
                        .calculateElasticSensitivityAtK(
                                (ElementPathBlock) element, EPSILON, bgpBlock);

                System.out.println("Elastic Stability: " + elasticStability);
                double beta = EPSILON / (2 * Math.log(2 / DELTA));
                // double smoothSensitivity = Math.exp(-1 * beta * k) *
                // elasticStability;

                // Func f = new Func("f", elasticStability);
                // BytecodeFunc func = f.toBytecodeFunc();
                // System.out.println(
                // "Elastic Stability: " + Math.round(func.apply(2)));

                SmoothResult smoothResult = GraphElasticSensitivity
                        .smoothElasticSensitivity(elasticStability, 0, beta, 0,
                                bgpBlock);
                double smoothSensitivity = smoothResult.getSensitivity();
                // System.out.println(
                // "Smooth Sensitivity: " + smoothSensitivity);

                // double beta = EPSILON / (2 * Math.log(2 / DELTA));
                // double smoothSensitivity = Math.exp(-1 * beta * k)
                // * elasticStability;

                // Se agrega el ruido con Laplace
                double scale = 2 * smoothSensitivity / EPSILON;
                SecureRandom random = new SecureRandom();
                double u = 0.5 - random.nextDouble();
                LaplaceDistribution l = new LaplaceDistribution(u, scale);

                Query query = QueryFactory.create(queryString);
                ResultSet results = HdtDataSource.ExcecuteQuery(query);
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get(soln.varNames().next());
                int result = x.asLiteral().getInt();

                double finalResult2 = result + l.sample();
                StringBuffer csvLine = new StringBuffer();
                csvLine.append(queryString);
                csvLine.append(",");
                csvLine.append(result);
                csvLine.append(",");
                csvLine.append(finalResult2);
                csvLine.append(",");
                csvLine.append(queryTriples);
                csvLine.append(",");
                csvLine.append(EPSILON);
                csvLine.append(",");
                csvLine.append(scale);
                csvLine.append(",");
                csvLine.append(elasticStability);
                csvLine.append(",");
                csvLine.append(k);
                csvLine.append(",");
                csvLine.append(tripSize);
                csvLine.append("\n");

                Files.write(Paths.get(outpuFile),
                        csvLine.toString().getBytes(),
                        StandardOpenOption.APPEND);

                System.out.println("Original result: " + result);
                System.out
                        .println("Private Result: " + Math.round(finalResult2));

            }
        }
    }

    private static double laplace(double scale)
    {
        Random random = new Random();
        double u = 0.5 - random.nextDouble();
        return -Math.signum(u) * scale * Math.log(1 - 2 * Math.abs(u));
    }

}
