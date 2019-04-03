package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import java.util.HashSet;
import java.util.List;

import org.apache.jena.sparql.core.TriplePath;

public class Join implements Cloneable
{
    public String name;
    public List<String> joinVariables; // Variables que se ocupan para hacer el Join
    public HashSet<String> ancestors; // Variables que componen al lado izquierdo del
    // join (otro join)
    // HashSet<String> newVariables; // Variables que componen al Triple que con
    // el que se esta haciendo JOIN
    public Join Left; // El join izquierdo con el que se esta haciendo JOIN
    public TriplePath Right; // El triple con el que se esta haciendo JOIN
    public TriplePath triple; // Para el caso base en caso de que el primer join
    // contenga solo triples

    public Join(String n, List<String> j, HashSet<String> a, Join L, TriplePath R)
    {
        name = n;
        joinVariables = j;
        ancestors = a;
        // newVariables = n;
        Left = L;
        Right = R;
    }

    public Join(TriplePath l)
    {
        triple = l;
    }

    public Join()
    {
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
