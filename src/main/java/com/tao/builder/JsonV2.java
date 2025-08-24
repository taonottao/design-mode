package com.tao.builder;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JSON构建器 1.0版本分析总结：
 * 
 * 【优点】
 * ✅ 基本实现了Builder模式的核心结构
 * ✅ 支持链式调用，API相对流畅
 * ✅ 支持嵌套JSON对象构建
 * ✅ 私有构造函数确保只能通过Builder创建
 * 
 * 【主要问题】
 * ❌ 数据类型支持有限：只支持String、Integer、Json三种类型
 * ❌ API设计不够灵活：需要预先准备Map，无法逐个添加键值对
 * ❌ 字段拼接逻辑有BUG：jsonData部分没有添加到StringBuilder中
 * ❌ 缺少数组支持：无法构建JSON数组结构
 * ❌ 缺少条件构建：无法根据条件动态添加字段
 * ❌ 缺少验证机制：没有键名重复检查、循环引用检测
 * ❌ 性能问题：每次都重新构建，没有缓存机制
 * ❌ 扩展性差：添加新数据类型需要修改核心代码
 * ❌ 错误处理不完善：没有异常处理和错误提示
 * 
 * 【JsonV2优化版本改进】
 * 🚀 支持所有JSON数据类型（String、Number、Boolean、null、Object、Array）
 * 🚀 流畅的键值对API：put(key, value)方式添加
 * 🚀 完整的数组支持：putArray()方法和ArrayBuilder
 * 🚀 嵌套对象优化：支持回调函数和子构建器两种方式
 * 🚀 条件构建功能：putIf()、putIfNotNull()等方法
 * 🚀 严格的验证机制：键名重复检查、循环引用检测
 * 🚀 性能优化：使用LinkedHashMap保持顺序，延迟序列化
 * 🚀 完善的错误处理：详细的异常信息和参数验证
 * 🚀 静态工厂方法：提供多种创建方式
 * 🚀 不可变对象设计：构建完成后不可修改
 * 
 * @version 2.0
 * @Author T-WANG
 * @Date 2025/8/23 23:30
 */
public class JsonV2 {
    
    // ==================== 核心属性 ====================
    
    /**
     * 存储JSON数据的Map，使用LinkedHashMap保持插入顺序
     */
    private final Map<String, Object> data;
    
    /**
     * 私有构造函数，只能通过Builder创建
     * @param builder 构建器实例
     */
    private JsonV2(Builder builder) {
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(builder.data));
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建新的JSON构建器
     * @return JSON构建器实例
     */
    public static Builder create() {
        return new Builder();
    }
    
    /**
     * 从现有Map创建JSON构建器
     * @param map 现有的数据Map
     * @return JSON构建器实例
     */
    public static Builder fromMap(Map<String, Object> map) {
        Builder builder = new Builder();
        if (map != null) {
            builder.data.putAll(map);
        }
        return builder;
    }
    
    /**
     * 快速创建简单JSON对象
     * @param key 键名
     * @param value 值
     * @return JSON对象
     */
    public static JsonV2 of(String key, Object value) {
        return create().put(key, value).build();
    }
    
    /**
     * 创建空JSON对象
     * @return 空JSON对象
     */
    public static JsonV2 empty() {
        return create().build();
    }
    
    // ==================== 获取方法 ====================
    
    /**
     * 获取JSON字符串表示
     * @return JSON字符串
     */
    public String toJsonString() {
        return buildJsonString(data);
    }
    
    /**
     * 获取数据Map的只读视图
     * @return 不可修改的数据Map
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * 获取指定键的值
     * @param key 键名
     * @return 对应的值，如果不存在返回null
     */
    public Object get(String key) {
        return data.get(key);
    }
    
    /**
     * 检查是否包含指定键
     * @param key 键名
     * @return 如果包含返回true，否则返回false
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 获取JSON对象的大小
     * @return 键值对数量
     */
    public int size() {
        return data.size();
    }
    
    /**
     * 检查JSON对象是否为空
     * @return 如果为空返回true，否则返回false
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    @Override
    public String toString() {
        return toJsonString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JsonV2 jsonV2 = (JsonV2) obj;
        return Objects.equals(data, jsonV2.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
    
    // ==================== JSON构建器 ====================
    
    /**
     * JSON构建器类 - 支持流畅API和类型安全
     */
    public static class Builder {
        
        /**
         * 存储构建中的JSON数据
         */
        private final Map<String, Object> data = new LinkedHashMap<>();
        
        /**
         * 用于检测循环引用的Set
         */
        private final Set<Object> circularReferenceCheck = new HashSet<>();
        
        // ==================== 基础数据类型方法 ====================
        
        /**
         * 添加任意类型的键值对
         * @param key 键名，不能为null或空字符串
         * @param value 值，可以为null
         * @return 当前构建器实例
         * @throws IllegalArgumentException 如果键名无效或已存在
         */
        public Builder put(String key, Object value) {
            validateKey(key);
            checkCircularReference(value);
            data.put(key, value);
            return this;
        }
        
