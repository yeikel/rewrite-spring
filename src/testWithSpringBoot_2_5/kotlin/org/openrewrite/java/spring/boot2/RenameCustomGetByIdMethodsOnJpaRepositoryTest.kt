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
package org.openrewrite.java.spring.boot2

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class RenameCustomGetByIdMethodsOnJpaRepositoryTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("spring-data")
            .build()

    override val recipe: Recipe
        get() = RenameCustomGetByIdMethodsOnJpaRepository()

    @Test
    fun renameCustomGetByIdMethod() = assertChanged(
        dependsOn = arrayOf(
            """
            public class My {}
        """
        ),
        before = """
            import org.springframework.data.jpa.repository.JpaRepository;
            import org.springframework.data.jpa.repository.Query;
            import org.springframework.data.repository.query.Param;
            public interface MyJpaRepository extends JpaRepository<My, Long> {
                @Query("from My t where t.id=:id")
                My getById(@Param("id") Long id);
            } 
        """,
        after = """
            import org.springframework.data.jpa.repository.JpaRepository;
            import org.springframework.data.jpa.repository.Query;
            import org.springframework.data.repository.query.Param;
            public interface MyJpaRepository extends JpaRepository<My, Long> {
                @Query("from My t where t.id=:id")
                My getMyById(@Param("id") Long id);
            } 
        """
    )

    @Test
    fun renameCustomGetByIdMethodWithIntermediateInterface() = assertChanged(
        dependsOn = arrayOf(
            """
                public class My {}
            """,

            """
                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.data.repository.NoRepositoryBean;
                @NoRepositoryBean
                public interface IntermediateRepository extends JpaRepository<My, Long> { }
            """
        ),
        before = """
            import org.springframework.data.jpa.repository.Query;
            import org.springframework.data.repository.query.Param;
            public interface MyJpaRepository extends IntermediateRepository {
                @Query("from My t where t.id=:id")
                My getById(@Param("id") Long id);
            } 
        """,
        after = """
            import org.springframework.data.jpa.repository.Query;
            import org.springframework.data.repository.query.Param;
            public interface MyJpaRepository extends IntermediateRepository {
                @Query("from My t where t.id=:id")
                My getMyById(@Param("id") Long id);
            } 
        """
    )

}