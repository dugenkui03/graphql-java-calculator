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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class MarketingServiceClient {

    static class Coupon {
        private int couponId;
        private int base;
        private int price;
        private int limitation;
        private String couponText;
        private List<Integer> bindingItemIds;

        public Coupon(int couponId, int base, int price, int limitation, String couponText, List<Integer> bindingItemIds) {
            this.couponId = couponId;
            this.base = base;
            this.price = price;
            this.limitation = limitation;
            this.couponText = couponText;
            this.bindingItemIds = bindingItemIds;
        }


        public int getCouponId() {
            return couponId;
        }

        public void setCouponId(int couponId) {
            this.couponId = couponId;
        }

        public int getBase() {
            return base;
        }

        public void setBase(int base) {
            this.base = base;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public int getLimitation() {
            return limitation;
        }

        public void setLimitation(int limitation) {
            this.limitation = limitation;
        }

        public String getCouponText() {
            return couponText;
        }

        public void setCouponText(String couponText) {
            this.couponText = couponText;
        }

        public List<Integer> getBindingItemIds() {
            return bindingItemIds;
        }

        public void setBindingItemIds(List<Integer> bindingItemIds) {
            this.bindingItemIds = bindingItemIds;
        }
    }

    public static Coupon getCouponInfoById(int couponId) {
//        System.out.println("MarketingServiceClient threadId "  + Thread.currentThread().getId());
        List<Integer> bindingItemIds = IntStream.range(couponId, couponId + 10).boxed().collect(Collectors.toList());
        return new Coupon(
                couponId,
                couponId + 20,
                couponId * 10 + 3,
                couponId + 1,
                "优惠券_" + couponId,
                bindingItemIds

        );
    }

    public static List<Coupon> batchCouponInfoByIds(List<Integer> couponIdList) {
        return couponIdList.stream().map(MarketingServiceClient::getCouponInfoById).collect(Collectors.toList());
    }

}
