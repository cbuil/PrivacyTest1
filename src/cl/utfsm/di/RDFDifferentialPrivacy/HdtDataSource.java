package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.IOException;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.NodeDictionary;

public class HdtDataSource
{

    private static HDT datasource;
    private static NodeDictionary dictionary;
    protected String title;
    protected String description;

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
    public HdtDataSource(String hdtFile)
            throws IOException
    {
        datasource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
    }

    public static int getCountResults(TriplePath triplePath,
            String variableName)
    {
        variableName = variableName.replace("“", "").replace("”", "");
        String countQueryString = "select (count(" + variableName
                + ") as ?count) where { " + triplePath.toString()
                + "} GROUP BY " + variableName + " " + "ORDER BY "
                + variableName + " DESC (?count) LIMIT 1 ";
        System.out.println(countQueryString);
        Node subject = triplePath.getSubject();
        Node predicate = triplePath.getSubject();
        Node object = triplePath.getSubject();

        // look up the result from the HDT datasource
        final int subjectId = subject == null ? 0
                : dictionary.getIntID(subject, TripleComponentRole.SUBJECT);
        final int predicateId = predicate == null ? 0
                : dictionary.getIntID(predicate, TripleComponentRole.PREDICATE);
        final int objectId = object == null ? 0
                : dictionary.getIntID(object, TripleComponentRole.OBJECT);
        if (subjectId < 0 || predicateId < 0 || objectId < 0)
        {
        }
        final Model triples = ModelFactory.createDefaultModel();

        String queryStr = "select ?v2 where { ?v0 <http://purl.org/goodrelations/price> ?v2}";
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query,
                triples))
        {
            ResultSet results = qexec.execSelect();
            if (results.hasNext())
            {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get("count");
                int res = x.asLiteral().getInt();
                System.out.println(res);
                return res;
            }
            else
                return 0;
        }
    }

}
