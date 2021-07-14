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

package calculator.engine;

import calculator.config.ConfigImpl;
import calculator.engine.service.CommodityServiceClient;
import calculator.graphql.GraphQLSource;
import calculator.util.GraphQLSourceHolder;
import calculator.validation.Validator;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphqlErrorException;
import graphql.ParseAndValidateResult;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static calculator.util.TestUtil.listsWithSameElements;
import static calculator.graphql.AsyncDataFetcher.async;

public class SupportDataFetcherResultTest {

    @Test
    public void DataFetcherResultTest(){
        DataFetcher<Object> commodityListDataFetcher = environment -> {
            Map<String, Object> arguments = environment.getArguments();
            List<Number> ids = (List<Number>) arguments.get("itemIds");
            return DataFetcherResult.newResult().data(CommodityServiceClient.batchItemBaseInfoByIds(ids)).build();
        };

        Map<String, Map<String, DataFetcher>> dfInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
        dfInfoMap.get("Commodity").put("itemList",async(commodityListDataFetcher));

        GraphQLSource graphqlSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dfInfoMap);

        String query = ""
                + "query( $couponId: Int){\n" +
                "    commodity{\n" +
                "        itemList(itemIds: 1)\n" +
                "        @argumentTransform(argumentName: \"itemIds\", operateType: MAP,dependencySources: \"itemIdList\",expression: \"itemIdList\")\n" +
                "        @filter(predicate: \"onSale\")\n" +
                "        {\n" +
                "           itemId\n" +
                "            name\n" +
                "            salePrice\n" +
                "            onSale\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    marketing{\n" +
                "        coupon(couponId: $couponId){\n" +
                "            bindingItemIds\n" +
                "            @fetchSource(name: \"itemIdList\")\n" +
                "        }\n" +
                "    }\n" +
                "}";

        ParseAndValidateResult validateResult = Validator.validateQuery(query, graphqlSource.getWrappedSchema(), ConfigImpl.newConfig().build());
        assert !validateResult.isFailure();

        ExecutionInput skipInput = ExecutionInput
                .newExecutionInput(query)
                .variables(Collections.singletonMap("couponId", 1L))
                .build();
        ExecutionResult executionResult = graphqlSource.getGraphQL().execute(skipInput);

        assert executionResult != null;
        assert executionResult.getErrors() == null || executionResult.getErrors().isEmpty();
        Map<String, Map<String, Object>> data = executionResult.getData();
        assert listsWithSameElements(((Map<String, List>) data.get("marketing").get("coupon")).get("bindingItemIds"), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        List<Map<String, Object>> itemListInfo = (List<Map<String, Object>>) data.get("commodity").get("itemList");
        assert itemListInfo.size() == 7;
        assert Objects.equals(
                itemListInfo.toString(),
                "[{itemId=1, name=item_name_1, salePrice=11, onSale=true}, " +
                        "{itemId=2, name=item_name_2, salePrice=21, onSale=true}, " +
                        "{itemId=4, name=item_name_4, salePrice=41, onSale=true}, " +
                        "{itemId=5, name=item_name_5, salePrice=51, onSale=true}, " +
                        "{itemId=7, name=item_name_7, salePrice=71, onSale=true}, " +
                        "{itemId=8, name=item_name_8, salePrice=81, onSale=true}, " +
                        "{itemId=10, name=item_name_10, salePrice=101, onSale=true}]"
        );

    }

    @Test
    public void withErrorDataFetcherResultTest(){

        {
            DataFetcher<Object> commodityListDataFetcher = environment -> {
                Map<String, Object> arguments = environment.getArguments();
                List<Number> ids = (List<Number>) arguments.get("itemIds");
                return DataFetcherResult.newResult()
                        .data(CommodityServiceClient.batchItemBaseInfoByIds(ids))
                        .error(GraphqlErrorException.newErrorException().message("mock exception").build())
                        .build();
            };

            Map<String, Map<String, DataFetcher>> dfInfoMap = GraphQLSourceHolder.defaultDataFetcherInfo();
            dfInfoMap.get("Commodity").put("itemList",async(commodityListDataFetcher));

            GraphQLSource graphqlSource = GraphQLSourceHolder.getGraphQLByDataFetcherMap(dfInfoMap);

            String query = ""
                    + "query( $couponId: Int){\n" +
                    "    commodity{\n" +
                    "        itemList(itemIds: 1)\n" +
                    "        @argumentTransform(argumentName: \"itemIds\", operateType: MAP,dependencySources: \"itemIdList\",expression: \"itemIdList\")\n" +
                    "        @filter(predicate: \"onSale\")\n" +
                    "        {\n" +
                    "           itemId\n" +
                    "            name\n" +
                    "            salePrice\n" +
                    "            onSale\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    marketing{\n" +
                    "        coupon(couponId: $couponId){\n" +
                    "            bindingItemIds\n" +
                    "            @fetchSource(name: \"itemIdList\")\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            ParseAndValidateResult validateResult = Validator.validateQuery(query, graphqlSource.getWrappedSchema(), ConfigImpl.newConfig().build());
            assert !validateResult.isFailure();

            ExecutionInput skipInput = ExecutionInput
                    .newExecutionInput(query)
                    .variables(Collections.singletonMap("couponId", 1L))
                    .build();
            ExecutionResult executionResult = graphqlSource.getGraphQL().execute(skipInput);

            assert executionResult != null;
            assert executionResult.getErrors().size() == 1;
            assert Objects.equals(executionResult.getErrors().get(0).getMessage(),"mock exception");
            Map<String, Map<String, Object>> data = executionResult.getData();
            assert listsWithSameElements(((Map<String, List>) data.get("marketing").get("coupon")).get("bindingItemIds"), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
            List<Map<String, Object>> itemListInfo = (List<Map<String, Object>>) data.get("commodity").get("itemList");
            assert itemListInfo.size() == 7;
            assert Objects.equals(
                    itemListInfo.toString(),
                    "[{itemId=1, name=item_name_1, salePrice=11, onSale=true}, " +
                            "{itemId=2, name=item_name_2, salePrice=21, onSale=true}, " +
                            "{itemId=4, name=item_name_4, salePrice=41, onSale=true}, " +
                            "{itemId=5, name=item_name_5, salePrice=51, onSale=true}, " +
                            "{itemId=7, name=item_name_7, salePrice=71, onSale=true}, " +
                            "{itemId=8, name=item_name_8, salePrice=81, onSale=true}, " +
                            "{itemId=10, name=item_name_10, salePrice=101, onSale=true}]"
            );
        }
    }
}
