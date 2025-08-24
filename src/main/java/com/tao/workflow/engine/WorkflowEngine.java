package com.tao.workflow.engine;

import com.tao.workflow.model.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流引擎接口
 * 
 * 定义了工作流引擎的核心功能，包括工作流的启动、执行、暂停、恢复、终止等操作。
 * 这是整个工作流系统的核心组件，负责协调各个步骤的执行和状态管理。
 * 
 * 主要职责：
 * 1. 工作流实例的生命周期管理
 * 2. 步骤执行的调度和协调
 * 3. 工作流状态的跟踪和更新
 * 4. 异常情况的处理和恢复
 * 5. 并发执行的控制和管理
 * 6. 工作流数据的传递和共享
 * 
 * 设计原则：
 * - 单一职责：专注于工作流的执行控制
 * - 开闭原则：支持扩展新的执行策略
 * - 依赖倒置：依赖抽象而非具体实现
 * - 接口隔离：提供清晰的操作接口
 * 
 * @author Tao
 * @version 1.0
 */
public interface WorkflowEngine {
    
    /**
     * 启动工作流实例
     * 
     * 根据工作流定义创建新的工作流实例，并开始执行第一个步骤。
     * 这是工作流生命周期的起点。
     * 
     * @param workflowId 工作流定义ID
     * @param startUserId 启动用户ID
     * @param initialContext 初始上下文数据
     * @return 创建的工作流实例
     * @throws WorkflowException 如果启动失败
     */
    WorkflowInstance startWorkflow(String workflowId, String startUserId, Map<String, Object> initialContext) throws Exception;
    
    /**
     * 启动工作流实例（带业务键）
     * 
     * 支持业务键的工作流启动，便于业务系统的集成和查询。
     * 
     * @param workflowId 工作流定义ID
     * @param businessKey 业务键，用于关联业务数据
     * @param startUserId 启动用户ID
     * @param initialContext 初始上下文数据
     * @return 创建的工作流实例
     * @throws WorkflowException 如果启动失败
     */
    WorkflowInstance startWorkflow(String workflowId, String businessKey, String startUserId, Map<String, Object> initialContext) throws WorkflowException;
    
    /**
     * 继续执行工作流实例
     * 
     * 当工作流实例处于等待状态时，通过此方法继续执行下一个步骤。
     * 通常用于用户任务完成后的流程推进。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 操作用户ID
     * @param stepResult 当前步骤的执行结果
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果执行失败
     */
    WorkflowInstance continueWorkflow(String instanceId, String userId, Map<String, Object> stepResult) throws WorkflowException;
    
    /**
     * 执行指定步骤
     * 
     * 直接执行工作流实例中的指定步骤，用于跳转或重新执行。
     * 
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @param userId 操作用户ID
     * @param stepContext 步骤执行上下文
     * @return 步骤执行结果
     * @throws WorkflowException 如果执行失败
     */
    StepExecutionResult executeStep(String instanceId, String stepId, String userId, Map<String, Object> stepContext) throws WorkflowException;
    
    /**
     * 暂停工作流实例
     * 
     * 将正在运行的工作流实例暂停，可以稍后恢复执行。
     * 暂停操作会保存当前的执行状态和上下文。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 操作用户ID
     * @param reason 暂停原因
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果暂停失败
     */
    WorkflowInstance suspendWorkflow(String instanceId, String userId, String reason) throws WorkflowException;
    
    /**
     * 恢复工作流实例
     * 
     * 恢复之前暂停的工作流实例，从暂停点继续执行。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 操作用户ID
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果恢复失败
     */
    WorkflowInstance resumeWorkflow(String instanceId, String userId) throws WorkflowException;
    
    /**
     * 终止工作流实例
     * 
     * 强制终止工作流实例的执行，终止后无法恢复。
     * 通常用于异常情况或业务需要。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 操作用户ID
     * @param reason 终止原因
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果终止失败
     */
    WorkflowInstance terminateWorkflow(String instanceId, String userId, String reason) throws WorkflowException;
    
    /**
     * 取消工作流实例
     * 
     * 取消工作流实例的执行，与终止类似但语义不同。
     * 取消通常表示正常的业务取消操作。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 操作用户ID
     * @param reason 取消原因
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果取消失败
     */
    WorkflowInstance cancelWorkflow(String instanceId, String userId, String reason) throws WorkflowException;
    
    /**
     * 获取工作流实例
     * 
     * 根据实例ID获取工作流实例的详细信息。
     * 
     * @param instanceId 工作流实例ID
     * @return 工作流实例，如果不存在返回空
     */
    Optional<WorkflowInstance> getWorkflowInstance(String instanceId);
    
    /**
     * 根据业务键获取工作流实例
     * 
     * 通过业务键查找相关的工作流实例。
     * 
     * @param businessKey 业务键
     * @return 工作流实例列表
     */
    List<WorkflowInstance> getWorkflowInstancesByBusinessKey(String businessKey);
    
    /**
     * 获取用户的待办任务
     * 
     * 获取指定用户当前需要处理的工作流任务。
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    List<WorkflowTask> getUserTasks(String userId);
    
     /**
     * 获取用户的待办任务（分页）
     * 
     * 分页获取指定用户当前需要处理的工作流任务。
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页的待办任务
     */
    PageResult<WorkflowTask> getUserTasks(String userId, int page, int size);
    
