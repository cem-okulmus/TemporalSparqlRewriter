package se.umu.cs.TemporalSparqlRewriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TemporalSparqlTransformerTest {

// comment

    @Test
    public void SparqlBasicTest() {
        String queryString =
                "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#> \n" +
                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                "SELECT distinct ?x\n" +
                "WHERE {\n" +
                "    << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .\n" +
                "    << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .\n" +
                "    ?r :location 'barcelona' .\n" +
                "    FILTER EXISTS {\n" +
                "                 << ?x :dept ?z2 >> time:hasTime ?y2 .\n" +
                "                 FILTER(time:intervalBefore(?y2,?y)).\n" +
                "                 FILTER EXISTS {\n" +
                "                     << ?x :dept ?z >> time:hasTime ?y3 .\n" +
                "                     FILTER(time:intervalIn(?y3,?y)).\n" +
                "                }\n" +
                "            }\n" +
                "        }";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        Assertions.assertNotEquals("", outQuery);
    }

    @Test
    public void SparqlUNIONTest() {
        String queryString =
                "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                "SELECT distinct ?x\n" +
                "WHERE {\n" +
                "       { << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .\n" +
                "    }\n" +
                "    UNION\n" +
                "    {\n" +
                "        << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .\n" +
                "        OPTIONAL { << ?i :loc ?u  >>              time:hasTime  ?y3 }.\n" +
                "        ?r :location 'barcelona' .\n" +
                "        FILTER (EXISTS {\n" +
                "             << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                "             FILTER(time:intervalBefore(?y2,?y)).\n" +
                "             FILTER EXISTS {\n" +
                "                 << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                "                 FILTER(time:intervalIn(?y2,?y)).\n" +
                "            }\n" +
                "        } && ?x > 2)\n" +
                "    }\n" +
                "}";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        Assertions.assertEquals("", outQuery);
    }


    @Test
    public void SparqlOptionalTest() {
        String queryString =
                "PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>\n" +
                "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                "SELECT distinct ?x\n" +
                "WHERE {\n" +
                "    { << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .\n" +
                "      \n" +
                "    }\n" +
                "    UNION\n" +
                "    {\n" +
                "        << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .\n" +
                "        OPTIONAL { << ?i :loc ?u  >>              time:hasTime  ?y3 }.\n" +
                "        ?r :location 'barcelona' .\n" +
                "        FILTER (EXISTS {\n" +
                "             << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                "             FILTER(time:intervalBefore(?y2,?y)).\n" +
                "             FILTER EXISTS {\n" +
                "                 << ?x :dept ?z >> time:hasTime ?y2 .\n" +
                "                 FILTER(time:intervalIn(?y2,?y)).\n" +
                "            }\n" +
                "        } && ?x > 2)\n" +
                "    }\n" +
                "}";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        Assertions.assertEquals("", outQuery);
    }



}

