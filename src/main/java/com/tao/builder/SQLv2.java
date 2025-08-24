package com.tao.builder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 2.0
 * @Author T-WANG
 * @Date 2025/8/23 20:30
 * 优化后的SQL建造者模式 - 方案二增强版
 * 相比于SQL 1.0版本，主要改进：
 * 1. 更清晰的阶段接口设计，提供更好的类型安全
 * 2. 增强的SELECT功能：支持JOIN、ORDER BY、GROUP BY、HAVING、LIMIT等
 * 3. 改进的UPDATE功能：类型安全的值设置、灵活的WHERE条件
 * 4. 重构的INSERT功能：支持键值对方式、批量插入
 * 5. 优化的DELETE功能：简化接口、可选WHERE条件
 */
public class SQLv2 {

    private SQLv2() {
    }

    /**
     * 创建SELECT语句构建器
     * @param columns 要查询的列名
     * @return SELECT语句的FROM阶段构建器
     */
    public static SelectFromStage select(String... columns) {
        return new SelectBuilder(columns);
    }
    
    /**
     * 创建SELECT *语句构建器
     * @return SELECT语句的FROM阶段构建器
     */
    public static SelectFromStage selectAll() {
        return new SelectBuilder("*");
    }

    /**
     * 创建UPDATE语句构建器
     * @return UPDATE语句的TABLE阶段构建器
     */
    public static UpdateTableStage update() {
        return new UpdateBuilder();
    }
    
    /**
     * 创建DELETE语句构建器
     * @return DELETE语句的FROM阶段构建器
     */
    public static DeleteFromStage delete() {
        return new DeleteBuilder();
    }
    
    /**
     * 创建INSERT语句构建器
     * @return INSERT语句的INTO阶段构建器
     */
    public static InsertIntoStage insert() {
        return new InsertBuilder();
    }

    // ==================== 接口定义 ====================
    
    /**
     * 构建最终SQL的接口
     */
    interface BuildStage {
        String buildSql();
    }
    
    // SELECT相关接口
    interface SelectFromStage {
        SelectWhereStage from(String table);
    }
    
    interface SelectWhereStage extends SelectOrderStage {
        SelectWhereStage leftJoin(String table, String condition);
        SelectWhereStage innerJoin(String table, String condition);
        SelectOrderStage where(String condition);
        SelectOrderStage groupBy(String... columns);
        SelectOrderStage having(String condition);
    }
    
    interface SelectOrderStage extends SelectLimitStage {
        SelectLimitStage orderBy(String column);
        SelectLimitStage orderByDesc(String column);
    }
    
    interface SelectLimitStage extends BuildStage {
        BuildStage limit(int count);
        BuildStage limit(int count, int offset);
    }
    
    // UPDATE相关接口
    interface UpdateTableStage {
        UpdateSetStage table(String table);
    }
    
    interface UpdateSetStage {
        UpdateSetStage set(String column, Object value);
        UpdateWhereStage where(String condition);
        BuildStage build(); // 允许不加WHERE条件
    }
    
    interface UpdateWhereStage extends BuildStage {
        // UPDATE的WHERE阶段，只能构建SQL
    }
    
    // DELETE相关接口
    interface DeleteFromStage {
        DeleteWhereStage from(String table);
    }
    
    interface DeleteWhereStage extends BuildStage {
        BuildStage where(String condition);
    }
    
    // INSERT相关接口
    interface InsertIntoStage {
        InsertValuesStage into(String table);
    }
    
    interface InsertValuesStage {
        InsertValuesStage value(String column, Object value);
        InsertValuesStage values(Map<String, Object> values);
        BuildStage build();
    }

    // ==================== 实现类 ====================

    /**
     * SELECT语句构建器 - 支持链式调用和类型安全
     */
    public static class SelectBuilder implements SelectFromStage, SelectWhereStage, SelectOrderStage, SelectLimitStage, BuildStage {
        private final String[] columns; // 查询列
        private String table; // 表名
        private final List<String> joins = new ArrayList<>(); // JOIN子句
        private String where; // WHERE条件
        private String orderBy; // ORDER BY子句
        private String groupBy; // GROUP BY子句
        private String having; // HAVING子句
        private Integer limit; // LIMIT数量
        private Integer offset; // OFFSET偏移量
        
        public SelectBuilder(String... columns) {
            this.columns = columns;
        }
        
        @Override
        public SelectWhereStage from(String table) {
            this.table = table;
            return this;
        }
        
        @Override
        public SelectWhereStage leftJoin(String table, String condition) {
            joins.add("LEFT JOIN " + table + " ON " + condition);
            return this;
        }
        
        @Override
        public SelectWhereStage innerJoin(String table, String condition) {
            joins.add("INNER JOIN " + table + " ON " + condition);
            return this;
        }
        
        @Override
        public SelectOrderStage where(String condition) {
            this.where = condition;
            return this;
        }
        
        @Override
        public SelectOrderStage groupBy(String... columns) {
            this.groupBy = String.join(", ", columns);
            return this;
        }
        
