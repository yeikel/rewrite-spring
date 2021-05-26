package org.openrewrite.java.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class JaxRsToSpring extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert JAX-RS annotations to Spring annotations";
    }

    @Override
    public String getDescription() {
        return "Spring annotations preceded the JAX-RS standard and are not compliant with the specification.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("javax.ws.rs.*");
    }

    public JaxRsToSpring() {
        doNext(new ChangeType("javax.ws.rs.PathParam", "org.springframework.web.bind.annotation.PathVariable"));
        doNext(new ChangeType("javax.ws.rs.QueryParam", "org.springframework.web.bind.annotation.RequestParam"));
        doNext(new ChangeType("javax.ws.rs.FormParam", "org.springframework.web.bind.annotation.RequestParam"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher pathAnnotation = new AnnotationMatcher("@javax.ws.rs.Path");

        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate requestMapping = template("@#{}Mapping(#{any()})")
                    .imports("org.springframework.web.bind.annotation.*")
                    .build();

            private final JavaTemplate requestMappingNoValue = template("@#{}Mapping")
                    .imports("org.springframework.web.bind.annotation.*")
                    .build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                for (J.Annotation annotation : c.getAllAnnotations()) {
                    if (pathAnnotation.matches(annotation) && annotation.getArguments() != null &&
                            !annotation.getArguments().isEmpty()) {
                        c = c.withTemplate(requestMapping, annotation.getCoordinates().replace(),
                                "Request",
                                annotation.getArguments().get(0));
                        maybeAddImport("org.springframework.web.bind.annotation.RequestMapping");
                    }
                }

                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDecl, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(methodDecl, ctx);

                Expression path = null;
                for (J.Annotation annotation : methodDecl.getAllAnnotations()) {
                    if (pathAnnotation.matches(annotation) && annotation.getArguments() != null &&
                            !annotation.getArguments().isEmpty()) {
                        path = annotation.getArguments().get(0);
                    }
                }

                Set<String> methods = new HashSet<>();
                for (J.Annotation annotation : methodDecl.getAllAnnotations()) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                    if (type != null) {
                        if (type.getFullyQualifiedName().startsWith("javax.ws.rs") &&
                                !type.getClassName().equals("Path")) {
                            methods.add(type.getClassName());
                        }
                    }
                }

                if (methods.isEmpty()) {
                    if (path != null) {
                        m = m.withTemplate(
                                requestMapping,
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                                "Request",
                                path
                        );
                        maybeAddImport("org.springframework.web.bind.annotation.RequestMapping");
                    }
                } else if (methods.size() == 1) {
                    String method = capitalizedMethod(methods.iterator().next());
                    if (path != null) {
                        m = m.withTemplate(
                                requestMapping,
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                                method,
                                path
                        );
                    } else {
                        m = m.withTemplate(
                                requestMappingNoValue,
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                                method
                        );
                    }
                    maybeAddImport("org.springframework.web.bind.annotation." + method + "Mapping");
                } else {
                    if (path != null) {
                        m = m.withTemplate(template("@RequestMapping(value = #{}, method = {#{}})")
                                        .imports("org.springframework.web.bind.annotation.RequestMapping")
                                        .staticImports("org.springframework.web.bind.annotation.RequestMethod.*")
                                        .build(),
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                                path,
                                String.join(",", methods)
                        );
                    } else {
                        m = m.withTemplate(template("@RequestMapping(method = {#{}})")
                                        .imports("org.springframework.web.bind.annotation.RequestMapping")
                                        .staticImports("org.springframework.web.bind.annotation.RequestMethod.*")
                                        .build(),
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                                String.join(",", methods)
                        );
                    }

                    for (String httpMethod : methods) {
                        maybeAddImport("org.springframework.web.bind.annotation.RequestMethod", httpMethod);
                    }
                    maybeAddImport("org.springframework.web.bind.annotation.RequestMapping");
                }

                return m;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                if (type != null && type.getFullyQualifiedName().startsWith("javax.ws.rs") &&
                        // we'll replace this in visitClassDeclaration
                        !(getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.ClassDeclaration)) {
                    if (!type.getClassName().equals("PathParam") && !type.getClassName().equals("QueryParam") &&
                        !type.getClassName().equals("FormParam")) {
                        maybeRemoveImport(type.getFullyQualifiedName());

                        //noinspection ConstantConditions
                        return null;
                    }
                }
                return annotation;
            }

            private String capitalizedMethod(String method) {
                return method.substring(0, 1).toUpperCase() + method.substring(1).toLowerCase();
            }
        };
    }
}
