package se.umu.cs.TemporalSparqlRewriter;

import org.apache.jena.graph.*;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;

import java.util.*;
import java.util.stream.Collectors;

public class TemporalRewriter extends TransformCopy {

    private List<Triple> expandStanza(Triple t, String groupVarname, String startVarname, String endVarName) {

        List<Triple> triples = new ArrayList<>();

        Node intName;
        if (groupVarname.equals("")){
            intName = Var.alloc(new Node_Variable(t.getObject().getName()));
        } else {
            intName = Var.alloc(new Node_Variable( groupVarname ));
        }


        Node intVar = Var.alloc(new Node_Variable(intName.getName() + "Interval"));
        Node intVarStart = Var.alloc(new Node_Variable(intVar.getName() + "Start"));
        Node intVarEnd = Var.alloc(new Node_Variable(intVar.getName() + "End"));
        Node intVarStartVar = Var.alloc(new Node_Variable(startVarname));
        Node intVarEndVar = Var.alloc(new Node_Variable(endVarName));

        triples.add(Triple.create(intName,
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#hasTime"),
                intVar));
        triples.add(Triple.create(intVar,
                NodeFactory.createLiteralString("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#Interval")));
        triples.add(Triple.create(intVar,
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#hasBeginning"),
                intVarStart));
        triples.add(Triple.create(intVarStart,
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#inXSDDateTimeStamp"),
                intVarStartVar));
        triples.add(Triple.create(intVar,
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#hasEnd"),
                intVarEnd));
        triples.add(Triple.create(intVarEnd,
                NodeFactory.createLiteralString("http://www.w3.org/2006/time#inXSDDateTimeStamp"),
                intVarEndVar));
        return triples;
    }


    @Override
    public Op transform(OpBGP opBGP){

        Set<Triple> timeTriples = opBGP.getPattern().getList().stream()
                .filter(t -> t.getSubject().isNodeTriple())
                .collect(Collectors.toSet());

        Set<Triple> untimeTriples = opBGP.getPattern().getList().stream()
                .filter(t -> !t.getSubject().isNodeTriple())
                .collect(Collectors.toSet());

        if (timeTriples.size() == 0 ){
            return opBGP;   // nothing to do in case of no time triples
        }

        String graphName = "";
        String startVarName = "";
        String endVarName = "";

        Set<Triple> graphGroup = new HashSet<>();

        for (Triple t : timeTriples){

            switch (t.getPredicate().toString()) {
                case "http://www.w3.org/2006/time#hasBeginning": {
                    startVarName = t.getObject().getName();
                    break;
                }
                case "http://www.w3.org/2006/time#hasEnd": {
                    endVarName = t.getObject().getName();
                    break;
                }
                case "http://www.w3.org/2006/time#hasTime": {
                    graphName = t.getObject().getName();
                    break;
                }
            }

            graphGroup.add(t.getSubject().getTriple()); // can be done safely due to filter above
        }


        if (startVarName.equals("")){
            startVarName = graphName +"IntervalStartVar";
        }
        if (endVarName.equals("")){
            endVarName = graphName +"IntervalEndVar";
        }



        String finalGraphName = graphName;
        String finalStartVarName = startVarName;
        String finalEndVarName = endVarName;

        Set<Triple> stanzaTriples = new HashSet<>();

        for (Triple t1 : timeTriples) {
            stanzaTriples.addAll(expandStanza(t1, finalGraphName, finalStartVarName, finalEndVarName));
        }

//                = timeTriples.stream()
//                .<Triple>mapMulti(
//                (triple,consumer) -> {
//                    for (Triple t : expandStanza(triple, finalGraphName, finalStartVarName, finalEndVarName)) {
//                        consumer.accept(t);
//                    }
//                }
//                ).collect(Collectors.toSet());


        BasicPattern newBP = new BasicPattern();

        for (Triple t1 : untimeTriples) {
            newBP.add(t1);
        }
        for (Triple t1 : stanzaTriples) {
            newBP.add(t1);
        }

        Op returnOp = new OpBGP(newBP);

//        for ( String graph : graphGroup.keySet()) {
            BasicPattern bp = new BasicPattern();

            for (Triple t : graphGroup){
                bp.add(t);
            }

            OpGraph og = new OpGraph(Var.alloc(new Node_Variable(finalGraphName)), new OpBGP(bp));

            returnOp  = OpJoin.create(og,returnOp);
//        }

        return returnOp;
    }



