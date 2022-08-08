package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.graph.DependencyGraph;

import java.util.Collection;
import java.util.Set;

public abstract class CDIAlternativeDependencyCollector<T extends BodyDeclaration<?>>
        extends AbstractTruncatedVisitor<DependencyGraph>
        implements DependencyCollector<T> {

    protected Set<String> alternatives;

    public void setAlternatives(Set<String> alternatives) {
        this.alternatives = alternatives;
    }

    public void calculateDependencies(Collection< TypeDeclaration<?>> ts, DependencyGraph dependencyGraph) {
        ts.forEach(t -> t.accept(this, dependencyGraph));
    }
}
