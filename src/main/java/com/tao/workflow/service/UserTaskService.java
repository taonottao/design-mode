package com.tao.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.UserTaskEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户任务服务接口
 * 提供用户任务的核心业务操作
 * 
 * @author tao
 * @since 2024-01-15
 */
public interface UserTaskService {

    /**
     * 创建用户任务
     * 
     * @param userTask 用户任务实体
     * @return 创建成功的用户任务
     */
    UserTaskEntity createUserTask(UserTaskEntity userTask);

    /**
     * 根据ID获取用户任务
     * 
     * @param taskId 任务ID
     * @return 用户任务，如果不存在则返回null
     */
    UserTaskEntity getUserTaskById(String taskId);

    /**
     * 根据实例ID获取用户任务
     * 
     * @param instanceId 实例ID
     * @return 用户任务列表
     */
    List<UserTaskEntity> getUserTasksByInstanceId(String instanceId);

    /**
     * 根据步骤ID获取用户任务
     * 
     * @param stepId 步骤ID
     * @return 用户任务列表
     */
    List<UserTaskEntity> getUserTasksByStepId(String stepId);

    /**
     * 获取用户的待办任务
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    List<UserTaskEntity> getPendingTasksByUser(String userId);

    /**
     * 获取用户组的待办任务
     * 
     * @param groupId 用户组ID
     * @return 待办任务列表
     */
    List<UserTaskEntity> getPendingTasksByGroup(String groupId);

    /**
     * 获取用户的已完成任务
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 已完成任务列表
     */
    List<UserTaskEntity> getCompletedTasksByUser(String userId, int limit);

    /**
     * 获取逾期的任务
     * 
     * @return 逾期任务列表
     */
    List<UserTaskEntity> getOverdueTasks();

    /**
     * 获取即将到期的任务
     * 
     * @param hours 小时数阈值
     * @return 即将到期的任务列表
     */
    List<UserTaskEntity> getTasksDueSoon(int hours);

