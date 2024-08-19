package se.umu.cs.TemporalSparqlRewrite.test;


import org.junit.jupiter.api.Test;
import se.umu.cs.TemporalSparqlRewrite.main.TemporalSparqlTransformer;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalSparqlTransformerTest {



    @Test
    public void SparqlBasicTest() {
        String queryString = """
                PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>
                PREFIX time: <http://www.w3.org/2006/time#>
                SELECT distinct ?x
                WHERE {
                    << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .
                    << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .
                    ?r :location 'barcelona' .
                    FILTER EXISTS {
                         << ?x :dept ?z2 >> time:hasTime ?y2 .
                         FILTER(time:intervalBefore(?y2,?y)).
                         FILTER EXISTS {
                             << ?x :dept ?z >> time:hasTime ?y3 .
                             FILTER(time:intervalIn(?y3,?y)).
                        }
                    }
                }""";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        assertNotEquals("", outQuery);
    }

    @Test
    public void SparqlUNIONTest() {
        String queryString = """
                PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>
                PREFIX time: <http://www.w3.org/2006/time#>
                SELECT distinct ?x
                WHERE {
                    { << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .
                      
                    }
                    UNION
                    {
                        << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .
                        OPTIONAL { << ?i :loc ?u  >>              time:hasTime  ?y3 }.
                        ?r :location 'barcelona' .
                        FILTER (EXISTS {
                             << ?x :dept ?z >> time:hasTime ?y2 .
                             FILTER(time:intervalBefore(?y2,?y)).
                             FILTER EXISTS {
                                 << ?x :dept ?z >> time:hasTime ?y2 .
                                 FILTER(time:intervalIn(?y2,?y)).
                            }
                        } && ?x > 2)
                    }
                }""";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        assertEquals("", outQuery);
    }


    @Test
    public void SparqlOptionalest() {
        String queryString = """
                PREFIX : <http://www.semanticweb.org/user/ontologies/2016/8/untitled-ontology-84#>
                PREFIX time: <http://www.w3.org/2006/time#>
                SELECT distinct ?x
                WHERE {
                    { << ?x :dept ?z  >>              time:hasTime  ?y; time:hasEnd ?endVar .
                      
                    }
                    UNION
                    {
                        << ?z :location 'barcelona' >>  time:hasTime  ?y; time:hasTime ?y .
                        OPTIONAL { << ?i :loc ?u  >>              time:hasTime  ?y3 }.
                        ?r :location 'barcelona' .
                        FILTER (EXISTS {
                             << ?x :dept ?z >> time:hasTime ?y2 .
                             FILTER(time:intervalBefore(?y2,?y)).
                             FILTER EXISTS {
                                 << ?x :dept ?z >> time:hasTime ?y2 .
                                 FILTER(time:intervalIn(?y2,?y)).
                            }
                        } && ?x > 2)
                    }
                }""";
        String outQuery = "";
        try{
            outQuery = TemporalSparqlTransformer.transform(queryString);
            System.out.println("Produced Query is \n \n" + outQuery );
        } catch (Exception e ){
            System.out.println("Exception: " + e);
        }


        assertEquals("", outQuery);
    }



}

