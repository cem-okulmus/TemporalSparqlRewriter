import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TemporalSparqlVisitor extends RecursiveElementVisitor {

    public List<Node> metaPredicates = new ArrayList<>();
    public List<TriplePath> metaTriples = new ArrayList<>();

    public Boolean wrongStarConstruct = false;



    public TemporalSparqlVisitor() {
        super(new ElementVisitorBase());
    }

    @Override
    public void startElement(ElementPathBlock el) {

    }

    @Override
    public void endElement(ElementPathBlock el) {

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


                System.out.println("TemporalSparqlVisitor: Triple triple " + next);

                metaPredicates.add(pred);
                metaTriples.add(next);
            }

            if  (obj.isNodeTriple()) {
                this.wrongStarConstruct = true; // we cannot parse such statements, hence  not allowed
            }




        }

    }
}
