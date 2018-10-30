package cl.utfsm.di.RDFDifferentialPrivacy;

import java.util.HashSet;
import java.util.List;

import org.apache.jena.sparql.core.TriplePath;

class Join implements Cloneable
{
    String name;
    List<String> joinVariables; // Variables que se ocupan para hacer el Join
    HashSet<String> ancestors; // Variables que componen al lado izquierdo del
    // join (otro join)
    // HashSet<String> newVariables; // Variables que componen al Triple que con
    // el que se esta haciendo JOIN
    Join Left; // El join izquierdo con el que se esta haciendo JOIN
    TriplePath Right; // El triple con el que se esta haciendo JOIN
    TriplePath triple; // Para el caso base en caso de que el primer join
    // contenga solo triples

    Join(String n, List<String> j, HashSet<String> a, Join L, TriplePath R)
    {
        name = n;
        joinVariables = j;
        ancestors = a;
        // newVariables = n;
        Left = L;
        Right = R;
    }

    Join(TriplePath l)
    {
        triple = l;
    }

    Join()
    {
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
