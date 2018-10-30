package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
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
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

public class GenerateCountQueries
{

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException, ParseException
    {
        ArrayList<LinkedList<Triple>> relatedTriplesList = new ArrayList<LinkedList<Triple>>();
        // create Options object
        Options options = new Options();

        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("o", "dir", true, "output directory");

        CommandLineParser parser = new DefaultParser();
        try
        {
            String queryString = "";
            String queryFile = "";
            String outputDir = "";

            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("f"))
            {
                queryFile = cmd.getOptionValue("f");
                File queryFileSystem = new File(queryFile);
                outputDir = queryFileSystem.getParent() + "/output/";
                queryString = new Scanner(queryFileSystem)
                        .useDelimiter("\\Z").next();
                queryFile = queryFileSystem.getName();

                // transform into Jena Query object
            }
            else
            {
                System.out.println("Missing SPARQL query file");
            }

            if (cmd.hasOption("o"))
            {
//                outputDir = cmd.getOptionValue("o");
            }
            else
            {
                System.out.println("Missing query directory");
            }

            Query q = QueryFactory.create(queryString);
            ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
            List<Element> elementList = queryPattern.getElements();
            LinkedList<Triple> triplesAdjList;

            for (Element element : elementList)
            {
                if (element instanceof ElementPathBlock)
                {
                    ElementPathBlock triplesBlock = (ElementPathBlock) element;
                    for (TriplePath triplePath : triplesBlock.getPattern())
                    {
                        Triple triple = triplePath.asTriple();
                        if (relatedTriplesList.isEmpty())
                        {
                            triplesAdjList = new LinkedList<Triple>();
                            triplesAdjList.add(triple);
                            relatedTriplesList.add(triplesAdjList);
                        }
                        else
                        {
                            boolean isIn = false;
                            for (LinkedList<Triple> tripleList : relatedTriplesList)
                            {
                                if (varsIn(tripleList.element(), triple))
                                {
                                    tripleList.add(triple);
                                    isIn = true;
                                    break;
                                }
                            }
                            if (!isIn)
                            {
                                triplesAdjList = new LinkedList<Triple>();
                                triplesAdjList.add(triple);
                                relatedTriplesList.add(triplesAdjList);
                            }
                        }
                    }
                }
            }

            int i = 0;
            while (i + 1 < relatedTriplesList.size())
            {
                LinkedList<Triple> tripleList = relatedTriplesList.get(i);
                int j = 0;
                while (j < tripleList.size())
                // for (Triple triple : tripleList)
                {
                    Triple triple = tripleList.getFirst();
                    tripleList.removeFirst();
                    if (i + 1 < relatedTriplesList.size())
                    {
                        if (findTriple(triple, relatedTriplesList.get(i + 1)))
                        {
                            tripleList.add(triple);
                            break;
                        }
                        else
                        {
                            tripleList.add(triple);
                        }
                    }
                    j++;
                }
                i++;
                System.out.println("--------------------");
            }

            for (Var var : q.getProjectVars())
            {
                String varStr = var.getName();
                StringBuffer newQueryString = new StringBuffer("SELECT (COUNT(?"
                        + varStr + ") as " + "?count_" + varStr + ") WHERE {\n");
                for (LinkedList<Triple> tripleList : relatedTriplesList)
                {
                    for (Triple triple : tripleList)
                    {
                        String subject;
                        String predicate;
                        String object;
                        if (triple.getSubject().isURI())
                        {
                            subject = "<" + triple.getSubject() + ">";
                        }
                        else
                        {
                            subject = triple.getSubject().toString();
                        }
                        if (triple.getPredicate().isURI())
                        {
                            predicate = "<" + triple.getPredicate() + ">";
                        }
                        else
                        {
                            predicate = triple.getPredicate().toString();
                        }
                        if (triple.getObject().isURI())
                        {
                            object = "<" + triple.getObject() + ">";
                        }
                        else
                        {
                            object = triple.getObject().toString();
                        }
                        newQueryString.append(subject + " " + predicate + " "
                                + object + " .\n");
                    }
                }
                newQueryString.append("}");
                Files.write(
                        Paths.get(outputDir + queryFile.replaceAll(".rq", "")
                                + "." + var.getName() + ".rq"),
                        newQueryString.toString().getBytes());
                System.out.println(newQueryString.toString());
            }

        }
        catch (Exception e)
        {
            System.out.print(e.getCause());
        }

    }

    private static boolean findTriple(Triple triple,
            LinkedList<Triple> linkedList)
    {
        for (Triple innerTriple : linkedList)
        {
            if (varsIn(innerTriple, triple))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean varsIn(Triple tripleKey, Triple triple)
    {
        ArrayList<String> vars = new ArrayList<String>();
        if (tripleKey.getSubject().isVariable())
        {
            vars.add(tripleKey.getSubject().getName());
        }
        if (tripleKey.getObject().isVariable())
        {
            vars.add(tripleKey.getObject().getName());
        }
        if (tripleKey.getPredicate().isVariable())
        {
            vars.add(tripleKey.getPredicate().getName());
        }
        if (triple.getSubject().isVariable())
        {
            if (vars.contains(triple.getSubject().getName()))
            {
                return true;
            }
        }
        if (triple.getObject().isVariable())
        {
            if (vars.contains(triple.getObject().getName()))
            {
                return true;
            }
        }
        if (triple.getPredicate().isVariable())
        {
            if (vars.contains(triple.getPredicate().getName()))
            {
                return true;
            }
        }
        return false;
    }
}
