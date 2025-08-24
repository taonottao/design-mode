package com.tao.workflow.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 工作流步骤实体类
 * 
 * 代表工作流中的一个执行步骤，包含步骤的基本信息、执行逻辑、条件判断等。
 * 每个步骤都是工作流执行的基本单元。
 * 
 * 核心特性：
 * 1. 不可变设计 - 步骤定义一旦创建不可修改
 * 2. 类型安全 - 使用枚举定义步骤类型
 * 3. 灵活配置 - 支持动态参数配置
 * 4. 条件执行 - 支持条件判断和分支
 * 
 * @author Tao
 * @version 1.0
 */
public class WorkflowStep {
    
    /** 步骤唯一标识 */
    private final String id;
    
    /** 步骤名称 */
    private final String name;
    
    /** 步骤描述 */
    private final String description;
    
    /** 步骤类型 */
    private final StepType type;
    
    /** 步骤在工作流中的顺序 */
    private final int order;
    
    /** 执行器类名 - 用于反射创建执行器实例 */
    private final String executorClass;
    
    /** 步骤配置参数 */
    private final Map<String, Object> config;
    
    /** 前置条件 - 决定步骤是否应该执行 */
    private final Function<Map<String, Object>, Boolean> precondition;
    
    /** 下一步骤ID - 用于流程控制 */
    private final String nextStepId;
    
    /** 错误处理步骤ID - 当前步骤执行失败时的跳转目标 */
    private final String errorStepId;
    
    /** 是否为可选步骤 - 可选步骤执行失败不会终止整个流程 */
    private final boolean optional;
    
    /** 超时时间（秒） - 步骤执行的最大允许时间 */
    private final long timeoutSeconds;
    
    /** 重试次数 - 步骤执行失败时的重试次数 */
    private final int retryCount;
    
    /** 创建时间 */
    private final LocalDateTime createTime;
    
    /**
     * 私有构造函数 - 只能通过Builder创建
     * 
     * @param builder 步骤构建器
     */
    private WorkflowStep(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.order = builder.order;
        this.executorClass = builder.executorClass;
        this.config = new ConcurrentHashMap<>(builder.config);
        this.precondition = builder.precondition;
        this.nextStepId = builder.nextStepId;
        this.errorStepId = builder.errorStepId;
        this.optional = builder.optional;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.retryCount = builder.retryCount;
        this.createTime = builder.createTime;
    }
    
    /**
     * 获取步骤ID
     * @return 步骤唯一标识
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取步骤名称
     * @return 步骤名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取步骤描述
     * @return 步骤描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取步骤类型
     * @return 步骤类型枚举
     */
    public StepType getType() {
        return type;
    }
    
    /**
     * 获取步骤顺序
     * @return 步骤在工作流中的顺序号
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * 获取执行器类名
     * @return 执行器的完整类名
     */
    public String getExecutorClass() {
        return executorClass;
    }
    
    /**
     * 获取步骤配置参数
     * @return 不可变的配置参数映射
     */
    public Map<String, Object> getConfig() {
        return new ConcurrentHashMap<>(config);
    }
    
    /**
     * 获取指定配置参数
     * @param key 配置键
     * @return 配置值，如果不存在返回null
     */
    public Object getConfigValue(String key) {
        return config.get(key);
    }
    
