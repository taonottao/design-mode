package com.tao.workflow.builder;

import java.util.*;
import java.util.function.Predicate;

/**
 * 条件步骤构建器
 * 
 * 专门用于构建条件分支逻辑的Builder，支持复杂的条件判断和分支路由。
 * 通过链式调用和函数式接口，提供了直观且强大的条件构建方式。
 * 
 * 主要解决的问题：
 * 1. 复杂条件逻辑的构建和管理
 * 2. 多分支路由的配置
 * 3. 条件表达式的类型安全
 * 4. 默认分支和异常处理
 * 5. 条件优先级和执行顺序
 * 
 * 使用示例：
 * <pre>
 * ConditionalStepBuilder conditionBuilder = new ConditionalStepBuilder()
 *     .when(ctx -> (Integer) ctx.get("amount") > 10000)
 *     .thenGoto("manager_approval")
 *     .when(ctx -> (Integer) ctx.get("amount") > 1000)
 *     .thenGoto("supervisor_approval")
 *     .otherwise("auto_approval")
 *     .onError("error_handler");
 * 
 * Map<String, Object> conditions = conditionBuilder.build();
 * </pre>
 * 
 * @author Tao
 * @version 1.0
 */
public class ConditionalStepBuilder {
    
    /**
     * 条件分支定义
     */
    public static class ConditionBranch {
        /** 条件表达式 */
        private final String condition;
        
        /** 目标步骤ID */
        private final String targetStepId;
        
        /** 条件优先级 */
        private final int priority;
        
        /** 条件描述 */
        private final String description;
        
        public ConditionBranch(String condition, String targetStepId, int priority, String description) {
            this.condition = condition;
            this.targetStepId = targetStepId;
            this.priority = priority;
            this.description = description;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public String getTargetStepId() {
            return targetStepId;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return String.format("ConditionBranch{condition='%s', target='%s', priority=%d}", 
                               condition, targetStepId, priority);
        }
    }
    
    /** 条件分支列表 */
    private final List<ConditionBranch> branches = new ArrayList<>();
    
    /** 默认分支（当所有条件都不满足时） */
    private String defaultBranch;
    
    /** 错误处理分支 */
    private String errorBranch;
    
    /** 条件评估策略 */
    private String evaluationStrategy = "FIRST_MATCH"; // FIRST_MATCH, ALL_MATCH, PRIORITY
    
    /** 当前条件优先级计数器 */
    private int priorityCounter = 1;
    
    /** 临时条件表达式（用于链式调用） */
    private String currentCondition;
    
    /** 临时条件描述 */
    private String currentDescription;
    
    /**
     * 默认构造函数
     */
    public ConditionalStepBuilder() {
        // 初始化默认配置
    }
    
    /**
     * 添加条件分支
     * 
     * 使用字符串表达式定义条件，支持SpEL或自定义表达式语法。
     * 
     * @param condition 条件表达式字符串
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder when(String condition) {
        this.currentCondition = condition;
        this.currentDescription = null;
        return this;
    }
    
    /**
     * 添加条件分支
     * 
     * 使用函数式接口定义条件，提供类型安全的条件判断。
     * 
     * @param condition 条件判断函数
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder when(Predicate<Map<String, Object>> condition) {
        // 将函数式条件转换为表达式字符串
        // 在实际实现中，可以使用表达式引擎或序列化函数
        this.currentCondition = convertPredicateToExpression(condition);
        this.currentDescription = null;
        return this;
    }
    
    /**
     * 添加带描述的条件分支
     * 
     * @param condition 条件表达式字符串
     * @param description 条件描述
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder when(String condition, String description) {
        this.currentCondition = condition;
        this.currentDescription = description;
        return this;
    }
    
    /**
     * 设置当前条件的目标步骤
     * 
     * 必须在调用when()之后调用此方法。
     * 
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     * @throws IllegalStateException 如果没有设置当前条件
     */
    public ConditionalStepBuilder thenGoto(String targetStepId) {
        if (currentCondition == null) {
            throw new IllegalStateException("必须先调用when()方法设置条件");
        }
        
        // 创建条件分支并添加到列表
        ConditionBranch branch = new ConditionBranch(
            currentCondition, 
            targetStepId, 
            priorityCounter++,
            currentDescription
        );
        branches.add(branch);
        
        // 清空临时状态
        currentCondition = null;
        currentDescription = null;
        
        return this;
    }
    
