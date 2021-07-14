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

package calculator.engine.directive;


import calculator.config.ConfigImpl;
import calculator.engine.script.AviatorScriptEvaluator;
import calculator.engine.script.ListContain;
import calculator.graphql.GraphQLSource;
import calculator.util.GraphQLSourceHolder;
import calculator.validation.Validator;
import com.googlecode.aviator.AviatorEvaluator;
import graphql.ExecutionResult;
import graphql.ParseAndValidateResult;
import graphql.schema.DataFetcher;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

public class SortTest {


    @Test
    public void sortCase_01() {
        Map<String, Map<String, DataFetcher>> dataFetcherInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dataFetcherInfoMap);

        String query = "" +
                "query sortCase_01{\n" +
                "    consumer{\n" +
                "        userInfoList(userIds: [3,4,1,2])\n" +
                "        @sort(key: \"userId\")\n" +
                "        {\n" +
                "            userId\n" +
                "            name\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        assert Objects.equals(
                executionResult.getData().toString(),
                "{consumer={userInfoList=[{userId=1, name=1_name}, {userId=2, name=2_name}, {userId=3, name=3_name}, {userId=4, name=4_name}]}}"
        );
    }


    @Test
    public void reversedSortCase_01() {
        Map<String, Map<String, DataFetcher>> dataFetcherInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dataFetcherInfoMap);

        String query = "" +
                "query sortCase_01{\n" +
                "    consumer{\n" +
                "        userInfoList(userIds: [3,4,1,2])\n" +
                "        @sort(key: \"userId\",reversed: true)\n" +
                "        {\n" +
                "            userId\n" +
                "            name\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        assert Objects.equals(
                executionResult.getData().toString(),
                "{consumer={userInfoList=[{userId=4, name=4_name}, {userId=3, name=3_name}, {userId=2, name=2_name}, {userId=1, name=1_name}]}}"
        );
    }

    @Test
    public void sortByCase_01() {
        Map<String, Map<String, DataFetcher>> dataFetcherInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dataFetcherInfoMap);

        String query = "" +
                "query sortByCase_01{\n" +
                "    consumer{\n" +
                "        userInfoList(userIds: [1,2,3,4])\n" +
                "        @sortBy(comparator: \"userId%2\")\n" +
                "        {\n" +
                "            userId\n" +
                "            name\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        assert Objects.equals(
                executionResult.getData().toString(),
                "{consumer={userInfoList=[{userId=2, name=2_name}, {userId=4, name=4_name}, {userId=1, name=1_name}, {userId=3, name=3_name}]}}"
        );
    }


    @Test
    public void reversedSortByCase_01() {
        Map<String, Map<String, DataFetcher>> dataFetcherInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dataFetcherInfoMap);

        String query = "" +
                "query sortByCase_01{\n" +
                "    consumer{\n" +
                "        userInfoList(userIds: [1,2,3,4])\n" +
                "        @sortBy(comparator: \"userId%2\",reversed: true)\n" +
                "        {\n" +
                "            userId\n" +
                "            name\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        assert Objects.equals(
                executionResult.getData().toString(),
                "{consumer={userInfoList=[{userId=1, name=1_name}, {userId=3, name=3_name}, {userId=2, name=2_name}, {userId=4, name=4_name}]}}"
        );
    }

    @Test
    public void sortByResult_case01() {
        AviatorEvaluator.addFunction(new ListContain());
        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(GraphQLSourceHolder.defaultDataFetcherInfo());

        String query = "" +
                "query sortResult_case01{\n" +
                "    commodity{\n" +
                "        itemList(itemIds: [3,4,1,2])\n" +
                "        @sortBy(comparator: \"sortKey\")\n" +
                "        {\n" +
                "            itemId\n" +
                "            sortKey: itemId @map(mapper: \"itemId\")\n" +
                "            salePrice\n" +
                "            saleAmount(itemId: 1)\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        Map<String, Map<String, Object>> data = executionResult.getData();
        System.out.println(Thread.currentThread().getId());
        assert Objects.equals(data.get("commodity").get("itemList").toString(),
                "[{itemId=1, sortKey=1, salePrice=11, saleAmount=10}, " +
                        "{itemId=2, sortKey=2, salePrice=21, saleAmount=10}, " +
                        "{itemId=3, sortKey=3, salePrice=31, saleAmount=10}, " +
                        "{itemId=4, sortKey=4, salePrice=41, saleAmount=10}]"
        );
    }

    // todo 线程block问题
    @Test
    public void sortByWithSource_case01() {
        Map<String, Map<String, DataFetcher>> dataFetcherInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        AviatorEvaluator.addFunction(new ListContain());

        GraphQLSource graphQLSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(
                dataFetcherInfoMap,
                ConfigImpl.newConfig().scriptEvaluator(new AviatorScriptEvaluator()).build()
        );

        String query = "" +
                "query sortByWithSource_case01{\n" +
                "    commodity{\n" +
                "        itemList(itemIds: [9,11,10,12])\n" +
                "        @sortBy(comparator: \"listContain(bindingItemIds111,itemId)\",dependencySources: \"bindingItemIds111\")\n" +
                "        {\n" +
                "            itemId\n" +
                "            name\n" +
                "            salePrice\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    marketing{\n" +
                "        coupon(couponId: 1){\n" +
                "            bindingItemIds @fetchSource(name: \"bindingItemIds111\")\n" +
                "        }\n" +
                "    }\n" +
                "}";
        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphQLSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionResult executionResult = graphQLSource.getGraphQL().execute(query);
        assert executionResult.getErrors().isEmpty();
        Map<String, Map<String, Object>> data = executionResult.getData();
        assert Objects.equals(
                data.get("commodity").get("itemList").toString(),
                "[{itemId=11, name=item_name_11, salePrice=111}, {itemId=12, name=item_name_12, salePrice=121}, {itemId=9, name=item_name_9, salePrice=91}, {itemId=10, name=item_name_10, salePrice=101}]"
        );
    }

}
