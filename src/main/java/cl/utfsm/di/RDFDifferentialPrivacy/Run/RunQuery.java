package cl.utfsm.di.RDFDifferentialPrivacy.Run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.ResultBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunQuery

{

    private static Logger logger = LogManager
            .getLogger(RunQuery.class.getName());

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException, ExecutionException
    {

        // create Options object
        Options options = new Options();

        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "data", true, "Endpoint address");
        String queryString = "";
        String queryFile = "";
        String dataFile = "";
        String endpoint = "";
        SecureRandom sc = new SecureRandom();
        
        for(int i = 0; i < 100; i++) {
            logger.info("secure random: " + sc.nextDouble());
        }

//        CommandLineParser parser = new DefaultParser();
//        try
//        {
//            CommandLine cmd = parser.parse(options, args);
//            if (cmd.hasOption("f"))
//            {
//                queryString = cmd.getOptionValue("f");
//            }
//            else
//            {
//                logger.info("Missing query file");
//            }
//            if (cmd.hasOption("d"))
//            {
//                dataFile = cmd.getOptionValue("d");
//            }
//            else
//            {
//                logger.info("Missing data file");
//            }
//            if (cmd.hasOption("e"))
//            {
//                endpoint = cmd.getOptionValue("e");
//            }
//            else
//            {
//                logger.info("Missing endpoint address");
//            }
//        }
//        catch (ParseException e1)
//        {
//            e1.getMessage();
//            System.exit(-1);
//        }
//
//        try
//        {
//            HdtDataSource hdtDataSource = new HdtDataSource(dataFile);
//
//            Path queryLocation = Paths.get(queryString);
//            logger.info(queryFile);
//            if (Files.isRegularFile(queryLocation))
//            {
//                queryString = new Scanner(new File(queryString))
//                        .useDelimiter("\\Z").next();
//                logger.info(queryString);
//                Query query = QueryFactory.create(queryString);
//                ResultSet rs = null;
//                if (endpoint != "")
//                {
//                    rs = hdtDataSource.excecuteQuery(query, endpoint);
//                }
//                else
//                {
//                    rs = hdtDataSource.excecuteQuery(query);
//                }
//                int i = 0;
//                while (rs.hasNext())
//                {
//                    logger.info(((ResultBinding) rs.next()).getBinding()
//                            .get(query.getProjectVars().get(0)));
//                    i++;
//                }
//                logger.info("results: " + i);
//
//            }
//        }
//        catch (IOException e1)
//        {
//            System.out.println("Exception: " + e1.getMessage());
//            System.exit(-1);
//        }
    }
}
