import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TemporalSparqlTransformer is meant to take a subset of Sparql-Star queries with a temporal semantics as input,
 *  and transform them into a regular (non-star) Sparql query, preserving the temporal semantics.
 */
public class TemporalSparqlTransformer {

    //        The allowed set of meta predicates; any other ones will lead to Sparql-Star query to be rejected.
    public static final List<String> list =
            List.of("http://www.w3.org/2006/time#hasTime",
                    "http://www.w3.org/2006/time#hasBeginning",
                    "http://www.w3.org/2006/time#hasEnd");


    public static String transform(String queryString) throws Exception{


        Query query = QueryFactory.create(queryString);


//        System.out.println("Hello world! Look at this query: " + query.toString());


        Op op = Algebra.compile(query);

        System.out.println("Algebra Op " + op);

        // This will walk through all parts of the query


//        ElementVisitorMine ev = new ElementVisitorMine();
        TemporalSparqlVisitor visitor = new TemporalSparqlVisitor();

        ElementWalker.walk(query.getQueryPattern(),visitor);

        for (Node predicate : visitor.metaPredicates ) {
            if (!list.contains(predicate.toString())){
                System.out.println("Problematic predicate in question: " + predicate);
                throw new Exception("Unacceptable Star-Sparql query");
            }
        }
        if (visitor.wrongStarConstruct){

            System.out.println("found unsupported quoted triple construction");
            throw new Exception("Unacceptable Star-Sparql query");
        }


        return queryString;
    }

}
