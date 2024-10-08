package se.umu.cs.TemporalSparqlRewriter;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

import java.util.*;


public class SparqlFormattingVisitor extends ElementVisitorBase {

    public List<Node> metaPredicates = new ArrayList<>();
    public List<TriplePath> metaTriples = new ArrayList<>();

    public boolean wrongStarConstruct = false;

    @Override
    public void visit(ElementPathBlock el) {

        Iterator<TriplePath> triples = el.patternElts();
        while (triples.hasNext()) {
            TriplePath next = triples.next();
            Node subj = next.getSubject();
            Node pred = next.getPredicate();
            Node obj = next.getObject();
            System.out.println("Checking triple: " +  next);
            if (subj.isNodeTriple()) {
                Triple tt = subj.getTriple();

//               Check for nested statements
                if ((tt.getSubject().isNodeTriple()) || (tt.getObject().isNodeTriple())){
                    this.wrongStarConstruct = true;
                }

//                Ensure the objects are variables
                if (!obj.isVariable()){
                    this.wrongStarConstruct = true;
                }

                System.out.println("TemporalSparqlVisitor: Triple triple " + next);

                metaPredicates.add(pred);
                metaTriples.add(next);
            }

            if  (obj.isNodeTriple()) {
                this.wrongStarConstruct = true; // we cannot parse such statements, hence  not allowed
            }

        }
    }

    @Override
    public void visit(ElementFilter el) {

       Expr e = el.getExpr();

       Deque<Expr> expressions = new ArrayDeque<>();
       expressions.push(e);


       while (expressions.size() > 0){

           Expr current = expressions.pop();

           if (current instanceof E_Exists) {
               E_Exists e1 = (E_Exists) current;
               ElementWalker.walk(e1.getElement(),this);

           } else
           if (current instanceof E_NotExists) {
               E_NotExists e2 = (E_NotExists) current;
               ElementWalker.walk(e2.getElement(),this);
           } else
           if (current instanceof E_LogicalAnd) {
               E_LogicalAnd e3 = (E_LogicalAnd) current;
               for (Expr andExpr : e3.getArgs()){
                   expressions.push(andExpr);
               }

           } else
           if (current instanceof E_LogicalOr) {
               E_LogicalOr e4 = (E_LogicalOr) current;

               for (Expr andExpr : e4.getArgs()){
                   expressions.push(andExpr);
               }

           } else
           if (current instanceof E_LogicalNot) {
               E_LogicalNot e5 = (E_LogicalNot) current;
               for (Expr andExpr : e5.getArgs()){
                   expressions.push(andExpr);
               }

           }

//           switch (current){
//               case E_Exists e1 -> ElementWalker.walk(e1.getElement(),this);
//               case E_NotExists e2  ->  ElementWalker.walk(e2.getElement(),this);
//               case E_LogicalAnd e3 ->{
//                   for (Expr andExpr : e3.getArgs()){
//                       expressions.push(andExpr);
//                   }
//               }
//               case E_LogicalOr e4 ->{
//                   for (Expr andExpr : e4.getArgs()){
//                       expressions.push(andExpr);
//                   }
//               }
//               case E_LogicalNot e5 ->{
//                   for (Expr andExpr : e5.getArgs()){
//                       expressions.push(andExpr);
//                   }
//               }
//               default -> {
//                   //handle everything else
//               }
//           }
        }

    }



//    TODO: Add suitable overrides for UNION, OPTIONAL, MINUS, and anything else that can realistically contain a BGP



}
