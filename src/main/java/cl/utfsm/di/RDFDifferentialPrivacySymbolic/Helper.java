package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import java.util.*;

public class Helper
{
    private static String queryCreator(List<String> triples, String queryHead)
    {
        String finalQuery = queryHead + "{";
        Iterator<String> Iterator = triples.iterator();
        while (Iterator.hasNext())
        {
            finalQuery = finalQuery + Iterator.next() + ".\n ";
        }
        finalQuery = finalQuery + "}";
        return finalQuery;
    }

    public static boolean isStarQuery(Query query)
    {
        List<Element> elements = ((ElementGroup) query.getQueryPattern())
                .getElements();
        ElementPathBlock element = (ElementPathBlock) elements.get(0);
        List<TriplePath> triplePath = element.getPattern().getList();
        String starQueryVariable = "";
        for (TriplePath tripleInQuery : triplePath)
        {
            if (tripleInQuery.getSubject().isVariable())
            {
                if (starQueryVariable.compareTo("") == 0)
                {
                    if (starQueryVariable.compareTo(starQueryVariable) != 0)
                    {
                        return false;
                    }
                }
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    public static List<String> triplePartExtractor(TriplePath triplePath)
    {
        List<String> result = new ArrayList<String>();
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
        result.add(subject);
        result.add(pred);
        result.add(object);

        return result;
    }

    public static String tripleFixer(TriplePath triplePath)
    {
        List<String> aux = triplePartExtractor(triplePath);
        String result = aux.get(0) + " " + aux.get(1) + " " + aux.get(2);
        return result;
    }

    public static int getMaxFreq(HashMap<String, Integer> map)
    {
        String highestMap = null;
        int mostFreqValue = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet())
        {
            if (entry.getValue() > mostFreqValue)
            {
                highestMap = entry.getKey();
                mostFreqValue = entry.getValue();
            }
        }
        return mostFreqValue;
    }

    public static boolean extractor(TriplePath triplePath,
            HashSet<String> ancestors)
    {
        List<String> aux = triplePartExtractor(triplePath);
        String subject = aux.get(0);
        String object = aux.get(2);
        if (ancestors.contains(subject) || ancestors.contains(object))
        {
            ancestors.add(subject);
            ancestors.add(object);
            return true;
        }
        else
        {
            ancestors.add(subject);
            ancestors.add(object);
            return false;
        }
    }
}
