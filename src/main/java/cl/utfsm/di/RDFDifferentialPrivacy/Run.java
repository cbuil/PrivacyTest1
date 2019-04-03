package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

public class Run
{
    public static void main(String[] args)
            throws IOException, CloneNotSupportedException
    {
        // create Options object
        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");

        CommandLineParser parser = new DefaultParser();
        try
        {
            String queryString = "";
            String queryFile = "";
            String dataFile = "";

            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q"))
            {
                queryString = cmd.getOptionValue("q");
            }
            else
            {
                System.out.println("Missing SPARQL query ");
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
                System.out.println("Missing SPARQL query file");
            }
            if (cmd.hasOption("d"))
            {
                dataFile = cmd.getOptionValue("d");
            }
            else
            {
                System.out.println("Missing data file");
            }

            int queryTriples = 0;
            HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
            Query q = QueryFactory.create(queryString);
            String construct = queryString.replaceFirst("SELECT.*WHERE",
                    "CONSTRUCT WHERE");

            Query constructQuery = QueryFactory.create(construct);
            double trip_size = 1;
            trip_size = HdtDataSource.getTripSize(constructQuery);

            ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
            List<Element> elementList = queryPattern.getElements();
            for (Element element : elementList)
            {
                if (element instanceof ElementPathBlock)
                {
                    // distance
                    int k = 1;
                    queryTriples++;
                    double DELTA = 1 / (Math.pow(trip_size, 2));

                    // privacy budget
                    double EPSILON = 0.1;

                    if (Helper.isStarQuery(q))
                    {
                        Expr elasticStability = Expr.valueOf(0);
                        Func f = new Func("f", elasticStability);
                        BytecodeFunc func = f.toBytecodeFunc();
                        double beta = EPSILON / (2 * Math.log(2 / DELTA));
                        // Expr expr = x;
                        double smoothSensitivity = Math.exp(-k * beta)
                                * func.apply(k);
                        GraphElasticSensitivity.setOfMappingsSensitivity(
                                elasticStability, smoothSensitivity, EPSILON,
                                k);
                        System.out.println("star query");
                    }

                    double elasticStability = GraphElasticSensitivity
                            .calculateElasticSensitivityAtK(k,
                                    (ElementPathBlock) element, EPSILON);

                    System.out
                            .println("Elastic Stability: " + elasticStability);

                    double beta = EPSILON / (2 * Math.log(2 / DELTA));
                    double smoothSensitivity = Math.exp(-1 * beta * k)
                            * elasticStability;

                    // Se agrega el ruido con Laplace
                    double scale = 2 * smoothSensitivity / EPSILON;
                    Random random = new Random();
                    double u = 0.5 - random.nextDouble();
                    LaplaceDistribution l = new LaplaceDistribution(u, scale);

                    Query query = QueryFactory.create(queryString);
                    ResultSet results = HdtDataSource.ExcecuteQuery(query);
                    QuerySolution soln = results.nextSolution();
                    RDFNode x = soln.get(soln.varNames().next());
                    int result = x.asLiteral().getInt();

                    double finalResult2 = result + l.sample();
                    // ksjhfakljshfdkjfhadjshf;lasdkngkjasdfbkjadsfbk;adjsfb;
                    // jdsbkjslfdnbkdlsfnb;sdjnfbajs,dbfkjabdkjbasdfk
                    StringBuffer csvLine = new StringBuffer();
                    csvLine.append(queryFile);
                    csvLine.append(",");
                    csvLine.append(result);
                    csvLine.append(",");
                    csvLine.append(finalResult2);
                    csvLine.append(",");
                    csvLine.append(queryTriples);
                    csvLine.append(",");
                    csvLine.append(EPSILON);
                    csvLine.append(",");
                    csvLine.append(smoothSensitivity);
                    csvLine.append(",");
                    csvLine.append(elasticStability);
                    csvLine.append(",");
                    csvLine.append(k);
                    csvLine.append(",");
                    csvLine.append(trip_size);
                    csvLine.append("\n");

                    Files.write(Paths.get("results.csv"),
                            csvLine.toString().getBytes(),
                            StandardOpenOption.APPEND);

                    System.out.println("Original result: " + result);
                    System.out.println(
                            "Private Result: " + Math.round(finalResult2));

                }
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private static double laplace(double scale)
    {
        Random random = new Random();
        double u = 0.5 - random.nextDouble();
        return -Math.signum(u) * scale * Math.log(1 - 2 * Math.abs(u));
    }

}
