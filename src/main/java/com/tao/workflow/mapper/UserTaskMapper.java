package com.tao.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.UserTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户任务Mapper接口
 * 提供用户任务的数据库操作方法
 * 
 * @author tao
 * @since 2024-01-15
 */
@Mapper
public interface UserTaskMapper extends BaseMapper<UserTaskEntity> {

    /**
     * 根据工作流实例ID查询用户任务
     * 
     * @param instanceId 工作流实例ID
     * @return 用户任务列表
     */
    List<UserTaskEntity> selectByInstanceId(@Param("instanceId") String instanceId);

    /**
     * 根据步骤ID查询用户任务
     * 
     * @param stepId 步骤ID
     * @return 用户任务列表
     */
    List<UserTaskEntity> selectByStepId(@Param("stepId") String stepId);

    /**
     * 查询指定用户的待办任务
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    List<UserTaskEntity> selectPendingTasksByUser(@Param("userId") String userId);

    /**
     * 查询指定用户的已完成任务
     * 
     * @param userId 用户ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 已完成任务列表
     */
    List<UserTaskEntity> selectCompletedTasksByUser(@Param("userId") String userId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定用户组的待办任务
     * 
     * @param groupId 用户组ID
     * @return 待办任务列表
     */
    List<UserTaskEntity> selectPendingTasksByGroup(@Param("groupId") String groupId);

    /**
     * 查询所有待办任务
     * 
     * @return 待办任务列表
     */
    List<UserTaskEntity> selectAllPendingTasks();

    /**
     * 查询已超期的任务
     * 
     * @return 已超期的任务列表
     */
    List<UserTaskEntity> selectOverdueTasks();

    /**
     * 查询即将到期的任务
     * 
     * @param hours 到期前小时数
     * @return 即将到期的任务列表
     */
    List<UserTaskEntity> selectTasksDueSoon(@Param("hours") Integer hours);

    /**
     * 分页查询用户任务
     * 
     * @param page 分页参数
     * @param instanceId 工作流实例ID（可选）
     * @param taskName 任务名称（可选，模糊查询）
     * @param assignee 指派人（可选）
     * @param status 任务状态（可选）
     * @param priority 任务优先级（可选）
     * @param startTime 创建开始时间（可选）
     * @param endTime 创建结束时间（可选）
     * @return 分页结果
     */
    IPage<UserTaskEntity> selectTasksWithConditions(
            Page<UserTaskEntity> page,
            @Param("instanceId") String instanceId,
            @Param("taskName") String taskName,
            @Param("assignee") String assignee,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计各状态的用户任务数量
     * 
     * @return 状态统计结果，key为状态，value为数量
     */
    List<Map<String, Object>> countTasksByStatus();

    /**
     * 统计各优先级的用户任务数量
     * 
     * @return 优先级统计结果，key为优先级，value为数量
     */
    List<Map<String, Object>> countTasksByPriority();

    /**
     * 查询指定用户的任务统计
     * 
     * @param userId 用户ID
     * @return 任务统计结果
     */
    Map<String, Object> getUserTaskStats(@Param("userId") String userId);

    /**
     * 查询最近创建的用户任务
     * 
     * @param limit 限制数量
     * @return 最近创建的用户任务列表
     */
    List<UserTaskEntity> selectRecentTasks(@Param("limit") Integer limit);

    /**
     * 查询高优先级的待办任务
     * 
     * @return 高优先级的待办任务列表
     */
    List<UserTaskEntity> selectHighPriorityPendingTasks();

    /**
     * 批量更新任务状态
     * 
     * @param taskIds 任务ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    Integer batchUpdateTaskStatus(@Param("taskIds") List<String> taskIds, 
                                 @Param("status") String status);

    /**
     * 完成用户任务
     * 
     * @param taskId 任务ID
     * @param completedBy 完成人
     * @param completedAt 完成时间
     * @param formData 表单数据（可选）
     * @return 更新的记录数
     */
    Integer completeTask(@Param("taskId") String taskId,
                        @Param("completedBy") String completedBy,
                        @Param("completedAt") LocalDateTime completedAt,
                        @Param("formData") String formData);

    /**
     * 取消用户任务
     * 
     * @param taskId 任务ID
     * @param cancelledBy 取消人
     * @param cancelledAt 取消时间
     * @return 更新的记录数
     */
    Integer cancelTask(@Param("taskId") String taskId,
                      @Param("cancelledBy") String cancelledBy,
                      @Param("cancelledAt") LocalDateTime cancelledAt);

    /**
     * 委派用户任务
     * 
     * @param taskId 任务ID
     * @param newAssignee 新指派人
     * @param delegatedBy 委派人
     * @param delegatedAt 委派时间
     * @return 更新的记录数
     */
    Integer delegateTask(@Param("taskId") String taskId,
                        @Param("newAssignee") String newAssignee,
                        @Param("delegatedBy") String delegatedBy,
                        @Param("delegatedAt") LocalDateTime delegatedAt);

    /**
     * 收回委派的任务
     * 
     * @param taskId 任务ID
     * @param reclaimedBy 收回人
     * @param reclaimedAt 收回时间
     * @return 更新的记录数
     */
    Integer reclaimTask(@Param("taskId") String taskId,
                       @Param("reclaimedBy") String reclaimedBy,
                       @Param("reclaimedAt") LocalDateTime reclaimedAt);

    /**
     * 更新任务的到期时间
     * 
     * @param taskId 任务ID
     * @param dueDate 新的到期时间
     * @return 更新的记录数
     */
    Integer updateTaskDueDate(@Param("taskId") String taskId, 
                             @Param("dueDate") LocalDateTime dueDate);

    /**
     * 更新任务的优先级
     * 
     * @param taskId 任务ID
     * @param priority 新的优先级
     * @return 更新的记录数
     */
    Integer updateTaskPriority(@Param("taskId") String taskId, 
                              @Param("priority") String priority);

    /**
     * 更新任务的表单数据
     * 
     * @param taskId 任务ID
     * @param formData 表单数据
     * @return 更新的记录数
     */
    Integer updateTaskFormData(@Param("taskId") String taskId, 
                              @Param("formData") String formData);

    /**
     * 查询用户任务的处理时间统计
     * 
     * @param startTime 统计开始时间（可选）
     * @param endTime 统计结束时间（可选）
     * @return 处理时间统计结果
     */
    Map<String, Object> getTaskProcessingTimeStats(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内的任务创建趋势
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 创建趋势统计
     */
    List<Map<String, Object>> getTaskCreationTrend(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户的任务处理效率统计
     * 
     * @return 用户效率统计结果
     */
    List<Map<String, Object>> getUserTaskEfficiencyStats();

    /**
     * 查询任务类型的分布统计
     * 
     * @return 任务类型分布统计
     */
    List<Map<String, Object>> getTaskTypeDistribution();

    /**
     * 根据关键词搜索用户任务
     * 
     * @param keyword 搜索关键词
     * @param userId 用户ID（可选，限制搜索范围）
     * @param limit 限制数量
     * @return 搜索结果列表
     */
    List<UserTaskEntity> searchTasks(@Param("keyword") String keyword,
                                    @Param("userId") String userId,
                                    @Param("limit") Integer limit);

    /**
     * 查询需要提醒的任务
     * 查询即将到期但未完成的任务
     * 
     * @param reminderHours 提醒提前小时数
     * @return 需要提醒的任务列表
     */
    List<UserTaskEntity> selectTasksForReminder(@Param("reminderHours") Integer reminderHours);

    /**
     * 查询长时间未处理的任务
     * 
     * @param days 未处理天数阈值
     * @return 长时间未处理的任务列表
     */
    List<UserTaskEntity> selectLongPendingTasks(@Param("days") Integer days);

    /**
     * 查询用户可以处理的任务
     * 包括直接指派给用户的任务和用户组的任务
     * 
     * @param userId 用户ID
     * @param userGroups 用户所属组列表
     * @return 用户可处理的任务列表
     */
    List<UserTaskEntity> selectTasksForUser(@Param("userId") String userId,
                                           @Param("userGroups") List<String> userGroups);

    /**
     * 检查用户是否可以处理指定任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param userGroups 用户所属组列表
     * @return 是否可以处理
     */
    Boolean canUserHandleTask(@Param("taskId") String taskId,
                             @Param("userId") String userId,
                             @Param("userGroups") List<String> userGroups);

    /**
     * 查询任务的委派历史
     * 
     * @param taskId 任务ID
     * @return 委派历史信息
     */
    List<Map<String, Object>> getTaskDelegationHistory(@Param("taskId") String taskId);

    /**
     * 查询需要清理的历史任务
     * 
     * @param days 保留天数
     * @return 需要清理的任务列表
     */
    List<UserTaskEntity> selectTasksForCleanup(@Param("days") Integer days);

    /**
     * 查询工作流实例的所有用户任务（包括历史任务）
     * 
     * @param instanceId 工作流实例ID
     * @return 实例的所有用户任务
     */
    List<UserTaskEntity> selectAllTasksByInstance(@Param("instanceId") String instanceId);

    /**
     * 查询活跃的用户任务数量
     * 不包括已完成和已取消的任务
     * 
     * @return 活跃任务数量
     */
    Integer countActiveTasks();

    /**
     * 查询指定用户的活跃任务数量
     * 
     * @param userId 用户ID
     * @return 用户的活跃任务数量
     */
    Integer countActiveTasksByUser(@Param("userId") String userId);
}