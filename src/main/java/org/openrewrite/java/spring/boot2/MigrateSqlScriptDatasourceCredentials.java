package org.openrewrite.java.spring.boot2;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MigrateSqlScriptDatasourceCredentials extends Recipe {

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return super.visit(before, ctx);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {

        final Set<String> credentialProperties = Stream.of(
                "spring.datasource.data-username",
                "spring.datasource.data-password",
                "spring.datasource.schema-username",
                "spring.datasource.schema-password",
                "spring.datasource.schema",
                "spring.datasource.data"
        ).collect(Collectors.toSet());

        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                Tree t = super.visit(tree, executionContext);

                if (SourceFile.class.isInstance(t.getClass())) {
                    SourceFile s = SourceFile.class.cast(t);
                    if(s.getSourcePath().toString().endsWith("resources/schema.sql")) {
                        s.getMarkers().addIfAbsent(new SearchResult(UUID.randomUUID(), "schema.sql"));
                        return s;
                    }
                    if(s.getSourcePath().toString().endsWith("resources/data.sql")) {
                        s.getMarkers().addIfAbsent(new SearchResult(UUID.randomUUID(), "data.sql"));
                        return s;
                    }
                } else if(Properties.Entry.class.isInstance(t)) {
                    Properties.Entry entry = Properties.Entry.class.cast(t);
                    if(credentialProperties.contains(entry.getBeforeEquals())) {
                        entry.getMarkers().addIfAbsent(new SearchResult(UUID.randomUUID(), entry.getBeforeEquals()));
                        return entry;
                    }
                }

                return t;
            }

        };

    }
}