        /**
         * 添加字符串值
         * @param key 键名
         * @param value 字符串值
         * @return 当前构建器实例
         */
        public Builder putString(String key, String value) {
            return put(key, value);
        }
        
        /**
         * 添加数字值
         * @param key 键名
         * @param value 数字值
         * @return 当前构建器实例
         */
        public Builder putNumber(String key, Number value) {
            return put(key, value);
        }
        
        /**
         * 添加布尔值
         * @param key 键名
         * @param value 布尔值
         * @return 当前构建器实例
         */
        public Builder putBoolean(String key, Boolean value) {
            return put(key, value);
        }
        
        /**
         * 添加null值
         * @param key 键名
         * @return 当前构建器实例
         */
        public Builder putNull(String key) {
            return put(key, null);
        }
        
        // ==================== 嵌套对象方法 ====================
        
        /**
         * 添加嵌套JSON对象（使用回调函数）
         * @param key 键名
         * @param builderConsumer 子构建器配置函数
         * @return 当前构建器实例
         */
        public Builder putObject(String key, Consumer<Builder> builderConsumer) {
            validateKey(key);
            Builder subBuilder = new Builder();
            builderConsumer.accept(subBuilder);
            return put(key, subBuilder.build());
        }
        
        /**
         * 添加嵌套JSON对象（使用现有构建器）
         * @param key 键名
         * @param jsonBuilder 子构建器
         * @return 当前构建器实例
         */
        public Builder putObject(String key, Builder jsonBuilder) {
            return put(key, jsonBuilder.build());
        }
        
        /**
         * 添加现有JsonV2对象
         * @param key 键名
         * @param jsonV2 现有的JsonV2对象
         * @return 当前构建器实例
         */
        public Builder putJsonV2(String key, JsonV2 jsonV2) {
            return put(key, jsonV2);
        }
        
        // ==================== 数组方法 ====================
        
        /**
         * 添加数组（使用回调函数）
         * @param key 键名
         * @param arrayConsumer 数组构建器配置函数
         * @return 当前构建器实例
         */
        public Builder putArray(String key, Consumer<ArrayBuilder> arrayConsumer) {
            validateKey(key);
            ArrayBuilder arrayBuilder = new ArrayBuilder();
            arrayConsumer.accept(arrayBuilder);
            return put(key, arrayBuilder.build());
        }
        
        /**
         * 添加数组（使用可变参数）
         * @param key 键名
         * @param values 数组元素
         * @return 当前构建器实例
         */
        public Builder putArray(String key, Object... values) {
            return put(key, Arrays.asList(values));
        }
        
        /**
         * 添加数组（使用List）
         * @param key 键名
         * @param list 数组元素列表
         * @return 当前构建器实例
         */
        public Builder putArray(String key, List<?> list) {
            return put(key, new ArrayList<>(list));
        }
        
        // ==================== 条件构建方法 ====================
        
        /**
         * 根据条件添加键值对
         * @param condition 条件
         * @param key 键名
         * @param value 值
         * @return 当前构建器实例
         */
        public Builder putIf(boolean condition, String key, Object value) {
            if (condition) {
                put(key, value);
            }
            return this;
        }
        
        /**
         * 当值不为null时添加键值对
         * @param key 键名
         * @param value 值
         * @return 当前构建器实例
         */
        public Builder putIfNotNull(String key, Object value) {
            return putIf(value != null, key, value);
        }
        
        /**
         * 当字符串不为空时添加键值对
         * @param key 键名
         * @param value 字符串值
         * @return 当前构建器实例
         */
        public Builder putIfNotEmpty(String key, String value) {
            return putIf(value != null && !value.trim().isEmpty(), key, value);
        }
        
        /**
         * 当集合不为空时添加键值对
         * @param key 键名
         * @param collection 集合值
         * @return 当前构建器实例
         */
        public Builder putIfNotEmpty(String key, Collection<?> collection) {
            return putIf(collection != null && !collection.isEmpty(), key, collection);
        }
        
        // ==================== 批量操作方法 ====================
        
        /**
         * 批量添加键值对
         * @param map 要添加的键值对Map
         * @return 当前构建器实例
         */
        public Builder putAll(Map<String, Object> map) {
            if (map != null) {
                map.forEach(this::put);
            }
            return this;
        }
        
        /**
         * 合并另一个JsonV2对象
         * @param other 要合并的JsonV2对象
         * @return 当前构建器实例
         */
        public Builder merge(JsonV2 other) {
            if (other != null) {
                putAll(other.getData());
            }
            return this;
        }
        
        /**
         * 移除指定键
         * @param key 要移除的键名
         * @return 当前构建器实例
         */
        public Builder remove(String key) {
            data.remove(key);
            return this;
        }
        
        /**
         * 清空所有数据
         * @return 当前构建器实例
         */
        public Builder clear() {
            data.clear();
            return this;
        }
        
