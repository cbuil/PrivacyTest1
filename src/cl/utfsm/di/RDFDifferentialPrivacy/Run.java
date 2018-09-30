package cl.utfsm.di.RDFDifferentialPrivacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.math.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.jena.query.*;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
                    double DELTA = 1 / (Math.pow(100000,2));

                    // privacy budget
                    double EPSILON = 0.1;


                    //int maxfreq = obtainMaxFreq(bgpIt, countVariable);
                    int i = 0;
                    int elasticStability = 0;
                    int mostFreqValue = 1;
                    int res = 0;
                    int res2 = 0;
                    List<String> aux1 = new ArrayList<>();
                    Join r1 = new Join();
                    Join rprime = new Join();
                    List<String> joinVariables = new ArrayList<String>();

                    //set para guardar los ancestros
                    HashSet<String> ancestors = new HashSet<String>();



                    while (bgpIt.hasNext())
                    {
/* Metodo antiguo para realizar el JOIN y calcular la maxima frecuencia en la tabla resultante

                            String finalQuery = queryCreator(triples, queryHead);
                            //set resultante de la ejecucion de la query de los triples hasta ahora
                            ResultSet results = HdtDataSource.ExcecuteQuery(QueryFactory.create(finalQuery));

                            HashMap<String, Integer> hmap = new HashMap<String, Integer>();
                            for (; results.hasNext(); ) {
                                QuerySolution soln = results.nextSolution();
                                String sol = soln.get(countVariable).toString();
                                if (hmap.containsKey(sol)) {
                                    hmap.put(sol, hmap.get(sol) + 1);
                                } else {
                                    hmap.put(sol, 1);
                                }
                            }
                            mostFreqValue = getMaxFreq(hmap);
*/

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

                            //se calcula la maxfreq de la(s) variables que participa(n) en el JOIN
                            if(joinVariables.size()>1){
                                // si son 2 variables participando en el join, se elige la que tenga la minima maxima frecuencia
                                res = Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1)));
                                res2 = Math.min(HdtDataSource.getCountResults(auxtriple, joinVariables.get(0)), HdtDataSource.getCountResults(auxtriple, joinVariables.get(1)));
                            }
                            else{
                                res = HdtDataSource.getCountResults(triple, joinVariables.get(0));
                                res2 = HdtDataSource.getCountResults(auxtriple, joinVariables.get(0));
                            }


                            //Check para ver si existen ancestros en comun
                            if(Helper.extractor(auxtriple,ancestors)){
                                elasticStability = res*1 + res2*1 + 1*1;

                                //se define el Join para ser utilizado en la siguiente iteracion
                                r1 = new Join(triple);
                                rprime = new Join("r1prime",joinVariables, (HashSet)ancestors.clone(),r1,auxtriple);
                            }
                            else{
                                elasticStability = Math.max(res*1,res2*1);

                                //se define el Join para ser utilizado en la siguiente iteracion
                                r1 = new Join(triple);
                                rprime = new Join("r1prime",joinVariables, (HashSet)ancestors.clone(),r1,auxtriple);
                            }
                        }
                        else{
                            // check para ver con cual de las variables del triple se esta haciendo el join
                            // y calcular su maxfreq con el rprime correspondiente
                            aux1 = Helper.triplePartExtractor(triple);
                            if (ancestors.contains(aux1.get(0)) && !ancestors.contains(aux1.get(2))) {
                                joinVariables = new ArrayList<String>();
                                joinVariables.add(aux1.get(0));
                                mostFreqValue = maxFreq(aux1.get(0), rprime);
                            } else if (!ancestors.contains(aux1.get(0)) && ancestors.contains(aux1.get(2))) {
                                joinVariables = new ArrayList<String>();
                                joinVariables.add(aux1.get(2));
                                mostFreqValue = maxFreq(aux1.get(2), rprime);
                            } else if (ancestors.contains(aux1.get(0)) && ancestors.contains(aux1.get(2))) {
                                joinVariables = new ArrayList<String>();
                                joinVariables.add(aux1.get(0));
                                joinVariables.add(aux1.get(2));
                                mostFreqValue = Math.min(maxFreq(aux1.get(0), rprime), maxFreq(aux1.get(2), rprime));
                            }

                            //Calculo de la maxfreq de la parte derecha del join
                            if(joinVariables.size()>1){
                                // si son 2 variables participando en el join, se elige la que tenga la minima maxima frecuencia
                                res = Math.min(HdtDataSource.getCountResults(triple, joinVariables.get(0)), HdtDataSource.getCountResults(triple, joinVariables.get(1)));
                            }
                            else{
                                res = HdtDataSource.getCountResults(triple, joinVariables.get(0));
                            }

                            //Check para ver si existen ancestros en comun
                            if(Helper.extractor(triple,ancestors)){
                                elasticStability = mostFreqValue * 1 + res * elasticStability + elasticStability * 1;
                                rprime = new Join("r"+String.valueOf(i+1)+"prime",joinVariables,(HashSet)ancestors.clone(), rprime, triple);
                            }
                            else {
                                elasticStability = Math.max(mostFreqValue * 1, res * elasticStability);
                                rprime = new Join("r"+String.valueOf(i+1)+"prime",joinVariables,(HashSet)ancestors.clone(), rprime, triple);
                            }
                        }
                        i++;
                        System.out.println(triple.toString());
                    }


                    System.out.println("Elastic Stability: "+ elasticStability);

                    double beta = EPSILON / (2 * Math.log(2 / DELTA));
                    double smoothSensitivity = Math.exp(-1 * beta) * elasticStability;

                    //Se agrega el ruido con Laplace
                    double scale = 2 * smoothSensitivity / EPSILON;
                    Random random = new Random();
                    double u = 0.5 - random.nextDouble();
                    double finalResult = -Math.signum(u) * scale * Math.log(1 - 2*Math.abs(u));

                    Query query = QueryFactory.create(queryString);
                    ResultSet results = HdtDataSource.ExcecuteQuery(query);
                    QuerySolution soln = results.nextSolution();
                    RDFNode x = soln.get(soln.varNames().next());
                    int result = x.asLiteral().getInt();
                    finalResult = result + finalResult;
                    System.out.println("Result: "+ result);
                    System.out.println("Final Result: "+ finalResult);



                }
            }
        }
        catch (ParseException | FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }


    private static int maxFreq(String var, Join join) throws CloneNotSupportedException{
        // Caso base
        if(join.triple!=null){
            return HdtDataSource.getCountResults(join.triple, var);
        }
        // Se revisa la cantidad de variables presentes en V'
        if(join.joinVariables.size()>1){
            //se crean dos join diferentes cada uno con una de las joinVariables
            Join left = (Join)join.clone();
            left.joinVariables.remove(1);

            Join right= (Join)join.clone();
            right.joinVariables.remove(0);

            return Math.min(maxFreq(left.joinVariables.get(0), left),maxFreq(right.joinVariables.get(0), right));
        }
        else{
            // Casos para ver a que lado del join pertenece la variable  (a1 in r1 || a1 in r2)
            if(join.ancestors.contains(var)){
                return maxFreq(var, join.Left) * HdtDataSource.getCountResults(join.Right , join.joinVariables.get(0));
            }
            else{
                return HdtDataSource.getCountResults(join.Right, var) * maxFreq(join.joinVariables.get(0), join.Left);
            }
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
