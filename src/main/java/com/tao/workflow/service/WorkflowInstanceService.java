package com.tao.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.WorkflowInstanceEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流实例服务接口
 * 提供工作流实例的核心业务操作
 * 
 * @author tao
 * @since 2024-01-15
 */
public interface WorkflowInstanceService {

    /**
     * 启动工作流实例
     * 
     * @param workflowId 工作流定义ID
     * @param startedBy 启动者
     * @param variables 初始变量
     * @return 创建的工作流实例
     */
    WorkflowInstanceEntity startWorkflowInstance(String workflowId, String startedBy, Map<String, Object> variables);

    /**
     * 根据ID获取工作流实例
     * 
     * @param instanceId 实例ID
     * @return 工作流实例，如果不存在则返回null
     */
    WorkflowInstanceEntity getWorkflowInstanceById(String instanceId);

    /**
     * 根据工作流ID获取所有实例
     * 
     * @param workflowId 工作流ID
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> getInstancesByWorkflowId(String workflowId);

    /**
     * 根据状态获取工作流实例
     * 
     * @param status 实例状态
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> getInstancesByStatus(WorkflowInstanceEntity.Status status);

    /**
     * 获取用户启动的工作流实例
     * 
     * @param startedBy 启动者
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> getInstancesByStarter(String startedBy);

    /**
     * 获取正在运行的工作流实例
     * 
     * @return 正在运行的工作流实例列表
     */
    List<WorkflowInstanceEntity> getRunningInstances();

    /**
     * 获取等待中的工作流实例
     * 
     * @return 等待中的工作流实例列表
     */
    List<WorkflowInstanceEntity> getWaitingInstances();

    /**
     * 获取已完成的工作流实例
     * 
     * @param limit 限制数量
     * @return 已完成的工作流实例列表
     */
    List<WorkflowInstanceEntity> getCompletedInstances(int limit);

