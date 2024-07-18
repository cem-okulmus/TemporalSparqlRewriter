import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.*;

import java.util.*;

public class STVCheckVisitor extends OpVisitorBase {

    public boolean STVCondition = true;

    @Override
    public void visit(OpBGP opBGP) {

        Set<String> timeVars = new HashSet<>();

        BasicPattern bp = opBGP.getPattern();
        System.out.println("Look at this pattern: " + bp.getList());


        List<Triple> triples = opBGP.getPattern().getList();

        for (Triple t : triples) {
            // check if quoted
            if ( t.getSubject().isNodeTriple() &&
                    (t.getPredicate().toString().equals("http://www.w3.org/2006/time#hasTime"))){
                timeVars.add(t.getObject().getName());
            }
        }

        if (timeVars.size() > 1) {   // right now: force all interval variables in a BGP to be the same
            System.out.println("Variables in block " + timeVars);
            this.STVCondition = false ;
        } else {
            System.out.println("Vars ok");
        }

        //TODO: add support for multiple time variables, if they are set to equal in a FILTER
    }


    @Override
    public void visit(OpFilter opFilt) {

        Op subOp = opFilt.getSubOp();
        List<Expr> exprList =   opFilt.getExprs().getList();
//        Class a  = subOp.getClass();
//        System.out.println("Look at this subOP: " + subOp + " with Class "  + a.getName() +"\n" );
//        System.out.println("Look at this exprList: " + exprList);


        Deque<Expr> expressions = new ArrayDeque<>();

        for (Expr e : exprList) {
            expressions.push(e);
        }


        while (expressions.size() > 0){

            Expr current = expressions.pop();
            switch (current){
                case E_Exists e1 -> {
                    OpWalker.walk(e1.getGraphPattern(),this);
                    break;
                }
                case E_NotExists e2  -> {

                    OpWalker.walk(e2.getGraphPattern(),this);
                    break;
                }
                case E_LogicalAnd e3 ->{
                    for (Expr andExpr : e3.getArgs()){
                        expressions.push(andExpr);
                    }
                    break;
                }
                case E_LogicalOr e4 ->{
                    for (Expr andExpr : e4.getArgs()){
                        expressions.push(andExpr);
                    }
                    break;
                }
                case E_LogicalNot e5 ->{
                    for (Expr andExpr : e5.getArgs()){
                        expressions.push(andExpr);
                    }
                    break;
                }
                default -> {
                    //handle everything else
                }
            }
        }

    }


//    TODO: Add suitable overrides for UNION, OPTIONAL, MINUS, and anything else that can realistically contain a BGP
}
