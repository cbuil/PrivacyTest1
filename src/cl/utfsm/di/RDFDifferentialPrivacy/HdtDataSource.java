package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

public class HdtDataSource
{

    private static HDT datasource;
    private static NodeDictionary dictionary;
    private static HDTGraph graph;
    private static Model triples;

    /**
     * Creates a new HdtDataSource.
     * 
     * @param title
     *            title of the datasource
     * @param description
     *            datasource description
     * @param hdtFile
     *            the HDT datafile
     * @throws IOException
     *             if the file cannot be loaded
     */
    public HdtDataSource(String hdtFile) throws IOException
    {
        datasource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
        graph = new HDTGraph(datasource);
        triples = ModelFactory.createModelForGraph(graph);
    }

    public static int getCountResults(TriplePath triplePath,
            String variableName)
    {
        List<String> aux = Helper.triplePartExtractor(triplePath);
        String subject = aux.get(0);
        String pred = aux.get(1);
        String object = aux.get(2);


        variableName = variableName.replace("“", "").replace("”", "");
        String countQueryString = "select (count(" + variableName
                + ") as ?count) where { " + subject + " " + pred + " " + object
                + " " + "} GROUP BY " + variableName + " " + "ORDER BY "
                + variableName + " DESC (?count) LIMIT 1 ";

        Query query = QueryFactory.create(countQueryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            ResultSet results = qexec.execSelect();
            if (results.hasNext())
            {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("count");
                int res = x.asLiteral().getInt();
                System.out.println("count: " + res);
                return res;
            }
            else
                return 0;
        }
    }

    public static ResultSet ExcecuteQuery(Query query){
        try(QueryExecution qexec = QueryExecutionFactory.create(query,triples)){
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results) ;
            return results;
        }
    }

}
