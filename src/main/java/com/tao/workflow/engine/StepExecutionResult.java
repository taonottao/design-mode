package com.tao.workflow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 步骤执行结果
 * 
 * 封装工作流步骤执行的结果信息，包括执行状态、输出数据、错误信息等。
 * 这个类是步骤执行器与工作流引擎之间的重要数据传递载体。
 * 
 * 主要包含的信息：
 * 1. 执行状态：成功、失败、等待、跳过等
 * 2. 输出数据：步骤执行产生的数据
 * 3. 下一步骤：指定下一个要执行的步骤
 * 4. 错误信息：执行失败时的错误详情
 * 5. 执行时间：开始和结束时间
 * 6. 重试信息：是否需要重试及重试配置
 * 
 * @author Tao
 * @version 1.0
 */
public class StepExecutionResult {
    
    /**
     * 执行状态枚举
     */
    public enum Status {
        /** 执行成功 */
        SUCCESS("执行成功", true, false),
        
        /** 执行失败 */
        FAILED("执行失败", false, false),
        
        /** 等待中（如用户任务等待用户操作） */
        WAITING("等待中", false, false),
        
        /** 已跳过 */
        SKIPPED("已跳过", true, false),
        
        /** 已取消 */
        CANCELLED("已取消", false, false),
        
        /** 超时 */
        TIMEOUT("执行超时", false, true),
        
        /** 需要重试 */
        RETRY("需要重试", false, true),
        
        /** 暂停 */
        SUSPENDED("已暂停", false, false),
        
        /** 执行中 */
        RUNNING("执行中", false, false),
        
        /** 条件不满足 */
        CONDITION_NOT_MET("条件不满足", true, false);
        
        private final String description;
        private final boolean completed;
        private final boolean retryable;
        
        Status(String description, boolean completed, boolean retryable) {
            this.description = description;
            this.completed = completed;
            this.retryable = retryable;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS;
        }
        
        public boolean isFailed() {
            return this == FAILED || this == TIMEOUT;
        }
        
        public boolean isWaiting() {
            return this == WAITING;
        }
    }
    
    /** 执行状态 */
    private final Status status;
    
    /** 状态描述 */
    private final String message;
    
    /** 输出数据 */
    private final Map<String, Object> outputData;
    
    /** 下一个步骤ID */
    private final String nextStepId;
    
    /** 错误信息 */
    private final String errorMessage;
    
    /** 异常对象 */
    private final Throwable exception;
    
    /** 开始时间 */
    private final long startTime;
    
    /** 结束时间 */
    private final long endTime;
    
    /** 执行耗时（毫秒） */
    private final long duration;
    
    /** 是否需要重试 */
    private final boolean needRetry;
    
    /** 重试延迟时间（毫秒） */
    private final long retryDelay;
    
    /** 重试次数 */
    private final int retryCount;
    
    /** 执行器名称 */
    private final String executorName;
    
    /** 扩展属性 */
    private final Map<String, Object> attributes;
    
    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    private StepExecutionResult(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.outputData = new HashMap<>(builder.outputData);
        this.nextStepId = builder.nextStepId;
        this.errorMessage = builder.errorMessage;
        this.exception = builder.exception;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = endTime - startTime;
        this.needRetry = builder.needRetry;
        this.retryDelay = builder.retryDelay;
        this.retryCount = builder.retryCount;
        this.executorName = builder.executorName;
        this.attributes = new HashMap<>(builder.attributes);
    }
    
    // Getters
    
    public Status getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Map<String, Object> getOutputData() {
        return new HashMap<>(outputData);
    }
    