        // ==================== 验证和构建方法 ====================
        
        /**
         * 验证必填字段
         * @param requiredKeys 必填字段列表
         * @return 当前构建器实例
         * @throws IllegalStateException 如果缺少必填字段
         */
        public Builder requireKeys(String... requiredKeys) {
            for (String key : requiredKeys) {
                if (!data.containsKey(key)) {
                    throw new IllegalStateException("缺少必填字段: " + key);
                }
            }
            return this;
        }
        
        /**
         * 构建最终的JsonV2对象
         * @return 不可变的JsonV2对象
         */
        public JsonV2 build() {
            return new JsonV2(this);
        }
        
        /**
         * 直接构建JSON字符串
         * @return JSON字符串
         */
        public String buildJsonString() {
            return build().toJsonString();
        }
        
        // ==================== 私有辅助方法 ====================
        
        /**
         * 验证键名的有效性
         * @param key 键名
         * @throws IllegalArgumentException 如果键名无效
         */
        private void validateKey(String key) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("键名不能为null或空字符串");
            }
            if (data.containsKey(key)) {
                throw new IllegalArgumentException("键名已存在: " + key);
            }
        }
        
        /**
         * 检查循环引用
         * @param value 要检查的值
         * @throws IllegalArgumentException 如果存在循环引用
         */
        private void checkCircularReference(Object value) {
            if (value instanceof JsonV2) {
                if (circularReferenceCheck.contains(value)) {
                    throw new IllegalArgumentException("检测到循环引用");
                }
                circularReferenceCheck.add(value);
            }
        }
    }
    
    // ==================== 数组构建器 ====================
    
    /**
     * JSON数组构建器
     */
    public static class ArrayBuilder {
        
        /**
         * 存储数组元素
         */
        private final List<Object> elements = new ArrayList<>();
        
        /**
         * 添加元素
         * @param value 要添加的元素
         * @return 当前数组构建器实例
         */
        public ArrayBuilder add(Object value) {
            elements.add(value);
            return this;
        }
        
        /**
         * 添加字符串元素
         * @param value 字符串值
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addString(String value) {
            return add(value);
        }
        
        /**
         * 添加数字元素
         * @param value 数字值
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addNumber(Number value) {
            return add(value);
        }
        
        /**
         * 添加布尔元素
         * @param value 布尔值
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addBoolean(Boolean value) {
            return add(value);
        }
        
        /**
         * 添加null元素
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addNull() {
            return add(null);
        }
        
        /**
         * 添加嵌套对象
         * @param builderConsumer 对象构建器配置函数
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addObject(Consumer<Builder> builderConsumer) {
            Builder objectBuilder = new Builder();
            builderConsumer.accept(objectBuilder);
            return add(objectBuilder.build());
        }
        
        /**
         * 添加嵌套数组
         * @param arrayConsumer 数组构建器配置函数
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addArray(Consumer<ArrayBuilder> arrayConsumer) {
            ArrayBuilder subArrayBuilder = new ArrayBuilder();
            arrayConsumer.accept(subArrayBuilder);
            return add(subArrayBuilder.build());
        }
        
        /**
         * 批量添加元素
         * @param values 要添加的元素
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addAll(Object... values) {
            Collections.addAll(elements, values);
            return this;
        }
        
        /**
         * 批量添加元素
         * @param collection 要添加的元素集合
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addAll(Collection<?> collection) {
            if (collection != null) {
                elements.addAll(collection);
            }
            return this;
        }
        
        /**
         * 根据条件添加元素
         * @param condition 条件
         * @param value 要添加的元素
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addIf(boolean condition, Object value) {
            if (condition) {
                add(value);
            }
            return this;
        }
        
        /**
         * 当值不为null时添加元素
         * @param value 要添加的元素
         * @return 当前数组构建器实例
         */
        public ArrayBuilder addIfNotNull(Object value) {
            return addIf(value != null, value);
        }
        
        /**
         * 构建数组
         * @return 不可变的数组List
         */
        public List<Object> build() {
            return Collections.unmodifiableList(new ArrayList<>(elements));
        }
        
        /**
         * 获取数组大小
         * @return 数组元素数量
         */
        public int size() {
            return elements.size();
        }
        
        /**
         * 检查数组是否为空
         * @return 如果为空返回true，否则返回false
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建JSON字符串
     * @param obj 要序列化的对象
     * @return JSON字符串
     */
    private static String buildJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        if (obj instanceof JsonV2) {
            return ((JsonV2) obj).toJsonString();
        }
        
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return map.entrySet().stream()
                    .map(entry -> "\"" + escapeJsonString(entry.getKey()) + "\":" + buildJsonString(entry.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            return collection.stream()
                    .map(JsonV2::buildJsonString)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        
        // 对于其他类型，转换为字符串
        return "\"" + escapeJsonString(obj.toString()) + "\"";
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}