//import org.apache.jena.arq.

public class Main {


    public static void main(String[] args) throws Exception {

        String queryString =  "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                "SELECT distinct ?x \n" +
                "WHERE {\n" +
                "<< ?x :dept ?z  >> time:hasTime ?y .\n" +
                "<< ?z :location 'barcelona' >> time:hasTime  ?y; time:hasTime ?y .\n" +
                "?r :location 'barcelona' .\n" +
                "FILTER (EXISTS {\n" +
                " << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                " FILTER(time:intervalBefore(?y2,?y)).\n" +
                "FILTER EXISTS {\n"+
                " << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                " FILTER(time:intervalIn(?y2,?y)).\n" +
                "}\n"+
                "} && ?x > 2)\n"+
                "}";



        try{
            String outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
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