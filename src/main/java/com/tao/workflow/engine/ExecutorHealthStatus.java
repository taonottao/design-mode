package com.tao.workflow.engine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 执行器健康状态类
 * 
 * 用于表示步骤执行器的健康状态信息，包括状态类型、描述信息、
 * 检查时间等详细信息。这个类提供了执行器健康监控的完整数据结构。
 * 
 * 主要功能：
 * 1. 表示执行器的当前健康状态
 * 2. 提供状态描述和详细信息
 * 3. 记录健康检查的时间戳
 * 4. 支持自定义健康指标数据
 * 5. 提供便捷的状态创建方法
 * 
 * @author Tao
 * @version 1.0
 */
public class ExecutorHealthStatus {
    
    /** 健康状态类型 */
    private final HealthType type;
    
    /** 状态描述信息 */
    private final String description;
    
    /** 健康检查时间 */
    private final LocalDateTime checkTime;
    
    /** 自定义健康指标数据 */
    private final Map<String, Object> metrics;
    
    /** 错误信息（如果有） */
    private final String errorMessage;
    
    /**
     * 私有构造函数
     * 
     * @param type 健康状态类型
     * @param description 状态描述
     * @param checkTime 检查时间
     * @param metrics 健康指标
     * @param errorMessage 错误信息
     */
    private ExecutorHealthStatus(HealthType type, String description, 
                                LocalDateTime checkTime, Map<String, Object> metrics, 
                                String errorMessage) {
        this.type = type;
        this.description = description;
        this.checkTime = checkTime;
        this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        this.errorMessage = errorMessage;
    }
    
    /**
     * 创建健康状态
     * 
     * @return 健康状态实例
     */
    public static ExecutorHealthStatus healthy() {
        return healthy("执行器运行正常");
    }
    
    /**
     * 创建健康状态（带描述）
     * 
     * @param description 状态描述
     * @return 健康状态实例
     */
    public static ExecutorHealthStatus healthy(String description) {
        return new ExecutorHealthStatus(
            HealthType.HEALTHY, 
            description, 
            LocalDateTime.now(), 
            null, 
            null
        );
    }
    
    /**
     * 创建健康状态（带指标）
     * 
     * @param description 状态描述
     * @param metrics 健康指标
     * @return 健康状态实例
     */
    public static ExecutorHealthStatus healthy(String description, Map<String, Object> metrics) {
        return new ExecutorHealthStatus(
            HealthType.HEALTHY, 
            description, 
            LocalDateTime.now(), 
            metrics, 
            null
        );
    }
    
    /**
     * 创建降级状态
     * 
     * @return 降级状态实例
     */
    public static ExecutorHealthStatus degraded() {
        return degraded("执行器性能降级");
    }
    
    /**
     * 创建降级状态（带描述）
     * 
     * @param description 状态描述
     * @return 降级状态实例
     */
    public static ExecutorHealthStatus degraded(String description) {
        return new ExecutorHealthStatus(
            HealthType.DEGRADED, 
            description, 
            LocalDateTime.now(), 
            null, 
            null
        );
    }
    
    /**
     * 创建降级状态（带指标）
     * 
     * @param description 状态描述
     * @param metrics 健康指标
     * @return 降级状态实例
     */
    public static ExecutorHealthStatus degraded(String description, Map<String, Object> metrics) {
        return new ExecutorHealthStatus(
            HealthType.DEGRADED, 
            description, 
            LocalDateTime.now(), 
            metrics, 
            null
        );
    }
    
    /**
     * 创建不健康状态
     * 
     * @return 不健康状态实例
     */
    public static ExecutorHealthStatus unhealthy() {
        return unhealthy("执行器运行异常");
    }
    
    /**
     * 创建不健康状态（带描述）
     * 
     * @param description 状态描述
     * @return 不健康状态实例
     */
    public static ExecutorHealthStatus unhealthy(String description) {
        return new ExecutorHealthStatus(
            HealthType.UNHEALTHY, 
            description, 
            LocalDateTime.now(), 
            null, 
            null
        );
    }
    
    /**
     * 创建不健康状态（带错误信息）
     * 
     * @param description 状态描述
     * @param errorMessage 错误信息
     * @return 不健康状态实例
     */
    public static ExecutorHealthStatus unhealthy(String description, String errorMessage) {
        return new ExecutorHealthStatus(
            HealthType.UNHEALTHY, 
            description, 
            LocalDateTime.now(), 
            null, 
            errorMessage
        );
    }
    
    /**
     * 创建不健康状态（带指标和错误信息）
     * 
     * @param description 状态描述
     * @param metrics 健康指标
     * @param errorMessage 错误信息
     * @return 不健康状态实例
     */
    public static ExecutorHealthStatus unhealthy(String description, Map<String, Object> metrics, String errorMessage) {
        return new ExecutorHealthStatus(
            HealthType.UNHEALTHY, 
            description, 
            LocalDateTime.now(), 
            metrics, 
            errorMessage
        );
    }
    
    /**
     * 检查是否为健康状态
     * 
     * @return 如果是健康状态返回true，否则返回false
     */
    public boolean isHealthy() {
        return type == HealthType.HEALTHY;
    }
    
    /**
     * 检查是否为降级状态
     * 
     * @return 如果是降级状态返回true，否则返回false
     */
    public boolean isDegraded() {
        return type == HealthType.DEGRADED;
    }
    
    /**
     * 检查是否为不健康状态
     * 
     * @return 如果是不健康状态返回true，否则返回false
     */
    public boolean isUnhealthy() {
        return type == HealthType.UNHEALTHY;
    }
    
    /**
     * 检查是否可以执行任务
     * 
     * 健康和降级状态都可以执行任务，不健康状态不能执行
     * 
     * @return 如果可以执行任务返回true，否则返回false
     */
    public boolean canExecute() {
        return type == HealthType.HEALTHY || type == HealthType.DEGRADED;
    }
    
    // Getters
    
    /**
     * 获取健康状态类型
     * 
     * @return 健康状态类型
     */
    public HealthType getType() {
        return type;
    }
    
    /**
     * 获取状态描述
     * 
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取检查时间
     * 
     * @return 检查时间
     */
    public LocalDateTime getCheckTime() {
        return checkTime;
    }
    
    /**
     * 获取健康指标
     * 
     * @return 健康指标的副本
     */
    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }
    
    /**
     * 获取错误信息
     * 
     * @return 错误信息，如果没有则返回null
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 转换为Map格式
     * 
     * @return 包含所有状态信息的Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type.name());
        result.put("description", description);
        result.put("checkTime", checkTime);
        result.put("metrics", new HashMap<>(metrics));
        if (errorMessage != null) {
            result.put("errorMessage", errorMessage);
        }
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExecutorHealthStatus{");
        sb.append("type=").append(type);
        sb.append(", description='").append(description).append('\''');
        sb.append(", checkTime=").append(checkTime);
        if (!metrics.isEmpty()) {
            sb.append(", metrics=").append(metrics);
        }
        if (errorMessage != null) {
            sb.append(", errorMessage='").append(errorMessage).append('\''');
        }
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 健康状态类型枚举
     */
    public enum HealthType {
        /** 健康状态 - 执行器运行正常 */
        HEALTHY("健康"),
        
        /** 降级状态 - 执行器性能下降但仍可工作 */
        DEGRADED("降级"),
        
        /** 不健康状态 - 执行器无法正常工作 */
        UNHEALTHY("不健康");
        
        private final String displayName;
        
        HealthType(String displayName) {
            this.displayName = displayName;
        }
        
        /**
         * 获取显示名称
         * 
         * @return 显示名称
         */
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}