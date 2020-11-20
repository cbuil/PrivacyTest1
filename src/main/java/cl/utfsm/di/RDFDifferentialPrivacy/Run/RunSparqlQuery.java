/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.utfsm.di.RDFDifferentialPrivacy.Run;

import java.util.concurrent.Callable;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author cbuil
 */
public class RunSparqlQuery implements Callable<String>
{

   Model triples;
   String queryString;
   String queryFileName;
   
   private static Logger logger = LogManager
            .getLogger(RunSparqlQuery.class.getName());


    RunSparqlQuery(Model dataSource, String queryString, String queryFileName)
    {
        this.triples = dataSource;
        this.queryString = queryString;
        this.queryFileName = queryFileName;
    }

   @Override
   public String call() 
    { 
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
             QuerySolution soln = results.nextSolution();
            RDFNode x = soln.get(soln.varNames().next());
            int countResult = x.asLiteral().getInt();
            logger.info("count query result (dataset): " + countResult);
            return queryFileName + '\t' + String.valueOf(countResult);
        }
        catch (Exception e) 
        { 
            // Throwing an exception 
            System.out.println ("Exception is caught"); 
        } 
        return queryFileName + "\t-1";
    } 
}

