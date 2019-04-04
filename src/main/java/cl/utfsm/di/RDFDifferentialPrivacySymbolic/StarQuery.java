package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.core.TriplePath;

public class StarQuery
{
    private List<TriplePath> triples;

    public StarQuery(List<TriplePath> triples)
    {
        this.triples = triples;
    }

    public StarQuery()
    {
        // TODO Auto-generated constructor stub
    }

    public boolean addStarQuery(List<TriplePath> triples)
    {
        String starQueryVariable = this.triples.get(0).getSubject().getName();
        for (TriplePath tripleInQuery : triples)
        {
            if (tripleInQuery.getSubject().isVariable())
            {
                if (starQueryVariable
                        .compareTo(tripleInQuery.getSubject().getName()) != 0)
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        return triples.addAll(triples);
    }

    public List<String> getVariables()
    {
        List<String> variables = new ArrayList<String>();
        for (TriplePath triplePath : triples)
        {
            if (triplePath.getSubject().isVariable())
            {
                if (!variables.contains(triplePath.getSubject().getName()))
                {
                    variables.add(triplePath.getSubject().getName());
                }
            }
            if (triplePath.getPredicate().isVariable())
            {
                if (!variables.contains(triplePath.getPredicate().getName()))
                {
                    variables.add(triplePath.getPredicate().getName());
                }
            }
            if (triplePath.getObject().isVariable())
            {
                if (!variables.contains(triplePath.getObject().getName()))
                {
                    variables.add(triplePath.getObject().getName());
                }
            }
        }
        return variables;
    }

    public String toString()
    {
        StringBuffer result = new StringBuffer();
        for (TriplePath triplePath : triples)
        {
            String subject = "";
            if (triplePath.asTriple().getMatchSubject() instanceof Node_URI)
            {
                subject = "<" + triplePath.asTriple().getMatchSubject().getURI()
                        + ">";
            }
            else if (triplePath.asTriple()
                    .getMatchSubject() instanceof Node_Variable)
            {
                subject = "?"
                        + triplePath.asTriple().getMatchSubject().getName();
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
                pred = "?"
                        + triplePath.asTriple().getMatchPredicate().getName();
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
            result.append(subject);
            result.append(" ");
            result.append(pred);
            result.append(" ");
            result.append(object);
            result.append(" . \n");
        }

        return result.toString();
    }

    public List<TriplePath> getTriples()
    {
        return triples;
    }
}
