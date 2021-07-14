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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static graphql.Assert.assertNotNull;

/**
 * 比 graphql-java 多两个 getter 方法，等17.0版本release后替换为 graphql-java 的 AsyncDataFetcher
 */
public class AsyncDataFetcher<T> implements DataFetcher<CompletableFuture<T>>, AsyncDataFetcherInterface<T> {

    public static <T> AsyncDataFetcher<T> async(DataFetcher<T> wrappedDataFetcher) {
        return new AsyncDataFetcher<>(wrappedDataFetcher);
    }

    public static <T> AsyncDataFetcher<T> async(DataFetcher<T> wrappedDataFetcher, Executor executor) {
        return new AsyncDataFetcher<>(wrappedDataFetcher, executor);
    }

    private final DataFetcher<T> wrappedDataFetcher;
    private final Executor executor;

    @Override
    public DataFetcher<T> getWrappedDataFetcher() {
        return wrappedDataFetcher;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public AsyncDataFetcher(DataFetcher<T> wrappedDataFetcher) {
        this(wrappedDataFetcher, ForkJoinPool.commonPool());
    }

    public AsyncDataFetcher(DataFetcher<T> wrappedDataFetcher, Executor executor) {
        this.wrappedDataFetcher = assertNotNull(wrappedDataFetcher, () -> "wrappedDataFetcher can't be null");
        this.executor = assertNotNull(executor, () -> "executor can't be null");
    }

    @Override
    public CompletableFuture<T> get(DataFetchingEnvironment environment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return wrappedDataFetcher.get(environment);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }, executor);
    }

}