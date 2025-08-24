package com.tao.workflow.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流实例实体类
 * 
 * 代表一个正在执行或已完成的工作流实例，记录了工作流的执行状态、
 * 当前步骤、执行上下文等运行时信息。
 * 
 * 核心特性：
 * 1. 状态管理 - 跟踪实例的执行状态
 * 2. 上下文保持 - 维护执行过程中的变量和数据
 * 3. 进度跟踪 - 记录当前执行到哪个步骤
 * 4. 错误处理 - 记录执行过程中的错误信息
 * 
 * @author Tao
 * @version 1.0
 */
public class WorkflowInstance {
    
    /** 实例唯一标识 */
    private final String id;
    
    /** 关联的工作流ID */
    private final String workflowId;
    
    /** 实例名称 */
    private final String name;
    
    /** 实例状态 */
    private volatile InstanceStatus status;
    
    /** 当前执行步骤ID */
    private volatile String currentStepId;
    
    /** 当前步骤顺序号 */
    private volatile int currentStepOrder;
    
    /** 执行上下文 - 存储流程变量和中间结果 */
    private final Map<String, Object> context;
    
    /** 实例配置参数 */
    private final Map<String, Object> config;
    
    /** 启动用户ID */
    private final String startUserId;
    
    /** 当前处理用户ID */
    private volatile String currentUserId;
    
    /** 业务键 - 用于业务系统关联 */
    private final String businessKey;
    
    /** 优先级 */
    private final int priority;
    
    /** 创建时间 */
    private final LocalDateTime createTime;
    
    /** 开始执行时间 */
    private volatile LocalDateTime startTime;
    
    /** 结束时间 */
    private volatile LocalDateTime endTime;
    
    /** 最后更新时间 */
    private volatile LocalDateTime updateTime;
    
    /** 错误信息 */
    private volatile String errorMessage;
    
    /** 错误堆栈 */
    private volatile String errorStack;
    
    /**
     * 私有构造函数 - 只能通过Builder创建
     * 
     * @param builder 实例构建器
     */
    private WorkflowInstance(Builder builder) {
        this.id = builder.id;
        this.workflowId = builder.workflowId;
        this.name = builder.name;
        this.status = builder.status;
        this.currentStepId = builder.currentStepId;
        this.currentStepOrder = builder.currentStepOrder;
        this.context = new ConcurrentHashMap<>(builder.context);
        this.config = new ConcurrentHashMap<>(builder.config);
        this.startUserId = builder.startUserId;
        this.currentUserId = builder.currentUserId;
        this.businessKey = builder.businessKey;
        this.priority = builder.priority;
        this.createTime = builder.createTime;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.updateTime = builder.updateTime;
        this.errorMessage = builder.errorMessage;
        this.errorStack = builder.errorStack;
    }
    
    /**
     * 获取实例ID
     * @return 实例唯一标识
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取工作流ID
     * @return 关联的工作流ID
     */
    public String getWorkflowId() {
        return workflowId;
    }
    
    /**
     * 获取实例名称
     * @return 实例名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取实例状态
     * @return 当前状态
     */
    public InstanceStatus getStatus() {
        return status;
    }
    
