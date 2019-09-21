package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import org.apache.jena.sparql.core.TriplePath;

public class MaxFreqQuery {
    private String query;
    private String variable;

    public MaxFreqQuery(String tp, String var) {
        query = tp;
        variable = var;
    }

    public int getQuerySize() {
        return query.length();
    }

    public String getQuery() {
        return query;
    }

    public String getVariableString() {
        return variable;
    }
}