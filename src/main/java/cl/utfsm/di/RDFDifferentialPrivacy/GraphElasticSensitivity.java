package cl.utfsm.di.RDFDifferentialPrivacy;

import static symjava.symbolic.Symbol.x;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javafx.util.Pair;

import org.apache.jena.sparql.core.TriplePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

public class GraphElasticSensitivity
{

    private static final Logger logger = LogManager
            .getLogger(GraphElasticSensitivity.class.getName());

    public static double setOfMappingsSensitivity(Expr elasticSensitivity,
            double prevSensitivity, double beta, int k)
    {
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);

        if (func1.apply(0) == 0 || (smoothSensitivity < prevSensitivity))
        {
            return prevSensitivity;
        } else
        {
            return setOfMappingsSensitivity(elasticSensitivity,
                    smoothSensitivity, beta, k + 1);
        }
    }

    public static Expr calculateElasticSensitivityAtK(int k,
            Map<String, List<TriplePath>> starQueriesMap, double EPSILON,
            HdtDataSource hdtDataSource,
            Map<String, List<Integer>> mapMostFreqValue,
            Map<String, List<StarQuery>> mapMostFreqValueStar)
            throws CloneNotSupportedException, ExecutionException
    {
        StarQuery starQueryPrime = new StarQuery();
        List<String> starVariables = new ArrayList<>();
        StarQuery prevQuery = null;
        starVariables.addAll(starQueriesMap.keySet());
        Iterator starsIterator = starVariables.iterator();
        PrintStream dummyStream = new PrintStream(new OutputStream()
        {
            @Override
            public void write(int b)
            {
                // NO-OP
            }
        });
        System.setOut(dummyStream);

        StarQuery newStarQueryJoined = null;
        while (starsIterator.hasNext())
        {
            String starVariable = (String) starsIterator.next();
            StarQuery starQueryFirst = new StarQuery(
                    starQueriesMap.get(starVariable));
            starQueriesMap.remove(starVariable);
            Expr elasticStabilityLeft = Expr.valueOf(0);
            Sensitivity smoothSensitivityLeft = new Sensitivity(0.0,
                    elasticStabilityLeft);
            elasticStabilityLeft = x;
            Sensitivity sensitivity = new Sensitivity(1.0,
                    elasticStabilityLeft);
            starQueryLeft.setElasticStability(elasticStabilityLeft);
            // left side smooth sensitivity

            // update to new graph size calculation
            String construct = "CONSTRUCT WHERE { " + starQueryLeft.toString()
                    + "}";
            Query constructQuery = QueryFactory.create(construct);
            long graphSize = hdtDataSource.graphSizeCache.get(constructQuery);
            double DELTA = 1 / (Math.pow(graphSize, 2));
            double beta = EPSILON / (2 * Math.log(2 / DELTA));

            smoothSensitivityLeft = smoothElasticSensitivityStar(
                    elasticStabilityLeft, sensitivity, beta, k, graphSize);
            logger.info("star query (smooth) sensitivity: "
                    + smoothSensitivityLeft);
            starQueryLeft.setQuerySentitivity(smoothSensitivityLeft);

            Expr res = Expr.valueOf(0);

            // base case: i == 0 && bgpIt.hasNext()
            // Getting first and second star queries to calculate its differential privacy
            // this should be 1 since these are tables
            if (starsIterator.hasNext() && starQueryPrime.getTriples().isEmpty())
            {
                // getting sensitivity of a star
//                Expr elasticStabilityFirstStar = x;
                Expr elasticStabilityFirstStar = Expr.valueOf(0);
                starQueryFirst.setElasticStability(elasticStabilityFirstStar);
                Sensitivity smoothSensitivityFirstStar = new Sensitivity(1.0,
                        elasticStabilityFirstStar);
                starQueryFirst.setQuerySentitivity(smoothSensitivityFirstStar);

                // second star query in the map
                starVariable = (String) starsIterator.next();
                StarQuery starPrime = new StarQuery(starQueriesMap.get(starVariable));
                starQueriesMap.remove(starVariable);
//                Expr elasticStabilityPrime = x;
                Expr elasticStabilityPrime = Expr.valueOf(0);
                Sensitivity smoothSensitivityPrime = new Sensitivity(1.0,
                        elasticStabilityPrime);
                starPrime.setQuerySentitivity(smoothSensitivityPrime);
                starPrime.setElasticStability(elasticStabilityPrime);

                // now we join
                newStarQueryJoined = calculateJoinSensitivity(starQueryFirst, starPrime, hdtDataSource, null);
                prevQuery = newStarQueryJoined;
            } else
            {
                // we only take into account one join variable
                // max(mf_k(a,r_1,x)S_R(r_2,x), mf_k(b,r_2,x)S_R(r_1,x))

                Expr mostFreqValueLeft = maxFreq(joinVariables.get(0),
                        starQueryLeft, hdtDataSource);
                logger.info("mostFreqValueLeft: " + mostFreqValueLeft);
                Expr mostFreqValueRight = maxFreq(joinVariables.get(0),
                        starPrime, hdtDataSource);
                logger.info("mostFreqValueRight: " + mostFreqValueRight);
                Func f1 = new Func("f1", mostFreqValueLeft.multiply(
                        starPrime.getQuerySentitivity().getSensitivity()));

                Func f2 = new Func("f2", mostFreqValueRight.multiply(
                        starQueryLeft.getQuerySentitivity().getSensitivity()));
                // BytecodeFunc func2 = f2.toBytecodeFunc();

                double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
                double f2Val = Math.round(f2.toBytecodeFunc().apply(1));
                if (f1Val > f2Val)
                {
                    res = f1;
                } else
                {
                    res = f2;
                }

                if (!secondTime)
                {
                    secondTime = true;
                } else
                {
                    mapMostFreqValue.get(joinVariables.get(0));
                    mapMostFreqValueStar.get(joinVariables.get(0));

                }
                // I generate new starQueryPrime
                // starPrime = new StarQuery(starQueryRight.getTriples());
                starPrime.addStarQuery(starQueryLeft.getTriples());
                starPrime.setElasticStability(res);
            } else
            {
                starPrime.addStarQuery(starQueryLeft.getTriples());
                starPrime.setElasticStability(
                        starQueryLeft.getElasticStability());
                Expr elasticStabilityFirstStar = Expr.valueOf(0);
                starQueryFirst.setElasticStability(elasticStabilityFirstStar);
                Sensitivity smoothSensitivityFirstStar = new Sensitivity(1.0,
                        elasticStabilityFirstStar);
                starQueryFirst.setQuerySentitivity(smoothSensitivityFirstStar);
                prevQuery.setMaxFrequency(new MaxFreqValue(prevQuery.getMaxFrequency().getStarQueryLeft(), new Pair<>(starQueryFirst, "")));
                newStarQueryJoined = calculateJoinSensitivity(starQueryFirst, newStarQueryJoined, hdtDataSource, prevQuery);
                prevQuery = newStarQueryJoined;
            }

        }
        return newStarQueryJoined.getElasticStability();

    }

    private static Expr mostPopularValue(String var, StarQuery starQuery,
            HdtDataSource hdtDataSource, StarQuery prevQueries)
            throws CloneNotSupportedException, ExecutionException
    {
        // base case: mf(a,r_1,x)
        Expr expr = x;
        expr = expr.plus(hdtDataSource.mostFrequenResultCache
                .get(new MaxFreqQuery(starQuery.toString(), var)));
        if (prevQueries != null)
        {
            StarQuery q1 = prevQueries.getMaxFrequency().getStarQueryLeft().getKey();
            StarQuery q2 = null;
            if (prevQueries.getMaxFrequency().getStarQueryRight() != null)
            {
                q2 = prevQueries.getMaxFrequency().getStarQueryRight().getKey();
            } else {
                q2 = starQuery;
            }
            List<String> joinVariables = q1.getVariables();
            joinVariables.retainAll(q2.getVariables());
            if (q1.getVariables().contains(var))
            {
                // mfk(var, q1, x)mfk(a3, q2, x) if a_1 \in r_1
                // mfk(var, q1, x)
                expr = Expr.valueOf(hdtDataSource.mostFrequenResultCache
                        .get(new MaxFreqQuery(q1.toString(), var)));
                // mfk(a3, q2, x)
                expr = expr.multiply(hdtDataSource.mostFrequenResultCache
                        .get(new MaxFreqQuery(q2.toString(), joinVariables.get(0))));
            } else
            {
                // mfk(var, q2, x)mfk(a2, q1, x) if a_1 \in r_2
                // mfk(var, q2, x)
                expr = Expr.valueOf(hdtDataSource.mostFrequenResultCache
                        .get(new MaxFreqQuery(q2.toString(), var)));
                // mfk(a2, q1, x)
                expr = expr.multiply(hdtDataSource.mostFrequenResultCache
                        .get(new MaxFreqQuery(q1.toString(), joinVariables.get(0))));
            }
        }
        return expr;
    }

    private static StarQuery calculateJoinSensitivity(StarQuery starQueryLeft, StarQuery starQueryRight, HdtDataSource hdtDataSource, StarQuery prevQuery) throws CloneNotSupportedException, ExecutionException
    {
        Expr res = Expr.valueOf(0);

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());

        Expr mostPopularValueLeft = mostPopularValue(joinVariables.get(0),
                starQueryLeft, hdtDataSource, prevQuery);
        logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
        Expr mostPopularValueRight = mostPopularValue(joinVariables.get(0),
                starQueryRight, hdtDataSource, prevQuery);
        logger.info("mostPopularValueRight: " + mostPopularValueRight);

        double sensitivityRight = starQueryRight.getQuerySentitivity().getSensitivity();
        double sensitivityLeft = starQueryLeft.getQuerySentitivity().getSensitivity();

        Func f1 = new Func("f1", mostPopularValueLeft.multiply(sensitivityRight));
        Func f2 = new Func("f2", mostPopularValueRight.multiply(sensitivityLeft));

        double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
        double f2Val = Math.round(f2.toBytecodeFunc().apply(1));
        if (f1Val > f2Val)
        {
            res = f1;
        } else
        {
            res = f2;

        }
        // I generate new starQueryPrime
        StarQuery newStarQueryPrime = new StarQuery(starQueryLeft.getTriples());
        newStarQueryPrime.addStarQuery(starQueryRight.getTriples());
        newStarQueryPrime.setElasticStability(res);

        MaxFreqValue mfValue = new MaxFreqValue(new Pair<StarQuery, String>(newStarQueryPrime, ""), null);
        newStarQueryPrime.setMaxFrequency(mfValue);

        return newStarQueryPrime;

    }

    public static Sensitivity smoothElasticSensitivity(Expr elasticSensitivity,
            double prevSensitivity, double beta, int k, long graphSize)
    {
        Sensitivity sensitivity = new Sensitivity(prevSensitivity,
                elasticSensitivity);
        PrintStream dummyStream = new PrintStream(new OutputStream()
        {
            @Override
            public void write(int b)
            {
                // NO-OP
            }
        });
        System.setOut(dummyStream);
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();
        int maxI = 0;
        for (int i = 0; i < graphSize; i++)
        {
            double kPrime = func1.apply(k);
            double smoothSensitivity = Math.exp(-k * beta) * kPrime;
            if (smoothSensitivity > prevSensitivity)
            {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
            k++;
        }
        sensitivity.setMaxK(maxI);
        sensitivity.setSensitivity(prevSensitivity);
        return sensitivity;
    }

    public static Sensitivity smoothElasticSensitivityStar(
            Expr elasticSensitivity, Sensitivity prevSensitivity, double beta,
            int k, long graphSize)
    {
        int maxI = 0;
        for (int i = 0; i < graphSize; i++)
        {
            Sensitivity smoothSensitivity = new Sensitivity(
                    Math.exp(-k * beta) * 1, elasticSensitivity);
            if (smoothSensitivity.getSensitivity() > prevSensitivity
                    .getSensitivity())
            {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
            k++;
        }
        Sensitivity sens = new Sensitivity(prevSensitivity.getSensitivity(),
                elasticSensitivity);
        sens.setMaxK(maxI);
        return sens;
    }

    /*
    * Calculates the Elastic Sensitivity for a list/map of star queries. The map 
    * contains the variable pointing to a star and the list within that map 
    * contains the triples in that star.
     */
    public static Expr calculateElasticSensitivityAtKCarlos(int k,
            Map<String, List<TriplePath>> starQueriesMap, double EPSILON,
            HdtDataSource hdtDataSource)
            throws CloneNotSupportedException, ExecutionException
    {
        StarQuery starPrime = new StarQuery();
        List<String> starVariables = new ArrayList<String>();
        starVariables.addAll(starQueriesMap.keySet());
        Iterator starsIterator = starVariables.iterator();
        PrintStream dummyStream = new PrintStream(new OutputStream()
        {
            public void write(int b)
            {
                // NO-OP
            }
        });

        System.setOut(dummyStream);
        boolean secondTime = false;
        while (starsIterator.hasNext())
        {
            String starVariable = (String) starsIterator.next();
            StarQuery starQueryLeft = new StarQuery(
                    starQueriesMap.get(starVariable));
            starQueriesMap.remove(starVariable);

            Sensitivity sensitivity = getElasticStability(starQueryLeft, EPSILON, hdtDataSource);

            Expr res = Expr.valueOf(0);

            // base case: i == 0 && bgpIt.hasNext()
            if (starsIterator.hasNext() && starPrime.getTriples().isEmpty())
            {
                starVariable = (String) starsIterator.next();
                starPrime = new StarQuery(starQueriesMap.get(starVariable));
                starQueriesMap.remove(starVariable);

                // must depend from the size of the database (the sensitivity)
                String construct = "CONSTRUCT WHERE { " + starPrime.toString() + "}";
                Query constructQuery = QueryFactory.create(construct);
                long graphSize = hdtDataSource.graphSizeCache.get(constructQuery);
                double DELTA = 1 / (Math.pow(graphSize, 2));
                double beta = EPSILON / (2 * Math.log(2 / DELTA));

                Expr elasticStabilityPrime = Expr.valueOf(0);
                Sensitivity smoothSensitivityPrime = new Sensitivity(0.0,
                        elasticStabilityPrime);
                elasticStabilityPrime = x;
                sensitivity = new Sensitivity(1.0, elasticStabilityPrime);
                smoothSensitivityPrime = smoothElasticSensitivityStar(
                        elasticStabilityPrime, sensitivity, beta, k, graphSize);
                logger.info("star query prime (smooth) sensitivity: "
                        + smoothSensitivityPrime.getSensitivity());
                starPrime.setQuerySentitivity(smoothSensitivityPrime);
                starPrime.setElasticStability(elasticStabilityPrime);
            }

            List<String> joinVariables = starQueryLeft.getVariables();
            joinVariables.retainAll(starPrime.getVariables());

            if (joinVariables.size() > 0)
            {
                // we only take into account one join variable
                // max(mf_k(a,r_1,x)S_R(r_2,x), mf_k(b,r_2,x)S_R(r_1,x))

                Expr mostFreqValueLeft = maxFreq(joinVariables.get(0),
                        starQueryLeft, hdtDataSource);
                logger.info("mostFreqValueLeft: " + mostFreqValueLeft);
                Expr mostFreqValueRight = maxFreq(joinVariables.get(0),
                        starPrime, hdtDataSource);
                logger.info("mostFreqValueRight: " + mostFreqValueRight);
                Func f1 = new Func("f1", mostFreqValueLeft.multiply(
                        starPrime.getQuerySentitivity().getSensitivity()));

                Func f2 = new Func("f2", mostFreqValueRight.multiply(
                        starQueryLeft.getQuerySentitivity().getSensitivity()));
                // BytecodeFunc func2 = f2.toBytecodeFunc();

                double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
                double f2Val = Math.round(f2.toBytecodeFunc().apply(1));
                if (f1Val > f2Val)
                {
                    res = f1;
                } else
                {
                    res = f2;
                }

                if (!secondTime)
                {
                    secondTime = true;
                } else
                {
                    mapMostFreqValue.get(joinVariables.get(0));
                    mapMostFreqValueStar.get(joinVariables.get(0));

                }
                // I generate new starQueryPrime
                // starPrime = new StarQuery(starQueryRight.getTriples());
                starPrime.addStarQuery(starQueryLeft.getTriples());
                starPrime.setElasticStability(res);
            } else
            {
                starPrime.addStarQuery(starQueryLeft.getTriples());
                starPrime.setElasticStability(
                        starQueryLeft.getElasticStability());
            }
        }
        return starPrime.getElasticStability();

    }

    private static Sensitivity getElasticStability(StarQuery starQueryLeft, double EPSILON, HdtDataSource hdtDataSource) throws ExecutionException
    {
        String construct = "CONSTRUCT WHERE { " + starQueryLeft.toString()
                + "}";
        Query constructQuery = QueryFactory.create(construct);
        long graphSize = hdtDataSource.graphSizeCache.get(constructQuery);
        double DELTA = 1 / (Math.pow(graphSize, 2));
        double beta = EPSILON / (2 * Math.log(2 / DELTA));
        Expr elasticStabilityLeft = Expr.valueOf(0);
        elasticStabilityLeft = x;
        starQueryLeft.setElasticStability(elasticStabilityLeft);
        // left side smooth sensitivity
        Sensitivity smoothSensitivityLeft = new Sensitivity(0.0,
                elasticStabilityLeft);
        Sensitivity sensitivity = new Sensitivity(1.0,
                elasticStabilityLeft);
        smoothSensitivityLeft = smoothElasticSensitivityStar(
                elasticStabilityLeft, sensitivity, beta, k, graphSize);
        logger.info("star query (smooth) sensitivity: "
                + smoothSensitivityLeft);
        starQueryLeft.setQuerySentitivity(smoothSensitivityLeft);
        return sensitivity;
    }
}
