package se.umu.cs.TemporalSparqlRewrite.main;

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
        Node intVarStartVar;
        if (startVarname.equals("")){
            intVarStartVar  = Var.alloc(new Node_Variable(intVarStart.getName() + "Var"));
        } else {
            intVarStartVar  = Var.alloc(new Node_Variable(startVarname));
        }

        Node intVarEndVar;
        if (endVarName.equals("")) {
            intVarEndVar = Var.alloc(new Node_Variable(intVarEnd.getName() + "Var"));
        } else {
            intVarEndVar  = Var.alloc(new Node_Variable(endVarName));
        }




        triples.add(new Triple(intName,
                NodeFactory.createLiteral("http://www.w3.org/2006/time#hasTime"),
                intVar));
        triples.add(new Triple(intVar,
                NodeFactory.createLiteral("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NodeFactory.createLiteral("http://www.w3.org/2006/time#Interval")));
        triples.add(new Triple(intVar,
                NodeFactory.createLiteral("http://www.w3.org/2006/time#hasBeginning"),
                intVarStart));
        triples.add(new Triple(intVarStart,
                NodeFactory.createLiteral("http://www.w3.org/2006/time#inXSDDateTimeStamp"),
                intVarStartVar));
        triples.add(new Triple(intVar,
                NodeFactory.createLiteral("http://www.w3.org/2006/time#hasEnd"),
                intVarEnd));
        triples.add(new Triple(intVarEnd,
                NodeFactory.createLiteral("http://www.w3.org/2006/time#inXSDDateTimeStamp"),
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

        Map<String,Set<Triple>> graphGroup = new HashMap<>();

        for (Triple t : timeTriples){

            switch (t.getPredicate().toString()) {
                case "http://www.w3.org/2006/time#hasBeginning" -> startVarName = t.getObject().getName();
                case "http://www.w3.org/2006/time#hasEnd" -> endVarName = t.getObject().getName();
                case "http://www.w3.org/2006/time#hasTime" -> {
                    graphName = t.getObject().getName();
                    if (graphGroup.containsKey(t.getObject().getName())){
                        Set<Triple> oldList = graphGroup.get(t.getObject().getName());
                        oldList.add(t.getSubject().getTriple());
                        graphGroup.put(t.getObject().getName(),oldList);
                    } else {
                        Set<Triple> newList = new HashSet<>();
                        newList.add(t.getSubject().getTriple());
                        graphGroup.put(t.getObject().getName(),newList);
                    }
                }
            }
        }

//        //        provide an ad-hoc graph name if none present in the timeTriples
//        if (graphName.equals("")){
//            if (startVarName.equals("")){
////                use  endvarname
//                graphName  = endVarName + "GraphName";
//            } else {
////                use starvarName
//                graphName  = startVarName + "GraphName";
//            }
//        }



        String finalGraphName = graphName;
        String finalStartVarName = startVarName;
        String finalEndVarName = endVarName;
        Set<Triple> stanzaTriples = timeTriples.stream()
                .<Triple>mapMulti(
                (triple,consumer) -> {
                    for (Triple t : expandStanza(triple, finalGraphName, finalStartVarName, finalEndVarName)) {
                        consumer.accept(t);
                    }
                }
                ).collect(Collectors.toSet());


        BasicPattern newBP = new BasicPattern();

        for (Triple t1 : untimeTriples) {
            newBP.add(t1);
        }
        for (Triple t1 : stanzaTriples) {
            newBP.add(t1);
        }

        Op returnOp = new OpBGP(newBP);

        for ( String graph : graphGroup.keySet()) {
            BasicPattern bp = new BasicPattern();

            for (Triple t : graphGroup.get(graph)){
                bp.add(t);
            }

            OpGraph og = new OpGraph(Var.alloc(new Node_Variable(graph)), new OpBGP(bp));

            returnOp  = OpJoin.create(og,returnOp);
        }

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

    private Expr timeIntervalin(Expr arg1, Expr arg2) {
        Expr subE1 = new E_GreaterThanOrEqual(new ExprVar(arg1.getVarName() + "IntervalStartVar"),
                new ExprVar(arg2.getVarName() + "IntervalStartVar"));

        Expr subE2 = new E_LessThanOrEqual(new ExprVar(arg1.getVarName() + "IntervalEndVar"),
                new ExprVar(arg2.getVarName() + "IntervalEndVar"));

        Expr subE3 = new E_LogicalNot(timeEquals(arg1,arg2));

        return new E_LogicalAnd(subE1,new E_LogicalAnd(subE2,subE3));
    }


    @Override
    public Op transform (OpFilter opfilt, Op supOp) {

        List<Expr> expressions = opfilt.getExprs().getList();

        List<Expr> newExpressions = new ArrayList<>();

        for (Expr e : expressions){
//            Class a  = e.getClass();
//            System.out.println("Currently checking an expr of class " + a.getName());
            if (e instanceof E_Function ec) {
                Expr output;
//                    System.out.println("Name of function: " + ec.getFunctionIRI());
                switch (ec.getFunctionIRI()) {
                    case "http://www.w3.org/2006/time#intervalBefore" ->
                            output = timeIntervalBefore(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalAfter" ->
                            output = timeIntervalBefore(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalFinishes" ->
                            output = timeFinishes(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalFinishedBy" ->
                            output = timeFinishes(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalMeets" ->
                            output = timeMeets(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalMetBy" ->
                            output = timeMeets(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalOverlappedBy" ->
                            output = timeOverlaps(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalOverlaps" ->
                            output = timeOverlaps(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalStartedBy" ->
                            output = timeStarts(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalStarts" ->
                            output = timeStarts(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalContains" ->
                            output = timeContains(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalDuring" ->
                            output = timeContains(ec.getArg(2), ec.getArg(1));
                    case "http://www.w3.org/2006/time#intervalIn" ->
                            output = timeIntervalin(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalEquals" ->
                            output = timeEquals(ec.getArg(1), ec.getArg(2));
                    case "http://www.w3.org/2006/time#intervalDisjoint" ->
                            output = timeDisjoint(ec.getArg(1), ec.getArg(2));
                    default -> output = e;  // unknown function, nothing to do
                }
                newExpressions.add(output);
            } else {
                newExpressions.add(e);
            }
        }

        return OpFilter.filterBy(new ExprList(newExpressions),supOp);

    }


}
