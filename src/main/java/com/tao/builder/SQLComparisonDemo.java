package com.tao.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 20:35
 * SQL建造者模式版本对比演示
 * 展示SQL 1.0版本与SQLv2优化版本的差异和改进
 */
public class SQLComparisonDemo {
    
    public static void main(String[] args) {
        System.out.println("==================== SQL建造者模式版本对比 ====================");
        
        // ==================== SELECT语句对比 ====================
        System.out.println("\n【SELECT语句对比】");
        
        System.out.println("\n1. 基础SELECT查询:");
        // SQL 1.0版本
        String sql1_select = SQL.select("id", "name", "age")
                .from("users")
                .where("age > 18")
                .buildSql();
        System.out.println("SQL 1.0: " + sql1_select);
        
        // SQLv2版本
        String sqlv2_select = SQLv2.select("id", "name", "age")
                .from("users")
                .where("age > 18")
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_select);
        
        System.out.println("\n2. 复杂SELECT查询(SQLv2新增功能):");
        System.out.println("SQL 1.0: 不支持JOIN、ORDER BY、LIMIT等高级功能");
        
        String sqlv2_complex = SQLv2.select("u.name", "p.title", "u.age")
                .from("users u")
                .leftJoin("posts p", "u.id = p.user_id")
                .where("u.age > 18")
                .orderByDesc("u.age")
                .limit(10)
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_complex);
        
        System.out.println("\n3. SELECT * 查询:");
        System.out.println("SQL 1.0: 需要手动传入\"*\"参数");
        String sql1_selectAll = SQL.select("*")
                .from("products")
                .where("price < 100")
                .buildSql();
        System.out.println("SQL 1.0: " + sql1_selectAll);
        
        System.out.println("SQLv2 : 提供专门的selectAll()方法");
        String sqlv2_selectAll = SQLv2.selectAll()
                .from("products")
                .where("price < 100")
                .limit(20, 10)
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_selectAll);
        
        // ==================== UPDATE语句对比 ====================
        System.out.println("\n\n【UPDATE语句对比】");
        
        System.out.println("\n1. 基础UPDATE语句:");
        // SQL 1.0版本
        String sql1_update = SQL.update()
                .table("users")
                .where("id = 1")
                .set("name", "张三")
                .set("age", "25")
                .buildSql();
        System.out.println("SQL 1.0: " + sql1_update);
        
        // SQLv2版本
        String sqlv2_update = SQLv2.update()
                .table("users")
                .set("name", "张三")
                .set("age", 25) // 支持不同数据类型
                .where("id = 1")
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_update);
        
        System.out.println("\n2. 不带WHERE条件的UPDATE:");
        System.out.println("SQL 1.0: 不支持可选WHERE条件");
        
        String sqlv2_updateAll = SQLv2.update()
                .table("users")
                .set("status", "active")
                .build() // SQLv2支持可选WHERE
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_updateAll);
        
        // ==================== INSERT语句对比 ====================
        System.out.println("\n\n【INSERT语句对比】");
        
        System.out.println("\n1. INSERT语句:");
        // SQL 1.0版本 - 需要分别传入列名和值数组
        String sql1_insert = SQL.insert("users")
                .columns(new String[]{"name", "age", "email"}, 
                        new String[]{"李四", "30", "lisi@example.com"})
                .buildSql();
        System.out.println("SQL 1.0: " + sql1_insert);
        
        // SQLv2版本 - 支持键值对方式
        String sqlv2_insert = SQLv2.insert()
                .into("users")
                .value("name", "李四")
                .value("age", 30)
                .value("email", "lisi@example.com")
                .build()
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_insert);
        
        System.out.println("\n2. 批量INSERT(SQLv2新增功能):");
        System.out.println("SQL 1.0: 不支持Map方式批量插入");
        
        Map<String, Object> values = new HashMap<>();
        values.put("name", "王五");
        values.put("age", 28);
        values.put("email", "wangwu@example.com");
        
        String sqlv2_batchInsert = SQLv2.insert()
                .into("users")
                .values(values)
                .build()
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_batchInsert);
        
        // ==================== DELETE语句对比 ====================
        System.out.println("\n\n【DELETE语句对比】");
        
        System.out.println("\n1. 基础DELETE语句:");
        // SQL 1.0版本
        String sql1_delete = SQL.delete()
                .from("users")
                .where("age < 18")
                .buildSql();
        System.out.println("SQL 1.0: " + sql1_delete);
        
        // SQLv2版本
        String sqlv2_delete = SQLv2.delete()
                .from("users")
                .where("age < 18")
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_delete);
        
        System.out.println("\n2. 删除所有记录:");
        // SQL 1.0版本 - 接口设计上必须调用where方法
        System.out.println("SQL 1.0: 接口设计限制，必须调用where方法");
        
        // SQLv2版本 - 支持可选WHERE
        String sqlv2_deleteAll = SQLv2.delete()
                .from("temp_table")
                .buildSql();
        System.out.println("SQLv2 : " + sqlv2_deleteAll);
        
        // ==================== 错误处理对比 ====================
        System.out.println("\n\n【错误处理对比】");
        
        System.out.println("\n1. UPDATE语句验证:");
        try {
            // SQLv2版本有更严格的验证
            SQLv2.update()
                    .table("users")
                    .where("id = 1")
                    .buildSql();
        } catch (IllegalStateException e) {
            System.out.println("SQLv2错误处理: " + e.getMessage());
        }
        
        System.out.println("\n2. INSERT语句验证:");
        try {
            // SQLv2版本有更严格的验证
            SQLv2.insert()
                    .into("users")
                    .build()
                    .buildSql();
        } catch (IllegalStateException e) {
            System.out.println("SQLv2错误处理: " + e.getMessage());
        }
        
        // ==================== 总结 ====================
        System.out.println("\n\n==================== 版本对比总结 ====================");
        System.out.println("\n【SQL 1.0版本特点】");
        System.out.println("✓ 基础的Builder模式实现");
        System.out.println("✓ 支持基本的SELECT、UPDATE、DELETE、INSERT操作");
        System.out.println("✓ 简单的链式调用");
        System.out.println("✗ 功能有限，不支持JOIN、ORDER BY、LIMIT等");
        System.out.println("✗ INSERT操作需要分别传入列名和值数组");
        System.out.println("✗ 接口设计不够灵活，某些操作强制要求WHERE条件");
        System.out.println("✗ 缺乏严格的参数验证");
        
        System.out.println("\n【SQLv2版本改进】");
        System.out.println("✓ 更清晰的阶段接口设计，提供更好的类型安全");
        System.out.println("✓ 增强的SELECT功能：支持JOIN、ORDER BY、GROUP BY、HAVING、LIMIT等");
        System.out.println("✓ 改进的UPDATE功能：类型安全的值设置、灵活的WHERE条件");
        System.out.println("✓ 重构的INSERT功能：支持键值对方式、批量插入、自动类型处理");
        System.out.println("✓ 优化的DELETE功能：简化接口、可选WHERE条件");
        System.out.println("✓ 增强的错误处理和参数验证");
        System.out.println("✓ 更好的扩展性和可维护性");
        
        System.out.println("\n【建议使用场景】");
        System.out.println("• SQL 1.0: 适用于简单的SQL构建需求，学习Builder模式基础概念");
        System.out.println("• SQLv2 : 适用于复杂的SQL构建需求，生产环境使用，追求类型安全和功能完整性");
    }
}