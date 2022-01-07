/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.spring.boot2;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.UUID;

public class FindCustomGetByIdMethodsOnJpaRepository extends JavaIsoVisitor<ExecutionContext> {
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
        if(md.getName().getSimpleName().equals("getById")) {
            J.ClassDeclaration classDeclaration = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).getValue();
            if(JavaType.Class.build(JPA_REPOSITORY).isAssignableFrom(classDeclaration.getType())) {
                Markers markers = md.getMarkers().addIfAbsent(new SearchResult(UUID.randomUUID(), "getById"));
                return md.withMarkers(markers);
            }
        }
        return md;
    }

    // TODO: check that Spring Boot 2.4 is in use and any JpaRepository implementation exists that defines a method 'getById' with type of ID as parameter
    // TODO: How can Maven/Gradle and CompilationUnits be visited in this Visitor?
}
