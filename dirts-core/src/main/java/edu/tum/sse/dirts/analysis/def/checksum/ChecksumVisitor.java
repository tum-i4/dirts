package edu.tum.sse.dirts.analysis.def.checksum;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.GenericVisitor;

/**
 * Enables to calculate the checksum of nodes
 */
public interface ChecksumVisitor extends GenericVisitor<Integer, Void> {
    int hashCode(final Node node);
}
