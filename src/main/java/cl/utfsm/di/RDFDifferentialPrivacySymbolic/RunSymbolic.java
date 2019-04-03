package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import org.apache.commons.cli.*;
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

import cl.utfsm.di.RDFDifferentialPrivacy.Helper;
import cl.utfsm.di.RDFDifferentialPrivacySymbolic.GraphElasticSensitivity;

import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.*;
import static symjava.symbolic.Symbol.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class RunSymbolic
{
    public static void main(String[] args)
            throws IOException, CloneNotSupportedException
    {

        // delta parameter: use 1/n^2, with n = 100000
        double DELTA = 1 / (Math.pow(10000000, 2));

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
            if (cmd.hasOption("e"))
            {
                queryDir = cmd.getOptionValue("e");
            }
            else
            {
                System.out.println("Missing query directory");
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
        Query q = QueryFactory.create(queryString);

        double beta = EPSILON / (2 * Math.log(2 / DELTA));

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        double smoothSensitivity = 0.0;
        Element element = elementList.get(0);
        if (element instanceof ElementPathBlock)
        {
            Expr elasticStability = Expr.valueOf(0);

            int k = 1;

            if (Helper.isStarQuery(q))
            {
                elasticStability = x;
                double sensitivity = k;
                smoothSensitivity = smoothElasticSensitivity(elasticStability,
                        sensitivity, beta, k);
                System.out.println("star query (smooth) sensitivity: "
                        + smoothSensitivity);
            }
            else
            {
                elasticStability = GraphElasticSensitivity
                        .calculateElasticSensitivityAtK(k,
                                (ElementPathBlock) element, EPSILON);

                Func f = new Func("f", elasticStability);
                BytecodeFunc func = f.toBytecodeFunc();
                System.out.println(
                        "Elastic Stability: " + Math.round(func.apply(2)));
                smoothSensitivity = smoothElasticSensitivity(elasticStability,
                        0, beta, 0);
                System.out.println(
                        "Path Smooth Sensitivity: " + smoothSensitivity);
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

            System.out.println("Original result: " + countQueryResult);
            System.out.println("Noise added: " + Math.round(noise));
            System.out.println("Private Result: " + Math.round(finalResult1));

        }
    }

    // TODO: change this to a while and add as limit the size of the query  
    private static double smoothElasticSensitivity(Expr elasticSensitivity,
            double prevSensitivity, double beta, int k)
    {
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);

        if (smoothSensitivity == 0 || (smoothSensitivity > prevSensitivity))
        {
            return prevSensitivity;
        }
        else
        {
            return smoothElasticSensitivity(elasticSensitivity,
                    smoothSensitivity, beta, k + 1);
        }
    }

}
