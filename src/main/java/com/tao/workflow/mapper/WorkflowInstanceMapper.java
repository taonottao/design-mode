package com.tao.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流实例Mapper接口
 * 提供工作流实例的数据库操作方法
 * 
 * @author tao
 * @since 2024-01-15
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {

    /**
     * 根据工作流ID查询所有实例
     * 
     * @param workflowId 工作流定义ID
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据状态查询工作流实例
     * 
     * @param status 实例状态
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> selectByStatus(@Param("status") String status);

    /**
     * 查询指定用户启动的工作流实例
     * 
     * @param startedBy 启动用户
     * @return 工作流实例列表
     */
    List<WorkflowInstanceEntity> selectByStartedBy(@Param("startedBy") String startedBy);

    /**
     * 查询正在运行的工作流实例
     * 
     * @return 正在运行的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectRunningInstances();

    /**
     * 查询等待中的工作流实例
     * 
     * @return 等待中的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectWaitingInstances();

    /**
     * 查询已完成的工作流实例
     * 
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 已完成的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectCompletedInstances(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询工作流实例
     * 
     * @param page 分页参数
     * @param workflowId 工作流定义ID（可选）
     * @param workflowName 工作流名称（可选，模糊查询）
     * @param status 实例状态（可选）
     * @param startedBy 启动用户（可选）
     * @param startTime 启动开始时间（可选）
     * @param endTime 启动结束时间（可选）
     * @return 分页结果
     */
    IPage<WorkflowInstanceEntity> selectInstancesWithConditions(
            Page<WorkflowInstanceEntity> page,
            @Param("workflowId") String workflowId,
            @Param("workflowName") String workflowName,
            @Param("status") String status,
            @Param("startedBy") String startedBy,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计各状态的工作流实例数量
     * 
     * @return 状态统计结果，key为状态，value为数量
     */
    List<Map<String, Object>> countInstancesByStatus();

    /**
     * 统计指定工作流定义的实例数量
     * 
     * @param workflowId 工作流定义ID
     * @return 实例数量统计，按状态分组
     */
    List<Map<String, Object>> countInstancesByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 查询指定用户启动的实例数量
     * 
     * @param startedBy 启动用户
     * @return 实例数量
     */
    Integer countInstancesByUser(@Param("startedBy") String startedBy);

    /**
     * 查询最近启动的工作流实例
     * 
     * @param limit 限制数量
     * @return 最近启动的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectRecentInstances(@Param("limit") Integer limit);

    /**
     * 查询长时间运行的工作流实例
     * 查询运行时间超过指定小时数的实例
     * 
     * @param hours 运行小时数阈值
     * @return 长时间运行的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectLongRunningInstances(@Param("hours") Integer hours);

    /**
     * 查询超时的工作流实例
     * 查询启动时间超过指定天数且仍在运行的实例
     * 
     * @param days 超时天数
     * @return 超时的工作流实例列表
     */
    List<WorkflowInstanceEntity> selectTimeoutInstances(@Param("days") Integer days);

    /**
     * 批量更新工作流实例状态
     * 
     * @param ids 实例ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    Integer batchUpdateStatus(@Param("ids") List<String> ids, @Param("status") String status);

    /**
     * 更新实例的当前步骤
     * 
     * @param instanceId 实例ID
     * @param currentStepId 当前步骤ID
     * @return 更新的记录数
     */
    Integer updateCurrentStep(@Param("instanceId") String instanceId, 
                             @Param("currentStepId") String currentStepId);

    /**
     * 更新实例的上下文数据
     * 
     * @param instanceId 实例ID
     * @param contextData 上下文数据（JSON格式）
     * @return 更新的记录数
     */
    Integer updateContextData(@Param("instanceId") String instanceId, 
                             @Param("contextData") String contextData);

    /**
     * 完成工作流实例
     * 更新状态为COMPLETED并设置完成时间
     * 
     * @param instanceId 实例ID
     * @param completedAt 完成时间
     * @return 更新的记录数
     */
    Integer completeInstance(@Param("instanceId") String instanceId, 
                            @Param("completedAt") LocalDateTime completedAt);

    /**
     * 终止工作流实例
     * 更新状态为TERMINATED并设置完成时间
     * 
     * @param instanceId 实例ID
     * @param terminatedAt 终止时间
     * @return 更新的记录数
     */
    Integer terminateInstance(@Param("instanceId") String instanceId, 
                             @Param("terminatedAt") LocalDateTime terminatedAt);

    /**
     * 取消工作流实例
     * 更新状态为CANCELLED并设置完成时间
     * 
     * @param instanceId 实例ID
     * @param cancelledAt 取消时间
     * @return 更新的记录数
     */
    Integer cancelInstance(@Param("instanceId") String instanceId, 
                          @Param("cancelledAt") LocalDateTime cancelledAt);

    /**
     * 查询工作流实例的执行统计
     * 包括平均执行时间、成功率等
     * 
     * @param workflowId 工作流定义ID（可选）
     * @param startTime 统计开始时间（可选）
     * @param endTime 统计结束时间（可选）
     * @return 执行统计结果
     */
    Map<String, Object> getInstanceExecutionStats(@Param("workflowId") String workflowId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内的实例创建趋势
     * 按日期分组统计实例创建数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 创建趋势统计，包含日期和数量
     */
    List<Map<String, Object>> getInstanceCreationTrend(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询工作流实例的性能统计
     * 按工作流定义分组，统计平均执行时间、成功率等
     * 
     * @return 性能统计结果
     */
    List<Map<String, Object>> getInstancePerformanceStats();

    /**
     * 查询用户的工作流实例统计
     * 统计每个用户启动的实例数量和状态分布
     * 
     * @return 用户实例统计结果
     */
    List<Map<String, Object>> getUserInstanceStats();

    /**
     * 查询需要清理的历史实例
     * 查询完成时间超过指定天数的已完成实例
     * 
     * @param days 保留天数
     * @return 需要清理的实例列表
     */
    List<WorkflowInstanceEntity> selectInstancesForCleanup(@Param("days") Integer days);

    /**
     * 根据关键词搜索工作流实例
     * 在工作流名称和实例ID中进行搜索
     * 
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索结果列表
     */
    List<WorkflowInstanceEntity> searchInstances(@Param("keyword") String keyword, 
                                                 @Param("limit") Integer limit);

    /**
     * 查询实例的详细信息（包含上下文数据）
     * 
     * @param instanceId 实例ID
     * @return 实例详细信息
     */
    WorkflowInstanceEntity selectInstanceWithDetails(@Param("instanceId") String instanceId);

    /**
     * 查询实例的基本信息（不包含上下文数据）
     * 用于列表查询，提高性能
     * 
     * @param instanceId 实例ID
     * @return 实例基本信息
     */
    WorkflowInstanceEntity selectInstanceBasicInfo(@Param("instanceId") String instanceId);

    /**
     * 检查实例是否可以执行指定操作
     * 根据当前状态判断是否可以暂停、恢复、取消等
     * 
     * @param instanceId 实例ID
     * @param operation 操作类型（SUSPEND、RESUME、CANCEL、TERMINATE）
     * @return 是否可以执行操作
     */
    Boolean canPerformOperation(@Param("instanceId") String instanceId, 
                               @Param("operation") String operation);
}