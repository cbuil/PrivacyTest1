package cl.utfsm.di.RDFDifferentialPrivacy;

import org.apache.commons.cli.*;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;

import static symjava.symbolic.Symbol.*;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Run
{
    public static void main(String[] args) throws IOException, CloneNotSupportedException
    {
        // create Options object
        Options options = new Options();

        // add q query option
        options.addOption("q", "query", true, "input SPARQL query");
        // add q query file option
        options.addOption("f", "qFile", true, "input SPARQL query File");
        // add e endpoint option
        options.addOption("e", "endpoint", true, "SPARQL endpoint");
        // add v COUNT variable option
        options.addOption("v", "var", true, "COUNT variable");

        CommandLineParser parser = new DefaultParser();
        try
        {
            String localEndpoint = "";
            String queryString = "";
            String queryFile = "";
            String countVariable = "";

            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q"))
            {
                queryString = cmd.getOptionValue("q");
            }
            else
            {
                System.out.println("Missing SPARQL query ");
            }
            if (cmd.hasOption("f"))
            {
                queryFile = cmd.getOptionValue("f");
                queryString = new Scanner(new File(queryFile))
                        .useDelimiter("\\Z").next();
                // transform into Jena Query object
            }
            else
            {
                System.out.println("Missing SPARQL query file");
            }
            if (cmd.hasOption("e"))
            {
                localEndpoint = cmd.getOptionValue("e");
            }
            else
            {
                System.out.println("Missing SPARQL endpoint ");
            }
            if (cmd.hasOption("v"))
            {
                countVariable = cmd.getOptionValue("v");
            }
            else
            {
                System.out.println("Missing COUNT variable");
            }

            HdtDataSource hdtDataSource = new HdtDataSource("resources/watdiv.10M.hdt");
            Query q = QueryFactory.create(queryString);

            //se obtiene el encabezado de la query
            //String queryHead = "SELECT " + countVariable +"\nWHERE\n";
            //String queryHead = q.toString();
            //String[] splitedQuery = queryHead.split("\\{");
            //queryHead = splitedQuery[0];

            ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
            List<Element> elementList = queryPattern.getElements();
            for (Element element : elementList)
            {
                if (element instanceof ElementTriplesBlock)
                {
                    ElementTriplesBlock triplesBlock = (ElementTriplesBlock) element;
                    BasicPattern bgp = triplesBlock.getPattern();
                    Iterator bgpIt = bgp.iterator();
                    while (bgpIt.hasNext())
                    {
                        Triple triple = (Triple) bgpIt.next();
                        System.out.println(triple.toString());
                    }
                }
                if (element instanceof ElementPathBlock)
                {
                    ElementPathBlock bgpBlock = (ElementPathBlock) element;
                    PathBlock pb = bgpBlock.getPattern();
                    Iterator bgpIt = pb.getList().iterator();

                    // delta parameter: use 1/n^2, with n = 100000
                    double DELTA = 1 / (Math.pow(10000000,2));

                    // privacy budget
                    double EPSILON = 0.1;

                    // distance
                    //int k = 1;


                    //int maxfreq = obtainMaxFreq(bgpIt, countVariable);
                    int i = 0;
                    Expr elasticStability = Expr.valueOf(0);
                    Expr mostFreqValue = Expr.valueOf(1);
                    Expr res = Expr.valueOf(0);
                    Expr res2 = Expr.valueOf(0);
                    List<String> aux1 = new ArrayList<>();
                    Join r1 = new Join();
                    Join rprime = new Join();
                    List<String> joinVariables = new ArrayList<String>();

                    //set para guardar los ancestros
                    HashSet<String> ancestors = new HashSet<String>();



                    while (bgpIt.hasNext())
                    {
                        TriplePath triple = (TriplePath) bgpIt.next();
                        if(i==0 && bgpIt.hasNext()){
                            //se agregan los primeros ancestros
                            Helper.extractor(triple,ancestors);

                            // se obtiene el siguiente triple, se agrega a la lista de triples y se obtiene la maxfreq
                            TriplePath auxtriple = (TriplePath) bgpIt.next();
                            System.out.println(auxtriple.toString());


                            // check para obtener la(s) variable(s) conecta(n) los triples en el JOIN
                            aux1 = Helper.triplePartExtractor(triple);
                            aux1.remove(1);
                            List<String> aux2 = Helper.triplePartExtractor(auxtriple);
                            aux2.remove(1);

                            if(aux2.contains(aux1.get(0))){
                                joinVariables.add(aux1.get(0));
                                if(aux2.contains(aux1.get(1))){
                                    joinVariables.add(aux1.get(1));
                                }
                            }
                            else if(aux2.contains(aux1.get(1))){
                                joinVariables.add(aux1.get(1));
                            }
                            else{
                                System.out.println("Join key missing, query not accepted");
                                System.exit(0);
                            }

                            //se calcula la maxfreq de la(s) variables que participa(n) en el JOIN
                            if(joinVariables.size()>1){
                                // si son 2 variables participando en el join, se elige la que tenga la minima maxima frecuencia

                                res = x.plus(Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1))));
                                res2 = x.plus(Math.min(HdtDataSource.getCountResults(auxtriple, joinVariables.get(0)), HdtDataSource.getCountResults(auxtriple, joinVariables.get(1))));

                                //res = k + Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1)));
                                //res2 = k + Math.min(HdtDataSource.getCountResults(auxtriple, joinVariables.get(0)), HdtDataSource.getCountResults(auxtriple, joinVariables.get(1)));
                            }
                            else{

                                //res = k + HdtDataSource.getCountResults(triple, joinVariables.get(0));
                                //res2 = k + HdtDataSource.getCountResults(auxtriple, joinVariables.get(0));

                                res = x.plus(HdtDataSource.getCountResults(triple, joinVariables.get(0)));
                                res2 = x.plus(HdtDataSource.getCountResults(auxtriple, joinVariables.get(0)));
                            }


                            //Check para ver si existen ancestros en comun
                            if(Helper.extractor(auxtriple,ancestors)){
                                //elasticStability = res*1 + res2*1 + 1*1;
                                elasticStability = res.plus(res2).plus(1);

                                //se define el Join para ser utilizado en la siguiente iteracion
                                r1 = new Join(triple);
                                rprime = new Join("r1prime",joinVariables, (HashSet)ancestors.clone(),r1,auxtriple);
                            }
                            else{
                                //elasticStability = Math.max(res*1,res2*1);

                                //Para poder obtener el maximo se evaluara la funcion en una distancia de 0
                                Func f1 = new Func("f1", res);
                                BytecodeFunc func1 = f1.toBytecodeFunc();

                                Func f2 = new Func("f2", res2);
                                BytecodeFunc func2 = f2.toBytecodeFunc();

                                elasticStability = Expr.valueOf(Math.max(func1.apply(1),func2.apply(1)));


                                //se define el Join para ser utilizado en la siguiente iteracion
                                r1 = new Join(triple);
                                rprime = new Join("r1prime",joinVariables, (HashSet)ancestors.clone(),r1,auxtriple);
                            }
                        }
                        else{
                            // check para ver con cual de las variables del triple se esta haciendo el join
                            // y calcular su maxfreq con el rprime correspondiente
                            aux1 = Helper.triplePartExtractor(triple);
                            joinVariables = new ArrayList<String>();
                            if (ancestors.contains(aux1.get(0)) && !ancestors.contains(aux1.get(2))) {
                                joinVariables.add(aux1.get(0));
                                mostFreqValue = maxFreq(aux1.get(0), rprime);
                                //mostFreqValue = maxFreq(aux1.get(0), rprime, k);
                            } else if (!ancestors.contains(aux1.get(0)) && ancestors.contains(aux1.get(2))) {
                                joinVariables.add(aux1.get(2));
                                mostFreqValue = maxFreq(aux1.get(2), rprime);
                                //mostFreqValue = maxFreq(aux1.get(2), rprime, k);
                            } else if (ancestors.contains(aux1.get(0)) && ancestors.contains(aux1.get(2))) {
                                joinVariables.add(aux1.get(0));
                                joinVariables.add(aux1.get(2));


                                //Para poder obtener el minimo se evaluara la funcion en una distancia de 0
                                Func f1 = new Func("f1", maxFreq(aux1.get(0), rprime));
                                BytecodeFunc func1 = f1.toBytecodeFunc();

                                Func f2 = new Func("f2", maxFreq(aux1.get(2), rprime));
                                BytecodeFunc func2 = f2.toBytecodeFunc();

                                mostFreqValue = Expr.valueOf(Math.min(Math.round(func1.apply(1)),Math.round(func2.apply(1))));
                                //mostFreqValue = Math.min(maxFreq(aux1.get(0), rprime, k), maxFreq(aux1.get(2), rprime, k));
                            }

                            //Calculo de la maxfreq de la parte derecha del join
                            if(joinVariables.size()>1){
                                // si son 2 variables participando en el join, se elige la que tenga la minima maxima frecuencia
                                //res = k + Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1)));
                                res = x.plus(Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1))));
                            }
                            else if(joinVariables.size()==1){
                                //res = k + HdtDataSource.getCountResults(triple, joinVariables.get(0));
                                res = x.plus(HdtDataSource.getCountResults(triple, joinVariables.get(0)));
                            }
                            else{
                                System.out.println("Join key missing, query not accepted");
                                System.exit(0);
                            }

                            //Check para ver si existen ancestros en comun
                            if(Helper.extractor(triple,ancestors)){
                                elasticStability = mostFreqValue.plus(elasticStability.multiply(res)).plus(elasticStability);
                               //elasticStability = mostFreqValue * 1 + res * elasticStability + elasticStability * 1;
                                rprime = new Join("r"+String.valueOf(i+1)+"prime",joinVariables,(HashSet)ancestors.clone(), rprime, triple);
                            }
                            else {
                                Func f1 = new Func("f1", mostFreqValue);
                                BytecodeFunc func1 = f1.toBytecodeFunc();

                                Func f2 = new Func("f2", elasticStability.multiply(res));
                                BytecodeFunc func2 = f2.toBytecodeFunc();

                                elasticStability = Expr.valueOf(Math.max(Math.round(func1.apply(1)),Math.round(func2.apply(1))));

                                //elasticStability = Math.max(mostFreqValue * 1, res * elasticStability);
                                rprime = new Join("r"+String.valueOf(i+1)+"prime",joinVariables,(HashSet)ancestors.clone(), rprime, triple);
                            }
                        }
                        i++;
                        System.out.println(triple.toString());
                    }




                    double beta = EPSILON / (2 * Math.log(2 / DELTA));
                    //double smoothSensitivity = Math.exp(-1 * beta * k) * elasticStability;

                    Func f = new Func("f", elasticStability);
                    BytecodeFunc func = f.toBytecodeFunc();
                    System.out.println("Elastic Stability: "+ Math.round(func.apply(2)));

                    double smoothSensitivity = smoothElasticSensitivity(elasticStability,0,beta,0);
                    System.out.println(smoothSensitivity);

                    //Se agrega el ruido con Laplace
                    double scale = 2 * smoothSensitivity / EPSILON;
                    Random random = new Random();
                    double u = 0.5 - random.nextDouble();
                    LaplaceDistribution l = new LaplaceDistribution(u,scale);
                    //double finalResult = -Math.signum(u) * scale * Math.log(1 - 2*Math.abs(u));

                    Query query = QueryFactory.create(queryString);
                    ResultSet results = HdtDataSource.ExcecuteQuery(query);
                    QuerySolution soln = results.nextSolution();
                    RDFNode x = soln.get(soln.varNames().next());
                    int result = x.asLiteral().getInt();

                    //double finalResult1 = result + finalResult;
                    double finalResult2 = result + l.sample();

                    System.out.println("Original result: "+ result);
                    //System.out.println("Private Result: "+ Math.round(finalResult1));
                    System.out.println("Private Result: "+ Math.round(finalResult2));




                }
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }


    private static Expr maxFreq(String var, Join join) throws CloneNotSupportedException{
        // Caso base
        if(join.triple!=null){
            Expr expr = x;
            expr = expr.plus(HdtDataSource.getCountResults(join.triple, var));
            return expr;
            //return k + HdtDataSource.getCountResults(join.triple, var);
        }
        // Se revisa la cantidad de variables presentes en V'
        if(join.joinVariables.size()>1){
            //se crean dos join diferentes cada uno con una de las joinVariables
            Join left = (Join)join.clone();
            left.joinVariables.remove(1);

            Join right= (Join)join.clone();
            right.joinVariables.remove(0);

            //Para poder obtener el minimo se evaluara la funcion en una distancia de 0
            Func f1 = new Func("f1", maxFreq(left.joinVariables.get(0), left));
            BytecodeFunc func1 = f1.toBytecodeFunc();

            Func f2 = new Func("f2", maxFreq(right.joinVariables.get(0), right));
            BytecodeFunc func2 = f2.toBytecodeFunc();


            return x.plus(Math.min(Math.round(func1.apply(1)),Math.round(func2.apply(1))));
            //return k + Math.min(maxFreq(left.joinVariables.get(0), left, k),maxFreq(right.joinVariables.get(0), right, k));
        }
        else{
            // Casos para ver a que lado del join pertenece la variable  (a1 in r1 || a1 in r2)
            if(join.ancestors.contains(var)){
                return x.plus(maxFreq(var, join.Left)).multiply(HdtDataSource.getCountResults(join.Right , join.joinVariables.get(0)));
                //return (k + maxFreq(var, join.Left, k)) * HdtDataSource.getCountResults(join.Right , join.joinVariables.get(0));
            }
            else{
                return x.plus(maxFreq(join.joinVariables.get(0), join.Left)).multiply(HdtDataSource.getCountResults(join.Right, var));
                //return HdtDataSource.getCountResults(join.Right, var) * (k + maxFreq(join.joinVariables.get(0), join.Left, k));
            }
        }
    }

    private static double smoothElasticSensitivity(Expr elasticSensitivity, double prevSensitivity, double beta, int k){
        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);


        if ( func1.apply(0) == 0 || (smoothSensitivity < prevSensitivity)){
            return prevSensitivity;
        }
        else{
            return smoothElasticSensitivity(elasticSensitivity, smoothSensitivity,beta,k+1);
        }
    }



}

class Join implements Cloneable{
    String name;
    List<String> joinVariables;          // Variables que se ocupan para hacer el Join
    HashSet<String> ancestors;           // Variables que componen al lado izquierdo del join (otro join)
    //HashSet<String> newVariables;        // Variables que componen al Triple que con el que se esta haciendo JOIN
    Join Left;                           // El join izquierdo con el que se esta haciendo JOIN
    TriplePath Right;                    // El triple con el que se esta haciendo JOIN
    TriplePath triple;        // Para el caso base en caso de que el primer join contenga solo triples

    Join(String n, List<String> j, HashSet<String> a,  Join L, TriplePath R){
        name = n;
        joinVariables = j;
        ancestors = a;
        //newVariables = n;
        Left = L;
        Right = R;
    }

    Join(TriplePath l){
        triple = l;
    }
    Join(){}

    @Override
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }
}
