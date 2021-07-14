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
package calculator.util;

import static calculator.engine.service.BusinessServiceClient.batchSellerInfoByIds;
import static calculator.engine.service.BusinessServiceClient.getSellerInfoById;
import static calculator.engine.service.ConsumerServiceClient.batchUserInfoByIds;
import static calculator.graphql.AsyncDataFetcher.async;

import calculator.config.ConfigImpl;
import calculator.engine.service.CommodityServiceClient;
import calculator.engine.service.ConsumerServiceClient;
import calculator.engine.service.MarketingServiceClient;
import calculator.graphql.CalculatorDocumentCachedProvider;
import calculator.graphql.DefaultGraphQLSourceBuilder;
import calculator.graphql.GraphQLSource;
import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GraphQLSourceHolder {

    private static DataFetcher<Map> emptyDataFetcher = environment -> Collections.emptyMap();

    private static DataFetcher userDataFetcher = environment -> {
        Integer userIdNumber = (Integer) environment.getArguments().get("userId");
        if (userIdNumber == null) {
            return null;
        }

        return ConsumerServiceClient.getUserInfoById(userIdNumber);
    };


    private static DataFetcher userListDataFetcher = environment -> {
        Map<String, Object> arguments = environment.getArguments();
        List<Integer> ids = (List<Integer>) arguments.get("userIds");
        return batchUserInfoByIds(ids);
    };

    private static DataFetcher sellerDataFetcher = environment ->
            getSellerInfoById(((Number) environment.getArguments().get("sellerId")).longValue());

    private static DataFetcher sellerListDataFetcher = environment -> {
        Map<String, Object> arguments = environment.getArguments();
        List<Long> ids = (List<Long>) arguments.get("sellerIds");
        return batchSellerInfoByIds(ids);
    };

    private static DataFetcher commodityDataFetcher = environment ->
            CommodityServiceClient.getItemBaseInfoById((Integer) environment.getArguments().get("itemId"));

    private static DataFetcher commodityListDataFetcher = environment -> {
        Map<String, Object> arguments = environment.getArguments();
        List<Number> ids = (List<Number>) arguments.get("itemIds");
        return CommodityServiceClient.batchItemBaseInfoByIds(ids);
    };

    private static DataFetcher couponDataFetcher = environment ->
            MarketingServiceClient.getCouponInfoById((Integer) environment.getArguments().get("couponId"));

    private static DataFetcher abInfoDataFetcher = environment -> environment.getArgumentOrDefault("userId", -1) % 7;


    private static DataFetcher couponListDataFetcher = environment -> {
        Map<String, Object> arguments = environment.getArguments();
        List<Integer> ids = (List<Integer>) arguments.get("couponIds");
        return MarketingServiceClient.batchCouponInfoByIds(ids);
    };

    public static GraphQLSchema getDefaultSchema() {
        RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();
        for (Map.Entry<String, Map<String, DataFetcher>> entry : defaultDataFetcherInfo().entrySet()) {
            TypeRuntimeWiring.Builder typeWiring = TypeRuntimeWiring.newTypeWiring(entry.getKey()).dataFetchers(entry.getValue());
            runtimeWiring.type(typeWiring);
        }
        return TestUtil.schemaByInputFile("schema.graphql", runtimeWiring.build());
    }

    public static GraphQLSource getGraphQLByDataFetcherMap(Map<String, Map<String, DataFetcher>> dataFetcherInfoMap) {
        return getGraphQLByDataFetcherMap(dataFetcherInfoMap,ConfigImpl.newConfig().build());
    }

    public static GraphQLSource getGraphQLByDataFetcherMap(Map<String, Map<String, DataFetcher>> dataFetcherInfoMap,ConfigImpl config) {
        GraphQLSchema schema = getSchemaByDataFetcherMap(dataFetcherInfoMap);
        DefaultGraphQLSourceBuilder sourceBuilder = new DefaultGraphQLSourceBuilder();
        sourceBuilder.wrapperConfig(config).originalSchema(schema)
                .preparsedDocumentProvider(new CalculatorDocumentCachedProvider() {
                    @Override
                    public PreparsedDocumentEntry getDocumentFromCache(ExecutionInput executionInput,
                                                                       Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidateFunction) {
                        return null;
                    }

                    @Override
                    public void setDocumentCache(ExecutionInput executionInput, PreparsedDocumentEntry cachedValue) {
                        // ignored
                    }
                });
        return sourceBuilder.build();
    }

    public static GraphQLSchema getSchemaByDataFetcherMap(Map<String, Map<String, DataFetcher>> dataFetcherInfoMap) {
        RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();
        for (Map.Entry<String, Map<String, DataFetcher>> entry : dataFetcherInfoMap.entrySet()) {
            TypeRuntimeWiring.Builder typeWiring = TypeRuntimeWiring.newTypeWiring(entry.getKey()).dataFetchers(entry.getValue());
            runtimeWiring.type(typeWiring);
        }
        return TestUtil.schemaByInputFile("schema.graphql", runtimeWiring.build());
    }

    public static Map<String, Map<String, DataFetcher>> defaultDataFetcherInfo(){
        Map<String, Map<String, DataFetcher>> dataFetcherInfo = new HashMap<>();

        Map<String, DataFetcher> queryFieldFetchers = new HashMap<>();
        queryFieldFetchers.put("consumer", async(emptyDataFetcher));
        queryFieldFetchers.put("business", async(emptyDataFetcher));
        queryFieldFetchers.put("commodity", async(emptyDataFetcher));
        queryFieldFetchers.put("marketing", async(emptyDataFetcher));
        queryFieldFetchers.put("toolInfo", async(emptyDataFetcher));
        dataFetcherInfo.put("Query", queryFieldFetchers);

        Map<String, DataFetcher> consumerFieldFetchers = new HashMap<>();
        consumerFieldFetchers.put("userInfo", async(userDataFetcher));
        consumerFieldFetchers.put("userInfoList", async(userListDataFetcher));
        dataFetcherInfo.put("Consumer", consumerFieldFetchers);

        Map<String, DataFetcher> businessFieldFetchers = new HashMap<>();
        businessFieldFetchers.put("sellerInfo", async(userDataFetcher));
        businessFieldFetchers.put("sellerInfoList", async(userListDataFetcher));
        dataFetcherInfo.put("Business", businessFieldFetchers);

        Map<String, DataFetcher> commodityFieldFetchers = new HashMap<>();
        commodityFieldFetchers.put("item", async(commodityDataFetcher));
        commodityFieldFetchers.put("itemList", async(commodityListDataFetcher));
        dataFetcherInfo.put("Commodity", commodityFieldFetchers);

        Map<String, DataFetcher> couponFieldFetchers = new HashMap<>();
        couponFieldFetchers.put("coupon", async(couponDataFetcher));
        commodityFieldFetchers.put("couponList", async(couponListDataFetcher));
        dataFetcherInfo.put("Marketing", couponFieldFetchers);

        Map<String, DataFetcher> toolInfoFieldFetchers = new HashMap<>();
        toolInfoFieldFetchers.put("abInfo", async(abInfoDataFetcher));
        dataFetcherInfo.put("ToolInfo", toolInfoFieldFetchers);

        Map<String, DataFetcher> itemBaseInfoDataFetcher = new HashMap<>();
        itemBaseInfoDataFetcher.put("saleAmount", async(environment -> {
            Integer itemId = environment.getArgumentOrDefault("itemId", 0);
            if (itemId == null) {
                return 0;
            }
            return itemId * 10;
        }));
        dataFetcherInfo.put("ItemBaseInfo", itemBaseInfoDataFetcher);

        return dataFetcherInfo;
    }
}