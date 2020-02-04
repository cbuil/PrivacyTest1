/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.utfsm.di.RDFDifferentialPrivacy.Run;

import cl.utfsm.di.RDFDifferentialPrivacy.HdtDataSource;
import java.util.concurrent.Callable;

/**
 *
 * @author cbuil
 */
public class RunSparqlQuery implements Callable<String>
{

   HdtDataSource hdtDataSource;
   String queryString;
   String queryFileName;


    RunSparqlQuery(HdtDataSource dataSource, String queryString, String queryFileName)
    {
        this.hdtDataSource = dataSource;
        this.queryString = queryString;
        this.queryFileName = queryFileName;
    }

   @Override
   public String call() 
    { 
        try
        { 
            int result = hdtDataSource.executeCountQuery(queryString);
            return queryFileName + '\t' + String.valueOf(result);
  
        } 
        catch (Exception e) 
        { 
            // Throwing an exception 
            System.out.println ("Exception is caught"); 
        } 
        return queryFileName + "\t-1";
    } 
}

