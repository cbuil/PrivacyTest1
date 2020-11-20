package cl.utfsm.di.RDFDifferentialPrivacy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static symjava.symbolic.Symbol.x;

public class GraphElasticSensitivity {

    private static final Logger logger = LogManager
            .getLogger(GraphElasticSensitivity.class.getName());

    public static double setOfMappingsSensitivity(Expr elasticSensitivity,
                                                  double prevSensitivity, double beta, int k) {
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);

        if (func1.apply(0) == 0 || (smoothSensitivity < prevSensitivity)) {
            return prevSensitivity;
        } else {
            return setOfMappingsSensitivity(elasticSensitivity,
                    smoothSensitivity, beta, k + 1);
        }
    }

    public static StarQuery calculateSensitivity(int k,
                                                 List<StarQuery> listStars, double EPSILON,
                                                 DataSource dataSource) throws ExecutionException, CloneNotSupportedException {
        PrintStream dummyStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // NO-OP
            }
        });
        StarQuery starQueryFirst = Collections.max(listStars);
        listStars.remove(starQueryFirst);

        if (listStars.size() > 1) {
            // S_G(star1, G)
            Expr elasticStabilityFirstStar = Expr.valueOf(1);
            starQueryFirst.setElasticStability(elasticStabilityFirstStar);
            StarQuery starQuerySecond = calculateSensitivity(k, listStars, EPSILON, dataSource);
            // S_G(star2, G)
            return calculateJoinSensitivity(starQueryFirst, starQuerySecond, dataSource);
        } else {
            // calculate sensibility for starQuery
            Expr elasticStabilityFirstStar = Expr.valueOf(1);
            starQueryFirst.setElasticStability(elasticStabilityFirstStar);

            // second star query in the map
            StarQuery starQuerySecond = Collections.max(listStars);
            listStars.remove(starQuerySecond);
//                Expr elasticStabilityPrime = x;
            Expr elasticStabilityPrime = Expr.valueOf(1);
            starQuerySecond.setElasticStability(elasticStabilityPrime);
            // now we join
            return calculateJoinSensitivity(starQueryFirst, starQuerySecond, dataSource);

        }
    }

    private static StarQuery calculateJoinSensitivity(StarQuery starQueryLeft, StarQuery starQueryRight, DataSource hdtDataSource) throws CloneNotSupportedException, ExecutionException {
        Expr res;

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());
        Expr mostPopularValueLeft;
        Expr mostPopularValueRight;
        if (starQueryLeft.getMostPopularValue() == null) {
            mostPopularValueLeft = mostPopularValue(joinVariables.get(0),
                    starQueryLeft, hdtDataSource);
            logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
            starQueryLeft.setMostPopularValue(mostPopularValueLeft);
        } else {
            mostPopularValueLeft = starQueryLeft.getMostPopularValue();
        }
        if (starQueryRight.getMostPopularValue() == null) {
            mostPopularValueRight = mostPopularValue(joinVariables.get(0),
                    starQueryRight, hdtDataSource);
            logger.info("mostPopularValueRight: " + mostPopularValueRight);
            starQueryRight.setMostPopularValue(mostPopularValueRight);
        } else {
            mostPopularValueRight = starQueryRight.getMostPopularValue();
        }
        Expr stabilityRight = starQueryRight.getElasticStability();
        Expr stabilityLeft = starQueryLeft.getElasticStability();

        // new stability
        Func f1 = new Func("f1", mostPopularValueRight.multiply(stabilityLeft));
        Func f2 = new Func("f2", mostPopularValueLeft.multiply(stabilityRight));

        // I generate new starQueryPrime
        StarQuery newStarQueryPrime = new StarQuery(starQueryLeft.getTriples());
        newStarQueryPrime.addStarQuery(starQueryRight.getTriples());

        double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
        double f2Val = Math.round(f2.toBytecodeFunc().apply(1));
        if (f1Val > f2Val) {
            newStarQueryPrime.setElasticStability(f1);
            newStarQueryPrime.setMostPopularValue(mostPopularValueRight);
        } else {
            newStarQueryPrime.setElasticStability(f2);
            newStarQueryPrime.setMostPopularValue(mostPopularValueLeft);
        }

        return newStarQueryPrime;

    }

    /*
     * mostPopularValue(joinVariable a, StarQuery starQuery, DataSource)
     */
    private static Expr mostPopularValue(String var, StarQuery starQuery,
                                         DataSource dataSource) {
        // base case: mp(a,s_1,G)
        Expr expr = x;
        expr = expr.plus(dataSource.mostFrequenResult(new MaxFreqQuery(starQuery.toString(), var)));
        return expr;
    }


    public static Sensitivity smoothElasticSensitivity(Expr elasticSensitivity,
                                                       double prevSensitivity, double beta, int k, long graphSize) {
        Sensitivity sensitivity = new Sensitivity(prevSensitivity,
                elasticSensitivity);
        PrintStream dummyStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(dummyStream);
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();
        int maxI = 0;
        for (int i = 0; i < graphSize; i++) {
            double kPrime = func1.apply(k);
            double smoothSensitivity = Math.exp(-k * beta) * kPrime;
            if (smoothSensitivity > prevSensitivity) {
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
            int k, long graphSize) {
        int maxI = 0;
        for (int i = 0; i < graphSize; i++) {
            Sensitivity smoothSensitivity = new Sensitivity(
                    Math.exp(-k * beta) * 1, elasticSensitivity);
            if (smoothSensitivity.getSensitivity() > prevSensitivity
                    .getSensitivity()) {
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
}
