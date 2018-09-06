package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;

public class Run
{
    public static void main(String[] args) throws IOException
    {
        // create Options object
        Options options = new Options();

        // add q query option
        options.addOption("q", "query", true, "input SPARQL query");
        // add q query file option
        options.addOption("f", "qFile", true, "input SPARQL query File");
        // add e endpoint option
        options.addOption("e", "endpoint", true, "SPARQL endpoint");
        // add v COUNT variable option
        options.addOption("v", "var", true, "COUNT variable");

        CommandLineParser parser = new DefaultParser();
        try
        {
            String localEndpoint = "";
            String queryString = "";
            String queryFile = "";
            String countVariable = "";
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
            if (cmd.hasOption("e"))
            {
                localEndpoint = cmd.getOptionValue("e");
            }
            else
            {
                System.out.println("Missing SPARQL endpoint ");
            }
            if (cmd.hasOption("v"))
            {
                countVariable = cmd.getOptionValue("v");
            }
            else
            {
                System.out.println("Missing COUNT variable");
            }
            
            HdtDataSource hdtDataSource = new HdtDataSource("resources/watdiv.10M.hdt");
            
            Query q = QueryFactory.create(queryString);
            ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
            List<Element> elementList = queryPattern.getElements();
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
                    int i = 0;
                    int maxfreq = 0;
                    while (bgpIt.hasNext())
                    {
                        TriplePath triple = (TriplePath) bgpIt.next();
                        int res = HdtDataSource.getCountResults(triple, countVariable);
                        if(i==0){
                            maxfreq = res;
                        }
                        else{
                            if(bgpIt.hasNext()){
                                maxfreq = Math.max(maxfreq, res);
                            }
                        }
                        i++;
                        System.out.println(triple.toString());

                    }
                    System.out.println("maxfreq: "+ maxfreq);
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