    /**
     * 设置当前条件的目标步骤（别名方法）
     * 
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder then(String targetStepId) {
        return thenGoto(targetStepId);
    }
    
    /**
     * 添加简单的条件分支
     * 
     * 一次性设置条件和目标步骤的便捷方法。
     * 
     * @param condition 条件表达式
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder addBranch(String condition, String targetStepId) {
        return when(condition).thenGoto(targetStepId);
    }
    
    /**
     * 添加带描述的条件分支
     * 
     * @param condition 条件表达式
     * @param targetStepId 目标步骤ID
     * @param description 条件描述
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder addBranch(String condition, String targetStepId, String description) {
        return when(condition, description).thenGoto(targetStepId);
    }
    
    /**
     * 添加数值比较条件
     * 
     * 便捷方法，用于常见的数值比较场景。
     * 
     * @param fieldName 字段名称
     * @param operator 比较操作符（>、>=、<、<=、==、!=）
     * @param value 比较值
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenNumber(String fieldName, String operator, Number value, String targetStepId) {
        String condition = String.format("%s %s %s", fieldName, operator, value);
        return addBranch(condition, targetStepId, 
                        String.format("%s %s %s", fieldName, operator, value));
    }
    
    /**
     * 添加字符串比较条件
     * 
     * @param fieldName 字段名称
     * @param operator 比较操作符（==、!=、contains、startsWith、endsWith）
     * @param value 比较值
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenString(String fieldName, String operator, String value, String targetStepId) {
        String condition;
        switch (operator.toLowerCase()) {
            case "contains":
                condition = String.format("%s.contains('%s')", fieldName, value);
                break;
            case "startswith":
                condition = String.format("%s.startsWith('%s')", fieldName, value);
                break;
            case "endswith":
                condition = String.format("%s.endsWith('%s')", fieldName, value);
                break;
            case "==":
            case "equals":
                condition = String.format("%s == '%s'", fieldName, value);
                break;
            case "!=":
            case "notequals":
                condition = String.format("%s != '%s'", fieldName, value);
                break;
            default:
                condition = String.format("%s %s '%s'", fieldName, operator, value);
        }
        
        return addBranch(condition, targetStepId, 
                        String.format("%s %s %s", fieldName, operator, value));
    }
    
    /**
     * 添加布尔值条件
     * 
     * @param fieldName 字段名称
     * @param expectedValue 期望的布尔值
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenBoolean(String fieldName, boolean expectedValue, String targetStepId) {
        String condition = expectedValue ? fieldName : "!" + fieldName;
        return addBranch(condition, targetStepId, 
                        String.format("%s is %s", fieldName, expectedValue));
    }
    
    /**
     * 添加空值检查条件
     * 
     * @param fieldName 字段名称
     * @param isNull 是否检查为空
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenNull(String fieldName, boolean isNull, String targetStepId) {
        String condition = isNull ? fieldName + " == null" : fieldName + " != null";
        String description = isNull ? fieldName + " is null" : fieldName + " is not null";
        return addBranch(condition, targetStepId, description);
    }
    
    /**
     * 添加集合包含条件
     * 
     * @param fieldName 字段名称
     * @param value 要检查的值
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenContains(String fieldName, Object value, String targetStepId) {
        String condition = String.format("%s.contains(%s)", fieldName, 
                                        value instanceof String ? "'" + value + "'" : value);
        return addBranch(condition, targetStepId, 
                        String.format("%s contains %s", fieldName, value));
    }
    
    /**
     * 添加范围检查条件
     * 
     * @param fieldName 字段名称
     * @param minValue 最小值（包含）
     * @param maxValue 最大值（包含）
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenInRange(String fieldName, Number minValue, Number maxValue, String targetStepId) {
        String condition = String.format("%s >= %s && %s <= %s", 
                                        fieldName, minValue, fieldName, maxValue);
        return addBranch(condition, targetStepId, 
                        String.format("%s between %s and %s", fieldName, minValue, maxValue));
    }
    
    /**
     * 添加复合条件（AND）
     * 
     * @param conditions 条件表达式数组
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenAll(String[] conditions, String targetStepId) {
        String condition = String.join(" && ", conditions);
        return addBranch(condition, targetStepId, "All conditions must be true");
    }
    
    /**
     * 添加复合条件（OR）
     * 
     * @param conditions 条件表达式数组
     * @param targetStepId 目标步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder whenAny(String[] conditions, String targetStepId) {
        String condition = "(" + String.join(") || (", conditions) + ")";
        return addBranch(condition, targetStepId, "Any condition must be true");
    }
    
    /**
     * 设置默认分支
     * 
     * 当所有条件都不满足时执行的步骤。
     * 
     * @param defaultStepId 默认步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder otherwise(String defaultStepId) {
        this.defaultBranch = defaultStepId;
        return this;
    }
    
    /**
     * 设置默认分支（别名方法）
     * 
     * @param defaultStepId 默认步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder defaultTo(String defaultStepId) {
        return otherwise(defaultStepId);
    }
    
    /**
     * 设置错误处理分支
     * 
     * 当条件评估过程中发生错误时执行的步骤。
     * 
     * @param errorStepId 错误处理步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder onError(String errorStepId) {
        this.errorBranch = errorStepId;
        return this;
    }
    
    /**
     * 设置条件评估策略
     * 
     * @param strategy 评估策略（FIRST_MATCH、ALL_MATCH、PRIORITY）
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder evaluationStrategy(String strategy) {
        this.evaluationStrategy = strategy;
        return this;
    }
    
    /**
     * 设置为第一匹配策略
     * 
     * 按添加顺序评估条件，第一个满足的条件被执行。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder firstMatch() {
        return evaluationStrategy("FIRST_MATCH");
    }
    
    /**
     * 设置为优先级策略
     * 
     * 按优先级顺序评估条件，优先级高的条件优先执行。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder priorityMatch() {
        return evaluationStrategy("PRIORITY");
    }
    
    /**
     * 设置条件优先级
     * 
     * 为最后添加的条件设置优先级。
     * 
     * @param priority 优先级（数字越大优先级越高）
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder priority(int priority) {
        if (!branches.isEmpty()) {
            ConditionBranch lastBranch = branches.get(branches.size() - 1);
            ConditionBranch updatedBranch = new ConditionBranch(
                lastBranch.getCondition(),
                lastBranch.getTargetStepId(),
                priority,
                lastBranch.getDescription()
            );
            branches.set(branches.size() - 1, updatedBranch);
        }
        return this;
    }
    
    /**
     * 验证条件配置的完整性
     * 
     * @throws IllegalStateException 如果配置不完整或不正确
     */
    private void validate() {
        if (branches.isEmpty()) {
            throw new IllegalStateException("至少需要添加一个条件分支");
        }
        
        // 检查是否有未完成的条件设置
        if (currentCondition != null) {
            throw new IllegalStateException("有未完成的条件设置，请调用thenGoto()方法");
        }
        
        // 验证所有分支都有有效的目标步骤
        for (ConditionBranch branch : branches) {
            if (branch.getTargetStepId() == null || branch.getTargetStepId().trim().isEmpty()) {
                throw new IllegalStateException("条件分支必须指定目标步骤ID");
            }
        }
        
        // 验证评估策略
        if (!Arrays.asList("FIRST_MATCH", "ALL_MATCH", "PRIORITY").contains(evaluationStrategy)) {
            throw new IllegalStateException("无效的评估策略: " + evaluationStrategy);
        }
    }
    