    public String getNextStepId() {
        return nextStepId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public boolean isNeedRetry() {
        return needRetry;
    }
    
    public long getRetryDelay() {
        return retryDelay;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public String getExecutorName() {
        return executorName;
    }
    
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    // 便利方法
    
    /**
     * 是否执行成功
     */
    public boolean isSuccess() {
        return status.isSuccess();
    }
    
    /**
     * 是否执行失败
     */
    public boolean isFailed() {
        return status.isFailed();
    }
    
    /**
     * 是否正在等待
     */
    public boolean isWaiting() {
        return status.isWaiting();
    }
    
    /**
     * 是否已完成（成功或跳过）
     */
    public boolean isCompleted() {
        return status.isCompleted();
    }
    
    /**
     * 是否可重试
     */
    public boolean isRetryable() {
        return status.isRetryable() || needRetry;
    }
    
    /**
     * 获取输出数据中的指定值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutputValue(String key, Class<T> type) {
        Object value = outputData.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException(String.format("无法将值 [%s] 转换为类型 [%s]", value, type.getName()));
    }
    
    /**
     * 获取输出数据中的字符串值
     */
    public String getOutputString(String key) {
        return getOutputValue(key, String.class);
    }
    
    /**
     * 获取输出数据中的整数值
     */
    public Integer getOutputInteger(String key) {
        return getOutputValue(key, Integer.class);
    }
    
    /**
     * 获取输出数据中的布尔值
     */
    public Boolean getOutputBoolean(String key) {
        return getOutputValue(key, Boolean.class);
    }
    
    /**
     * 获取扩展属性值
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
        throw new ClassCastException(String.format("无法将属性值 [%s] 转换为类型 [%s]", value, type.getName()));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StepExecutionResult that = (StepExecutionResult) o;
        return startTime == that.startTime &&
               endTime == that.endTime &&
               needRetry == that.needRetry &&
               retryDelay == that.retryDelay &&
               retryCount == that.retryCount &&
               status == that.status &&
               Objects.equals(message, that.message) &&
               Objects.equals(outputData, that.outputData) &&
               Objects.equals(nextStepId, that.nextStepId) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(executorName, that.executorName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, message, outputData, nextStepId, errorMessage, 
                          startTime, endTime, needRetry, retryDelay, retryCount, executorName);
    }
    
    @Override
    public String toString() {
        return String.format("StepExecutionResult{status=%s, message='%s', duration=%dms, nextStep='%s'}", 
                           status, message, duration, nextStepId);
    }
    
    // 静态工厂方法
    
    /**
     * 创建成功结果
     */
    public static StepExecutionResult success() {
        return new Builder(Status.SUCCESS).build();
    }
    
    /**
     * 创建成功结果（带消息）
     */
    public static StepExecutionResult success(String message) {
        return new Builder(Status.SUCCESS).message(message).build();
    }
    
    /**
     * 创建成功结果（带输出数据）
     */
    public static StepExecutionResult success(Map<String, Object> outputData) {
        return new Builder(Status.SUCCESS).outputData(outputData).build();
    }
    
    /**
     * 创建成功结果（带下一步骤）
     */
    public static StepExecutionResult success(String message, String nextStepId) {
        return new Builder(Status.SUCCESS).message(message).nextStepId(nextStepId).build();
    }
    
    /**
     * 创建失败结果
     */
    public static StepExecutionResult failed(String errorMessage) {
        return new Builder(Status.FAILED).errorMessage(errorMessage).build();
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static StepExecutionResult failed(String errorMessage, Throwable exception) {
        return new Builder(Status.FAILED).errorMessage(errorMessage).exception(exception).build();
    }
    
    /**
     * 创建等待结果
     */
    public static StepExecutionResult waiting(String message) {
        return new Builder(Status.WAITING).message(message).build();
    }
    
    /**
     * 创建跳过结果
     */
    public static StepExecutionResult skipped(String reason) {
        return new Builder(Status.SKIPPED).message(reason).build();
    }
    
    /**
     * 创建超时结果
     */
    public static StepExecutionResult timeout(String message) {
        return new Builder(Status.TIMEOUT).errorMessage(message).needRetry(true).build();
    }
    
    /**
     * 创建需要重试的结果
     */
    public static StepExecutionResult retry(String message, long retryDelay) {
        return new Builder(Status.RETRY).message(message).needRetry(true).retryDelay(retryDelay).build();
    }
    
    /**
     * 创建Builder实例
     */
    public static Builder builder(Status status) {
        return new Builder(status);
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private final Status status;
        private String message;
        private final Map<String, Object> outputData = new HashMap<>();
        private String nextStepId;
        private String errorMessage;
        private Throwable exception;
        private long startTime = System.currentTimeMillis();
        private long endTime = System.currentTimeMillis();
        private boolean needRetry = false;
        private long retryDelay = 0;
        private int retryCount = 0;
        private String executorName;
        private final Map<String, Object> attributes = new HashMap<>();
        
        public Builder(Status status) {
            this.status = Objects.requireNonNull(status, "状态不能为空");
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder outputData(Map<String, Object> outputData) {
            if (outputData != null) {
                this.outputData.putAll(outputData);
            }
            return this;
        }
        
        public Builder outputData(String key, Object value) {
            this.outputData.put(key, value);
            return this;
        }
        
        public Builder nextStepId(String nextStepId) {
            this.nextStepId = nextStepId;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder needRetry(boolean needRetry) {
            this.needRetry = needRetry;
            return this;
        }
        
        public Builder retryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }
        
        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }
        
        public Builder executorName(String executorName) {
            this.executorName = executorName;
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }
        
        public StepExecutionResult build() {
            return new StepExecutionResult(this);
        }
    }
}