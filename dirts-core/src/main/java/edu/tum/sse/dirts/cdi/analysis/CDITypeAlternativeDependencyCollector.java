package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;

public class CDITypeAlternativeDependencyCollector extends CDIAlternativeDependencyCollector<TypeDeclaration<?>> {

    //##################################################################################################################
    // Visitor pattern - Methods inherited from TypeDependencyCollector

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);

        if (CDIUtil.isAlternativeNode(n)
                || n.getMethods().stream().anyMatch(m -> CDIUtil.isProducerNode(m) && CDIUtil.isAlternativeNode(m))
                || n.getFields().stream().anyMatch(m -> CDIUtil.isProducerNode(m) && CDIUtil.isAlternativeNode(m))
        ) {
            // Add edge to potential entry from alternatives in beans.xml
            try {
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
                String fromNode = lookup(resolvedReferenceTypeDeclaration);
                if (alternatives.contains(fromNode)) {
                    String toNode = CDIUtil.lookupXMlAlternativeName(fromNode);
                    dependencyGraph.addEdge(fromNode, toNode, EdgeType.DI_CDI);
                }
            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void visit(EnumDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);
    }

    @Override
    public void visit(AnnotationDeclaration n, DependencyGraph dependencyGraph) {
        super.visit(n, dependencyGraph);
    }
}
