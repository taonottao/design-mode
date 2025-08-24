package com.tao.workflow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 步骤执行上下文
 * 
 * 封装工作流步骤执行过程中需要的上下文信息，包括执行参数、共享数据、
 * 用户信息、执行环境等。这个类是步骤执行过程中数据传递的载体。
 * 
 * 主要功能：
 * 1. 存储步骤执行的输入参数
 * 2. 提供工作流实例的共享数据访问
 * 3. 管理执行过程中的临时数据
 * 4. 提供用户和权限信息
 * 5. 记录执行的时间和环境信息
 * 6. 支持线程安全的数据访问
 * 
 * @author Tao
 * @version 1.0
 */
public class StepExecutionContext {
    
    /** 执行用户ID */
    private final String userId;
    
    /** 执行用户名 */
    private final String userName;
    
    /** 用户角色列表 */
    private final String[] userRoles;
    
    /** 步骤输入参数 */
    private final Map<String, Object> inputParameters;
    
    /** 工作流实例上下文数据（只读） */
    private final Map<String, Object> instanceContext;
    
    /** 步骤执行过程中的临时数据（可读写） */
    private final Map<String, Object> temporaryData;
    
    /** 执行开始时间 */
    private final long startTime;
    
    /** 执行超时时间（毫秒） */
    private final Long timeoutMillis;
    
    /** 当前重试次数 */
    private final int retryCount;
    
    /** 执行环境信息 */
    private final Map<String, Object> environment;
    
    /** 扩展属性 */
    private final Map<String, Object> attributes;
    
    /** 是否为异步执行 */
    private final boolean async;
    
    /** 执行优先级 */
    private final int priority;
    
    /** 执行标签 */
    private final Map<String, String> labels;
    
    /** 父级上下文（用于子流程） */
    private final StepExecutionContext parentContext;
    
    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    private StepExecutionContext(Builder builder) {
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.userRoles = builder.userRoles != null ? builder.userRoles.clone() : new String[0];
        this.inputParameters = new HashMap<>(builder.inputParameters);
        this.instanceContext = new HashMap<>(builder.instanceContext);
        this.temporaryData = new ConcurrentHashMap<>(builder.temporaryData);
        this.startTime = builder.startTime;
        this.timeoutMillis = builder.timeoutMillis;
        this.retryCount = builder.retryCount;
        this.environment = new HashMap<>(builder.environment);
        this.attributes = new HashMap<>(builder.attributes);
        this.async = builder.async;
        this.priority = builder.priority;
        this.labels = new HashMap<>(builder.labels);
        this.parentContext = builder.parentContext;
    }
    
    // Getters
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String[] getUserRoles() {
        return userRoles.clone();
    }
    
    public Map<String, Object> getInputParameters() {
        return new HashMap<>(inputParameters);
    }
    
    public Map<String, Object> getInstanceContext() {
        return new HashMap<>(instanceContext);
    }
    
