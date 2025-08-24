package com.tao.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tao.workflow.entity.UserTaskEntity;
import com.tao.workflow.mapper.UserTaskMapper;
import com.tao.workflow.service.UserTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户任务服务实现类
 * 提供用户任务的具体业务逻辑实现
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTaskServiceImpl extends ServiceImpl<UserTaskMapper, UserTaskEntity> implements UserTaskService {

    private final UserTaskMapper userTaskMapper;

    /**
     * 创建用户任务
     * 
     * @param userTask 用户任务实体
     * @return 创建成功的用户任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskEntity createUserTask(UserTaskEntity userTask) {
        log.info("创建用户任务: {}", userTask.getTaskName());
        
        // 设置默认值
        if (userTask.getStatus() == null) {
            userTask.setStatus(UserTaskEntity.Status.PENDING);
        }
        if (userTask.getPriority() == null) {
            userTask.setPriority(UserTaskEntity.Priority.MEDIUM);
        }
        if (userTask.getCreatedTime() == null) {
            userTask.setCreatedTime(LocalDateTime.now());
        }
        
        // 保存任务
        save(userTask);
        
        log.info("用户任务创建成功，任务ID: {}", userTask.getTaskId());
        return userTask;
    }

    /**
     * 根据ID获取用户任务
     * 
     * @param taskId 任务ID
     * @return 用户任务，如果不存在则返回null
     */
    @Override
    public UserTaskEntity getUserTaskById(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return getById(taskId);
    }

    /**
     * 根据实例ID获取用户任务
     * 
     * @param instanceId 实例ID
     * @return 用户任务列表
     */
    @Override
    public List<UserTaskEntity> getUserTasksByInstanceId(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectByInstanceId(instanceId);
    }

    /**
     * 根据步骤ID获取用户任务
     * 
     * @param stepId 步骤ID
     * @return 用户任务列表
     */
    @Override
    public List<UserTaskEntity> getUserTasksByStepId(String stepId) {
        if (StrUtil.isBlank(stepId)) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectByStepId(stepId);
    }

    /**
     * 获取用户的待办任务
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    @Override
    public List<UserTaskEntity> getPendingTasksByUser(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectPendingTasksByUser(userId);
    }

    /**
     * 获取用户组的待办任务
     * 
     * @param groupId 用户组ID
     * @return 待办任务列表
     */
    @Override
    public List<UserTaskEntity> getPendingTasksByGroup(String groupId) {
        if (StrUtil.isBlank(groupId)) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectPendingTasksByGroup(groupId);
    }

    /**
     * 获取用户的已完成任务
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 已完成任务列表
     */
    @Override
    public List<UserTaskEntity> getCompletedTasksByUser(String userId, int limit) {
        if (StrUtil.isBlank(userId) || limit <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectCompletedTasksByUser(userId, limit);
    }

    /**
     * 获取逾期的任务
     * 
     * @return 逾期任务列表
     */
    @Override
    public List<UserTaskEntity> getOverdueTasks() {
        return userTaskMapper.selectOverdueTasks();
    }

    /**
     * 获取即将到期的任务
     * 
     * @param hours 小时数阈值
     * @return 即将到期的任务列表
     */
    @Override
    public List<UserTaskEntity> getTasksDueSoon(int hours) {
        if (hours <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectTasksDueSoon(hours);
    }

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
    @Override
    public IPage<UserTaskEntity> getUserTasksWithConditions(
            Page<UserTaskEntity> page,
            String instanceId,
            String assignee,
            UserTaskEntity.Status status,
            UserTaskEntity.Priority priority,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        return userTaskMapper.selectUserTasksWithConditions(
                page, instanceId, assignee, status, priority, startDate, endDate);
    }

    /**
     * 更新用户任务
     * 
     * @param userTask 要更新的用户任务
     * @return 更新后的用户任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskEntity updateUserTask(UserTaskEntity userTask) {
        if (userTask == null || StrUtil.isBlank(userTask.getTaskId())) {
            throw new IllegalArgumentException("用户任务或任务ID不能为空");
        }
        
        log.info("更新用户任务: {}", userTask.getTaskId());
        
        // 设置更新时间
        userTask.setUpdatedTime(LocalDateTime.now());
        
        // 更新任务
        updateById(userTask);
        
        log.info("用户任务更新成功: {}", userTask.getTaskId());
        return userTask;
    }

    /**
     * 完成用户任务
     * 
     * @param taskId 任务ID
     * @param userId 完成者ID
     * @param formData 表单数据
     * @return 是否完成成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeUserTask(String taskId, String userId, String formData) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        log.info("完成用户任务: {}, 完成者: {}", taskId, userId);
        
        // 检查任务是否存在且可以完成
        UserTaskEntity task = getById(taskId);
        if (task == null || task.getStatus() != UserTaskEntity.Status.PENDING) {
            log.warn("任务不存在或状态不正确: {}", taskId);
            return false;
        }
        
        // 检查用户是否有权限完成任务
        if (!canUserHandleTask(taskId, userId)) {
            log.warn("用户无权限完成任务: {}, 用户: {}", taskId, userId);
            return false;
        }
        
        // 更新任务状态
        int result = userTaskMapper.completeTask(taskId, userId, formData);
        
        if (result > 0) {
            log.info("用户任务完成成功: {}", taskId);
            return true;
        } else {
            log.warn("用户任务完成失败: {}", taskId);
            return false;
        }
    }

    /**
     * 取消用户任务
     * 
     * @param taskId 任务ID
     * @param reason 取消原因
     * @return 是否取消成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelUserTask(String taskId, String reason) {
        if (StrUtil.isBlank(taskId)) {
            return false;
        }
        
        log.info("取消用户任务: {}, 原因: {}", taskId, reason);
        
        int result = userTaskMapper.cancelTask(taskId, reason);
        
        if (result > 0) {
            log.info("用户任务取消成功: {}", taskId);
            return true;
        } else {
            log.warn("用户任务取消失败: {}", taskId);
            return false;
        }
    }

    /**
     * 委派用户任务
     * 
     * @param taskId 任务ID
     * @param fromUserId 委派人ID
     * @param toUserId 被委派人ID
     * @param reason 委派原因
     * @return 是否委派成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delegateUserTask(String taskId, String fromUserId, String toUserId, String reason) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(fromUserId) || StrUtil.isBlank(toUserId)) {
            return false;
        }
        
        log.info("委派用户任务: {}, 从 {} 到 {}, 原因: {}", taskId, fromUserId, toUserId, reason);
        
        // 检查委派人是否有权限
        if (!canUserHandleTask(taskId, fromUserId)) {
            log.warn("用户无权限委派任务: {}, 用户: {}", taskId, fromUserId);
            return false;
        }
        
        int result = userTaskMapper.delegateTask(taskId, fromUserId, toUserId, reason);
        
        if (result > 0) {
            log.info("用户任务委派成功: {}", taskId);
            return true;
        } else {
            log.warn("用户任务委派失败: {}", taskId);
            return false;
        }
    }

    /**
     * 收回委派的任务
     * 
     * @param taskId 任务ID
     * @param userId 收回人ID
     * @return 是否收回成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reclaimUserTask(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        log.info("收回委派任务: {}, 收回人: {}", taskId, userId);
        
        int result = userTaskMapper.reclaimTask(taskId, userId);
        
        if (result > 0) {
            log.info("委派任务收回成功: {}", taskId);
            return true;
        } else {
            log.warn("委派任务收回失败: {}", taskId);
            return false;
        }
    }

    /**
     * 更新任务到期时间
     * 
     * @param taskId 任务ID
     * @param dueDate 新的到期时间
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskDueDate(String taskId, LocalDateTime dueDate) {
        if (StrUtil.isBlank(taskId)) {
            return false;
        }
        
        log.info("更新任务到期时间: {}, 新到期时间: {}", taskId, dueDate);
        
        int result = userTaskMapper.updateTaskDueDate(taskId, dueDate);
        
        if (result > 0) {
            log.info("任务到期时间更新成功: {}", taskId);
            return true;
        } else {
            log.warn("任务到期时间更新失败: {}", taskId);
            return false;
        }
    }

    /**
     * 更新任务优先级
     * 
     * @param taskId 任务ID
     * @param priority 新的优先级
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskPriority(String taskId, UserTaskEntity.Priority priority) {
        if (StrUtil.isBlank(taskId) || priority == null) {
            return false;
        }
        
        log.info("更新任务优先级: {}, 新优先级: {}", taskId, priority);
        
        int result = userTaskMapper.updateTaskPriority(taskId, priority);
        
        if (result > 0) {
            log.info("任务优先级更新成功: {}", taskId);
            return true;
        } else {
            log.warn("任务优先级更新失败: {}", taskId);
            return false;
        }
    }

    /**
     * 更新任务表单数据
     * 
     * @param taskId 任务ID
     * @param formData 表单数据
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskFormData(String taskId, String formData) {
        if (StrUtil.isBlank(taskId)) {
            return false;
        }
        
        log.info("更新任务表单数据: {}", taskId);
        
        int result = userTaskMapper.updateTaskFormData(taskId, formData);
        
        if (result > 0) {
            log.info("任务表单数据更新成功: {}", taskId);
            return true;
        } else {
            log.warn("任务表单数据更新失败: {}", taskId);
            return false;
        }
    }

    /**
     * 批量更新任务状态
     * 
     * @param taskIds 任务ID列表
     * @param status 目标状态
     * @return 更新成功的数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateTaskStatus(List<String> taskIds, UserTaskEntity.Status status) {
        if (CollUtil.isEmpty(taskIds) || status == null) {
            return 0;
        }
        
        log.info("批量更新任务状态: {} 个任务, 目标状态: {}", taskIds.size(), status);
        
        int result = userTaskMapper.batchUpdateTaskStatus(taskIds, status);
        
        log.info("批量更新任务状态完成，成功更新: {} 个任务", result);
        return result;
    }

    /**
     * 根据状态统计任务数量
     * 
     * @param status 状态
     * @return 任务数量
     */
    @Override
    public long countByStatus(UserTaskEntity.Status status) {
        if (status == null) {
            return 0;
        }
        return userTaskMapper.countByStatus(status);
    }

    /**
     * 根据优先级统计任务数量
     * 
     * @param priority 优先级
     * @return 任务数量
     */
    @Override
    public long countByPriority(UserTaskEntity.Priority priority) {
        if (priority == null) {
            return 0;
        }
        return userTaskMapper.countByPriority(priority);
    }

    /**
     * 获取用户任务统计信息
     * 
     * @param userId 用户ID
     * @return 用户任务统计
     */
    @Override
    public Map<String, Object> getUserTaskStatistics(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Collections.emptyMap();
        }
        return userTaskMapper.getUserTaskStatistics(userId);
    }

    /**
     * 获取最近创建的任务
     * 
     * @param limit 限制数量
     * @return 最近创建的任务列表
     */
    @Override
    public List<UserTaskEntity> getRecentTasks(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectRecentTasks(limit);
    }

    /**
     * 获取高优先级的待办任务
     * 
     * @param limit 限制数量
     * @return 高优先级待办任务列表
     */
    @Override
    public List<UserTaskEntity> getHighPriorityPendingTasks(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectHighPriorityPendingTasks(limit);
    }

    /**
     * 根据关键字搜索用户任务
     * 
     * @param keyword 搜索关键字
     * @param page 分页参数
     * @return 搜索结果
     */
    @Override
    public IPage<UserTaskEntity> searchUserTasks(String keyword, Page<UserTaskEntity> page) {
        if (StrUtil.isBlank(keyword)) {
            return page(page);
        }
        return userTaskMapper.searchUserTasks(keyword, page);
    }

    /**
     * 获取任务处理时间统计
     * 
     * @return 处理时间统计数据
     */
    @Override
    public List<Map<String, Object>> getTaskProcessingTimeStatistics() {
        return userTaskMapper.getTaskProcessingTimeStatistics();
    }

    /**
     * 获取任务创建趋势
     * 
     * @param days 统计天数
     * @return 创建趋势数据
     */
    @Override
    public List<Map<String, Object>> getTaskCreationTrend(int days) {
        if (days <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.getTaskCreationTrend(days);
    }

    /**
     * 获取用户效率统计
     * 
     * @param userId 用户ID
     * @return 用户效率统计
     */
    @Override
    public Map<String, Object> getUserEfficiencyStatistics(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Collections.emptyMap();
        }
        return userTaskMapper.getUserEfficiencyStatistics(userId);
    }

    /**
     * 获取任务类型分布统计
     * 
     * @return 任务类型分布数据
     */
    @Override
    public List<Map<String, Object>> getTaskTypeDistribution() {
        return userTaskMapper.getTaskTypeDistribution();
    }

    /**
     * 获取需要提醒的任务
     * 
     * @param hours 提前提醒小时数
     * @return 需要提醒的任务列表
     */
    @Override
    public List<UserTaskEntity> getTasksForReminder(int hours) {
        if (hours <= 0) {
            return Collections.emptyList();
        }
        return userTaskMapper.selectTasksForReminder(hours);
    }

    /**
     * 清理已完成的旧任务
     * 
     * @param days 保留天数
     * @return 清理的任务数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupCompletedTasks(int days) {
        if (days <= 0) {
            return 0;
        }
        
        log.info("清理 {} 天前的已完成任务", days);
        
        int result = userTaskMapper.cleanupCompletedTasks(days);
        
        log.info("清理已完成任务完成，清理数量: {}", result);
        return result;
    }

    /**
     * 检查用户是否可以处理指定任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否可以处理
     */
    @Override
    public boolean canUserHandleTask(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        Integer count = userTaskMapper.canUserHandleTask(taskId, userId);
        return count != null && count > 0;
    }

    /**
     * 获取任务的委派历史
     * 
     * @param taskId 任务ID
     * @return 委派历史列表
     */
    @Override
    public List<Map<String, Object>> getTaskDelegationHistory(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return Collections.emptyList();
        }
        return userTaskMapper.getTaskDelegationHistory(taskId);
    }

    /**
     * 获取用户的活跃任务数量
     * 
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    @Override
    public long getActiveTaskCountByUser(String userId) {
        if (StrUtil.isBlank(userId)) {
            return 0;
        }
        return userTaskMapper.getActiveTaskCountByUser(userId);
    }

    /**
     * 分配任务给用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否分配成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignTaskToUser(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        log.info("分配任务给用户: {}, 用户: {}", taskId, userId);
        
        // 检查任务是否存在且未分配
        UserTaskEntity task = getById(taskId);
        if (task == null || StrUtil.isNotBlank(task.getAssignee())) {
            log.warn("任务不存在或已分配: {}", taskId);
            return false;
        }
        
        // 更新任务分配
        task.setAssignee(userId);
        task.setUpdatedTime(LocalDateTime.now());
        
        boolean result = updateById(task);
        
        if (result) {
            log.info("任务分配成功: {}", taskId);
        } else {
            log.warn("任务分配失败: {}", taskId);
        }
        
        return result;
    }

    /**
     * 从候选用户中认领任务
     * 
     * @param taskId 任务ID
     * @param userId 认领用户ID
     * @return 是否认领成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimTask(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        log.info("用户认领任务: {}, 用户: {}", taskId, userId);
        
        // 检查用户是否在候选用户列表中
        List<String> candidateUsers = getTaskCandidateUsers(taskId);
        if (!candidateUsers.contains(userId)) {
            log.warn("用户不在候选用户列表中: {}, 用户: {}", taskId, userId);
            return false;
        }
        
        // 分配任务给用户
        return assignTaskToUser(taskId, userId);
    }

    /**
     * 释放任务（取消分配）
     * 
     * @param taskId 任务ID
     * @return 是否释放成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return false;
        }
        
        log.info("释放任务: {}", taskId);
        
        // 检查任务是否存在
        UserTaskEntity task = getById(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        // 清除任务分配
        task.setAssignee(null);
        task.setUpdatedTime(LocalDateTime.now());
        
        boolean result = updateById(task);
        
        if (result) {
            log.info("任务释放成功: {}", taskId);
        } else {
            log.warn("任务释放失败: {}", taskId);
        }
        
        return result;
    }

    /**
     * 获取任务的表单数据
     * 
     * @param taskId 任务ID
     * @return 表单数据映射
     */
    @Override
    public Map<String, Object> getTaskFormData(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return Collections.emptyMap();
        }
        
        UserTaskEntity task = getById(taskId);
        if (task == null || StrUtil.isBlank(task.getFormData())) {
            return Collections.emptyMap();
        }
        
        try {
            return JSONUtil.toBean(task.getFormData(), Map.class);
        } catch (Exception e) {
            log.warn("解析任务表单数据失败: {}", taskId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 设置任务的表单数据
     * 
     * @param taskId 任务ID
     * @param formData 表单数据
     * @return 是否设置成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setTaskFormData(String taskId, Map<String, Object> formData) {
        if (StrUtil.isBlank(taskId)) {
            return false;
        }
        
        String formDataJson = null;
        if (formData != null && !formData.isEmpty()) {
            formDataJson = JSONUtil.toJsonStr(formData);
        }
        
        return updateTaskFormData(taskId, formDataJson);
    }

    /**
     * 获取任务的候选用户列表
     * 
     * @param taskId 任务ID
     * @return 候选用户ID列表
     */
    @Override
    public List<String> getTaskCandidateUsers(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return Collections.emptyList();
        }
        
        UserTaskEntity task = getById(taskId);
        if (task == null || StrUtil.isBlank(task.getCandidateUsers())) {
            return Collections.emptyList();
        }
        
        try {
            return JSONUtil.toList(task.getCandidateUsers(), String.class);
        } catch (Exception e) {
            log.warn("解析候选用户列表失败: {}", taskId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取任务的候选用户组列表
     * 
     * @param taskId 任务ID
     * @return 候选用户组ID列表
     */
    @Override
    public List<String> getTaskCandidateGroups(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return Collections.emptyList();
        }
        
        UserTaskEntity task = getById(taskId);
        if (task == null || StrUtil.isBlank(task.getCandidateGroups())) {
            return Collections.emptyList();
        }
        
        try {
            return JSONUtil.toList(task.getCandidateGroups(), String.class);
        } catch (Exception e) {
            log.warn("解析候选用户组列表失败: {}", taskId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 添加任务候选用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTaskCandidateUser(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        List<String> candidateUsers = new ArrayList<>(getTaskCandidateUsers(taskId));
        if (!candidateUsers.contains(userId)) {
            candidateUsers.add(userId);
            
            UserTaskEntity task = getById(taskId);
            if (task != null) {
                task.setCandidateUsers(JSONUtil.toJsonStr(candidateUsers));
                task.setUpdatedTime(LocalDateTime.now());
                return updateById(task);
            }
        }
        
        return true;
    }

    /**
     * 移除任务候选用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否移除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTaskCandidateUser(String taskId, String userId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(userId)) {
            return false;
        }
        
        List<String> candidateUsers = new ArrayList<>(getTaskCandidateUsers(taskId));
        if (candidateUsers.remove(userId)) {
            UserTaskEntity task = getById(taskId);
            if (task != null) {
                task.setCandidateUsers(candidateUsers.isEmpty() ? null : JSONUtil.toJsonStr(candidateUsers));
                task.setUpdatedTime(LocalDateTime.now());
                return updateById(task);
            }
        }
        
        return true;
    }

    /**
     * 添加任务候选用户组
     * 
     * @param taskId 任务ID
     * @param groupId 用户组ID
     * @return 是否添加成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTaskCandidateGroup(String taskId, String groupId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(groupId)) {
            return false;
        }
        
        List<String> candidateGroups = new ArrayList<>(getTaskCandidateGroups(taskId));
        if (!candidateGroups.contains(groupId)) {
            candidateGroups.add(groupId);
            
            UserTaskEntity task = getById(taskId);
            if (task != null) {
                task.setCandidateGroups(JSONUtil.toJsonStr(candidateGroups));
                task.setUpdatedTime(LocalDateTime.now());
                return updateById(task);
            }
        }
        
        return true;
    }

    /**
     * 移除任务候选用户组
     * 
     * @param taskId 任务ID
     * @param groupId 用户组ID
     * @return 是否移除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTaskCandidateGroup(String taskId, String groupId) {
        if (StrUtil.isBlank(taskId) || StrUtil.isBlank(groupId)) {
            return false;
        }
        
        List<String> candidateGroups = new ArrayList<>(getTaskCandidateGroups(taskId));
        if (candidateGroups.remove(groupId)) {
            UserTaskEntity task = getById(taskId);
            if (task != null) {
                task.setCandidateGroups(candidateGroups.isEmpty() ? null : JSONUtil.toJsonStr(candidateGroups));
                task.setUpdatedTime(LocalDateTime.now());
                return updateById(task);
            }
        }
        
        return true;
    }
}