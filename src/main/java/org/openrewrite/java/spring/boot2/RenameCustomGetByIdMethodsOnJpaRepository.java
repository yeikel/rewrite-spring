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
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.shaded.jgit.api.errors.RefNotAdvertisedException;

import java.util.List;

@Incubating(since = "4.15.0")
public class RenameCustomGetByIdMethodsOnJpaRepository extends Recipe {

    public static final String FQ_JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";

    @Override
    public String getDisplayName() {
        return "Refactor custom getById() methods on JpaRepositories.";
    }

    @Override
    public String getDescription() {
        return "Find all custom getById() methods and rename them to get<return type>ById().";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new FindCustomGetByIdMethodsOnJpaRepository();
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext executionContext) {
                J.ClassDeclaration classDecl = super.visitClassDeclaration(classDeclaration, executionContext);
                if (extendsJpaRepository(classDeclaration)) {
                    classDecl.getBody().getStatements().forEach(statement -> {
                            if (isRelevantMethodDeclaration(statement, classDecl)) {
                                String fqNameOfIdType = getTypeOfId(classDecl);
                                String simpleNameOfEntityType = getSimpleNameOfEntityType(classDecl);
                                String newMethodName = "get" + simpleNameOfEntityType + "ById";
                                boolean matchOverrides = true; // TODO: is this correct ?
                                ChangeMethodName changeMethodName = new ChangeMethodName(FQ_JPA_REPOSITORY + " getById(" + fqNameOfIdType + ")", newMethodName, matchOverrides);
                                doAfterVisit(changeMethodName);
                            }
                        }
                    );
                }
                return classDecl;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext o) {
                return super.visitMethodDeclaration(method, o);
            }
        };
    }

    private boolean isRelevantMethodDeclaration(Statement statement, J.ClassDeclaration classDecl) {
        if(isMethodDeclaration(statement)) {
            J.MethodDeclaration methodDeclaration = J.MethodDeclaration.class.cast(statement);
            String fqNameOfIdType = getTypeOfId(classDecl);
            String fqNameOfEntityType = getTypeOfEntity(classDecl);
            return methodDeclaration.getName().getSimpleName().equals("getById")
                    && returnsEntity(methodDeclaration, fqNameOfEntityType)
                    && takesOnlyIdAsParameter(methodDeclaration, fqNameOfIdType);
        } else {
            return false;
        }
    }

    private String getSimpleNameOfEntityType(J.ClassDeclaration classDecl) {
        JavaType.Parameterized repo = findTopMostParametrizedJpaRepository(classDecl.getType());
        return ((JavaType.Class)repo.getTypeParameters().get(0)).getClassName();
    }

    private boolean takesOnlyIdAsParameter(J.MethodDeclaration methodDeclaration, String fqNameOfIdType) {
        if (methodDeclaration.getParameters().size() != 1) {
            return false;
        }
        Statement parameter = methodDeclaration.getParameters().get(0);
        return fqNameOfIdType.equals(((JavaType.Class) ((J.VariableDeclarations) parameter).getVariables().get(0).getType()).getFullyQualifiedName());
    }

    private boolean returnsEntity(J.MethodDeclaration methodDeclaration, String fqNameOfEntityType) {
        JavaType type = methodDeclaration.getReturnTypeExpression().getType();
        if (JavaType.Class.class.isInstance(type)) {
            JavaType.Class fullyQualifiedReturnType = JavaType.Class.class.cast(type);
            return fqNameOfEntityType.equals(fullyQualifiedReturnType.getFullyQualifiedName());
        }
        return false;
    }

    private String getTypeOfEntity(J.ClassDeclaration classDecl) {
        JavaType.Parameterized repo = findTopMostParametrizedJpaRepository(classDecl.getType());
        return ((JavaType.Class)repo.getTypeParameters().get(0)).getFullyQualifiedName();
    }

    private JavaType.Parameterized findTopMostParametrizedJpaRepository(JavaType.FullyQualified fullyQualified) {
        List<JavaType.FullyQualified> interfaces = fullyQualified.getInterfaces();
        for(JavaType.FullyQualified i : interfaces) {
            if(i.isAssignableTo(FQ_JPA_REPOSITORY)) {
                if(JavaType.Parameterized.class.isInstance(i)) {
                    JavaType.Parameterized parameterized = JavaType.Parameterized.class.cast(i);
                    if(parameterized.getTypeParameters().size() == 2) {
                        return parameterized;
                    }
                }
            }
            return findTopMostParametrizedJpaRepository(i);
        }
        throw new RuntimeException("FaILED!");
    }

    private String getTypeOfId(J.ClassDeclaration classDecl) {
        JavaType.Parameterized repo = findTopMostParametrizedJpaRepository(classDecl.getType());
        return ((JavaType.Class)repo.getTypeParameters().get(1)).getFullyQualifiedName();
    }

    private boolean isMethodDeclaration(Statement s) {
        return J.MethodDeclaration.class.isAssignableFrom(s.getClass());
    }

    private boolean extendsJpaRepository(J.ClassDeclaration classDecl) {
        return JavaType.Class.build(FQ_JPA_REPOSITORY).isAssignableFrom(classDecl.getType());
    }
}
