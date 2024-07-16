import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElementVisitorMine extends ElementVisitorBase {


    public List<Node> metaPredicates = new ArrayList<>();
    public List<TriplePath> metaTriples = new ArrayList<>();


    @Override
    public void visit(ElementPathBlock el) {
        // ...go through all the triples...
        Iterator<TriplePath> triples = el.patternElts();
        while (triples.hasNext()) {
            TriplePath next = triples.next();
            Node subj = next.getSubject();

            if (subj.isNodeTriple()) {
                Triple tt = subj.getTriple();
                System.out.println("ElementVisitorMine: Triple triple " + tt);

                metaPredicates.add(next.getPredicate());
                metaTriples.add(next);
            }


        }
    }

}
