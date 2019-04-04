package cl.utfsm.di.RDFDifferentialPrivacy.symbolic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import java.security.SecureRandom;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;

import cl.utfsm.di.RDFDifferentialPrivacy.GraphElasticSensitivity;
import cl.utfsm.di.RDFDifferentialPrivacy.HdtDataSource;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

public class RunSymbolic
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

            HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
            Query q = QueryFactory.create(queryString);

            String construct = queryString.replaceFirst("SELECT.*WHERE",
                    "CONSTRUCT WHERE");
            Query constructQuery = QueryFactory.create(construct);
            double tripSize = HdtDataSource.getTripSize(constructQuery);

            // se obtiene el encabezado de la query
            // String queryHead = "SELECT " + countVariable +"\nWHERE\n";
            // String queryHead = q.toString();
            // String[] splitedQuery = queryHead.split("\\{");
            // queryHead = splitedQuery[0];

            ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
            List<Element> elementList = queryPattern.getElements();
            int queryTriples = 0;
            for (Element element : elementList)
            {
                if (element instanceof ElementTriplesBlock)
                {
                    ElementTriplesBlock triplesBlock = (ElementTriplesBlock) element;
                    BasicPattern bgp = triplesBlock.getPattern();
                    Iterator bgpIt = bgp.iterator();
                    while (bgpIt.hasNext())
                    {
                        Triple triple = (Triple) bgpIt.next();
                        System.out.println(triple.toString());
                    }
                }
                if (element instanceof ElementPathBlock)
                {
                    ElementPathBlock bgpBlock = (ElementPathBlock) element;
                    PathBlock pb = bgpBlock.getPattern();
                    Iterator bgpIt = pb.getList().iterator();

                    // delta parameter: use 1/n^2, with n = 100000
                    double DELTA = 1 / (Math.pow(tripSize, 2));

                    // privacy budget
                    double EPSILON = 0.1;
                    
                    queryTriples += ((ElementPathBlock) element).getPattern()
                            .size();

                    Expr elasticStability = GraphElasticSensitivity
                            .calculateElasticSensitivityAtK(
                                    (ElementPathBlock) element, EPSILON,
                                    bgpBlock);

                    double beta = EPSILON / (2 * Math.log(2 / DELTA));
                    // double smoothSensitivity = Math.exp(-1 * beta * k) *
                    // elasticStability;

                    Func f = new Func("f", elasticStability);
                    BytecodeFunc func = f.toBytecodeFunc();
                    System.out.println(
                            "Elastic Stability: " + Math.round(func.apply(2)));

                    SmoothResult smoothResult = GraphElasticSensitivity
                            .smoothElasticSensitivity(elasticStability, 0, beta,
                                    0, bgpBlock);
                    double smoothSensitivity = smoothResult.getSensitivity();
                    System.out.println(
                            "Smooth Sensitivity: " + smoothSensitivity);

                    // Se agrega el ruido con Laplace
                    double scale = 2 * smoothSensitivity / EPSILON;
                    SecureRandom random = new SecureRandom();
                    double u = 0.5 - random.nextDouble();
                    LaplaceDistribution l = new LaplaceDistribution(u, scale);
                    // double finalResult = -Math.signum(u) * scale * Math.log(1
                    // - 2*Math.abs(u));

                    Query query = QueryFactory.create(queryString);
                    ResultSet results = HdtDataSource.excecuteQuery(query);
                    QuerySolution soln = results.nextSolution();
                    RDFNode x = soln.get(soln.varNames().next());
                    int result = x.asLiteral().getInt();

                    // double finalResult1 = result + finalResult;
                    double finalResult2 = result + l.sample();

                    System.out.println("Elastic Stabiliy: " + elasticStability);
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
                    csvLine.append(smoothResult.getK());
                    csvLine.append(",");
                    csvLine.append(tripSize);
                    csvLine.append("\n");

                    Files.write(Paths.get("results_symbolic.csv"),
                            csvLine.toString().getBytes(),
                            StandardOpenOption.APPEND);

                    System.out.println("Original result: " + result);
                    // System.out.println("Private Result: "+
                    // Math.round(finalResult1));
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

}
