package com.tao.workflow.executor;

import com.tao.workflow.engine.*;
import com.tao.workflow.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 抽象步骤执行器基类
 * 
 * 为具体的步骤执行器提供通用功能和默认实现，包括：
 * 1. 基础的执行框架和生命周期管理
 * 2. 超时处理和异常处理机制
 * 3. 重试逻辑和延迟计算
 * 4. 健康检查和监控指标
 * 5. 资源管理和环境准备
 * 6. 配置验证和前置条件检查
 * 
 * 子类只需要实现核心的业务逻辑方法即可。
 * 
 * @author Tao
 * @version 1.0
 */
public abstract class AbstractStepExecutor implements StepExecutor {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /** 执行器名称 */
    private final String executorName;
    
    /** 执行器版本 */
    private final String executorVersion;
    
    /** 支持的步骤类型 */
    private final Set<StepType> supportedStepTypes;
    
    /** 执行器配置 */
    private final Map<String, Object> executorConfig;
    
    /** 执行统计信息 */
    private final ExecutorStatistics statistics;
    
    /** 健康状态 */
    private volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    
    /** 最后健康检查时间 */
    private volatile long lastHealthCheckTime = System.currentTimeMillis();
    
    /**
     * 构造函数
     */
    protected AbstractStepExecutor(String executorName, String executorVersion, Set<StepType> supportedStepTypes) {
        this.executorName = Objects.requireNonNull(executorName, "执行器名称不能为空");
        this.executorVersion = Objects.requireNonNull(executorVersion, "执行器版本不能为空");
        this.supportedStepTypes = new HashSet<>(Objects.requireNonNull(supportedStepTypes, "支持的步骤类型不能为空"));
        this.executorConfig = new HashMap<>();
        this.statistics = new ExecutorStatistics();
        
        logger.info("步骤执行器已初始化: {} v{}, 支持类型: {}", executorName, executorVersion, supportedStepTypes);
    }
    
    /**
     * 简化构造函数（单一步骤类型）
     */
    protected AbstractStepExecutor(String executorName, String executorVersion, StepType supportedStepType) {
        this(executorName, executorVersion, Collections.singleton(supportedStepType));
    }
    