    /**
     * 分页查询工作流实例
     * 
     * @param page 分页参数
     * @param workflowId 工作流ID（可选）
     * @param status 状态（可选）
     * @param startedBy 启动者（可选）
     * @param startDate 开始时间范围（可选）
     * @param endDate 结束时间范围（可选）
     * @return 分页结果
     */
    IPage<WorkflowInstanceEntity> getInstancesWithConditions(
            Page<WorkflowInstanceEntity> page,
            String workflowId,
            WorkflowInstanceEntity.Status status,
            String startedBy,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * 更新工作流实例
     * 
     * @param instance 要更新的工作流实例
     * @return 更新后的工作流实例
     */
    WorkflowInstanceEntity updateWorkflowInstance(WorkflowInstanceEntity instance);

    /**
     * 更新工作流实例的当前步骤
     * 
     * @param instanceId 实例ID
     * @param currentStepId 当前步骤ID
     * @return 是否更新成功
     */
    boolean updateCurrentStep(String instanceId, String currentStepId);

    /**
     * 更新工作流实例的上下文数据
     * 
     * @param instanceId 实例ID
     * @param contextData 上下文数据
     * @return 是否更新成功
     */
    boolean updateContextData(String instanceId, String contextData);

    /**
     * 完成工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否完成成功
     */
    boolean completeWorkflowInstance(String instanceId);

    /**
     * 暂停工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否暂停成功
     */
    boolean suspendWorkflowInstance(String instanceId);

    /**
     * 恢复工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否恢复成功
     */
    boolean resumeWorkflowInstance(String instanceId);

    /**
     * 取消工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否取消成功
     */
    boolean cancelWorkflowInstance(String instanceId);

    /**
     * 终止工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否终止成功
     */
    boolean terminateWorkflowInstance(String instanceId);

    /**
     * 批量更新工作流实例状态
     * 
     * @param instanceIds 实例ID列表
     * @param status 目标状态
     * @return 更新成功的数量
     */
    int batchUpdateStatus(List<String> instanceIds, WorkflowInstanceEntity.Status status);

    /**
     * 根据工作流ID统计实例数量
     * 
     * @param workflowId 工作流ID
     * @return 实例数量
     */
    long countByWorkflowId(String workflowId);

    /**
     * 根据状态统计实例数量
     * 
     * @param status 状态
     * @return 实例数量
     */
    long countByStatus(WorkflowInstanceEntity.Status status);

    /**
     * 根据启动者统计实例数量
     * 
     * @param startedBy 启动者
     * @return 实例数量
     */
    long countByStarter(String startedBy);

    /**
     * 获取最近创建的工作流实例
     * 
     * @param limit 限制数量
     * @return 最近创建的工作流实例列表
     */
    List<WorkflowInstanceEntity> getRecentInstances(int limit);

    /**
     * 获取长时间运行的工作流实例
     * 
     * @param hours 运行时间阈值（小时）
     * @return 长时间运行的工作流实例列表
     */
    List<WorkflowInstanceEntity> getLongRunningInstances(int hours);

    /**
     * 获取超时的工作流实例
     * 
     * @param timeoutMinutes 超时阈值（分钟）
     * @return 超时的工作流实例列表
     */
    List<WorkflowInstanceEntity> getTimeoutInstances(int timeoutMinutes);

    /**
     * 根据关键字搜索工作流实例
     * 
     * @param keyword 搜索关键字
     * @param page 分页参数
     * @return 搜索结果
     */
    IPage<WorkflowInstanceEntity> searchInstances(String keyword, Page<WorkflowInstanceEntity> page);

    /**
     * 获取工作流实例的执行统计信息
     * 
     * @return 执行统计信息
     */
    Map<String, Object> getExecutionStatistics();

    /**
     * 获取工作流实例的创建趋势
     * 
     * @param days 统计天数
     * @return 创建趋势数据
     */
    List<Map<String, Object>> getCreationTrend(int days);

    /**
     * 获取工作流实例的性能统计
     * 
     * @return 性能统计数据
     */
    Map<String, Object> getPerformanceStatistics();

    /**
     * 获取用户的工作流实例统计
     * 
     * @param startedBy 启动者
     * @return 用户实例统计
     */
    Map<String, Object> getUserInstanceStatistics(String startedBy);

    /**
     * 清理旧的工作流实例
     * 
     * @param days 保留天数
     * @return 清理的实例数量
     */
    int cleanupOldInstances(int days);

    /**
     * 检查实例是否可以执行指定操作
     * 
     * @param instanceId 实例ID
     * @param operation 操作类型
     * @return 是否可以执行
     */
    boolean canPerformOperation(String instanceId, String operation);

    /**
     * 获取工作流实例的执行历史
     * 
     * @param instanceId 实例ID
     * @return 执行历史列表
     */
    List<Map<String, Object>> getInstanceExecutionHistory(String instanceId);

    /**
     * 获取工作流实例的变量
     * 
     * @param instanceId 实例ID
     * @return 实例变量映射
     */
    Map<String, Object> getInstanceVariables(String instanceId);

    /**
     * 设置工作流实例的变量
     * 
     * @param instanceId 实例ID
     * @param variables 要设置的变量
     * @return 是否设置成功
     */
    boolean setInstanceVariables(String instanceId, Map<String, Object> variables);

    /**
     * 获取工作流实例的当前任务
     * 
     * @param instanceId 实例ID
     * @return 当前任务列表
     */
    List<Map<String, Object>> getCurrentTasks(String instanceId);

    /**
     * 重启失败的工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否重启成功
     */
    boolean restartFailedInstance(String instanceId);

    /**
     * 跳过当前步骤
     * 
     * @param instanceId 实例ID
     * @param reason 跳过原因
     * @return 是否跳过成功
     */
    boolean skipCurrentStep(String instanceId, String reason);

    /**
     * 回退到指定步骤
     * 
     * @param instanceId 实例ID
     * @param targetStepId 目标步骤ID
     * @param reason 回退原因
     * @return 是否回退成功
     */
    boolean rollbackToStep(String instanceId, String targetStepId, String reason);
}