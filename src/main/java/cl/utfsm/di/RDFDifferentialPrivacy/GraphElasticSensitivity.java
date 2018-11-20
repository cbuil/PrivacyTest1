package cl.utfsm.di.RDFDifferentialPrivacy;

import static symjava.symbolic.Symbol.x;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import cl.utfsm.di.RDFDifferentialPrivacy.symbolic.SmoothResult;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

public class GraphElasticSensitivity
{

    public static double calculateElasticSensitivityAtK(int k,
            ElementPathBlock element, double EPSILON)
            throws CloneNotSupportedException
    {
        ElementPathBlock bgpBlock = (ElementPathBlock) element;
        PathBlock pb = bgpBlock.getPattern();
        Iterator bgpIt = pb.getList().iterator();

        int i = 0;
        int elasticStability = 0;
        int mostFreqValue = 1;
        int res = 0;
        int res2 = 0;
        List<String> aux1 = new ArrayList<>();
        Join r1 = new Join();
        Join rprime = new Join();
        List<String> joinVariables = new ArrayList<String>();

        // set para guardar los ancestros
        HashSet<String> ancestors = new HashSet<String>();

        while (bgpIt.hasNext())
        {
            TriplePath triple = (TriplePath) bgpIt.next();
            if (i == 0 && bgpIt.hasNext())
            {
                // se agregan los primeros ancestros
                Helper.extractor(triple, ancestors);

                // se obtiene el siguiente triple, se agrega a la
                // lista de triples y se obtiene la maxfreq
                TriplePath auxtriple = (TriplePath) bgpIt.next();
                System.out.println(auxtriple.toString());

                // check para obtener la(s) variable(s) conecta(n)
                // los triples en el JOIN
                aux1 = Helper.triplePartExtractor(triple);
                aux1.remove(1);
                List<String> aux2 = Helper.triplePartExtractor(auxtriple);
                aux2.remove(1);

                if (aux2.contains(aux1.get(0)))
                {
                    joinVariables.add(aux1.get(0));
                    if (aux2.contains(aux1.get(1)))
                    {
                        joinVariables.add(aux1.get(1));
                    }
                }
                else if (aux2.contains(aux1.get(1)))
                {
                    joinVariables.add(aux1.get(1));
                }
                else
                {
                    System.out.println("Join key missing, query not accepted");
                    System.exit(0);
                }

                // se calcula la maxfreq de la(s) variables que
                // participa(n) en el JOIN
                if (joinVariables.size() > 1)
                {
                    // si son 2 variables participando en el join,
                    // se elige la que tenga la minima maxima
                    // frecuencia
                    res = k + Math.min(
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(1)));
                    res2 = k + Math.min(
                            HdtDataSource.getCountResults(auxtriple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(auxtriple,
                                    joinVariables.get(1)));
                }
                else
                {
                    res = k + HdtDataSource.getCountResults(triple,
                            joinVariables.get(0));
                    res2 = k + HdtDataSource.getCountResults(auxtriple,
                            joinVariables.get(0));
                }

                // Check para ver si existen ancestros en comun
                if (Helper.extractor(auxtriple, ancestors))
                {
                    // elasticStability = res * 1 + res2 * 1 + 1 * 1;
                    elasticStability = Math.max(res * 1, res2 * 1);

                    // se define el Join para ser utilizado en la
                    // siguiente iteracion
                    r1 = new Join(triple);
                    rprime = new Join("r1prime", joinVariables,
                            (HashSet) ancestors.clone(), r1, auxtriple);
                }
                else
                {
                    elasticStability = Math.max(res * 1, res2 * 1);

                    // se define el Join para ser utilizado en la
                    // siguiente iteracion
                    r1 = new Join(triple);
                    rprime = new Join("r1prime", joinVariables,
                            (HashSet) ancestors.clone(), r1, auxtriple);
                }
            }
            else
            {
                // check para ver con cual de las variables del
                // triple se esta haciendo el join
                // y calcular su maxfreq con el rprime
                // correspondiente
                aux1 = Helper.triplePartExtractor(triple);
                joinVariables = new ArrayList<String>();
                if (ancestors.contains(aux1.get(0))
                        && !ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(0));
                    mostFreqValue = maxFreq(aux1.get(0), rprime, k);
                }
                else if (!ancestors.contains(aux1.get(0))
                        && ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(2));
                    mostFreqValue = maxFreq(aux1.get(2), rprime, k);
                }
                else if (ancestors.contains(aux1.get(0))
                        && ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(0));
                    joinVariables.add(aux1.get(2));
                    mostFreqValue = Math.min(maxFreq(aux1.get(0), rprime, k),
                            maxFreq(aux1.get(2), rprime, k));
                }

