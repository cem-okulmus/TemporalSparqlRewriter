//import org.apache.jena.arq.
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.*;

import java.util.*;

public class Main {


    public static void main(String[] args) throws Exception {

        String queryString =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                "SELECT distinct ?x \n" +
                "WHERE {\n" +
                "<< ?x :dept ?z  >> time:hasTime ?y .\n" +
                "<< ?z :location 'barcelona' >> time:hasTime  ?v; time:hasTime ?v2 .\n" +
                "?r :location 'barcelona' .\n" +
                "EXISTS {\n" +
                " << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                " FILTER(time:intervalBefore(?y2,?y)).\n" +
                "EXISTS {\n"+
                " << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                " FILTER(time:intervalBefore(?y2,?y)).\n" +
                "}\n"+
                "}\n"+
                "}";



        try{
            String outQuery = TemporalSparqlTransformer.transform(queryString);
        } catch (Exception e ){
            throw e;
//            System.out.println("Exception: " + e);
//            System.exit(1);
        }




//        System.out.println("Collected meta Triples " + metaTriples);
//        System.out.println("Collected meta predicates " + metaPredicates);


//        Check which meta statements are there
        // Only accept if it uses some from a limited set
//        Next make sure that STV property is held
//        Identify subexpressions in algebra that hold STVs
//        Only Accept stv subexpressions  of limited form
//        Then transform it all into a flat non-star non-temporal SPARQL query (build a new one)

    }
}