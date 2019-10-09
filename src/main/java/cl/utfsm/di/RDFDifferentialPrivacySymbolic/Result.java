package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import symjava.symbolic.Expr;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Result
{
    public String query;
    public double epsilon;
    public List<Double> privateResult;
    public int k;
    public int result;
    public Map<String, Integer> tripleSelectivity;
    public int queryTriples;
    public double scale;
    public Expr elasticStability;
    public int graphSize;
    public boolean starQuery;

    public Result(String query, double epsilon, List<Double> resultList, int k, int result2,
            int queryTriples, double scale, Expr elasticStability2,
            int graphSize, boolean starQuery, Map<String, Integer> maxFreqMap)
    {
        this.query = query;
        this.epsilon = epsilon;
        this.privateResult = resultList;
        this.k = k;
        this.result = result2;
        this.queryTriples = queryTriples;
        this.scale = scale;
        this.elasticStability = elasticStability2;
        this.graphSize = graphSize;
        this.starQuery = starQuery;
        this.tripleSelectivity = maxFreqMap;
    }

    public String toString()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