    @Override
    public final StepExecutionResult execute(WorkflowStep step, StepExecutionContext context) {
        Objects.requireNonNull(step, "工作流步骤不能为空");
        Objects.requireNonNull(context, "执行上下文不能为空");
        
        long startTime = System.currentTimeMillis();
        String stepId = step.getId();
        
        logger.info("开始执行步骤: {} (类型: {}, 执行器: {})", stepId, step.getType(), executorName);
        
        try {
            // 1. 验证步骤类型支持
            if (!isSupportedStepType(step.getType())) {
                String errorMsg = String.format("执行器 [%s] 不支持步骤类型 [%s]", executorName, step.getType());
                logger.error(errorMsg);
                return createFailureResult(step, errorMsg, startTime);
            }
            
            // 2. 验证配置
            try {
                validateConfiguration(step);
            } catch (Exception e) {
                String errorMsg = "步骤配置验证失败: " + e.getMessage();
                logger.error(errorMsg, e);
                return createFailureResult(step, errorMsg, startTime);
            }
            
            // 3. 检查前置条件
            if (!checkPreconditions(step, context)) {
                String errorMsg = "步骤前置条件检查失败";
                logger.warn(errorMsg);
                return createSkippedResult(step, errorMsg, startTime);
            }
            
            // 4. 准备执行环境
            try {
                prepareEnvironment(step, context);
            } catch (Exception e) {
                String errorMsg = "准备执行环境失败: " + e.getMessage();
                logger.error(errorMsg, e);
                return createFailureResult(step, errorMsg, startTime);
            }
            
            // 5. 执行核心业务逻辑
            StepExecutionResult result;
            try {
                result = doExecute(step, context);
                if (result == null) {
                    result = createSuccessResult(step, Collections.emptyMap(), startTime);
                }
            } catch (Exception e) {
                String errorMsg = "步骤执行失败: " + e.getMessage();
                logger.error(errorMsg, e);
                result = createFailureResult(step, errorMsg, startTime, e);
            }
            
            // 6. 清理环境
            try {
                cleanupEnvironment(step, context);
            } catch (Exception e) {
                logger.warn("清理执行环境失败: {}", e.getMessage(), e);
                // 清理失败不影响执行结果
            }
            
            // 7. 更新统计信息
            updateStatistics(result, System.currentTimeMillis() - startTime);
            
            logger.info("步骤执行完成: {} (状态: {}, 耗时: {}ms)", 
                       stepId, result.getStatus(), System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (Exception e) {
            // 捕获所有未处理的异常
            String errorMsg = "步骤执行发生未知异常: " + e.getMessage();
            logger.error(errorMsg, e);
            
            StepExecutionResult result = createFailureResult(step, errorMsg, startTime, e);
            updateStatistics(result, System.currentTimeMillis() - startTime);
            
            return result;
        }
    }
    
    /**
     * 子类需要实现的核心执行逻辑
     * 
     * @param step 工作流步骤
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    protected abstract StepExecutionResult doExecute(WorkflowStep step, StepExecutionContext context) throws Exception;
    
    @Override
    public boolean isSupportedStepType(StepType stepType) {
        return supportedStepTypes.contains(stepType);
    }
    
    @Override
    public String getExecutorName() {
        return executorName;
    }
    
    @Override
    public String getExecutorVersion() {
        return executorVersion;
    }
    
    @Override
    public void validateConfiguration(WorkflowStep step) throws WorkflowException {
        Objects.requireNonNull(step, "工作流步骤不能为空");
        
        // 基础验证
        if (step.getId() == null || step.getId().trim().isEmpty()) {
            throw new WorkflowException("步骤ID不能为空", WorkflowException.ErrorType.CONFIGURATION_ERROR);
        }
        
        if (step.getType() == null) {
            throw new WorkflowException("步骤类型不能为空", WorkflowException.ErrorType.CONFIGURATION_ERROR);
        }
        
        // 调用子类的具体验证逻辑
        doValidateConfiguration(step);
    }
    
    /**
     * 子类可以重写的配置验证方法
     */
    protected void doValidateConfiguration(WorkflowStep step) throws WorkflowException {
        // 默认实现：无额外验证
    }
    
    @Override
    public void prepareEnvironment(WorkflowStep step, StepExecutionContext context) throws WorkflowException {
        // 默认实现：记录准备日志
        logger.debug("准备执行环境: {} (执行器: {})", step.getId(), executorName);
        
        // 调用子类的具体准备逻辑
        doPrepareEnvironment(step, context);
    }
    
    /**
     * 子类可以重写的环境准备方法
     */
    protected void doPrepareEnvironment(WorkflowStep step, StepExecutionContext context) throws WorkflowException {
        // 默认实现：无额外准备
    }
    
    @Override
    public void cleanupEnvironment(WorkflowStep step, StepExecutionContext context) {
        // 默认实现：记录清理日志
        logger.debug("清理执行环境: {} (执行器: {})", step.getId(), executorName);
        
        // 调用子类的具体清理逻辑
        doCleanupEnvironment(step, context);
    }
    
    /**
     * 子类可以重写的环境清理方法
     */
    protected void doCleanupEnvironment(WorkflowStep step, StepExecutionContext context) {
        // 默认实现：无额外清理
    }
    
    @Override
    public void handleTimeout(WorkflowStep step, StepExecutionContext context) {
        logger.warn("步骤执行超时: {} (执行器: {}, 超时时间: {}秒)", 
                   step.getId(), executorName, step.getTimeoutSeconds());
        
        // 调用子类的具体超时处理逻辑
        doHandleTimeout(step, context);
    }
    
    /**
     * 子类可以重写的超时处理方法
     */
    protected void doHandleTimeout(WorkflowStep step, StepExecutionContext context) {
        // 默认实现：无额外处理
    }
    
    @Override
    public void handleException(WorkflowStep step, StepExecutionContext context, Exception exception) {
        logger.error("步骤执行异常: {} (执行器: {})", step.getId(), executorName, exception);
        
        // 调用子类的具体异常处理逻辑
        doHandleException(step, context, exception);
    }
    
    /**
     * 子类可以重写的异常处理方法
     */
    protected void doHandleException(WorkflowStep step, StepExecutionContext context, Exception exception) {
        // 默认实现：无额外处理
    }
    
    @Override
    public boolean isRetryable(WorkflowStep step, StepExecutionContext context, Exception exception) {
        // 默认重试策略：检查步骤配置和异常类型
        if (step.getRetryCount() == null || step.getRetryCount() <= 0) {
            return false;
        }
        
        // 某些异常类型不应该重试
        if (exception instanceof WorkflowException) {
            WorkflowException we = (WorkflowException) exception;
            return we.isRetryable();
        }
        
        // 调用子类的具体重试判断逻辑
        return doIsRetryable(step, context, exception);
    }
    
    /**
     * 子类可以重写的重试判断方法
     */
    protected boolean doIsRetryable(WorkflowStep step, StepExecutionContext context, Exception exception) {
        // 默认实现：大部分异常都可以重试
        return !(exception instanceof IllegalArgumentException || 
                exception instanceof NullPointerException);
    }
    
    @Override
    public long calculateRetryDelay(WorkflowStep step, StepExecutionContext context, int retryCount) {
        // 默认重试延迟策略：指数退避
        long baseDelay = 1000; // 1秒
        long maxDelay = 300000; // 5分钟
        
        long delay = Math.min(maxDelay, baseDelay * (1L << Math.min(retryCount, 10)));
        
        // 调用子类的具体延迟计算逻辑
        return doCalculateRetryDelay(step, context, retryCount, delay);
    }
    
    /**
     * 子类可以重写的重试延迟计算方法
     */
    protected long doCalculateRetryDelay(WorkflowStep step, StepExecutionContext context, int retryCount, long defaultDelay) {
        return defaultDelay;
    }
    
    @Override
    public HealthStatus getHealthStatus() {
        // 定期更新健康状态
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > 60000) { // 1分钟检查一次
            updateHealthStatus();
            lastHealthCheckTime = now;
        }
        
        return healthStatus;
    }
    
    /**
     * 更新健康状态
     */
    protected void updateHealthStatus() {
        try {
            // 调用子类的健康检查逻辑
            HealthStatus newStatus = doHealthCheck();
            if (newStatus != healthStatus) {
                logger.info("执行器健康状态变更: {} -> {} (执行器: {})", healthStatus, newStatus, executorName);
                healthStatus = newStatus;
            }
        } catch (Exception e) {
            logger.error("健康检查失败: {} (执行器: {})", e.getMessage(), executorName, e);
            healthStatus = HealthStatus.UNHEALTHY;
        }
    }
    
    /**
     * 子类可以重写的健康检查方法
     */
    protected HealthStatus doHealthCheck() {
        return HealthStatus.HEALTHY;
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("executorName", executorName);
        metrics.put("executorVersion", executorVersion);
        metrics.put("supportedStepTypes", supportedStepTypes);
        metrics.put("healthStatus", healthStatus);
        metrics.put("statistics", statistics.toMap());
        
        // 添加子类的自定义指标
        Map<String, Object> customMetrics = doGetMetrics();
        if (customMetrics != null) {
            metrics.putAll(customMetrics);
        }
        
        return metrics;
    }
    
    /**
     * 子类可以重写的自定义指标方法
     */
    protected Map<String, Object> doGetMetrics() {
        return Collections.emptyMap();
    }
    
    @Override
    public long estimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        // 默认估算：基于历史平均执行时间
        long avgTime = statistics.getAverageExecutionTime();
        if (avgTime > 0) {
            return avgTime;
        }
        
        // 调用子类的具体估算逻辑
        return doEstimateExecutionTime(step, context);
    }
    
