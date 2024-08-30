package se.umu.cs.temporalsparqlrewriter.main;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.syntax.ElementWalker;

import java.util.List;

/**
 * main.TemporalSparqlTransformer is meant to take a subset of Sparql-Star queries with a temporal semantics as input,
 *  and transform them into a regular (non-star) Sparql query, preserving the temporal semantics.
 */
public class TemporalSparqlTransformer {

    //        The allowed set of meta predicates; any other ones will lead to Sparql-Star query to be rejected.
    public static final List<String> list =
            List.of("http://www.w3.org/2006/time#hasTime",
                    "http://www.w3.org/2006/time#hasBeginning",
                    "http://www.w3.org/2006/time#hasEnd");


    /**
     *
     * @param query A Sparql-Star query that uses quoted triples to add time intervals to facts.
     * @return  True if the query fits the required structure, False otherwise.
     */
    private static boolean correctTemporalStarStatements(Query query) {
        SparqlFormattingVisitor visitor = new SparqlFormattingVisitor();

        ElementWalker.walk(query.getQueryPattern(),visitor);

        for (Node predicate : visitor.metaPredicates ) {
            if (!list.contains(predicate.toString())){
                System.out.println("Problematic predicate in question: " + predicate);
                return false;
            }
        }
        if (visitor.wrongStarConstruct){
            System.out.println("found unsupported quoted triple construction");
            return false;
        }

        return true;
    }


    /**
     * @param query A Sparql-Star query that uses quoted triples to add time  intervals to facts
     * @return Returns True if the query only uses single-time variable (STV) blocks, False otherwise
     */
    private static boolean correctSTVShape(Query query) {
        STVCheckVisitor visitor = new STVCheckVisitor();
        Op op = Algebra.compile(query);
        OpWalker.walk(op,visitor);

        return visitor.STVCondition;
    }



    /**
     * @param queryString A string describing a Sparql-Star query
     * @return A string-representation of a Sparql query without quoted triples, and an encoding of temporal relations
     *  that can be evaluated on a regular Sparql endpoint while maintaining the desired temporal semantics (such as
     *  expressing Allen's relations between time intervals).
     * @throws Exception  Exceptions to be thrown if the input query  does not purport to the desired shape
     */
    public static String transform(String queryString) throws Exception{

        Query query = QueryFactory.create(queryString);
        Op op = Algebra.compile(query);

        System.out.println("Algebra Op " + op);


//        TODO: replace these ad-hoc generic exceptions with a limited set of specialised ones later.

        // --------------------------------
        // Check that query is in correct shape
        // --------------------------------
        if (!correctTemporalStarStatements(query) ){
            throw new Exception("Unacceptable Star-Sparql query. Full Sparql-Star format not supported!");
        }

        // --------------------------------
        // Check that query only features STV BGP blocks, at any level
        // --------------------------------
        if (!correctSTVShape(query) ){
            throw new Exception("Unclear temporal semantics, require single-time variable blocks only!");
        }

        // --------------------------------
        // Build a new query that replaces any quoted triple, and replaces FILTER statements with interval comparisons
        // --------------------------------

        TemporalRewriter tr = new TemporalRewriter();

        Op newQuery  = Transformer.transform(tr,op);

        Query producedQuery = OpAsQuery.asQuery(newQuery);

        return producedQuery.toString();
    }

}
