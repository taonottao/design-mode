package com.tao.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 23:35
 * JSON构建器版本对比演示
 * 展示Json 1.0版本与JsonV2优化版本的差异和改进
 */
public class JsonComparisonDemo {
    
    public static void main(String[] args) {
        System.out.println("==================== JSON构建器版本对比演示 ====================");
        
        // ==================== 基础JSON构建对比 ====================
        System.out.println("\n【基础JSON构建对比】");
        
        System.out.println("\n1. 简单键值对构建:");
        
        // Json 1.0版本 - 需要预先准备Map
        System.out.println("Json 1.0版本:");
        Map<String, String> strData = new HashMap<>();
        strData.put("name", "张三");
        strData.put("city", "北京");
        
        Map<String, Integer> intData = new HashMap<>();
        intData.put("age", 25);
        intData.put("score", 95);
        
        String json1_simple = Json.builder()
                .strData(strData)
                .intData(intData)
                .build();
        System.out.println("结果: " + json1_simple);
        System.out.println("问题: 需要预先准备Map，API不够流畅，且存在拼接BUG");
        
        // JsonV2版本 - 流畅的键值对API
        System.out.println("\nJsonV2版本:");
        String jsonv2_simple = JsonV2.create()
                .putString("name", "张三")
                .putString("city", "北京")
                .putNumber("age", 25)
                .putNumber("score", 95)
                .buildJsonString();
        System.out.println("结果: " + jsonv2_simple);
        System.out.println("优势: 流畅的API，逐个添加键值对，类型安全");
        
        // ==================== 数据类型支持对比 ====================
        System.out.println("\n【数据类型支持对比】");
        
        System.out.println("\n2. 多种数据类型支持:");
        System.out.println("Json 1.0: 仅支持String、Integer、Json三种类型");
        
        System.out.println("\nJsonV2: 支持所有JSON数据类型");
        String jsonv2_types = JsonV2.create()
                .putString("name", "李四")
                .putNumber("age", 30)
                .putNumber("salary", 8500.50)
                .putBoolean("married", true)
                .putBoolean("hasChildren", false)
                .putNull("middleName")
                .buildJsonString();
        System.out.println("结果: " + jsonv2_types);
        
        // ==================== 嵌套对象构建对比 ====================
        System.out.println("\n【嵌套对象构建对比】");
        
        System.out.println("\n3. 嵌套JSON对象:");
        System.out.println("Json 1.0: 需要先创建子Json对象，然后放入Map中");
        
        System.out.println("\nJsonV2: 支持回调函数方式，更加直观");
        String jsonv2_nested = JsonV2.create()
                .putString("name", "王五")
                .putNumber("age", 28)
                .putObject("address", addr -> addr
                        .putString("province", "北京市")
                        .putString("city", "北京市")
                        .putString("district", "朝阳区")
                        .putString("street", "三里屯街道")
                )
                .putObject("company", company -> company
                        .putString("name", "科技有限公司")
                        .putString("department", "研发部")
                        .putString("position", "高级工程师")
                )
                .buildJsonString();
        System.out.println("结果: " + jsonv2_nested);
        
        // ==================== 数组支持对比 ====================
        System.out.println("\n【数组支持对比】");
        
        System.out.println("\n4. JSON数组构建:");
        System.out.println("Json 1.0: 不支持数组构建");
        
        System.out.println("\nJsonV2: 完整的数组支持");
        String jsonv2_array = JsonV2.create()
                .putString("name", "赵六")
                .putArray("hobbies", arr -> arr
                        .addString("编程")
                        .addString("阅读")
                        .addString("旅游")
                )
                .putArray("scores", arr -> arr
                        .addNumber(95)
                        .addNumber(87)
                        .addNumber(92)
                )
                .putArray("friends", arr -> arr
                        .addObject(friend -> friend
                                .putString("name", "小明")
                                .putNumber("age", 25)
                        )
                        .addObject(friend -> friend
                                .putString("name", "小红")
                                .putNumber("age", 23)
                        )
                )
                .buildJsonString();
        System.out.println("结果: " + jsonv2_array);
        
        // ==================== 条件构建对比 ====================
        System.out.println("\n【条件构建对比】");
        
        System.out.println("\n5. 条件性字段添加:");
        System.out.println("Json 1.0: 不支持条件构建");
        
        System.out.println("\nJsonV2: 支持多种条件构建方式");
        boolean includeEmail = true;
        boolean includePhone = false;
        String email = "user@example.com";
        String phone = null;
        
        String jsonv2_conditional = JsonV2.create()
                .putString("name", "条件测试")
                .putNumber("age", 30)
                .putIf(includeEmail, "email", email)
                .putIf(includePhone, "phone", "13800138000")
                .putIfNotNull("mobile", phone)
                .putIfNotEmpty("description", "")
                .buildJsonString();
        System.out.println("结果: " + jsonv2_conditional);
        
        // ==================== 静态工厂方法对比 ====================
        System.out.println("\n【静态工厂方法对比】");
        
        System.out.println("\n6. 快速创建方式:");
        System.out.println("Json 1.0: 只有builder()方法");
        
        System.out.println("\nJsonV2: 多种静态工厂方法");
        
        // 快速创建单键值对
        JsonV2 quickJson = JsonV2.of("message", "Hello World");
        System.out.println("快速创建: " + quickJson.toJsonString());
        
        // 空JSON对象
        JsonV2 emptyJson = JsonV2.empty();
        System.out.println("空对象: " + emptyJson.toJsonString());
        
        // 从Map创建
        Map<String, Object> existingData = new HashMap<>();
        existingData.put("key1", "value1");
        existingData.put("key2", 42);
        JsonV2 fromMapJson = JsonV2.fromMap(existingData)
                .putString("key3", "value3")
                .build();
        System.out.println("从Map创建: " + fromMapJson.toJsonString());
        
        // ==================== 验证和错误处理对比 ====================
        System.out.println("\n【验证和错误处理对比】");
        
        System.out.println("\n7. 参数验证:");
        System.out.println("Json 1.0: 无参数验证，容易出错");
        
        System.out.println("\nJsonV2: 完善的参数验证");
        try {
            JsonV2.create()
                    .putString("name", "测试用户")
                    .putNumber("age", 25)
                    .requireKeys("name", "age", "email") // 验证必填字段
                    .build();
        } catch (IllegalStateException e) {
            System.out.println("验证失败: " + e.getMessage());
        }
        
        try {
            JsonV2.create()
                    .putString("key1", "value1")
                    .putString("key1", "value2"); // 重复键名
        } catch (IllegalArgumentException e) {
            System.out.println("重复键名检查: " + e.getMessage());
        }
        
        // ==================== 性能和功能对比总结 ====================
        System.out.println("\n==================== 版本对比总结 ====================");
        
        System.out.println("\n【Json 1.0版本特点】");
        System.out.println("✅ 基本实现了Builder模式");
        System.out.println("✅ 支持链式调用");
        System.out.println("✅ 支持基础的嵌套对象");
        System.out.println("❌ 数据类型支持有限（仅String、Integer、Json）");
        System.out.println("❌ API设计不够灵活（需要预先准备Map）");
        System.out.println("❌ 存在字段拼接BUG（jsonData未添加到结果中）");
        System.out.println("❌ 不支持数组构建");
        System.out.println("❌ 缺少条件构建功能");
        System.out.println("❌ 缺少参数验证和错误处理");
        
        System.out.println("\n【JsonV2版本改进】");
        System.out.println("🚀 支持所有JSON数据类型（String、Number、Boolean、null、Object、Array）");
        System.out.println("🚀 流畅的键值对API：put(key, value)方式");
        System.out.println("🚀 完整的数组支持：putArray()和ArrayBuilder");
        System.out.println("🚀 嵌套对象优化：回调函数和子构建器两种方式");
        System.out.println("🚀 条件构建功能：putIf()、putIfNotNull()等");
        System.out.println("🚀 严格的验证机制：键名重复检查、循环引用检测");
        System.out.println("🚀 性能优化：LinkedHashMap保持顺序，延迟序列化");
        System.out.println("🚀 完善的错误处理：详细异常信息和参数验证");
        System.out.println("🚀 静态工厂方法：多种创建方式");
        System.out.println("🚀 不可变对象设计：构建完成后不可修改");
        
        System.out.println("\n【建议使用场景】");
        System.out.println("• Json 1.0: 适用于学习Builder模式基础概念，简单JSON构建需求");
        System.out.println("• JsonV2 : 适用于复杂JSON构建需求，生产环境使用，追求类型安全和功能完整性");
        
        System.out.println("\n【Builder模式核心价值体现】");
        System.out.println("1. 流畅的API设计 - 链式调用提供良好的可读性");
        System.out.println("2. 复杂度隐藏 - 将复杂的JSON构建过程封装");
        System.out.println("3. 类型安全 - 编译时检查，避免运行时错误");
        System.out.println("4. 参数验证 - 在构建过程中进行验证");
        System.out.println("5. 扩展性 - 易于添加新的功能和数据类型支持");
    }
}