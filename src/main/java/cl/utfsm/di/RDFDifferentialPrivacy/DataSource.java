/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.utfsm.di.RDFDifferentialPrivacy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.TriplePath;

/**
 *
 * @author cbuil
 */
public interface DataSource
{
    public int mostFrequenResult(MaxFreqQuery maxFreqQuery);

    public ResultSet excecuteQuery(Query query);

    public long getGraphSize(Query query);
    
    public int executeCountQuery(String queryString);
    
    public long getGraphSizeTriples(List<List<String>> triplePatternsCount);
    
     public void setMostFreqValueMaps(
            Map<String, List<TriplePath>> starQueriesMap,
            List<List<String>> triplePatterns) throws ExecutionException;
     
    public Map<String, List<StarQuery>> getMapMostFreqValueStar();
    
    public Map<String, List<Integer>> getMapMostFreqValue();
    
}
