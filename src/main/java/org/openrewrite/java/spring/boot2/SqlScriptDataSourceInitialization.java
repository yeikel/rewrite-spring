/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: fkrueger
 */
package org.openrewrite.java.spring.boot2;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the 'SQL Script DataSource Initialization' step required when upgrading from Spring Boot 2.4 to 2.5.
 *
 * See <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.5-Release-Notes#sql-script-datasource-initialization">Spring Boot 2.5 Release Notes</a>.
 */
public class SqlScriptDataSourceInitialization extends Recipe {

    private static final Set<String> DEPRECATED_PROPERTIES = Stream.of(
            "spring.datasource.continue-on-error",
            "spring.datasource.sql-script-encoding",
            "spring.datasource.initialization-mode",
            "spring.datasource.platform",
            "spring.datasource.separator",
            "spring.datasource.schema",
            "spring.datasource.schema-password",
            "spring.datasource.schema-username",
            "spring.datasource.data",
            "spring.datasource.data-password",
            "spring.datasource.data-username"
    ).collect(Collectors.toSet());

    public static final String DEPRECATED_PROPERTY_USAGE_MARKER = "DEPRECATED_PROPERTY_USAGE_MARKER";

    public SqlScriptDataSourceInitialization() {
        doNext(new FindSpringBootPropertyFilesWithDeprecatedDatasourceInitializationProperties());
    }

    @Override
    public String getDisplayName() {
        return "...";
    }

    private class FindSpringBootPropertyFilesWithDeprecatedDatasourceInitializationProperties extends Recipe {

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        protected TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PropertiesVisitor<ExecutionContext>() {
                @Override
                public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
                    Properties.File f = (Properties.File) super.visitFile(file, executionContext);

                    if(f.getSourcePath().toString().matches(".*/application.*\\.properties")) {

                    }

                    return f;
                }

                @Override
                public Properties visitEntry(Properties.Entry e, ExecutionContext executionContext) {
                    Properties properties = super.visitEntry(e, executionContext);

                    if(Properties.Entry.class.isInstance(properties)) {
                        Properties.Entry p = Properties.Entry.class.cast(properties);
                        String propertyName = p.getBeforeEquals();
                        if(isDeprecatedProperty(propertyName)) {
                            p.getMarkers().addIfAbsent(new SearchResult(UUID.randomUUID(), DEPRECATED_PROPERTY_USAGE_MARKER));
                            return p;
                        }
                    }

                    return properties;
                }

                private boolean isDeprecatedProperty(String propertyName) {
                    return DEPRECATED_PROPERTIES.contains(propertyName);
                }
            };
        }
    }
}
