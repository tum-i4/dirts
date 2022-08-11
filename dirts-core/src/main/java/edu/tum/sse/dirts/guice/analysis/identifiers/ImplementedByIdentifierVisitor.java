package edu.tum.sse.dirts.guice.analysis.identifiers;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.guice.util.GuiceUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Identifies just-in-time bindings
 * <p>
 * Rationale:
 * https://github.com/google/guice/wiki/JustInTimeBindings
 */
public class ImplementedByIdentifierVisitor extends AbstractIdentifierVisitor<BeanStorage<GuiceBinding>> {

    //##################################################################################################################
    // Singleton pattern

    private static final ImplementedByIdentifierVisitor singleton = new ImplementedByIdentifierVisitor();

    private ImplementedByIdentifierVisitor() {
    }

    public static void identifyDependencies(Collection<TypeDeclaration<?>> typeDeclarations,
                                            BeanStorage<GuiceBinding> arg) {
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            typeDeclaration.accept(singleton, arg);
        }
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ClassOrInterfaceDeclaration n, BeanStorage<GuiceBinding> arg) {
        Set<AnnotationExpr> maybeAnnotationExpr = GuiceUtil.getImplementedByAnnotation(n);
        for (AnnotationExpr annotationExpr : maybeAnnotationExpr) {

            ResolvedReferenceTypeDeclaration implementedType = null;
            ResolvedReferenceTypeDeclaration byType = null;

            try {
                //******************************************************************************************************
                // Type that is implemented
                implementedType = n.resolve();

            } catch (RuntimeException e) {
                Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
            }

            //**********************************************************************************************************
            // Type that implements
            ResolvedType type = null;
            if (annotationExpr.isSingleMemberAnnotationExpr()) {
                SingleMemberAnnotationExpr singleMemberAnnotationExpr = annotationExpr.asSingleMemberAnnotationExpr();

                try {
                    ResolvedType resolvedType = singleMemberAnnotationExpr.getMemberValue().calculateResolvedType();
                    if (resolvedType.isReferenceType()) {
                        type = JavaParserUtils.extractClassType(resolvedType.asReferenceType(),
                                Set.of("java.lang.Class"));
                    }
                } catch (RuntimeException e) {
                    Log.log(FINE, "Exception in " + JavaParserUtils.class.getSimpleName() + ": " + e.getMessage());
                }


            } else if (annotationExpr.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normalAnnotationExpr = annotationExpr.asNormalAnnotationExpr();
                for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
                    if (pair.getNameAsString().equals("value")) {
                        try {
                            ResolvedType resolvedType = pair.getValue().calculateResolvedType();
                            if (resolvedType.isReferenceType()) {
                                type = JavaParserUtils.extractClassType(resolvedType.asReferenceType(),
                                        Set.of("java.lang.Class"));
                            }
                        } catch (RuntimeException e) {
                            Log.log(FINE, "Exception in " + JavaParserUtils.class.getSimpleName() + ": "
                                    + e.getMessage());
                        }
                    }
                }
            }
            if (type != null && type.isReferenceType()) {
                ResolvedReferenceType resolvedReferenceType = type.asReferenceType();
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration =
                        resolvedReferenceType.getTypeDeclaration();
                if (maybeTypeDeclaration.isPresent()) {
                    byType = maybeTypeDeclaration.get();
                }
            }

            //**********************************************************************************************************
            // Add binding

            if (implementedType != null && byType != null) {
                arg.addBeanByTypeDeclaration(implementedType, new GuiceBinding(byType));
            }


        }
    }
}