    /**
     * 将Predicate转换为表达式字符串
     * 
     * 这是一个简化的实现，实际项目中可能需要更复杂的序列化逻辑。
     * 
     * @param predicate 条件判断函数
     * @return 表达式字符串
     */
    private String convertPredicateToExpression(Predicate<Map<String, Object>> predicate) {
        // 简化实现：返回函数的字符串表示
        // 在实际项目中，可以使用ASM、字节码分析或表达式树来实现更精确的转换
        return predicate.toString();
    }
    
    /**
     * 构建条件配置
     * 
     * 返回包含所有条件配置的Map，用于在WorkflowStep中存储。
     * 
     * @return 条件配置Map
     * @throws IllegalStateException 如果配置不完整或不正确
     */
    public Map<String, Object> build() {
        // 验证配置
        validate();
        
        Map<String, Object> config = new HashMap<>();
        
        // 添加条件分支
        List<Map<String, Object>> branchConfigs = new ArrayList<>();
        for (ConditionBranch branch : branches) {
            Map<String, Object> branchConfig = new HashMap<>();
            branchConfig.put("condition", branch.getCondition());
            branchConfig.put("targetStepId", branch.getTargetStepId());
            branchConfig.put("priority", branch.getPriority());
            if (branch.getDescription() != null) {
                branchConfig.put("description", branch.getDescription());
            }
            branchConfigs.add(branchConfig);
        }
        
        // 按优先级排序（如果使用优先级策略）
        if ("PRIORITY".equals(evaluationStrategy)) {
            branchConfigs.sort((a, b) -> 
                Integer.compare((Integer) b.get("priority"), (Integer) a.get("priority")));
        }
        
        config.put("branches", branchConfigs);
        config.put("evaluationStrategy", evaluationStrategy);
        
        if (defaultBranch != null) {
            config.put("defaultBranch", defaultBranch);
        }
        
        if (errorBranch != null) {
            config.put("errorBranch", errorBranch);
        }
        
        // 添加元数据
        config.put("branchCount", branches.size());
        config.put("hasDefault", defaultBranch != null);
        config.put("hasErrorHandler", errorBranch != null);
        
        return config;
    }
    
    /**
     * 获取条件分支数量
     * 
     * @return 分支数量
     */
    public int getBranchCount() {
        return branches.size();
    }
    
    /**
     * 检查是否有默认分支
     * 
     * @return 如果有默认分支返回true，否则返回false
     */
    public boolean hasDefaultBranch() {
        return defaultBranch != null;
    }
    
    /**
     * 检查是否有错误处理分支
     * 
     * @return 如果有错误处理分支返回true，否则返回false
     */
    public boolean hasErrorBranch() {
        return errorBranch != null;
    }
    
    /**
     * 获取所有条件分支的副本
     * 
     * @return 条件分支列表的副本
     */
    public List<ConditionBranch> getBranches() {
        return new ArrayList<>(branches);
    }
    
    /**
     * 清空所有条件
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ConditionalStepBuilder clear() {
        branches.clear();
        defaultBranch = null;
        errorBranch = null;
        evaluationStrategy = "FIRST_MATCH";
        priorityCounter = 1;
        currentCondition = null;
        currentDescription = null;
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("ConditionalStepBuilder{branchCount=%d, strategy='%s', hasDefault=%s}", 
                           branches.size(), evaluationStrategy, defaultBranch != null);
    }
}