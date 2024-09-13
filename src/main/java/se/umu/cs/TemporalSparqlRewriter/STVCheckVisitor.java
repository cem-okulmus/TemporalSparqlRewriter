package se.umu.cs.TemporalSparqlRewriter;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpLateral;
import org.apache.jena.sparql.expr.*;

import java.util.*;

public class STVCheckVisitor extends OpVisitorBase {

    public boolean STVCondition = true;



    private Map<String,Set<String>> visitInternal(OpBGP opBGP) {

        Map<String,Set<String>> timeVarMap = new HashMap<>();

//        BasicPattern bp = opBGP.getPattern();
//        System.out.println("Look at this pattern: " + bp.getList());


        List<Triple> triples = opBGP.getPattern().getList();

        for (Triple t : triples) {
            // check if quoted
            if ( t.getSubject().isNodeTriple() ){
                Set<String> timeVars;

                if (timeVarMap.containsKey((t.getPredicate().toString()))) {
                    timeVars = timeVarMap.get(t.getPredicate().toString());
                } else {
                    timeVars = new HashSet<>();
                }

                timeVars.add(t.getObject().getName());
                timeVarMap.put(t.getPredicate().toString(),timeVars);

            }
        }
        return timeVarMap;
    }



    @Override
    public void visit(OpBGP opBGP) {
        Map<String,Set<String>> timeVarMap = visitInternal(opBGP);

        boolean intervalDefined = false;

        boolean endPointDefined = false;



        for (String predicate : timeVarMap.keySet()) {
            Set<String> timeVars = timeVarMap.get(predicate);

            if ( predicate.equals("http://www.w3.org/2006/time#hasTime") ){
                intervalDefined = true;
            }

            if ( predicate.equals("http://www.w3.org/2006/time#hasEnd")
                    || predicate.equals("http://www.w3.org/2006/time#hasBeginning")  ){
                endPointDefined = true;
            }

            if (timeVars.size() > 1) {   // right now: force all interval variables in a BGP to be the same
                System.out.println("Variables in block " + timeVars);
                this.STVCondition = false ;
            } else {
                System.out.println("Vars ok");
            }
        }

        if (!intervalDefined && endPointDefined) {
            this.STVCondition = false ;
        }

    }


    @Override
    public void  visit(OpLeftJoin opJoin){
//     if we left join two BGPs, then they agree on all the variables

        Op op1 = opJoin.getLeft();
        Op op2 = opJoin.getRight();


        if ((op1 instanceof OpBGP)  && (op2 instanceof  OpBGP) ){
            Map<String,Set<String>> timeVars1 =  this.visitInternal((OpBGP) op1);
            Map<String,Set<String>> timeVars2 =  this.visitInternal((OpBGP) op2);

            for (String pred : timeVars1.keySet()) {
                if (!( timeVars1.get(pred).equals(timeVars2.get(pred)))){
                    this.STVCondition = false;
                }
            }
            for (String pred : timeVars2.keySet()) {
                if (!( timeVars2.get(pred).equals(timeVars1.get(pred)))){
                    this.STVCondition = false;
                }
            }
        }


    }

    @Override
    public void  visit(OpJoin opJoin){
//     if we join two BGPs, then they agree on all the variables

        Op op1 = opJoin.getLeft();
        Op op2 = opJoin.getRight();

        if ((op1 instanceof OpBGP)  && (op2 instanceof  OpBGP) ){
            Map<String,Set<String>> timeVars1 =  this.visitInternal((OpBGP) op1);
            Map<String,Set<String>> timeVars2 =  this.visitInternal((OpBGP) op2);

            for (String pred : timeVars1.keySet()) {
                if (!( timeVars1.get(pred).equals(timeVars2.get(pred)))){
                    this.STVCondition = false;
                }
            }
            for (String pred : timeVars2.keySet()) {
                if (!( timeVars2.get(pred).equals(timeVars1.get(pred)))){
                    this.STVCondition = false;
                }
            }
        }


    }

    @Override
    public void  visit(OpLateral opJoin){
//     if we lateral join two BGPs, then they agree on all the variables

        Op op1 = opJoin.getLeft();
        Op op2 = opJoin.getRight();

        if ((op1 instanceof OpBGP)  && (op2 instanceof  OpBGP) ){
            Map<String,Set<String>> timeVars1 =  this.visitInternal((OpBGP) op1);
            Map<String,Set<String>> timeVars2 =  this.visitInternal((OpBGP) op2);

            for (String pred : timeVars1.keySet()) {
                if (!( timeVars1.get(pred).equals(timeVars2.get(pred)))){
                    this.STVCondition = false;
                }
            }
            for (String pred : timeVars2.keySet()) {
                if (!( timeVars2.get(pred).equals(timeVars1.get(pred)))){
                    this.STVCondition = false;
                }
            }
        }


    }


    @Override
    public void visit(OpFilter opFilt) {

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


            if (current instanceof  E_Exists) {
                E_Exists e1 = (E_Exists) current;
                OpWalker.walk(e1.getGraphPattern(),this);

            } else
            if (current instanceof  E_NotExists) {
                E_NotExists e2 = (E_NotExists) current;
                OpWalker.walk(e2.getGraphPattern(),this);

            } else
            if (current instanceof  E_LogicalAnd) {
                E_LogicalAnd e3 = (E_LogicalAnd) current;

                for (Expr andExpr : e3.getArgs()){
                    expressions.push(andExpr);
                }

            } else
            if (current instanceof  E_LogicalOr) {
                E_LogicalOr e4 = (E_LogicalOr) current;

                for (Expr andExpr : e4.getArgs()){
                    expressions.push(andExpr);
                }

            } else
            if (current instanceof  E_LogicalNot) {
                E_LogicalNot e5 = (E_LogicalNot) current;

                for (Expr andExpr : e5.getArgs()){
                    expressions.push(andExpr);
                }

            }


//            switch (current.getClass()){
//                case E_Exists : {
//
//                    OpWalker.walk(e1.getGraphPattern(),this);
//                    break;
//                }
//                case E_NotExists e2  -> OpWalker.walk(e2.getGraphPattern(),this);
//                case E_LogicalAnd e3 -> {
//                    for (Expr andExpr : e3.getArgs()){
//                        expressions.push(andExpr);
//                    }
//                }
//                case E_LogicalOr e4 -> {
//                    for (Expr andExpr : e4.getArgs()){
//                        expressions.push(andExpr);
//                    }
//                }
//                case E_LogicalNot e5 -> {
//                    for (Expr andExpr : e5.getArgs()){
//                        expressions.push(andExpr);
//                    }
//                }
//                default -> {
//                    //handle everything else
//                }
//            }
        }
    }
//    TODO: Add suitable overrides for UNION, OPTIONAL, MINUS, and anything else that can realistically contain a BGP
}
