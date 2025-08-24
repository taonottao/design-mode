package com.tao.workflow.engine;

import com.tao.workflow.model.WorkflowStep;
import com.tao.workflow.model.WorkflowInstance;
import java.util.Map;

/**
 * 步骤执行器接口
 * 
 * 定义了工作流步骤的执行逻辑，每个具体的步骤类型都需要实现此接口。
 * 这是工作流引擎中的核心组件，负责具体步骤的业务逻辑执行。
 * 
 * 设计原则：
 * 1. 单一职责：每个执行器只负责一种类型的步骤
 * 2. 开闭原则：可以扩展新的步骤执行器而不修改现有代码
 * 3. 依赖倒置：依赖抽象接口而非具体实现
 * 4. 接口隔离：提供清晰简洁的执行接口
 * 
 * 执行器类型：
 * - TaskStepExecutor：普通任务步骤执行器
 * - UserTaskStepExecutor：用户任务步骤执行器
 * - ServiceCallStepExecutor：服务调用步骤执行器
 * - ScriptStepExecutor：脚本步骤执行器
 * - EmailStepExecutor：邮件步骤执行器
 * - ConditionalStepExecutor：条件步骤执行器
 * - ParallelStepExecutor：并行步骤执行器
 * - TimerStepExecutor：定时器步骤执行器
 * 
 * @author Tao
 * @version 1.0
 */
public interface StepExecutor {
    
    /**
     * 执行工作流步骤
     * 
     * 这是步骤执行器的核心方法，负责执行具体的业务逻辑。
     * 执行过程中需要处理各种异常情况，并返回执行结果。
     * 
     * @param step 要执行的工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文，包含步骤执行所需的数据
     * @return 步骤执行结果
     * @throws WorkflowException 如果执行过程中发生错误
     */
    StepExecutionResult execute(WorkflowStep step, WorkflowInstance instance, StepExecutionContext context) throws WorkflowException;
    
    /**
     * 检查是否支持指定的步骤类型
     * 
     * 用于工作流引擎选择合适的执行器。
     * 
     * @param stepType 步骤类型
     * @return 如果支持返回true，否则返回false
     */
    boolean supports(String stepType);
    
    /**
     * 获取执行器名称
     * 
     * 用于标识和日志记录。
     * 
     * @return 执行器名称
     */
    String getName();
    
    /**
     * 获取执行器版本
     * 
     * 用于版本管理和兼容性检查。
     * 
     * @return 执行器版本
     */
    String getVersion();
    
    /**
     * 验证步骤配置
     * 
     * 在步骤执行前验证配置的正确性，提前发现配置问题。
     * 
     * @param step 工作流步骤
     * @throws WorkflowException 如果配置无效
     */
    default void validateConfiguration(WorkflowStep step) throws WorkflowException {
        // 默认实现：不进行验证
        // 子类可以重写此方法提供具体的验证逻辑
    }
    
    /**
     * 准备执行环境
     * 
     * 在步骤执行前进行必要的准备工作，如资源分配、连接建立等。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @throws WorkflowException 如果准备失败
     */
    default void prepare(WorkflowStep step, WorkflowInstance instance, StepExecutionContext context) throws WorkflowException {
        // 默认实现：不进行准备
        // 子类可以重写此方法提供具体的准备逻辑
    }
    
    /**
     * 清理执行环境
     * 
     * 在步骤执行后进行清理工作，如资源释放、连接关闭等。
     * 无论执行成功还是失败，都会调用此方法。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @param result 执行结果（可能为null）
     */
    default void cleanup(WorkflowStep step, WorkflowInstance instance, StepExecutionContext context, StepExecutionResult result) {
        // 默认实现：不进行清理
        // 子类可以重写此方法提供具体的清理逻辑
    }
    
    /**
     * 处理执行超时
     * 
     * 当步骤执行超时时调用此方法，执行器可以进行超时处理逻辑。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @return 超时处理结果
     * @throws WorkflowException 如果超时处理失败
     */
    default StepExecutionResult handleTimeout(WorkflowStep step, WorkflowInstance instance, StepExecutionContext context) throws WorkflowException {
        // 默认实现：抛出超时异常
        throw WorkflowException.timeoutError(
            String.format("步骤 [%s] 执行超时", step.getName()),
            instance.getId(),
            step.getId()
        );
    }
    
    /**
     * 处理执行异常
     * 
     * 当步骤执行过程中发生异常时调用此方法，执行器可以进行异常处理和恢复。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @param exception 发生的异常
     * @return 异常处理结果，如果返回null则表示无法处理
     * @throws WorkflowException 如果异常处理失败
     */
    default StepExecutionResult handleException(WorkflowStep step, WorkflowInstance instance, 
                                               StepExecutionContext context, Exception exception) throws WorkflowException {
        // 默认实现：不处理异常，重新抛出
        if (exception instanceof WorkflowException) {
            throw (WorkflowException) exception;
        } else {
            throw WorkflowException.executionError(
                String.format("步骤 [%s] 执行失败: %s", step.getName(), exception.getMessage()),
                exception,
                instance.getId(),
                step.getId()
            );
        }
    }
    