        @Override
        public SelectOrderStage having(String condition) {
            this.having = condition;
            return this;
        }
        
        @Override
        public SelectLimitStage orderBy(String column) {
            this.orderBy = column;
            return this;
        }
        
        @Override
        public SelectLimitStage orderByDesc(String column) {
            this.orderBy = column + " DESC";
            return this;
        }
        
        @Override
        public BuildStage limit(int count) {
            this.limit = count;
            return this;
        }
        
        @Override
        public BuildStage limit(int count, int offset) {
            this.limit = count;
            this.offset = offset;
            return this;
        }
        
        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            
            // SELECT子句
            sql.append("SELECT ").append(String.join(", ", columns));
            
            // FROM子句
            sql.append(" FROM ").append(table);
            
            // JOIN子句
            if (!joins.isEmpty()) {
                sql.append(" ").append(String.join(" ", joins));
            }
            
            // WHERE子句
            if (where != null && !where.trim().isEmpty()) {
                sql.append(" WHERE ").append(where);
            }
            
            // GROUP BY子句
            if (groupBy != null && !groupBy.trim().isEmpty()) {
                sql.append(" GROUP BY ").append(groupBy);
            }
            
            // HAVING子句
            if (having != null && !having.trim().isEmpty()) {
                sql.append(" HAVING ").append(having);
            }
            
            // ORDER BY子句
            if (orderBy != null && !orderBy.trim().isEmpty()) {
                sql.append(" ORDER BY ").append(orderBy);
            }
            
            // LIMIT子句
            if (limit != null) {
                sql.append(" LIMIT ").append(limit);
                if (offset != null) {
                    sql.append(" OFFSET ").append(offset);
                }
            }
            
            return sql.toString();
        }
    }

    /**
     * UPDATE语句构建器 - 增强版
     */
    public static class UpdateBuilder implements UpdateTableStage, UpdateSetStage, UpdateWhereStage {
        private String table; // 表名
        private String where; // WHERE条件
        private final Map<String, Object> setMap = new LinkedHashMap<>(); // SET子句
        
        @Override
        public UpdateSetStage table(String table) {
            this.table = table;
            return this;
        }
        
        @Override
        public UpdateSetStage set(String column, Object value) {
            if (value instanceof String) {
                setMap.put(column, "'" + value + "'");
            } else {
                setMap.put(column, value);
            }
            return this;
        }
        
        @Override
        public UpdateWhereStage where(String condition) {
            this.where = condition;
            return this;
        }
        
        @Override
        public BuildStage build() {
            return this;
        }
        
        @Override
        public String buildSql() {
            if (setMap.isEmpty()) {
                throw new IllegalStateException("UPDATE语句必须包含至少一个SET子句");
            }
            
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(table).append(" SET ");
            
            String setString = setMap.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", "));
            
            sql.append(setString);
            
            if (where != null && !where.trim().isEmpty()) {
                sql.append(" WHERE ").append(where);
            }
            
            return sql.toString();
        }
    }

    /**
     * INSERT语句构建器 - 增强版
     */
    public static class InsertBuilder implements InsertIntoStage, InsertValuesStage, BuildStage {
        private String table; // 表名
        private final Map<String, Object> valueMap = new LinkedHashMap<>(); // 列值映射
        
        @Override
        public InsertValuesStage into(String table) {
            this.table = table;
            return this;
        }
        
        @Override
        public InsertValuesStage value(String column, Object value) {
            valueMap.put(column, value);
            return this;
        }
        
        @Override
        public InsertValuesStage values(Map<String, Object> values) {
            valueMap.putAll(values);
            return this;
        }
        
        @Override
        public BuildStage build() {
            return this;
        }
        
        @Override
        public String buildSql() {
            if (valueMap.isEmpty()) {
                throw new IllegalStateException("INSERT语句必须包含至少一个值");
            }
            
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(table);
            
            // 构建列名部分
            String columns = String.join(", ", valueMap.keySet());
            sql.append("(").append(columns).append(")");
            
            // 构建值部分
            String values = valueMap.values().stream()
                .map(value -> {
                    if (value instanceof String) {
                        return "'" + value + "'";
                    } else {
                        return String.valueOf(value);
                    }
                })
                .collect(Collectors.joining(", "));
            
            sql.append(" VALUES(").append(values).append(")");
            
            return sql.toString();
        }
    }

    /**
     * DELETE语句构建器 - 增强版
     */
    public static class DeleteBuilder implements DeleteFromStage, DeleteWhereStage, BuildStage {
        private String table; // 表名
        private String where; // WHERE条件
        
        @Override
        public DeleteWhereStage from(String table) {
            this.table = table;
            return this;
        }
        
        @Override
        public BuildStage where(String condition) {
            this.where = condition;
            return this;
        }
        
        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(table);
            
            if (where != null && !where.trim().isEmpty()) {
                sql.append(" WHERE ").append(where);
            }
            
            return sql.toString();
        }
    }
}