    /**
     * 获取工作流实例的执行历史
     * 
     * 获取工作流实例中所有步骤的执行历史记录。
     * 
     * @param instanceId 工作流实例ID
     * @return 执行历史列表
     */
    List<StepExecutionHistory> getExecutionHistory(String instanceId);
    
    /**
     * 获取工作流实例的当前状态
     * 
     * 获取工作流实例的详细状态信息，包括当前步骤、上下文等。
     * 
     * @param instanceId 工作流实例ID
     * @return 工作流状态信息
     */
    Optional<InstanceStatus> getWorkflowState(String instanceId);
    
    /**
     * 更新工作流实例上下文
     * 
     * 更新工作流实例的上下文数据，用于在步骤间传递数据。
     * 
     * @param instanceId 工作流实例ID
     * @param contextUpdates 上下文更新数据
     * @param userId 操作用户ID
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果更新失败
     */
    WorkflowInstance updateWorkflowContext(String instanceId, Map<String, Object> contextUpdates, String userId) throws WorkflowException;
    
    /**
     * 重试失败的步骤
     * 
     * 重新执行之前失败的步骤，用于错误恢复。
     * 
     * @param instanceId 工作流实例ID
     * @param stepId 步骤ID
     * @param userId 操作用户ID
     * @return 步骤执行结果
     * @throws WorkflowException 如果重试失败
     */
    StepExecutionResult retryStep(String instanceId, String stepId, String userId) throws WorkflowException;
    
    /**
     * 跳过当前步骤
     * 
     * 跳过当前正在执行或等待的步骤，继续执行下一个步骤。
     * 通常用于异常处理或特殊业务场景。
     * 
     * @param instanceId 工作流实例ID
     * @param stepId 要跳过的步骤ID
     * @param userId 操作用户ID
     * @param reason 跳过原因
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果跳过失败
     */
    WorkflowInstance skipStep(String instanceId, String stepId, String userId, String reason) throws WorkflowException;
    
    /**
     * 回退到指定步骤
     * 
     * 将工作流实例回退到之前的某个步骤，用于流程回滚。
     * 
     * @param instanceId 工作流实例ID
     * @param targetStepId 目标步骤ID
     * @param userId 操作用户ID
     * @param reason 回退原因
     * @return 更新后的工作流实例
     * @throws WorkflowException 如果回退失败
     */
    WorkflowInstance rollbackToStep(String instanceId, String targetStepId, String userId, String reason) throws WorkflowException;
    
    /**
     * 检查工作流实例是否可以执行指定操作
     * 
     * 验证当前工作流实例的状态是否允许执行指定的操作。
     * 
     * @param instanceId 工作流实例ID
     * @param operation 操作类型
     * @param userId 操作用户ID
     * @return 如果可以执行返回true，否则返回false
     */
    boolean canPerformOperation(String instanceId, WorkflowOperation operation, String userId);
    
    /**
     * 获取工作流实例的可执行操作列表
     * 
     * 获取当前用户对指定工作流实例可以执行的操作列表。
     * 
     * @param instanceId 工作流实例ID
     * @param userId 用户ID
     * @return 可执行操作列表
     */
    List<WorkflowOperation> getAvailableOperations(String instanceId, String userId);
    
    /**
     * 批量处理工作流实例
     * 
     * 批量执行多个工作流实例的相同操作，提高处理效率。
     * 
     * @param instanceIds 工作流实例ID列表
     * @param operation 操作类型
     * @param userId 操作用户ID
     * @param parameters 操作参数
     * @return 批量处理结果
     * @throws WorkflowException 如果批量处理失败
     */
    BatchOperationResult batchOperation(List<String> instanceIds, WorkflowOperation operation, String userId, Map<String, Object> parameters) throws WorkflowException;
    
    /**
     * 获取工作流引擎统计信息
     * 
     * 获取工作流引擎的运行统计信息，用于监控和分析。
     * 
     * @return 统计信息
     */
    WorkflowEngineStatistics getStatistics();
    
    /**
     * 获取活跃的工作流实例数量
     * 
     * 获取当前正在运行或等待的工作流实例数量。
     * 
     * @return 活跃实例数量
     */
    long getActiveInstanceCount();
    
    /**
     * 获取指定工作流定义的实例数量
     * 
     * 获取指定工作流定义创建的实例总数。
     * 
     * @param workflowId 工作流定义ID
     * @return 实例数量
     */
    long getInstanceCount(String workflowId);
    
    /**
     * 清理已完成的工作流实例
     * 
     * 清理指定时间之前完成的工作流实例，释放存储空间。
     * 
     * @param beforeTimestamp 时间戳，清理此时间之前完成的实例
     * @param batchSize 批处理大小
     * @return 清理的实例数量
     */
    int cleanupCompletedInstances(long beforeTimestamp, int batchSize);
    
    /**
     * 导出工作流实例数据
     * 
     * 导出指定工作流实例的完整数据，用于备份或迁移。
     * 
     * @param instanceId 工作流实例ID
     * @return 实例数据的JSON表示
     * @throws WorkflowException 如果导出失败
     */
    String exportWorkflowInstance(String instanceId) throws WorkflowException;
    
    /**
     * 导入工作流实例数据
     * 
     * 从导出的数据中恢复工作流实例。
     * 
     * @param instanceData 实例数据的JSON表示
     * @param userId 操作用户ID
     * @return 导入的工作流实例
     * @throws WorkflowException 如果导入失败
     */
    WorkflowInstance importWorkflowInstance(String instanceData, String userId) throws WorkflowException;
}