    private Expr timeIntervalBefore(Expr arg1, Expr arg2) {
        return new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));
    }

    private Expr timeFinishes(Expr arg1, Expr arg2) {
        Expr subE1 = new E_GreaterThan(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_Equals(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));
        return new E_LogicalAnd(subE1,subE2);
    }

    private Expr timeMeets(Expr arg1, Expr arg2) {
        return new E_Equals(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));
    }

    private Expr timeOverlaps(Expr arg1, Expr arg2) {
        Expr subE1 = new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_GreaterThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE3 = new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));


        return new E_LogicalAnd(subE1,new E_LogicalAnd(subE2,subE3));
    }

    private Expr timeStarts(Expr arg1, Expr arg2) {
        Expr subE1 = new E_Equals(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));
        return new E_LogicalAnd(subE1,subE2);
    }

    private Expr timeContains(Expr arg1, Expr arg2) {
        Expr subE1 = new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_GreaterThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));
        return new E_LogicalAnd(subE1,subE2);
    }

    private Expr timeDisjoint(Expr arg1, Expr arg2) {
        Expr subE1 = new E_GreaterThan(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));

        Expr subE2 = new E_LessThan(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));
        return new E_LogicalOr(subE1,subE2);
    }

    private Expr timeEquals(Expr arg1, Expr arg2) {
        Expr subE1 = new E_Equals(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_Equals(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));
        return new E_LogicalAnd(subE1,subE2);
    }

    private Expr timeIntervalIn(Expr arg1, Expr arg2) {
        Expr subE1 = new E_GreaterThanOrEqual(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_LessThanOrEqual(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));

        Expr subE3 = new E_LogicalNot(timeEquals(arg1,arg2));

        return new E_LogicalAnd(subE1,new E_LogicalAnd(subE2,subE3));
    }


    @Override
    public Op transform (OpFilter opfilt, Op supOp) {

        System.out.println("Currently at Filter: " + opfilt +" with subOP" + opfilt.getSubOp());

        List<Expr> expressions = opfilt.getExprs().getList();

        List<Expr> newExpressions = new ArrayList<>();

        for (Expr e : expressions){
//            Class a  = e.getClass();
//            System.out.println("Currently checking an expr of class " + a.getName());

//            if (e instanceof E_Exists ex) {
//                System.out.println("Currently checking exists: " + ex.toString());
//
//                System.out.println("\n\n with SubOP \n " + supOp.toString());
//            }

            if (e instanceof E_Function) {
                E_Function ec = (E_Function) e;
                Expr output;
//                    System.out.println("Name of function: " + ec.getFunctionIRI());
                switch (ec.getFunctionIRI()) {
                    case "http://www.w3.org/2006/time#intervalBefore": {
                        output = timeIntervalBefore(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalAfter": {
                        output = timeIntervalBefore(ec.getArg(2), ec.getArg(1));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalFinishes": {
                        output = timeFinishes(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalFinishedBy": {
                        output = timeFinishes(ec.getArg(2), ec.getArg(1));
                        break;
                    }
                    case "http://www.w3.org/2006/time#intervalMeets": {
                        output = timeMeets(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalMetBy": {
                        output = timeMeets(ec.getArg(2), ec.getArg(1));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalOverlappedBy": {
                        output = timeOverlaps(ec.getArg(2), ec.getArg(1));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalOverlaps": {
                        output = timeOverlaps(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalStartedBy": {
                        output = timeStarts(ec.getArg(2), ec.getArg(1));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalStarts": {
                        output = timeStarts(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalContains": {
                        output = timeContains(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalDuring": {
                        output = timeContains(ec.getArg(2), ec.getArg(1));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalIn": {
                        output = timeIntervalIn(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalEquals": {
                        output = timeEquals(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    case "http://www.w3.org/2006/time#intervalDisjoint": {
                        output = timeDisjoint(ec.getArg(1), ec.getArg(2));
                        break;
                    }

                    default: {
                        output = e;  // unknown function, nothing to do
                        break;
                    }
                }
                newExpressions.add(output);
            }
            else if (e instanceof E_Exists) {
                E_Exists ex = (E_Exists) e;

                System.out.println("----------   CALLING EXISTS TRANSFORMER -----------");

                  Expr exprs =   ex.getExpr();

                System.out.println("Exprs in the exist "  + exprs);

                   Op opInsideExists =  ex.getGraphPattern();

//                   Op innerOp = null;
//
//                   if (opInsideExists instanceof OpFilter opF) {
//
//                       newExpressions.addAll(opF.getExprs().getList());
//
//                       innerOp = opF.getSubOp();
//                   } else if (opInsideExists instanceof OpBGP inneropB) {
//                      innerOp = inneropB;
//                   }

//                   if ( innerOp != null ){
//                       supOp = OpJoin.create(supOp,innerOp);
//                   }

                supOp = OpJoin.create(opInsideExists,supOp);

            }
            else {
                newExpressions.add(e);
            }
        }

        return OpFilter.filterBy(new ExprList(newExpressions),supOp);

    }


}
