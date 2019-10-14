package cl.utfsm.di.RDFDifferentialPrivacySymbolic;

import symjava.symbolic.Expr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Result
{
    public String query;
    public double epsilon;
    public List<Double> privateResult;
    public double sensitivity;
    public List<Integer> result;
    public Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    public Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();
    public int maxK;
    public double scale;
    public String elasticStability;
    public long graphSize;
    public boolean starQuery;

    public Result(String query, double epsilon, List<Double> resultList, double sensitivity, List<Integer>  result,
            int maxK, double scale, Expr elasticStability,
            long graphSize, boolean starQuery, Map<String, List<Integer>> mapMostFreqValue, Map<String, List<StarQuery>> mapMostFreqValueStar)
    {
        this.query = query;
        this.epsilon = epsilon;
        this.privateResult = resultList;
        this.sensitivity = sensitivity;
        this.result = result;
        this.maxK = maxK;
        this.scale = scale;
        this.elasticStability = elasticStability.toString();
        this.graphSize = graphSize;
        this.starQuery = starQuery;
        this.mapMostFreqValue = mapMostFreqValue;
        this.mapMostFreqValueStar = mapMostFreqValueStar;
    }

    public String toString()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
