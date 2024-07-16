import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.expr.E_Exists;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemporalSparqlOPVisitor extends OpVisitorBase {

    public boolean STVBrokenlocal = false;

    @Override
    public void visit(OpBGP opBGP) {

        Set<String> timeVars = new HashSet<>();

        BasicPattern bp = opBGP.getPattern();
        System.out.println("Look at this pattern: " + bp.getList());


        List<Triple> triples = opBGP.getPattern().getList();

        for (Triple t : triples) {
            // check if quoted
            if ( t.getSubject().isNodeTriple() &&
                    (t.getPredicate().toString().equals("<http://www.w3.org/2006/time#hasTime"))){
                timeVars.add(t.getObject().getName());
            }
        }

        if (timeVars.size() != 1) {   // right now: force all interval variables in a BGP to the same
            System.out.println("Variables in block " + timeVars);
            STVBrokenlocal = false ;
        }

        //TODO: add support for multiple time variables, if they are set to equal in a FILTER
    }


    @Override
    public void visit(OpFilter opFilt) {

        Op subOp = opFilt.getSubOp();
        List<Expr> exprList =   opFilt.getExprs().getList();
        Class a  = subOp.getClass();
//        System.out.println("Look at this subOP: " + subOp + " with Class "  + a.getName() +"\n" );
//        System.out.println("Look at this exprList: " + exprList);


        for (Expr e : exprList){
            switch (e){
                case E_Exists e1 -> {
                    OpWalker.walk(e1.getGraphPattern(),this);
                }
                case E_NotExists e2  -> {
                    OpWalker.walk(e2.getGraphPattern(),this);
                }
                default -> {
                    //handle everything else
                }
            }
        }
    }


//    TODO: Add suitable overrides for UNION, OPTIONAL, MINUS, and anything else that can realistically contain a BGP
}