    /**
     * 子类可以重写的执行时间估算方法
     */
    protected long doEstimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        // 默认估算：30秒
        return 30000;
    }
    
    @Override
    public Map<String, Object> getResourceRequirements(WorkflowStep step, StepExecutionContext context) {
        Map<String, Object> requirements = new HashMap<>();
        requirements.put("cpu", 0.1); // 默认CPU需求
        requirements.put("memory", 64); // 默认内存需求（MB）
        requirements.put("disk", 10); // 默认磁盘需求（MB）
        
        // 添加子类的具体资源需求
        Map<String, Object> customRequirements = doGetResourceRequirements(step, context);
        if (customRequirements != null) {
            requirements.putAll(customRequirements);
        }
        
        return requirements;
    }
    
    /**
     * 子类可以重写的资源需求方法
     */
    protected Map<String, Object> doGetResourceRequirements(WorkflowStep step, StepExecutionContext context) {
        return Collections.emptyMap();
    }
    
    @Override
    public boolean checkPreconditions(WorkflowStep step, StepExecutionContext context) {
        // 基础前置条件检查
        if (healthStatus == HealthStatus.UNHEALTHY) {
            logger.warn("执行器健康状态异常，跳过步骤执行: {} (执行器: {})", step.getId(), executorName);
            return false;
        }
        
        // 调用子类的具体前置条件检查
        return doCheckPreconditions(step, context);
    }
    
    /**
     * 子类可以重写的前置条件检查方法
     */
    protected boolean doCheckPreconditions(WorkflowStep step, StepExecutionContext context) {
        return true; // 默认通过
    }
    
    @Override
    public Map<String, Object> generateExecutionReport(WorkflowStep step, StepExecutionContext context, StepExecutionResult result) {
        Map<String, Object> report = new HashMap<>();
        report.put("stepId", step.getId());
        report.put("stepType", step.getType());
        report.put("executorName", executorName);
        report.put("executorVersion", executorVersion);
        report.put("status", result.getStatus());
        report.put("executionTime", result.getExecutionTime());
        report.put("retryCount", context.getRetryCount());
        report.put("timestamp", LocalDateTime.now());
        
        // 添加子类的自定义报告内容
        Map<String, Object> customReport = doGenerateExecutionReport(step, context, result);
        if (customReport != null) {
            report.putAll(customReport);
        }
        
        return report;
    }
    
    /**
     * 子类可以重写的执行报告生成方法
     */
    protected Map<String, Object> doGenerateExecutionReport(WorkflowStep step, StepExecutionContext context, StepExecutionResult result) {
        return Collections.emptyMap();
    }
    
    // 辅助方法
    
    /**
     * 创建成功结果
     */
    protected StepExecutionResult createSuccessResult(WorkflowStep step, Map<String, Object> outputData, long startTime) {
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.SUCCESS)
            .stepId(step.getId())
            .executorName(executorName)
            .outputData(outputData)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * 创建失败结果
     */
    protected StepExecutionResult createFailureResult(WorkflowStep step, String errorMessage, long startTime) {
        return createFailureResult(step, errorMessage, startTime, null);
    }
    
    /**
     * 创建失败结果（带异常）
     */
    protected StepExecutionResult createFailureResult(WorkflowStep step, String errorMessage, long startTime, Exception exception) {
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.FAILED)
            .stepId(step.getId())
            .executorName(executorName)
            .errorMessage(errorMessage)
            .exception(exception)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * 创建跳过结果
     */
    protected StepExecutionResult createSkippedResult(WorkflowStep step, String message, long startTime) {
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.SKIPPED)
            .stepId(step.getId())
            .executorName(executorName)
            .message(message)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * 创建等待结果
     */
    protected StepExecutionResult createWaitingResult(WorkflowStep step, String message, Map<String, Object> outputData, long startTime) {
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.WAITING)
            .stepId(step.getId())
            .executorName(executorName)
            .message(message)
            .outputData(outputData)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * 创建重试结果
     */
    protected StepExecutionResult createRetryResult(WorkflowStep step, String message, long startTime) {
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.RETRY)
            .stepId(step.getId())
            .executorName(executorName)
            .message(message)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(StepExecutionResult result, long executionTime) {
        statistics.incrementTotalExecutions();
        statistics.addExecutionTime(executionTime);
        
        switch (result.getStatus()) {
            case SUCCESS:
                statistics.incrementSuccessfulExecutions();
                break;
            case FAILED:
                statistics.incrementFailedExecutions();
                break;
            case TIMEOUT:
                statistics.incrementTimeoutExecutions();
                break;
            case RETRY:
                statistics.incrementRetryExecutions();
                break;
            default:
                break;
        }
    }
    
    /**
     * 获取执行器配置
     */
    protected Map<String, Object> getExecutorConfig() {
        return new HashMap<>(executorConfig);
    }
    
    /**
     * 设置执行器配置
     */
    protected void setExecutorConfig(String key, Object value) {
        executorConfig.put(key, value);
    }
    
    /**
     * 获取执行器配置值
     */
    @SuppressWarnings("unchecked")
    protected <T> T getExecutorConfig(String key, Class<T> type, T defaultValue) {
        Object value = executorConfig.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * 获取统计信息
     */
    protected ExecutorStatistics getStatistics() {
        return statistics;
    }
    
    // 内部类
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY("健康"),
        DEGRADED("降级"),
        UNHEALTHY("不健康");
        
        private final String description;
        
        HealthStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 执行器统计信息
     */
    public static class ExecutorStatistics {
        private volatile long totalExecutions = 0;
        private volatile long successfulExecutions = 0;
        private volatile long failedExecutions = 0;
        private volatile long timeoutExecutions = 0;
        private volatile long retryExecutions = 0;
        private volatile long totalExecutionTime = 0;
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = 0;
        
        public synchronized void incrementTotalExecutions() {
            totalExecutions++;
        }
        
        public synchronized void incrementSuccessfulExecutions() {
            successfulExecutions++;
        }
        
        public synchronized void incrementFailedExecutions() {
            failedExecutions++;
        }
        
        public synchronized void incrementTimeoutExecutions() {
            timeoutExecutions++;
        }
        
        public synchronized void incrementRetryExecutions() {
            retryExecutions++;
        }
        
        public synchronized void addExecutionTime(long executionTime) {
            totalExecutionTime += executionTime;
            minExecutionTime = Math.min(minExecutionTime, executionTime);
            maxExecutionTime = Math.max(maxExecutionTime, executionTime);
        }
        
        public long getTotalExecutions() {
            return totalExecutions;
        }
        
        public long getSuccessfulExecutions() {
            return successfulExecutions;
        }
        
        public long getFailedExecutions() {
            return failedExecutions;
        }
        
        public long getTimeoutExecutions() {
            return timeoutExecutions;
        }
        
        public long getRetryExecutions() {
            return retryExecutions;
        }
        
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }
        
        public double getFailureRate() {
            return totalExecutions > 0 ? (double) failedExecutions / totalExecutions : 0.0;
        }
        
        public long getAverageExecutionTime() {
            return totalExecutions > 0 ? totalExecutionTime / totalExecutions : 0;
        }
        
        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }
        
        public long getMaxExecutionTime() {
            return maxExecutionTime;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalExecutions", totalExecutions);
            map.put("successfulExecutions", successfulExecutions);
            map.put("failedExecutions", failedExecutions);
            map.put("timeoutExecutions", timeoutExecutions);
            map.put("retryExecutions", retryExecutions);
            map.put("successRate", getSuccessRate());
            map.put("failureRate", getFailureRate());
            map.put("averageExecutionTime", getAverageExecutionTime());
            map.put("minExecutionTime", getMinExecutionTime());
            map.put("maxExecutionTime", getMaxExecutionTime());
            return map;
        }
        
        @Override
        public String toString() {
            return String.format("ExecutorStatistics{total=%d, success=%d, failed=%d, successRate=%.2f%%, avgTime=%dms}", 
                               totalExecutions, successfulExecutions, failedExecutions, getSuccessRate() * 100, getAverageExecutionTime());
        }
    }
}