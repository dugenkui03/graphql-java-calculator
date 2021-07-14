/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package calculator.graphql;

import calculator.config.Config;
import calculator.engine.ExecutionEngine;
import calculator.engine.SchemaWrapper;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


public class DefaultGraphQLSourceBuilder implements GraphQLSource.Builder {

    private Config wrapperConfig;

    private GraphQLSchema originalSchema;

    private PreparsedDocumentProvider preparsedDocumentProvider;

    private final List<Instrumentation> instrumentations = new ArrayList<>();

    private Consumer<GraphQL.Builder> graphQLTransform = ignored -> {
    };


    @Override
    public GraphQLSource.Builder wrapperConfig(Config wrapperConfig) {
        this.wrapperConfig = wrapperConfig;
        return this;
    }


    @Override
    public GraphQLSource.Builder originalSchema(GraphQLSchema originalSchema) {
        this.originalSchema = originalSchema;
        return this;
    }

    @Override
    public GraphQLSource.Builder preparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
        this.preparsedDocumentProvider = preparsedDocumentProvider;
        return this;
    }

    @Override
    public GraphQLSource.Builder instrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof ChainedInstrumentation) {
            List<Instrumentation> instrumentations = ((ChainedInstrumentation) instrumentation).getInstrumentations();
            this.instrumentations.addAll(instrumentations);
        } else {
            this.instrumentations.add(instrumentation);
        }

        return this;
    }

    @Override
    public GraphQLSource.Builder instrumentations(List<Instrumentation> instrumentations) {
        this.instrumentations.addAll(instrumentations);
        return this;
    }

    @Override
    public GraphQLSource.Builder graphQLTransform(Consumer<GraphQL.Builder> graphQLTransform) {
        this.graphQLTransform = graphQLTransform;
        return this;
    }

    @Override
    public GraphQLSource build() {
        Objects.requireNonNull(wrapperConfig);
        Objects.requireNonNull(originalSchema);

        GraphQLSchema wrappedSchema = SchemaWrapper.wrap(wrapperConfig, originalSchema);
        instrumentations.add(ExecutionEngine.newInstance(wrapperConfig));

        GraphQL.Builder graphQLBuilder = GraphQL.newGraphQL(wrappedSchema);
        graphQLBuilder.queryExecutionStrategy(new AsyncExecutionStrategy());
        graphQLBuilder.instrumentation(new ChainedInstrumentation(instrumentations));


        if (preparsedDocumentProvider != null) {
            if (preparsedDocumentProvider instanceof CalculatorDocumentCachedProvider) {
                ((CalculatorDocumentCachedProvider) preparsedDocumentProvider).setWrapperConfig(wrapperConfig);
                ((CalculatorDocumentCachedProvider) preparsedDocumentProvider).setWrappedSchema(wrappedSchema);
            }
            graphQLBuilder.preparsedDocumentProvider(preparsedDocumentProvider);
        }
        graphQLTransform.accept(graphQLBuilder);

        return new DefaultGraphQLSource(wrappedSchema, graphQLBuilder.build());
    }


    private static class DefaultGraphQLSource implements GraphQLSource {

        private final GraphQLSchema wrappedSchema;

        private final GraphQL graphQL;

        DefaultGraphQLSource(GraphQLSchema wrappedSchema, GraphQL graphQL) {
            this.wrappedSchema = wrappedSchema;
            this.graphQL = graphQL;

        }

        @Override
        public GraphQLSchema getWrappedSchema() {
            return wrappedSchema;
        }

        @Override
        public GraphQL getGraphQL() {
            return graphQL;
        }
    }

}