    /**
     * 获取指定配置参数（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = config.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 检查前置条件是否满足
     * 
     * @param context 执行上下文，包含当前流程的所有变量
     * @return 如果前置条件满足返回true，否则返回false
     */
    public boolean checkPrecondition(Map<String, Object> context) {
        // 如果没有设置前置条件，默认为true
        if (precondition == null) {
            return true;
        }
        
        try {
            return precondition.apply(context);
        } catch (Exception e) {
            // 前置条件执行异常，记录日志并返回false
            System.err.println("步骤前置条件执行异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取下一步骤ID
     * @return 下一步骤的ID，如果没有则返回null
     */
    public String getNextStepId() {
        return nextStepId;
    }
    
    /**
     * 获取错误处理步骤ID
     * @return 错误处理步骤的ID，如果没有则返回null
     */
    public String getErrorStepId() {
        return errorStepId;
    }
    
    /**
     * 检查是否为可选步骤
     * @return 如果是可选步骤返回true，否则返回false
     */
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * 获取超时时间
     * @return 超时时间（秒）
     */
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    /**
     * 获取重试次数
     * @return 重试次数
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    /**
     * 创建步骤构建器
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 基于现有步骤创建构建器
     * @param step 现有步骤
     * @return 预填充的构建器实例
     */
    public static Builder builder(WorkflowStep step) {
        return new Builder(step);
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowStep{id='%s', name='%s', type=%s, order=%d, optional=%s}",
                id, name, type, order, optional);
    }
    
    /**
     * 工作流步骤构建器
     * 
     * 使用Builder模式构建复杂的步骤配置，提供：
     * 1. 参数验证和默认值设置
     * 2. 链式调用的流畅API
     * 3. 类型安全的配置方法
     * 4. 灵活的条件和配置设置
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private StepType type = StepType.TASK; // 默认为任务类型
        private int order;
        private String executorClass;
        private Map<String, Object> config = new ConcurrentHashMap<>();
        private Function<Map<String, Object>, Boolean> precondition;
        private String nextStepId;
        private String errorStepId;
        private boolean optional = false; // 默认为必需步骤
        private long timeoutSeconds = 300; // 默认5分钟超时
        private int retryCount = 0; // 默认不重试
        private LocalDateTime createTime = LocalDateTime.now();
        
        /**
         * 默认构造函数
         */
        public Builder() {
        }
        
        /**
         * 基于现有步骤的构造函数
         * @param step 现有步骤
         */
        public Builder(WorkflowStep step) {
            this.id = step.id;
            this.name = step.name;
            this.description = step.description;
            this.type = step.type;
            this.order = step.order;
            this.executorClass = step.executorClass;
            this.config = new ConcurrentHashMap<>(step.config);
            this.precondition = step.precondition;
            this.nextStepId = step.nextStepId;
            this.errorStepId = step.errorStepId;
            this.optional = step.optional;
            this.timeoutSeconds = step.timeoutSeconds;
            this.retryCount = step.retryCount;
            this.createTime = step.createTime;
        }
        
        /**
         * 设置步骤ID
         * @param id 步骤唯一标识
         * @return 构建器实例
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * 设置步骤名称
         * @param name 步骤名称
         * @return 构建器实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * 设置步骤描述
         * @param description 步骤描述
         * @return 构建器实例
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * 设置步骤类型
         * @param type 步骤类型
         * @return 构建器实例
         */
        public Builder type(StepType type) {
            this.type = type;
            return this;
        }
        
        /**
         * 设置步骤顺序
         * @param order 步骤顺序号
         * @return 构建器实例
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }
        
        /**
         * 设置执行器类名
         * @param executorClass 执行器完整类名
         * @return 构建器实例
         */
        public Builder executorClass(String executorClass) {
            this.executorClass = executorClass;
            return this;
        }
        
        /**
         * 设置配置参数
         * @param key 配置键
         * @param value 配置值
         * @return 构建器实例
         */
        public Builder config(String key, Object value) {
            this.config.put(key, value);
            return this;
        }
        
        /**
         * 批量设置配置参数
         * @param config 配置参数映射
         * @return 构建器实例
         */
        public Builder config(Map<String, Object> config) {
            if (config != null) {
                this.config.putAll(config);
            }
            return this;
        }
        
        /**
         * 设置前置条件
         * @param precondition 前置条件函数
         * @return 构建器实例
         */
        public Builder precondition(Function<Map<String, Object>, Boolean> precondition) {
            this.precondition = precondition;
            return this;
        }
        
        /**
         * 设置下一步骤ID
         * @param nextStepId 下一步骤ID
         * @return 构建器实例
         */
        public Builder nextStepId(String nextStepId) {
            this.nextStepId = nextStepId;
            return this;
        }
        
        /**
         * 设置错误处理步骤ID
         * @param errorStepId 错误处理步骤ID
         * @return 构建器实例
         */
        public Builder errorStepId(String errorStepId) {
            this.errorStepId = errorStepId;
            return this;
        }
        
        /**
         * 设置为可选步骤
         * @param optional 是否可选
         * @return 构建器实例
         */
        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        /**
         * 设置超时时间
         * @param timeoutSeconds 超时时间（秒）
         * @return 构建器实例
         */
        public Builder timeout(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }
        
        /**
         * 设置重试次数
         * @param retryCount 重试次数
         * @return 构建器实例
         */
        public Builder retry(int retryCount) {
            this.retryCount = Math.max(0, retryCount); // 确保重试次数非负
            return this;
        }
        
        /**
         * 构建步骤实例
         * 
         * 在构建过程中进行参数验证和默认值设置
         * 
         * @return 工作流步骤实例
         * @throws IllegalStateException 如果构建参数不合法
         */
        public WorkflowStep build() {
            // 验证必填字段
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalStateException("步骤ID不能为空");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("步骤名称不能为空");
            }
            if (type == null) {
                throw new IllegalStateException("步骤类型不能为空");
            }
            if (order < 1) {
                throw new IllegalStateException("步骤顺序必须大于0");
            }
            
            // 验证执行器类名（对于TASK类型步骤）
            if (type == StepType.TASK && (executorClass == null || executorClass.trim().isEmpty())) {
                throw new IllegalStateException("任务类型步骤必须指定执行器类名");
            }
            
            // 验证超时时间
            if (timeoutSeconds <= 0) {
                throw new IllegalStateException("超时时间必须大于0");
            }
            
            return new WorkflowStep(this);
        }
    }
}