    /**
     * 分页查询用户任务
     * 
     * @param page 分页参数
     * @param instanceId 实例ID（可选）
     * @param assignee 指派人（可选）
     * @param status 状态（可选）
     * @param priority 优先级（可选）
     * @param startDate 创建开始时间（可选）
     * @param endDate 创建结束时间（可选）
     * @return 分页结果
     */
    IPage<UserTaskEntity> getUserTasksWithConditions(
            Page<UserTaskEntity> page,
            String instanceId,
            String assignee,
            UserTaskEntity.Status status,
            UserTaskEntity.Priority priority,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * 更新用户任务
     * 
     * @param userTask 要更新的用户任务
     * @return 更新后的用户任务
     */
    UserTaskEntity updateUserTask(UserTaskEntity userTask);

    /**
     * 完成用户任务
     * 
     * @param taskId 任务ID
     * @param userId 完成者ID
     * @param formData 表单数据
     * @return 是否完成成功
     */
    boolean completeUserTask(String taskId, String userId, String formData);

    /**
     * 取消用户任务
     * 
     * @param taskId 任务ID
     * @param reason 取消原因
     * @return 是否取消成功
     */
    boolean cancelUserTask(String taskId, String reason);

    /**
     * 委派用户任务
     * 
     * @param taskId 任务ID
     * @param fromUserId 委派人ID
     * @param toUserId 被委派人ID
     * @param reason 委派原因
     * @return 是否委派成功
     */
    boolean delegateUserTask(String taskId, String fromUserId, String toUserId, String reason);

    /**
     * 收回委派的任务
     * 
     * @param taskId 任务ID
     * @param userId 收回人ID
     * @return 是否收回成功
     */
    boolean reclaimUserTask(String taskId, String userId);

    /**
     * 更新任务到期时间
     * 
     * @param taskId 任务ID
     * @param dueDate 新的到期时间
     * @return 是否更新成功
     */
    boolean updateTaskDueDate(String taskId, LocalDateTime dueDate);

    /**
     * 更新任务优先级
     * 
     * @param taskId 任务ID
     * @param priority 新的优先级
     * @return 是否更新成功
     */
    boolean updateTaskPriority(String taskId, UserTaskEntity.Priority priority);

    /**
     * 更新任务表单数据
     * 
     * @param taskId 任务ID
     * @param formData 表单数据
     * @return 是否更新成功
     */
    boolean updateTaskFormData(String taskId, String formData);

    /**
     * 批量更新任务状态
     * 
     * @param taskIds 任务ID列表
     * @param status 目标状态
     * @return 更新成功的数量
     */
    int batchUpdateTaskStatus(List<String> taskIds, UserTaskEntity.Status status);

    /**
     * 根据状态统计任务数量
     * 
     * @param status 状态
     * @return 任务数量
     */
    long countByStatus(UserTaskEntity.Status status);

    /**
     * 根据优先级统计任务数量
     * 
     * @param priority 优先级
     * @return 任务数量
     */
    long countByPriority(UserTaskEntity.Priority priority);

    /**
     * 获取用户任务统计信息
     * 
     * @param userId 用户ID
     * @return 用户任务统计
     */
    Map<String, Object> getUserTaskStatistics(String userId);

    /**
     * 获取最近创建的任务
     * 
     * @param limit 限制数量
     * @return 最近创建的任务列表
     */
    List<UserTaskEntity> getRecentTasks(int limit);

    /**
     * 获取高优先级的待办任务
     * 
     * @param limit 限制数量
     * @return 高优先级待办任务列表
     */
    List<UserTaskEntity> getHighPriorityPendingTasks(int limit);

    /**
     * 根据关键字搜索用户任务
     * 
     * @param keyword 搜索关键字
     * @param page 分页参数
     * @return 搜索结果
     */
    IPage<UserTaskEntity> searchUserTasks(String keyword, Page<UserTaskEntity> page);

    /**
     * 获取任务处理时间统计
     * 
     * @return 处理时间统计数据
     */
    List<Map<String, Object>> getTaskProcessingTimeStatistics();

    /**
     * 获取任务创建趋势
     * 
     * @param days 统计天数
     * @return 创建趋势数据
     */
    List<Map<String, Object>> getTaskCreationTrend(int days);

    /**
     * 获取用户效率统计
     * 
     * @param userId 用户ID
     * @return 用户效率统计
     */
    Map<String, Object> getUserEfficiencyStatistics(String userId);

    /**
     * 获取任务类型分布统计
     * 
     * @return 任务类型分布数据
     */
    List<Map<String, Object>> getTaskTypeDistribution();

    /**
     * 获取需要提醒的任务
     * 
     * @param hours 提前提醒小时数
     * @return 需要提醒的任务列表
     */
    List<UserTaskEntity> getTasksForReminder(int hours);

    /**
     * 清理已完成的旧任务
     * 
     * @param days 保留天数
     * @return 清理的任务数量
     */
    int cleanupCompletedTasks(int days);

    /**
     * 检查用户是否可以处理指定任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否可以处理
     */
    boolean canUserHandleTask(String taskId, String userId);

    /**
     * 获取任务的委派历史
     * 
     * @param taskId 任务ID
     * @return 委派历史列表
     */
    List<Map<String, Object>> getTaskDelegationHistory(String taskId);

    /**
     * 获取用户的活跃任务数量
     * 
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    long getActiveTaskCountByUser(String userId);

    /**
     * 分配任务给用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否分配成功
     */
    boolean assignTaskToUser(String taskId, String userId);

    /**
     * 从候选用户中认领任务
     * 
     * @param taskId 任务ID
     * @param userId 认领用户ID
     * @return 是否认领成功
     */
    boolean claimTask(String taskId, String userId);

    /**
     * 释放任务（取消分配）
     * 
     * @param taskId 任务ID
     * @return 是否释放成功
     */
    boolean releaseTask(String taskId);

    /**
     * 获取任务的表单数据
     * 
     * @param taskId 任务ID
     * @return 表单数据映射
     */
    Map<String, Object> getTaskFormData(String taskId);

    /**
     * 设置任务的表单数据
     * 
     * @param taskId 任务ID
     * @param formData 表单数据
     * @return 是否设置成功
     */
    boolean setTaskFormData(String taskId, Map<String, Object> formData);

    /**
     * 获取任务的候选用户列表
     * 
     * @param taskId 任务ID
     * @return 候选用户ID列表
     */
    List<String> getTaskCandidateUsers(String taskId);

    /**
     * 获取任务的候选用户组列表
     * 
     * @param taskId 任务ID
     * @return 候选用户组ID列表
     */
    List<String> getTaskCandidateGroups(String taskId);

    /**
     * 添加任务候选用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否添加成功
     */
    boolean addTaskCandidateUser(String taskId, String userId);

    /**
     * 移除任务候选用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否移除成功
     */
    boolean removeTaskCandidateUser(String taskId, String userId);

    /**
     * 添加任务候选用户组
     * 
     * @param taskId 任务ID
     * @param groupId 用户组ID
     * @return 是否添加成功
     */
    boolean addTaskCandidateGroup(String taskId, String groupId);

    /**
     * 移除任务候选用户组
     * 
     * @param taskId 任务ID
     * @param groupId 用户组ID
     * @return 是否移除成功
     */
    boolean removeTaskCandidateGroup(String taskId, String groupId);
}