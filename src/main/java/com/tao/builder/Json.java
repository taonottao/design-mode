package com.tao.builder;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 23:21
 */
public class Json {

    private Map<String, String> strData;

    private Map<String, Json> jsonData;

    private Map<String, Integer> intData;

    private Json(){}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Map<String, String> strData;
        private Map<String, Json> jsonData;
        private Map<String, Integer> intData;

        public Builder strData(Map<String, String> strData) {
            this.strData = strData;
            return this;
        }
        public Builder jsonData(Map<String, Json> jsonData) {
            this.jsonData = jsonData;
            return this;
        }
        public Builder intData(Map<String, Integer> intData) {
            this.intData = intData;
            return this;
        }
        public String build(){
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            if (strData != null) {
                String strDataStr = strData.entrySet().stream()
                        .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                        .collect(Collectors.joining(","));
                sb.append(strDataStr);
            }
            if (jsonData != null) {
                String jsonDataStr = jsonData.entrySet().stream()
                        .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue().builder().build())
                        .collect(Collectors.joining(","));
            }
            if (intData != null) {
                String intDataStr = intData.entrySet().stream()
                        .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                        .collect(Collectors.joining(","));
                sb.append(intDataStr);
            }
            sb.append("}");
            return sb.toString();
        }

    }
}
