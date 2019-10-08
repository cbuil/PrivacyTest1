package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.core.TriplePath;

import symjava.symbolic.Expr;

public class StarQuery
{
    private List<TriplePath> triples;

    // the smoothed sensitivity of the star query
    private double querySentitivity;

    private int keyCardinality;

    // elastic stability is the formula by which we calculate the sensitivity,
    // only appears when there are more than two star queries
    private Expr elasticStability;
    
    private Expr maxFrequency;

    public StarQuery(List<TriplePath> triples)
    {
        this.triples = triples;
    }

    public StarQuery()
    {
        this.triples = new ArrayList<TriplePath>();
    }

    public boolean addStarQuery(List<TriplePath> triples)
    {
        return this.triples.addAll(triples);
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

    public double getQuerySentitivity()
    {
        return querySentitivity;
    }

    public void setQuerySentitivity(double res)
    {
        this.querySentitivity = res;
    }

    public Expr getElasticStability()
    {
        return elasticStability;
    }

    public void setElasticStability(Expr elasticStability)
    {
        this.elasticStability = elasticStability;
    }

    public Expr getMaxFrequency()
    {
        return maxFrequency;
    }

    public void setMaxFrequency(Expr maxFrequency)
    {
        this.maxFrequency = maxFrequency;
    }
}