                // Calculo de la maxfreq de la parte derecha del
                // join
                if (joinVariables.size() > 1)
                {
                    // si son 2 variables participando en el join,
                    // se elige la que tenga la minima maxima
                    // frecuencia
                    res = k + Math.min(
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(1)));
                }
                else if (joinVariables.size() == 1)
                {
                    res = k + HdtDataSource.getCountResults(triple,
                            joinVariables.get(0));
                }
                else
                {
                    System.out.println("Join key missing, query not accepted");
                    System.exit(0);
                }

                // Check para ver si existen ancestros en comun
                if (Helper.extractor(triple, ancestors))
                {
                    // elasticStability = mostFreqValue * 1
                    // + res * elasticStability + elasticStability * 1;
                    elasticStability = Math.max(res * 1, res2 * 1);
                    rprime = new Join("r" + String.valueOf(i + 1) + "prime",
                            joinVariables, (HashSet) ancestors.clone(), rprime,
                            triple);
                }
                else
                {
                    elasticStability = Math.max(mostFreqValue * 1,
                            res * elasticStability);
                    rprime = new Join("r" + String.valueOf(i + 1) + "prime",
                            joinVariables, (HashSet) ancestors.clone(), rprime,
                            triple);
                }
            }
            i++;
            System.out.println(triple.toString());
        }
        return elasticStability;
    }

    private static int maxFreq(String var, Join join, int k)
            throws CloneNotSupportedException
    {
        // Caso base
        if (join.triple != null)
        {
            return k + HdtDataSource.getCountResults(join.triple, var);
        }
        // Se revisa la cantidad de variables presentes en V'
        if (join.joinVariables.size() > 1)
        {
            // se crean dos join diferentes cada uno con una de las
            // joinVariables
            Join left = (Join) join.clone();
            left.joinVariables.remove(1);

            Join right = (Join) join.clone();
            right.joinVariables.remove(0);

            return k + Math.min(maxFreq(left.joinVariables.get(0), left, k),
                    maxFreq(right.joinVariables.get(0), right, k));
        }
        else
        {
            // Casos para ver a que lado del join pertenece la variable (a1 in
            // r1 || a1 in r2)
            if (join.ancestors.contains(var))
            {
                return (k + maxFreq(var, join.Left, k)) * HdtDataSource
                        .getCountResults(join.Right, join.joinVariables.get(0));
            }
            else
            {
                return HdtDataSource.getCountResults(join.Right, var) * (k
                        + maxFreq(join.joinVariables.get(0), join.Left, k));
            }
        }
    }

    public static Expr calculateElasticSensitivityAtK(ElementPathBlock element,
            double ePSILON, ElementPathBlock bgpBlock)
            throws CloneNotSupportedException
    {
        // distance
        // int k = 1;

        // int maxfreq = obtainMaxFreq(bgpIt, countVariable);
        int i = 0;
        Expr elasticStability = Expr.valueOf(0);
        Expr mostFreqValue = Expr.valueOf(1);
        Expr res = Expr.valueOf(0);
        Expr res2 = Expr.valueOf(0);
        List<String> aux1 = new ArrayList<>();
        Join r1 = new Join();
        Join rprime = new Join();
        List<String> joinVariables = new ArrayList<String>();

        PathBlock pb = bgpBlock.getPattern();
        Iterator bgpIt = pb.getList().iterator();

        // set para guardar los ancestros
        HashSet<String> ancestors = new HashSet<String>();

        while (bgpIt.hasNext())
        {
            TriplePath triple = (TriplePath) bgpIt.next();
            if (i == 0 && bgpIt.hasNext())
            {
                // se agregan los primeros ancestros
                Helper.extractor(triple, ancestors);

                // se obtiene el siguiente triple, se agrega a la
                // lista de triples y se obtiene la maxfreq
                TriplePath auxtriple = (TriplePath) bgpIt.next();
                // System.out.println(auxtriple.toString());

                // check para obtener la(s) variable(s) conecta(n)
                // los triples en el JOIN
                aux1 = Helper.triplePartExtractor(triple);
                aux1.remove(1);
                List<String> aux2 = Helper.triplePartExtractor(auxtriple);
                aux2.remove(1);

                if (aux2.contains(aux1.get(0)))
                {
                    joinVariables.add(aux1.get(0));
                    if (aux2.contains(aux1.get(1)))
                    {
                        joinVariables.add(aux1.get(1));
                    }
                }
                else if (aux2.contains(aux1.get(1)))
                {
                    joinVariables.add(aux1.get(1));
                }
                else
                {
                    System.out.println("Join key missing, query not accepted");
                    System.exit(0);
                }

                // se calcula la maxfreq de la(s) variables que
                // participa(n) en el JOIN
                if (joinVariables.size() > 1)
                {
                    // si son 2 variables participando en el join,
                    // se elige la que tenga la minima maxima
                    // frecuencia

                    res = x.plus(Math.min(
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(1))));
                    res2 = x.plus(Math.min(
                            HdtDataSource.getCountResults(auxtriple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(auxtriple,
                                    joinVariables.get(1))));

                    // res = k +
                    // Math.min(HdtDataSource.getCountResults(triple,
                    // joinVariables.get(0)),
                    // HdtDataSource.getCountResults(triple,
                    // joinVariables.get(1)));
                    // res2 = k +
                    // Math.min(HdtDataSource.getCountResults(auxtriple,
                    // joinVariables.get(0)),
                    // HdtDataSource.getCountResults(auxtriple,
                    // joinVariables.get(1)));
                }
                else
                {

                    // res = k +
                    // HdtDataSource.getCountResults(triple,
                    // joinVariables.get(0));
                    // res2 = k +
                    // HdtDataSource.getCountResults(auxtriple,
                    // joinVariables.get(0));

                    res = x.plus(HdtDataSource.getCountResults(triple,
                            joinVariables.get(0)));
                    res2 = x.plus(HdtDataSource.getCountResults(auxtriple,
                            joinVariables.get(0)));
                }

                // Check para ver si existen ancestros en comun
                if (Helper.extractor(auxtriple, ancestors))
                {
                    // elasticStability = res*1 + res2*1 + 1*1;
                    // elasticStability = res.plus(res2).plus(1);

                    Func f1 = new Func("f1", res);
                    BytecodeFunc func1 = f1.toBytecodeFunc();

                    Func f2 = new Func("f2", res2);
                    BytecodeFunc func2 = f2.toBytecodeFunc();

                    elasticStability = Expr
                            .valueOf(Math.max(func1.apply(1), func2.apply(1)));

                    // se define el Join para ser utilizado en la
                    // siguiente iteracion
                    r1 = new Join(triple);
                    rprime = new Join("r1prime", joinVariables,
                            (HashSet) ancestors.clone(), r1, auxtriple);
                }
                else
                {
                    // elasticStability = Math.max(res*1,res2*1);

                    // Para poder obtener el maximo se evaluara la
                    // funcion en una distancia de 0
                    Func f1 = new Func("f1", res);
                    BytecodeFunc func1 = f1.toBytecodeFunc();

                    Func f2 = new Func("f2", res2);
                    BytecodeFunc func2 = f2.toBytecodeFunc();

                    elasticStability = Expr
                            .valueOf(Math.max(func1.apply(1), func2.apply(1)));

                    // se define el Join para ser utilizado en la
                    // siguiente iteracion
                    r1 = new Join(triple);
                    rprime = new Join("r1prime", joinVariables,
                            (HashSet) ancestors.clone(), r1, auxtriple);
                }
            }
            else
            {
                // check para ver con cual de las variables del
                // triple se esta haciendo el join
                // y calcular su maxfreq con el rprime
                // correspondiente
                aux1 = Helper.triplePartExtractor(triple);
                joinVariables = new ArrayList<String>();
                if (ancestors.contains(aux1.get(0))
                        && !ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(0));
                    mostFreqValue = maxFreq(aux1.get(0), rprime);
                    // mostFreqValue = maxFreq(aux1.get(0), rprime,
                    // k);
                }
                else if (!ancestors.contains(aux1.get(0))
                        && ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(2));
                    mostFreqValue = maxFreq(aux1.get(2), rprime);
                    // mostFreqValue = maxFreq(aux1.get(2), rprime,
                    // k);
                }
                else if (ancestors.contains(aux1.get(0))
                        && ancestors.contains(aux1.get(2)))
                {
                    joinVariables.add(aux1.get(0));
                    joinVariables.add(aux1.get(2));

                    // Para poder obtener el minimo se evaluara la
                    // funcion en una distancia de 0
                    Func f1 = new Func("f1", maxFreq(aux1.get(0), rprime));
                    BytecodeFunc func1 = f1.toBytecodeFunc();

                    Func f2 = new Func("f2", maxFreq(aux1.get(2), rprime));
                    BytecodeFunc func2 = f2.toBytecodeFunc();

                    mostFreqValue = Expr
                            .valueOf(Math.min(Math.round(func1.apply(1)),
                                    Math.round(func2.apply(1))));
                    // mostFreqValue = Math.min(maxFreq(aux1.get(0),
                    // rprime, k), maxFreq(aux1.get(2), rprime, k));
                }

                // Calculo de la maxfreq de la parte derecha del
                // join
                if (joinVariables.size() > 1)
                {
                    // si son 2 variables participando en el join,
                    // se elige la que tenga la minima maxima
                    // frecuencia
                    // res = k +
                    // Math.min(HdtDataSource.getCountResults(triple,
                    // joinVariables.get(0)),
                    // HdtDataSource.getCountResults(triple,
                    // joinVariables.get(1)));
                    res = x.plus(Math.min(
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(0)),
                            HdtDataSource.getCountResults(triple,
                                    joinVariables.get(1))));
                }
                else if (joinVariables.size() == 1)
                {
                    res = x.plus(HdtDataSource.getCountResults(triple,
                            joinVariables.get(0)));
                }
                else
                {
                    System.out.println("Join key missing, query not accepted");
                    System.exit(0);
                }

                // Check para ver si existen ancestros en comun
                if (Helper.extractor(triple, ancestors))
                {
                    elasticStability = mostFreqValue
                            .plus(elasticStability.multiply(res))
                            .plus(elasticStability);
                    // elasticStability = mostFreqValue * 1 + res *
                    // elasticStability + elasticStability * 1;
                    rprime = new Join("r" + String.valueOf(i + 1) + "prime",
                            joinVariables, (HashSet) ancestors.clone(), rprime,
                            triple);
                }
                else
                {
                    Func f1 = new Func("f1", mostFreqValue);
                    BytecodeFunc func1 = f1.toBytecodeFunc();

                    Func f2 = new Func("f2", elasticStability.multiply(res));
                    BytecodeFunc func2 = f2.toBytecodeFunc();

                    elasticStability = Expr
                            .valueOf(Math.max(Math.round(func1.apply(1)),
                                    Math.round(func2.apply(1))));

                    // elasticStability = Math.max(mostFreqValue *
                    // 1, res * elasticStability);
                    rprime = new Join("r" + String.valueOf(i + 1) + "prime",
                            joinVariables, (HashSet) ancestors.clone(), rprime,
                            triple);
                }
            }
            i++;
            // System.out.println(triple.toString());
        }
        return elasticStability;
    }

    public static SmoothResult smoothElasticSensitivity(Expr elasticSensitivity,
            double prevSensitivity, double beta, int k,
            ElementPathBlock bgpBlock)
    {
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double elasticSensitivityAtK = func1.apply(k);

        // System.out.println("k: " + k);
        double smoothSensitivity = Math.exp(-k * beta) * elasticSensitivityAtK;
        System.out.println("prev sensitivity: " + prevSensitivity
                + " smoothSensitivity: " + smoothSensitivity);

        // it is unnecessary to caculate more than 1000 since utility at that distance is 0 
        if (k == 500)
        {
            return new SmoothResult(prevSensitivity, k);
        }
        if (elasticSensitivityAtK == 0 || (smoothSensitivity < prevSensitivity))
        {
            return new SmoothResult(prevSensitivity, k);
        }
        else
        {
            return smoothElasticSensitivity(elasticSensitivity,
                    smoothSensitivity, beta, k + 1, bgpBlock);
        }
    }

    private static Expr maxFreq(String var, Join join)
            throws CloneNotSupportedException
    {
        // Caso base
        if (join.triple != null)
        {
            Expr expr = x;
            expr = expr.plus(HdtDataSource.getCountResults(join.triple, var));
            return expr;
            // return k + HdtDataSource.getCountResults(join.triple, var);
        }
        // Se revisa la cantidad de variables presentes en V'
        if (join.joinVariables.size() > 1)
        {
            // se crean dos join diferentes cada uno con una de las
            // joinVariables
            Join left = (Join) join.clone();
            left.joinVariables.remove(1);

            Join right = (Join) join.clone();
            right.joinVariables.remove(0);

            // Para poder obtener el minimo se evaluara la funcion en una
            // distancia de 0
            Func f1 = new Func("f1", maxFreq(left.joinVariables.get(0), left));
            BytecodeFunc func1 = f1.toBytecodeFunc();

            Func f2 = new Func("f2",
                    maxFreq(right.joinVariables.get(0), right));
            BytecodeFunc func2 = f2.toBytecodeFunc();

            return x.plus(Math.min(Math.round(func1.apply(1)),
                    Math.round(func2.apply(1))));
            // return k + Math.min(maxFreq(left.joinVariables.get(0), left,
            // k),maxFreq(right.joinVariables.get(0), right, k));
        }
        else
        {
            // Casos para ver a que lado del join pertenece la variable (a1 in
            // r1 || a1 in r2)
            if (join.ancestors.contains(var))
            {
                return x.plus(maxFreq(var, join.Left))
                        .multiply(HdtDataSource.getCountResults(join.Right,
                                join.joinVariables.get(0)));
                // return (k + maxFreq(var, join.Left, k)) *
                // HdtDataSource.getCountResults(join.Right ,
                // join.joinVariables.get(0));
            }
            else
            {
                return x.plus(maxFreq(join.joinVariables.get(0), join.Left))
                        .multiply(
                                HdtDataSource.getCountResults(join.Right, var));
                // return HdtDataSource.getCountResults(join.Right, var) * (k +
                // maxFreq(join.joinVariables.get(0), join.Left, k));
            }
        }
    }
}
