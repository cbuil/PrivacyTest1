package cl.utfsm.di.RDFDifferentialPrivacy.Run;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.cli.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GetPrivacySchema{

     private static final Logger logger = LogManager
            .getLogger(GetPrivacySchema.class.getName());


    public static void main(String[] args){
         // create Options object
        Options options = new Options();

        String urisFile = "";
        String propertiesFile = "";
        String endpoint = "";
        String outputFile = "";

        options.addOption("f", "qFile", true, "input URIs File");
        options.addOption("p", "pFile", true, "input properties file");
        options.addOption("e", "endpoint", true, "SPARQL endpoint o TDB dir");
        options.addOption("o", "outoput", true, "Output file");
        CommandLineParser parser = new DefaultParser();
        try
        {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("f"))
            {
                urisFile = cmd.getOptionValue("f");
            } else
            {
                logger.info("Missing URIs starting file");
            }
            if (cmd.hasOption("p"))
            {
                propertiesFile = cmd.getOptionValue("p");
            } else
            {
                logger.info("Missing properties file");
            }
            if (cmd.hasOption("e"))
            {
                endpoint = cmd.getOptionValue("e");
            } else
            {
                logger.info("Missing endpoint");
            }
            if (cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            } else
            {
                logger.info("Missing endpoint");
            }

        }  catch (ParseException e1)
        {
            System.out.println(e1.getMessage());
            System.exit(-1);
        }
        //List<String> propertiesList = new ArrayList<String>();
        final String endpointURL = endpoint;
        // Dataset dataset = TDBFactory.createDataset(endpoint);
        // dataset.begin(ReadWrite.READ);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(outputFile), "utf-8"))) {
              final List<String> propertiesList = Files.readAllLines(Paths.get(propertiesFile));
              try (Stream<String> stream = Files.lines(Paths.get(urisFile))) {
                  stream.forEach(uri-> {
                        System.out.println(uri);
                        for (String p : propertiesList) {
                            String queryString  = "SELECT * WHERE { <" + uri + "> <" + p + "> ?o }";
                            Query query = QueryFactory.create(queryString);
                            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointURL, query);
                            ResultSet results = qexec.execSelect();
                            while(results.hasNext()){
                                QuerySolution soln = results.nextSolution();
                                RDFNode x = soln.get("?o");
                                try {
                                    if(x.isLiteral()){
                                        writer.write("<" + uri + "> " + "<" + p + "> \"" + x.asNode().getLiteral() + "\" .\n");
                                    } else if (x.isURIResource()) {
                                        writer.write("<" + uri + "> " + "<" + p + "> <" + x.asNode().getURI() + "> .\n");
                                    } else {
                                        // do nothing
                                    }
                                } catch (IOException e) {
                                    //TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            qexec.close();
                        }
                    });
        }} catch(Exception e){
            logger.info(e.getMessage());
            System.exit(-1);
        }
    }
}