    /**
     * 检查是否可以重试
     * 
     * 判断当前步骤在发生异常后是否可以重试。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @param exception 发生的异常
     * @param retryCount 当前重试次数
     * @return 如果可以重试返回true，否则返回false
     */
    default boolean canRetry(WorkflowStep step, WorkflowInstance instance, 
                           StepExecutionContext context, Exception exception, int retryCount) {
        // 默认实现：检查步骤配置的重试次数
        return retryCount < step.getRetryCount() && 
               (exception instanceof WorkflowException && ((WorkflowException) exception).isRetryable());
    }
    
    /**
     * 计算重试延迟时间
     * 
     * 计算下次重试的延迟时间（毫秒）。
     * 
     * @param step 工作流步骤
     * @param retryCount 当前重试次数
     * @return 延迟时间（毫秒）
     */
    default long calculateRetryDelay(WorkflowStep step, int retryCount) {
        // 默认实现：指数退避策略
        // 基础延迟时间为1秒，每次重试延迟时间翻倍，最大不超过60秒
        long baseDelay = 1000; // 1秒
        long maxDelay = 60000; // 60秒
        long delay = baseDelay * (1L << Math.min(retryCount, 6)); // 最多64倍
        return Math.min(delay, maxDelay);
    }
    
    /**
     * 获取执行器的健康状态
     * 
     * 检查执行器是否处于健康状态，可以正常执行步骤。
     * 
     * @return 健康状态信息
     */
    default ExecutorHealthStatus getHealthStatus() {
        // 默认实现：返回健康状态
        return ExecutorHealthStatus.healthy();
    }
    
    /**
     * 获取执行器的性能指标
     * 
     * 获取执行器的性能统计信息，用于监控和优化。
     * 
     * @return 性能指标
     */
    default ExecutorMetrics getMetrics() {
        // 默认实现：返回空的性能指标
        return ExecutorMetrics.empty();
    }
    
    /**
     * 预估步骤执行时间
     * 
     * 根据步骤配置和历史数据预估执行时间。
     * 
     * @param step 工作流步骤
     * @param context 执行上下文
     * @return 预估执行时间（毫秒），-1表示无法预估
     */
    default long estimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        // 默认实现：返回步骤配置的超时时间的一半，如果没有配置则返回-1
        Integer timeout = step.getTimeout();
        return timeout != null ? timeout * 500L : -1L; // 超时时间的一半
    }
    
    /**
     * 获取步骤执行的资源需求
     * 
     * 获取执行此步骤所需的资源信息，用于资源调度和管理。
     * 
     * @param step 工作流步骤
     * @param context 执行上下文
     * @return 资源需求信息
     */
    default ResourceRequirement getResourceRequirement(WorkflowStep step, StepExecutionContext context) {
        // 默认实现：返回最小资源需求
        return ResourceRequirement.minimal();
    }
    
    /**
     * 检查执行前置条件
     * 
     * 在步骤执行前检查是否满足执行的前置条件。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @return 前置条件检查结果
     * @throws WorkflowException 如果前置条件检查失败
     */
    default PreconditionCheckResult checkPreconditions(WorkflowStep step, WorkflowInstance instance, 
                                                      StepExecutionContext context) throws WorkflowException {
        // 默认实现：检查步骤配置的前置条件
        String precondition = step.getPrecondition();
        if (precondition == null || precondition.trim().isEmpty()) {
            return PreconditionCheckResult.passed();
        }
        
        // 这里可以集成表达式引擎来评估前置条件
        // 暂时简单实现：如果有前置条件就认为通过
        return PreconditionCheckResult.passed();
    }
    
    /**
     * 生成步骤执行报告
     * 
     * 生成步骤执行的详细报告，用于审计和分析。
     * 
     * @param step 工作流步骤
     * @param instance 工作流实例
     * @param context 执行上下文
     * @param result 执行结果
     * @return 执行报告
     */
    default StepExecutionReport generateReport(WorkflowStep step, WorkflowInstance instance, 
                                             StepExecutionContext context, StepExecutionResult result) {
        // 默认实现：生成基础报告
        return StepExecutionReport.builder()
            .stepId(step.getId())
            .stepName(step.getName())
            .executorName(getName())
            .instanceId(instance.getId())
            .startTime(context.getStartTime())
            .endTime(result != null ? result.getEndTime() : System.currentTimeMillis())
            .status(result != null ? result.getStatus() : "FAILED")
            .build();
    }
}