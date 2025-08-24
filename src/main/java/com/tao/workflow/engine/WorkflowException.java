package com.tao.workflow.engine;

/**
 * 工作流异常类
 * 
 * 用于表示工作流执行过程中发生的各种异常情况。
 * 提供了丰富的异常信息和错误分类，便于问题诊断和处理。
 * 
 * 异常分类：
 * 1. 配置异常：工作流定义错误、步骤配置错误等
 * 2. 执行异常：步骤执行失败、超时、资源不足等
 * 3. 状态异常：非法状态转换、并发冲突等
 * 4. 权限异常：用户权限不足、操作不被允许等
 * 5. 数据异常：数据不一致、约束违反等
 * 
 * @author Tao
 * @version 1.0
 */
public class WorkflowException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 异常错误码
     */
    private final String errorCode;
    
    /**
     * 工作流实例ID（如果适用）
     */
    private final String instanceId;
    
    /**
     * 步骤ID（如果适用）
     */
    private final String stepId;
    
    /**
     * 异常类型
     */
    private final WorkflowErrorType errorType;
    
    /**
     * 是否可重试
     */
    private final boolean retryable;
    
    /**
     * 异常发生时间戳
     */
    private final long timestamp;
    
    /**
     * 工作流错误类型枚举
     */
    public enum WorkflowErrorType {
        /** 配置错误 */
        CONFIGURATION_ERROR("配置错误", false),
        
        /** 执行错误 */
        EXECUTION_ERROR("执行错误", true),
        
        /** 状态错误 */
        STATE_ERROR("状态错误", false),
        
        /** 权限错误 */
        PERMISSION_ERROR("权限错误", false),
        
        /** 数据错误 */
        DATA_ERROR("数据错误", false),
        
        /** 超时错误 */
        TIMEOUT_ERROR("超时错误", true),
        
        /** 资源错误 */
        RESOURCE_ERROR("资源错误", true),
        
        /** 网络错误 */
        NETWORK_ERROR("网络错误", true),
        
        /** 系统错误 */
        SYSTEM_ERROR("系统错误", true),
        
        /** 业务错误 */
        BUSINESS_ERROR("业务错误", false),
        
        /** 未知错误 */
        UNKNOWN_ERROR("未知错误", false);
        
        private final String description;
        private final boolean defaultRetryable;
        
        WorkflowErrorType(String description, boolean defaultRetryable) {
            this.description = description;
            this.defaultRetryable = defaultRetryable;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isDefaultRetryable() {
            return defaultRetryable;
        }
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public WorkflowException(String message) {
        this(message, WorkflowErrorType.UNKNOWN_ERROR);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public WorkflowException(String message, Throwable cause) {
        this(message, cause, WorkflowErrorType.UNKNOWN_ERROR);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param errorType 错误类型
     */
    public WorkflowException(String message, WorkflowErrorType errorType) {
        this(message, null, errorType, null, null, null, errorType.isDefaultRetryable());
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     * @param errorType 错误类型
     */
    public WorkflowException(String message, Throwable cause, WorkflowErrorType errorType) {
        this(message, cause, errorType, null, null, null, errorType.isDefaultRetryable());
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param errorType 错误类型
     * @param errorCode 错误码
     */
    public WorkflowException(String message, WorkflowErrorType errorType, String errorCode) {
        this(message, null, errorType, errorCode, null, null, errorType.isDefaultRetryable());
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param errorType 错误类型
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     */
    public WorkflowException(String message, WorkflowErrorType errorType, String instanceId, String stepId) {
        this(message, null, errorType, null, instanceId, stepId, errorType.isDefaultRetryable());
    }
    
    /**
     * 完整构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     * @param errorType 错误类型
     * @param errorCode 错误码
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @param retryable 是否可重试
     */
    public WorkflowException(String message, Throwable cause, WorkflowErrorType errorType, 
                           String errorCode, String instanceId, String stepId, boolean retryable) {
        super(message, cause);
        this.errorType = errorType != null ? errorType : WorkflowErrorType.UNKNOWN_ERROR;
        this.errorCode = errorCode;
        this.instanceId = instanceId;
        this.stepId = stepId;
        this.retryable = retryable;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取工作流实例ID
     * 
     * @return 工作流实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * 获取步骤ID
     * 
     * @return 步骤ID
     */
    public String getStepId() {
        return stepId;
    }
    
    /**
     * 获取错误类型
     * 
     * @return 错误类型
     */
    public WorkflowErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * 是否可重试
     * 
     * @return 如果可重试返回true，否则返回false
     */
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * 获取异常发生时间戳
     * 
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 是否为配置错误
     * 
     * @return 如果是配置错误返回true，否则返回false
     */
    public boolean isConfigurationError() {
        return errorType == WorkflowErrorType.CONFIGURATION_ERROR;
    }
    
    /**
     * 是否为执行错误
     * 
     * @return 如果是执行错误返回true，否则返回false
     */
    public boolean isExecutionError() {
        return errorType == WorkflowErrorType.EXECUTION_ERROR;
    }
    
    /**
     * 是否为状态错误
     * 
     * @return 如果是状态错误返回true，否则返回false
     */
    public boolean isStateError() {
        return errorType == WorkflowErrorType.STATE_ERROR;
    }
    
    /**
     * 是否为权限错误
     * 
     * @return 如果是权限错误返回true，否则返回false
     */
    public boolean isPermissionError() {
        return errorType == WorkflowErrorType.PERMISSION_ERROR;
    }
    
    /**
     * 是否为超时错误
     * 
     * @return 如果是超时错误返回true，否则返回false
     */
    public boolean isTimeoutError() {
        return errorType == WorkflowErrorType.TIMEOUT_ERROR;
    }
    
    /**
     * 是否为业务错误
     * 
     * @return 如果是业务错误返回true，否则返回false
     */
    public boolean isBusinessError() {
        return errorType == WorkflowErrorType.BUSINESS_ERROR;
    }
    
    /**
     * 获取详细的错误信息
     * 
     * @return 详细错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[工作流异常] ");
        sb.append(errorType.getDescription());
        
        if (errorCode != null) {
            sb.append(" (错误码: ").append(errorCode).append(")");
        }
        
        sb.append(": ").append(getMessage());
        
        if (instanceId != null) {
            sb.append(" [实例ID: ").append(instanceId).append("]");
        }
        
        if (stepId != null) {
            sb.append(" [步骤ID: ").append(stepId).append("]");
        }
        
        sb.append(" [可重试: ").append(retryable ? "是" : "否").append("]");
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getDetailedMessage();
    }
    
    // 静态工厂方法，用于创建特定类型的异常
    
    /**
     * 创建配置错误异常
     * 
     * @param message 错误消息
     * @return 配置错误异常
     */
    public static WorkflowException configurationError(String message) {
        return new WorkflowException(message, WorkflowErrorType.CONFIGURATION_ERROR);
    }
    
    /**
     * 创建配置错误异常
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @return 配置错误异常
     */
    public static WorkflowException configurationError(String message, Throwable cause) {
        return new WorkflowException(message, cause, WorkflowErrorType.CONFIGURATION_ERROR);
    }
    
    /**
     * 创建执行错误异常
     * 
     * @param message 错误消息
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @return 执行错误异常
     */
    public static WorkflowException executionError(String message, String instanceId, String stepId) {
        return new WorkflowException(message, WorkflowErrorType.EXECUTION_ERROR, instanceId, stepId);
    }
    
    /**
     * 创建执行错误异常
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @return 执行错误异常
     */
    public static WorkflowException executionError(String message, Throwable cause, String instanceId, String stepId) {
        return new WorkflowException(message, cause, WorkflowErrorType.EXECUTION_ERROR, null, instanceId, stepId, true);
    }
    
    /**
     * 创建状态错误异常
     * 
     * @param message 错误消息
     * @param instanceId 工作流实例ID
     * @return 状态错误异常
     */
    public static WorkflowException stateError(String message, String instanceId) {
        return new WorkflowException(message, WorkflowErrorType.STATE_ERROR, instanceId, null);
    }
    
    /**
     * 创建权限错误异常
     * 
     * @param message 错误消息
     * @param instanceId 工作流实例ID
     * @return 权限错误异常
     */
    public static WorkflowException permissionError(String message, String instanceId) {
        return new WorkflowException(message, WorkflowErrorType.PERMISSION_ERROR, instanceId, null);
    }
    
    /**
     * 创建超时错误异常
     * 
     * @param message 错误消息
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @return 超时错误异常
     */
    public static WorkflowException timeoutError(String message, String instanceId, String stepId) {
        return new WorkflowException(message, WorkflowErrorType.TIMEOUT_ERROR, instanceId, stepId);
    }
    
    /**
     * 创建业务错误异常
     * 
     * @param message 错误消息
     * @param errorCode 业务错误码
     * @param instanceId 工作流实例ID
     * @return 业务错误异常
     */
    public static WorkflowException businessError(String message, String errorCode, String instanceId) {
        return new WorkflowException(message, null, WorkflowErrorType.BUSINESS_ERROR, errorCode, instanceId, null, false);
    }
    
    /**
     * 创建数据错误异常
     * 
     * @param message 错误消息
     * @return 数据错误异常
     */
    public static WorkflowException dataError(String message) {
        return new WorkflowException(message, WorkflowErrorType.DATA_ERROR);
    }
    
    /**
     * 创建数据错误异常
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @return 数据错误异常
     */
    public static WorkflowException dataError(String message, Throwable cause) {
        return new WorkflowException(message, cause, WorkflowErrorType.DATA_ERROR);
    }
    
    /**
     * 创建系统错误异常
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @return 系统错误异常
     */
    public static WorkflowException systemError(String message, Throwable cause) {
        return new WorkflowException(message, cause, WorkflowErrorType.SYSTEM_ERROR);
    }
}