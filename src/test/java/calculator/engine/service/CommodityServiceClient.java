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

package calculator.engine.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommodityServiceClient {

    static class ItemBaseInfo {
        private int itemId;

        private int sellerId;

        public ItemBaseInfo(int itemId, int sellerId, String name, int salePrice, int stockAmount, boolean onSale) {
            this.itemId = itemId;
            this.sellerId = sellerId;
            this.name = name;
            this.salePrice = salePrice;
            this.stockAmount = stockAmount;
            this.onSale = onSale;
        }

        private String name;

        private int salePrice;

        private int stockAmount;

        private boolean onSale;


        public int getItemId() {
            return itemId;
        }

        public void setItemId(int itemId) {
            this.itemId = itemId;
        }

        public int getSellerId() {
            return sellerId;
        }

        public void setSellerId(int sellerId) {
            this.sellerId = sellerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSalePrice() {
            return salePrice;
        }

        public void setSalePrice(int salePrice) {
            this.salePrice = salePrice;
        }

        public int getStockAmount() {
            return stockAmount;
        }

        public void setStockAmount(int stockAmount) {
            this.stockAmount = stockAmount;
        }

        public boolean isOnSale() {
            return onSale;
        }

        public void setOnSale(boolean onSale) {
            this.onSale = onSale;
        }
    }


    public static ItemBaseInfo getItemBaseInfoById(Number itemId) {
        return new ItemBaseInfo(
                itemId.intValue(),
                itemId.intValue() + 1,
                "item_name_" + itemId.intValue(),
                itemId.intValue() * 10 + 1,
                itemId.intValue() * 5 + 1,
                itemId.intValue() % 3 != 0
        );
    }

    public static List<ItemBaseInfo> batchItemBaseInfoByIds(List<Number> itemIds) {
//        System.out.println("CommodityServiceClient threadId "  + Thread.currentThread().getId());
        if (itemIds == null) {
            return Collections.emptyList();
        }
        return itemIds.stream().map(CommodityServiceClient::getItemBaseInfoById).collect(Collectors.toList());
    }


}