    /**
     * 设置实例状态
     * 
     * 状态变更时自动更新时间戳
     * 
     * @param status 新状态
     */
    public void setStatus(InstanceStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
        
        // 根据状态设置特殊时间戳
        if (status == InstanceStatus.RUNNING && this.startTime == null) {
            this.startTime = LocalDateTime.now();
        } else if (status.isFinalState() && this.endTime == null) {
            this.endTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取当前步骤ID
     * @return 当前执行步骤的ID
     */
    public String getCurrentStepId() {
        return currentStepId;
    }
    
    /**
     * 设置当前步骤
     * 
     * @param stepId 步骤ID
     * @param stepOrder 步骤顺序号
     */
    public void setCurrentStep(String stepId, int stepOrder) {
        this.currentStepId = stepId;
        this.currentStepOrder = stepOrder;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 获取当前步骤顺序号
     * @return 当前步骤的顺序号
     */
    public int getCurrentStepOrder() {
        return currentStepOrder;
    }
    
    /**
     * 获取执行上下文
     * @return 执行上下文的副本
     */
    public Map<String, Object> getContext() {
        return new ConcurrentHashMap<>(context);
    }
    
    /**
     * 获取上下文变量
     * @param key 变量名
     * @return 变量值，如果不存在返回null
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }
    
    /**
     * 获取上下文变量（带默认值）
     * @param key 变量名
     * @param defaultValue 默认值
     * @return 变量值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        Object value = context.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 设置上下文变量
     * @param key 变量名
     * @param value 变量值
     */
    public void setContextValue(String key, Object value) {
        context.put(key, value);
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 批量设置上下文变量
     * @param variables 变量映射
     */
    public void setContextValues(Map<String, Object> variables) {
        if (variables != null) {
            context.putAll(variables);
            this.updateTime = LocalDateTime.now();
        }
    }
    
    /**
     * 移除上下文变量
     * @param key 变量名
     * @return 被移除的变量值
     */
    public Object removeContextValue(String key) {
        Object removed = context.remove(key);
        if (removed != null) {
            this.updateTime = LocalDateTime.now();
        }
        return removed;
    }
    
    /**
     * 获取实例配置参数
     * @return 配置参数的副本
     */
    public Map<String, Object> getConfig() {
        return new ConcurrentHashMap<>(config);
    }
    
    /**
     * 获取配置参数
     * @param key 配置键
     * @return 配置值，如果不存在返回null
     */
    public Object getConfigValue(String key) {
        return config.get(key);
    }
    
    /**
     * 获取配置参数（带默认值）
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
     * 获取启动用户ID
     * @return 启动用户ID
     */
    public String getStartUserId() {
        return startUserId;
    }
    
    /**
     * 获取当前处理用户ID
     * @return 当前处理用户ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * 设置当前处理用户
     * @param userId 用户ID
     */
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 获取业务键
     * @return 业务键
     */
    public String getBusinessKey() {
        return businessKey;
    }
    
    /**
     * 获取优先级
     * @return 优先级数值，数值越大优先级越高
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    /**
     * 获取开始执行时间
     * @return 开始执行时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * 获取结束时间
     * @return 结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * 获取最后更新时间
     * @return 最后更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    /**
     * 获取错误信息
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 设置错误信息
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 获取错误堆栈
     * @return 错误堆栈信息
     */
    public String getErrorStack() {
        return errorStack;
    }
    
    /**
     * 设置错误堆栈
     * @param errorStack 错误堆栈信息
     */
    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 设置错误信息（包含异常）
     * @param exception 异常对象
     */
    public void setError(Exception exception) {
        this.errorMessage = exception.getMessage();
        this.errorStack = getStackTrace(exception);
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 清除错误信息
     */
    public void clearError() {
        this.errorMessage = null;
        this.errorStack = null;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 检查是否有错误
     * @return 如果有错误返回true，否则返回false
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    /**
     * 计算执行时长（毫秒）
     * @return 执行时长，如果未开始或未结束返回-1
     */
    public long getExecutionDuration() {
        if (startTime == null) {
            return -1;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * 获取异常堆栈信息
     * @param exception 异常对象
     * @return 堆栈信息字符串
     */
    private String getStackTrace(Exception exception) {
        if (exception == null) {
            return null;
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 创建实例构建器
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 基于现有实例创建构建器
     * @param instance 现有实例
     * @return 预填充的构建器实例
     */
    public static Builder builder(WorkflowInstance instance) {
        return new Builder(instance);
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowInstance{id='%s', workflowId='%s', status=%s, currentStep='%s', businessKey='%s'}",
                id, workflowId, status, currentStepId, businessKey);
    }
    
    /**
     * 工作流实例构建器
     * 
     * 使用Builder模式构建工作流实例，提供：
     * 1. 灵活的实例配置
     * 2. 默认值设置
     * 3. 参数验证
     * 4. 链式调用API
     */
    public static class Builder {
        private String id;
        private String workflowId;
        private String name;
        private InstanceStatus status = InstanceStatus.CREATED;
        private String currentStepId;
        private int currentStepOrder = 0;
        private Map<String, Object> context = new ConcurrentHashMap<>();
        private Map<String, Object> config = new ConcurrentHashMap<>();
        private String startUserId;
        private String currentUserId;
        private String businessKey;
        private int priority = 0; // 默认优先级
        private LocalDateTime createTime = LocalDateTime.now();
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime updateTime = LocalDateTime.now();
        private String errorMessage;
        private String errorStack;
        
        /**
         * 默认构造函数
         */
        public Builder() {
        }
        
        /**
         * 基于现有实例的构造函数
         * @param instance 现有实例
         */
        public Builder(WorkflowInstance instance) {
            this.id = instance.id;
            this.workflowId = instance.workflowId;
            this.name = instance.name;
            this.status = instance.status;
            this.currentStepId = instance.currentStepId;
            this.currentStepOrder = instance.currentStepOrder;
            this.context = new ConcurrentHashMap<>(instance.context);
            this.config = new ConcurrentHashMap<>(instance.config);
            this.startUserId = instance.startUserId;
            this.currentUserId = instance.currentUserId;
            this.businessKey = instance.businessKey;
            this.priority = instance.priority;
            this.createTime = instance.createTime;
            this.startTime = instance.startTime;
            this.endTime = instance.endTime;
            this.updateTime = LocalDateTime.now();
            this.errorMessage = instance.errorMessage;
            this.errorStack = instance.errorStack;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder status(InstanceStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder currentStep(String stepId, int stepOrder) {
            this.currentStepId = stepId;
            this.currentStepOrder = stepOrder;
            return this;
        }
        
        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            if (context != null) {
                this.context.putAll(context);
            }
            return this;
        }
        
        public Builder config(String key, Object value) {
            this.config.put(key, value);
            return this;
        }
        
        public Builder config(Map<String, Object> config) {
            if (config != null) {
                this.config.putAll(config);
            }
            return this;
        }
        
        public Builder startUserId(String startUserId) {
            this.startUserId = startUserId;
            return this;
        }
        
        public Builder currentUserId(String currentUserId) {
            this.currentUserId = currentUserId;
            return this;
        }
        
        public Builder businessKey(String businessKey) {
            this.businessKey = businessKey;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * 构建工作流实例
         * 
         * @return 工作流实例
         * @throws IllegalStateException 如果构建参数不合法
         */
        public WorkflowInstance build() {
            // 验证必填字段
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalStateException("实例ID不能为空");
            }
            if (workflowId == null || workflowId.trim().isEmpty()) {
                throw new IllegalStateException("工作流ID不能为空");
            }
            if (startUserId == null || startUserId.trim().isEmpty()) {
                throw new IllegalStateException("启动用户ID不能为空");
            }
            
            return new WorkflowInstance(this);
        }
    }
}