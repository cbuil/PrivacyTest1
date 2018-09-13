package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.query.*;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
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

            //se obtiene el encabezado de la query
            String queryHead = "SELECT " + countVariable +"\nWHERE\n";
            //String queryHead = q.toString();
            //String[] splitedQuery = queryHead.split("\\{");
            //queryHead = splitedQuery[0];

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

                    //int maxfreq = obtainMaxFreq(bgpIt, countVariable);
                    int i = 0;
                    int elasticStability = 0;
                    int mostFreqValue = 1;
                    //lista para guardar los triples
                    List<String> triples = new ArrayList<String>();
                    while (bgpIt.hasNext())
                    {
                        TriplePath triple = (TriplePath) bgpIt.next();

                        if(i!=0) {
                            String finalQuery = queryCreator(triples, queryHead);
                            //set resultante de la ejecucion de la query de los triples hasta ahora
                            ResultSet results = HdtDataSource.ExcecuteQuery(QueryFactory.create(finalQuery));

                            HashMap<String, Integer> hmap = new HashMap<String, Integer>();
                            for (; results.hasNext(); ) {
                                QuerySolution soln = results.nextSolution();
                                String sol = soln.get(countVariable).toString();
                                if (hmap.containsKey(sol)) {
                                    hmap.put(sol, hmap.get(sol) + 1);
                                } else {
                                    hmap.put(sol, 1);
                                }
                            }
                            mostFreqValue = getMaxFreq(hmap);
                        }
                        //se agrega el triple actual a la lista de triples
                        triples.add(tripleFixer(triple));

                        int res = HdtDataSource.getCountResults(triple, countVariable);
                        if(i==0 && bgpIt.hasNext()){
                            TriplePath auxtriple = (TriplePath) bgpIt.next();
                            System.out.println(auxtriple.toString());
                            triples.add(tripleFixer(auxtriple));
                            int res2 = HdtDataSource.getCountResults(auxtriple, countVariable);
                            elasticStability = Math.max(res*1,res2*1);
                        }
                        else{
                            elasticStability = Math.max(mostFreqValue*1, res * elasticStability);
                        }
                        i++;
                        System.out.println(triple.toString());
                    }


                    System.out.println("Elastic Stability: "+ elasticStability);
                }
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    public static String queryCreator(List<String> triples, String queryHead){
        String finalQuery = queryHead + "{";
        Iterator<String> Iterator = triples.iterator();
        while(Iterator.hasNext()){
            finalQuery = finalQuery + Iterator.next() + ".\n ";
        }
        finalQuery = finalQuery + "}";
        return finalQuery;
    }

    public static String tripleFixer(TriplePath triplePath){

        String subject = "";
        if (triplePath.asTriple().getMatchSubject() instanceof Node_URI)
        {
            subject = "<" + triplePath.asTriple().getMatchSubject().getURI()
                    + ">";
        }
        else if (triplePath.asTriple()
                .getMatchSubject() instanceof Node_Variable)
        {
            subject = "?" + triplePath.asTriple().getMatchSubject().getName();
        }
        String pred = "";
        if (triplePath.asTriple().getMatchPredicate() instanceof Node_URI)
        {
            pred = "<" + triplePath.asTriple().getMatchPredicate().getURI()
                    + ">";
        }
        else if (triplePath.asTriple()
                .getMatchPredicate() instanceof Node_Variable)
        {
            pred = "?" + triplePath.asTriple().getMatchPredicate().getName();
        }
        String object = "";
        if (triplePath.asTriple().getMatchObject() instanceof Node_URI)
        {
            object = "<" + triplePath.asTriple().getMatchObject().getURI()
                    + ">";
        }
        else if (triplePath.asTriple()
                .getMatchObject() instanceof Node_Variable)
        {
            object = "?" + triplePath.asTriple().getMatchObject().getName();
        }
        String result = subject + " " + pred + " " + object;
        return result;
    }


    public static int getMaxFreq(HashMap<String,Integer> map) {
        String highestMap = null;
        int mostFreqValue = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > mostFreqValue) {
                highestMap = entry.getKey();
                mostFreqValue = entry.getValue();
            }
        }
        return mostFreqValue;
    }

}