    public Map<String, Object> getTemporaryData() {
        return new HashMap<>(temporaryData);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public Long getTimeoutMillis() {
        return timeoutMillis;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public Map<String, Object> getEnvironment() {
        return new HashMap<>(environment);
    }
    
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public Map<String, String> getLabels() {
        return new HashMap<>(labels);
    }
    
    public StepExecutionContext getParentContext() {
        return parentContext;
    }
    
    // 便利方法
    
    /**
     * 获取输入参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getInputParameter(String key, Class<T> type) {
        Object value = inputParameters.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将输入参数 [%s] 的值 [%s] 转换为类型 [%s]", 
                                                  key, value, type.getName()));
    }
    
    /**
     * 获取输入参数值（带默认值）
     */
    public <T> T getInputParameter(String key, Class<T> type, T defaultValue) {
        T value = getInputParameter(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取字符串类型的输入参数
     */
    public String getInputString(String key) {
        return getInputParameter(key, String.class);
    }
    
    /**
     * 获取字符串类型的输入参数（带默认值）
     */
    public String getInputString(String key, String defaultValue) {
        return getInputParameter(key, String.class, defaultValue);
    }
    
    /**
     * 获取整数类型的输入参数
     */
    public Integer getInputInteger(String key) {
        return getInputParameter(key, Integer.class);
    }
    
    /**
     * 获取整数类型的输入参数（带默认值）
     */
    public Integer getInputInteger(String key, Integer defaultValue) {
        return getInputParameter(key, Integer.class, defaultValue);
    }
    
    /**
     * 获取布尔类型的输入参数
     */
    public Boolean getInputBoolean(String key) {
        return getInputParameter(key, Boolean.class);
    }
    
    /**
     * 获取布尔类型的输入参数（带默认值）
     */
    public Boolean getInputBoolean(String key, Boolean defaultValue) {
        return getInputParameter(key, Boolean.class, defaultValue);
    }
    
    /**
     * 获取实例上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key, Class<T> type) {
        Object value = instanceContext.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将上下文数据 [%s] 的值 [%s] 转换为类型 [%s]", 
                                                  key, value, type.getName()));
    }
    
    /**
     * 获取实例上下文数据（带默认值）
     */
    public <T> T getContextData(String key, Class<T> type, T defaultValue) {
        T value = getContextData(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 设置临时数据
     */
    public void setTemporaryData(String key, Object value) {
        temporaryData.put(key, value);
    }
    
    /**
     * 获取临时数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getTemporaryData(String key, Class<T> type) {
        Object value = temporaryData.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将临时数据 [%s] 的值 [%s] 转换为类型 [%s]", 
                                                  key, value, type.getName()));
    }
    
    /**
     * 获取临时数据（带默认值）
     */
    public <T> T getTemporaryData(String key, Class<T> type, T defaultValue) {
        T value = getTemporaryData(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 移除临时数据
     */
    public Object removeTemporaryData(String key) {
        return temporaryData.remove(key);
    }
    
    /**
     * 清空临时数据
     */
    public void clearTemporaryData() {
        temporaryData.clear();
    }
    
    /**
     * 检查用户是否具有指定角色
     */
    public boolean hasRole(String role) {
        if (userRoles == null || role == null) {
            return false;
        }
        for (String userRole : userRoles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查用户是否具有任意一个指定角色
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查用户是否具有所有指定角色
     */
    public boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取环境变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getEnvironment(String key, Class<T> type) {
        Object value = environment.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将环境变量 [%s] 的值 [%s] 转换为类型 [%s]", 
                                                  key, value, type.getName()));
    }
    
    /**
     * 获取环境变量（带默认值）
     */
    public <T> T getEnvironment(String key, Class<T> type, T defaultValue) {
        T value = getEnvironment(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取标签值
     */
    public String getLabel(String key) {
        return labels.get(key);
    }
    
    /**
     * 获取标签值（带默认值）
     */
    public String getLabel(String key, String defaultValue) {
        String value = labels.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 检查是否包含指定标签
     */
    public boolean hasLabel(String key) {
        return labels.containsKey(key);
    }
    
    /**
     * 检查标签值是否匹配
     */
    public boolean hasLabel(String key, String value) {
        String labelValue = labels.get(key);
        return Objects.equals(labelValue, value);
    }
    
    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将扩展属性 [%s] 的值 [%s] 转换为类型 [%s]", 
                                                  key, value, type.getName()));
    }
    
    /**
     * 获取扩展属性（带默认值）
     */
    public <T> T getAttribute(String key, Class<T> type, T defaultValue) {
        T value = getAttribute(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 检查是否已超时
     */
    public boolean isTimeout() {
        if (timeoutMillis == null) {
            return false;
        }
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }
    
    /**
     * 获取剩余时间（毫秒）
     */
    public long getRemainingTime() {
        if (timeoutMillis == null) {
            return Long.MAX_VALUE;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, timeoutMillis - elapsed);
    }
    
    /**
     * 获取已执行时间（毫秒）
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 是否为首次执行（非重试）
     */
    public boolean isFirstExecution() {
        return retryCount == 0;
    }
    
    /**
     * 创建子上下文
     */
    public StepExecutionContext createChildContext(String childUserId) {
        return builder()
            .userId(childUserId)
            .userName(this.userName)
            .userRoles(this.userRoles)
            .instanceContext(this.instanceContext)
            .environment(this.environment)
            .parentContext(this)
            .build();
    }
    
    @Override
    public String toString() {
        return String.format("StepExecutionContext{userId='%s', retryCount=%d, async=%s, priority=%d}", 
                           userId, retryCount, async, priority);
    }
    
    // 静态工厂方法
    
    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 创建简单的执行上下文
     */
    public static StepExecutionContext simple(String userId) {
        return builder().userId(userId).build();
    }
    
    /**
     * 创建带输入参数的执行上下文
     */
    public static StepExecutionContext withInput(String userId, Map<String, Object> inputParameters) {
        return builder().userId(userId).inputParameters(inputParameters).build();
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private String userId;
        private String userName;
        private String[] userRoles;
        private final Map<String, Object> inputParameters = new HashMap<>();
        private final Map<String, Object> instanceContext = new HashMap<>();
        private final Map<String, Object> temporaryData = new HashMap<>();
        private long startTime = System.currentTimeMillis();
        private Long timeoutMillis;
        private int retryCount = 0;
        private final Map<String, Object> environment = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean async = false;
        private int priority = 0;
        private final Map<String, String> labels = new HashMap<>();
        private StepExecutionContext parentContext;
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }
        
        public Builder userRoles(String... userRoles) {
            this.userRoles = userRoles;
            return this;
        }
        
        public Builder inputParameters(Map<String, Object> inputParameters) {
            if (inputParameters != null) {
                this.inputParameters.putAll(inputParameters);
            }
            return this;
        }
        
        public Builder inputParameter(String key, Object value) {
            this.inputParameters.put(key, value);
            return this;
        }
        
        public Builder instanceContext(Map<String, Object> instanceContext) {
            if (instanceContext != null) {
                this.instanceContext.putAll(instanceContext);
            }
            return this;
        }
        
        public Builder contextData(String key, Object value) {
            this.instanceContext.put(key, value);
            return this;
        }
        
        public Builder temporaryData(Map<String, Object> temporaryData) {
            if (temporaryData != null) {
                this.temporaryData.putAll(temporaryData);
            }
            return this;
        }
        
        public Builder temporaryData(String key, Object value) {
            this.temporaryData.put(key, value);
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder timeoutMillis(Long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }
        
        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }
        
        public Builder environment(Map<String, Object> environment) {
            if (environment != null) {
                this.environment.putAll(environment);
            }
            return this;
        }
        
        public Builder environment(String key, Object value) {
            this.environment.put(key, value);
            return this;
        }
        
        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                this.labels.putAll(labels);
            }
            return this;
        }
        
        public Builder label(String key, String value) {
            this.labels.put(key, value);
            return this;
        }
        
        public Builder parentContext(StepExecutionContext parentContext) {
            this.parentContext = parentContext;
            return this;
        }
        
        public StepExecutionContext build() {
            Objects.requireNonNull(userId, "用户ID不能为空");
            return new StepExecutionContext(this);
        }
    }
}