package com.tao.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tao.workflow.entity.UserTaskEntity;
import com.tao.workflow.service.UserTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户任务控制器
 * 提供用户任务的REST API接口
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/user-tasks")
@RequiredArgsConstructor
@Api(tags = "用户任务管理")
public class UserTaskController {

    private final UserTaskService userTaskService;

    /**
     * 创建用户任务
     * 
     * @param request 创建请求
     * @return 创建的用户任务
     */
    @PostMapping
    @ApiOperation("创建用户任务")
    public ResponseEntity<UserTaskEntity> createUserTask(
            @Valid @RequestBody CreateUserTaskRequest request) {
        log.info("创建用户任务: {}", request.getTaskName());
        
        UserTaskEntity task = new UserTaskEntity();
        task.setInstanceId(request.getInstanceId());
        task.setStepId(request.getStepId());
        task.setTaskName(request.getTaskName());
        task.setTaskDescription(request.getTaskDescription());
        task.setAssignee(request.getAssignee());
        task.setCandidateUsers(request.getCandidateUsers());
        task.setCandidateGroups(request.getCandidateGroups());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setFormData(request.getFormData());
        
        UserTaskEntity result = userTaskService.createUserTask(task);
        
        log.info("用户任务创建成功，任务ID: {}", result.getTaskId());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取用户任务
     * 
     * @param taskId 任务ID
     * @return 用户任务
     */
    @GetMapping("/{taskId}")
    @ApiOperation("根据ID获取用户任务")
    public ResponseEntity<UserTaskEntity> getUserTaskById(
            @ApiParam("任务ID") @PathVariable String taskId) {
        log.info("获取用户任务: {}", taskId);
        
        UserTaskEntity task = userTaskService.getUserTaskById(taskId);
        
        if (task == null) {
            log.warn("用户任务不存在: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(task);
    }

    /**
     * 根据实例ID获取任务列表
     * 
     * @param instanceId 实例ID
     * @return 任务列表
     */
    @GetMapping("/by-instance/{instanceId}")
    @ApiOperation("根据实例ID获取任务列表")
    public ResponseEntity<List<UserTaskEntity>> getTasksByInstanceId(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例任务列表: {}", instanceId);
        
        List<UserTaskEntity> tasks = userTaskService.getTasksByInstanceId(instanceId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 根据步骤ID获取任务列表
     * 
     * @param stepId 步骤ID
     * @return 任务列表
     */
    @GetMapping("/by-step/{stepId}")
    @ApiOperation("根据步骤ID获取任务列表")
    public ResponseEntity<List<UserTaskEntity>> getTasksByStepId(
            @ApiParam("步骤ID") @PathVariable String stepId) {
        log.info("获取步骤任务列表: {}", stepId);
        
        List<UserTaskEntity> tasks = userTaskService.getTasksByStepId(stepId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 根据用户获取任务列表
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    @GetMapping("/by-user/{userId}")
    @ApiOperation("根据用户获取任务列表")
    public ResponseEntity<List<UserTaskEntity>> getTasksByUser(
            @ApiParam("用户ID") @PathVariable String userId) {
        log.info("获取用户任务列表: {}", userId);
        
        List<UserTaskEntity> tasks = userTaskService.getTasksByUser(userId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 根据组获取任务列表
     * 
     * @param groupId 组ID
     * @return 任务列表
     */
    @GetMapping("/by-group/{groupId}")
    @ApiOperation("根据组获取任务列表")
    public ResponseEntity<List<UserTaskEntity>> getTasksByGroup(
            @ApiParam("组ID") @PathVariable String groupId) {
        log.info("获取组任务列表: {}", groupId);
        
        List<UserTaskEntity> tasks = userTaskService.getTasksByGroup(groupId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取用户待办任务
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    @GetMapping("/pending/user/{userId}")
    @ApiOperation("获取用户待办任务")
    public ResponseEntity<List<UserTaskEntity>> getPendingTasksByUser(
            @ApiParam("用户ID") @PathVariable String userId) {
        log.info("获取用户待办任务: {}", userId);
        
        List<UserTaskEntity> tasks = userTaskService.getPendingTasksByUser(userId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取组待办任务
     * 
     * @param groupId 组ID
     * @return 待办任务列表
     */
    @GetMapping("/pending/group/{groupId}")
    @ApiOperation("获取组待办任务")
    public ResponseEntity<List<UserTaskEntity>> getPendingTasksByGroup(
            @ApiParam("组ID") @PathVariable String groupId) {
        log.info("获取组待办任务: {}", groupId);
        
        List<UserTaskEntity> tasks = userTaskService.getPendingTasksByGroup(groupId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取用户已完成任务
     * 
     * @param userId 用户ID
     * @return 已完成任务列表
     */
    @GetMapping("/completed/user/{userId}")
    @ApiOperation("获取用户已完成任务")
    public ResponseEntity<List<UserTaskEntity>> getCompletedTasksByUser(
            @ApiParam("用户ID") @PathVariable String userId) {
        log.info("获取用户已完成任务: {}", userId);
        
        List<UserTaskEntity> tasks = userTaskService.getCompletedTasksByUser(userId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取组已完成任务
     * 
     * @param groupId 组ID
     * @return 已完成任务列表
     */
    @GetMapping("/completed/group/{groupId}")
    @ApiOperation("获取组已完成任务")
    public ResponseEntity<List<UserTaskEntity>> getCompletedTasksByGroup(
            @ApiParam("组ID") @PathVariable String groupId) {
        log.info("获取组已完成任务: {}", groupId);
        
        List<UserTaskEntity> tasks = userTaskService.getCompletedTasksByGroup(groupId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取用户逾期任务
     * 
     * @param userId 用户ID
     * @return 逾期任务列表
     */
    @GetMapping("/overdue/user/{userId}")
    @ApiOperation("获取用户逾期任务")
    public ResponseEntity<List<UserTaskEntity>> getOverdueTasksByUser(
            @ApiParam("用户ID") @PathVariable String userId) {
        log.info("获取用户逾期任务: {}", userId);
        
        List<UserTaskEntity> tasks = userTaskService.getOverdueTasksByUser(userId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取组逾期任务
     * 
     * @param groupId 组ID
     * @return 逾期任务列表
     */
    @GetMapping("/overdue/group/{groupId}")
    @ApiOperation("获取组逾期任务")
    public ResponseEntity<List<UserTaskEntity>> getOverdueTasksByGroup(
            @ApiParam("组ID") @PathVariable String groupId) {
        log.info("获取组逾期任务: {}", groupId);
        
        List<UserTaskEntity> tasks = userTaskService.getOverdueTasksByGroup(groupId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取用户即将到期任务
     * 
     * @param userId 用户ID
     * @param hours 小时数
     * @return 即将到期任务列表
     */
    @GetMapping("/due-soon/user/{userId}")
    @ApiOperation("获取用户即将到期任务")
    public ResponseEntity<List<UserTaskEntity>> getDueSoonTasksByUser(
            @ApiParam("用户ID") @PathVariable String userId,
            @ApiParam("小时数") @RequestParam(defaultValue = "24") int hours) {
        log.info("获取用户即将到期任务: {} - {}小时", userId, hours);
        
        List<UserTaskEntity> tasks = userTaskService.getDueSoonTasksByUser(userId, hours);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取组即将到期任务
     * 
     * @param groupId 组ID
     * @param hours 小时数
     * @return 即将到期任务列表
     */
    @GetMapping("/due-soon/group/{groupId}")
    @ApiOperation("获取组即将到期任务")
    public ResponseEntity<List<UserTaskEntity>> getDueSoonTasksByGroup(
            @ApiParam("组ID") @PathVariable String groupId,
            @ApiParam("小时数") @RequestParam(defaultValue = "24") int hours) {
        log.info("获取组即将到期任务: {} - {}小时", groupId, hours);
        
        List<UserTaskEntity> tasks = userTaskService.getDueSoonTasksByGroup(groupId, hours);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 分页查询用户任务
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    @ApiOperation("分页查询用户任务")
    public ResponseEntity<IPage<UserTaskEntity>> getTasksWithConditions(
            @Valid @RequestBody QueryUserTaskRequest request) {
        log.info("分页查询用户任务，页码: {}, 页大小: {}", request.getCurrent(), request.getSize());
        
        IPage<UserTaskEntity> page = userTaskService.getTasksWithConditions(
                request.getCurrent(), request.getSize(), request.getInstanceId(), 
                request.getStepId(), request.getAssignee(), request.getStatus(), 
                request.getPriority(), request.getStartTime(), request.getEndTime());
        
        return ResponseEntity.ok(page);
    }

    /**
     * 更新用户任务
     * 
     * @param taskId 任务ID
     * @param request 更新请求
     * @return 更新后的用户任务
     */
    @PutMapping("/{taskId}")
    @ApiOperation("更新用户任务")
    public ResponseEntity<UserTaskEntity> updateUserTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody UpdateUserTaskRequest request) {
        log.info("更新用户任务: {}", taskId);
        
        UserTaskEntity task = userTaskService.getUserTaskById(taskId);
        if (task == null) {
            log.warn("用户任务不存在: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(request.getTaskName())) {
            task.setTaskName(request.getTaskName());
        }
        if (StrUtil.isNotBlank(request.getTaskDescription())) {
            task.setTaskDescription(request.getTaskDescription());
        }
        if (StrUtil.isNotBlank(request.getAssignee())) {
            task.setAssignee(request.getAssignee());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (StrUtil.isNotBlank(request.getFormData())) {
            task.setFormData(request.getFormData());
        }
        
        UserTaskEntity result = userTaskService.updateUserTask(task);
        
        log.info("用户任务更新成功: {}", taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * 完成用户任务
     * 
     * @param taskId 任务ID
     * @param request 完成请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/complete")
    @ApiOperation("完成用户任务")
    public ResponseEntity<Void> completeUserTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody CompleteUserTaskRequest request) {
        log.info("完成用户任务: {} - {}", taskId, request.getCompletedBy());
        
        boolean result = userTaskService.completeUserTask(taskId, request.getCompletedBy(), 
                request.getComment(), request.getFormData());
        
        if (result) {
            log.info("用户任务完成成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("用户任务完成失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 取消用户任务
     * 
     * @param taskId 任务ID
     * @param request 取消请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/cancel")
    @ApiOperation("取消用户任务")
    public ResponseEntity<Void> cancelUserTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody CancelUserTaskRequest request) {
        log.info("取消用户任务: {} - {}", taskId, request.getCancelledBy());
        
        boolean result = userTaskService.cancelUserTask(taskId, request.getCancelledBy(), request.getReason());
        
        if (result) {
            log.info("用户任务取消成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("用户任务取消失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 委派用户任务
     * 
     * @param taskId 任务ID
     * @param request 委派请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/delegate")
    @ApiOperation("委派用户任务")
    public ResponseEntity<Void> delegateUserTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody DelegateUserTaskRequest request) {
        log.info("委派用户任务: {} -> {}", taskId, request.getNewAssignee());
        
        boolean result = userTaskService.delegateUserTask(taskId, request.getNewAssignee(), 
                request.getDelegatedBy(), request.getReason());
        
        if (result) {
            log.info("用户任务委派成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("用户任务委派失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 收回用户任务
     * 
     * @param taskId 任务ID
     * @param request 收回请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/reclaim")
    @ApiOperation("收回用户任务")
    public ResponseEntity<Void> reclaimUserTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody ReclaimUserTaskRequest request) {
        log.info("收回用户任务: {} - {}", taskId, request.getReclaimedBy());
        
        boolean result = userTaskService.reclaimUserTask(taskId, request.getReclaimedBy(), request.getReason());
        
        if (result) {
            log.info("用户任务收回成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("用户任务收回失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新任务到期时间
     * 
     * @param taskId 任务ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{taskId}/due-date")
    @ApiOperation("更新任务到期时间")
    public ResponseEntity<Void> updateTaskDueDate(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskDueDateRequest request) {
        log.info("更新任务到期时间: {} -> {}", taskId, request.getDueDate());
        
        boolean result = userTaskService.updateTaskDueDate(taskId, request.getDueDate());
        
        if (result) {
            log.info("任务到期时间更新成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务到期时间更新失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新任务优先级
     * 
     * @param taskId 任务ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{taskId}/priority")
    @ApiOperation("更新任务优先级")
    public ResponseEntity<Void> updateTaskPriority(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskPriorityRequest request) {
        log.info("更新任务优先级: {} -> {}", taskId, request.getPriority());
        
        boolean result = userTaskService.updateTaskPriority(taskId, request.getPriority());
        
        if (result) {
            log.info("任务优先级更新成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务优先级更新失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新任务表单数据
     * 
     * @param taskId 任务ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{taskId}/form-data")
    @ApiOperation("更新任务表单数据")
    public ResponseEntity<Void> updateTaskFormData(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskFormDataRequest request) {
        log.info("更新任务表单数据: {}", taskId);
        
        boolean result = userTaskService.updateTaskFormData(taskId, request.getFormData());
        
        if (result) {
            log.info("任务表单数据更新成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务表单数据更新失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量更新任务状态
     * 
     * @param request 批量更新请求
     * @return 更新数量
     */
    @PostMapping("/batch-update-status")
    @ApiOperation("批量更新任务状态")
    public ResponseEntity<Integer> batchUpdateTaskStatus(
            @Valid @RequestBody BatchUpdateTaskStatusRequest request) {
        log.info("批量更新任务状态: {} 个任务", request.getTaskIds().size());
        
        int count = userTaskService.batchUpdateTaskStatus(request.getTaskIds(), request.getStatus());
        
        log.info("批量更新任务状态完成，更新数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 统计任务数量
     * 
     * @param status 状态（可选）
     * @return 任务数量
     */
    @GetMapping("/count")
    @ApiOperation("统计任务数量")
    public ResponseEntity<Long> countTasks(
            @ApiParam("状态") @RequestParam(required = false) UserTaskEntity.Status status) {
        log.info("统计任务数量，状态: {}", status);
        
        long count = userTaskService.countTasks(status);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 根据优先级统计任务数量
     * 
     * @param priority 优先级
     * @return 任务数量
     */
    @GetMapping("/count-by-priority")
    @ApiOperation("根据优先级统计任务数量")
    public ResponseEntity<Long> countTasksByPriority(
            @ApiParam("优先级") @RequestParam UserTaskEntity.Priority priority) {
        log.info("统计优先级任务数量: {}", priority);
        
        long count = userTaskService.countTasksByPriority(priority);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 获取用户任务统计
     * 
     * @param userId 用户ID
     * @return 用户任务统计
     */
    @GetMapping("/user-statistics")
    @ApiOperation("获取用户任务统计")
    public ResponseEntity<Map<String, Object>> getUserTaskStatistics(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("获取用户任务统计: {}", userId);
        
        Map<String, Object> statistics = userTaskService.getUserTaskStatistics(userId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取最近任务列表
     * 
     * @param limit 限制数量
     * @return 最近任务列表
     */
    @GetMapping("/recent")
    @ApiOperation("获取最近任务列表")
    public ResponseEntity<List<UserTaskEntity>> getRecentTasks(
            @ApiParam("限制数量") @RequestParam(defaultValue = "10") int limit) {
        log.info("获取最近任务列表，限制数量: {}", limit);
        
        List<UserTaskEntity> tasks = userTaskService.getRecentTasks(limit);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取高优先级待办任务
     * 
     * @param userId 用户ID
     * @return 高优先级待办任务列表
     */
    @GetMapping("/high-priority-pending")
    @ApiOperation("获取高优先级待办任务")
    public ResponseEntity<List<UserTaskEntity>> getHighPriorityPendingTasks(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("获取高优先级待办任务: {}", userId);
        
        List<UserTaskEntity> tasks = userTaskService.getHighPriorityPendingTasks(userId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取处理时间统计
     * 
     * @param userId 用户ID（可选）
     * @return 处理时间统计
     */
    @GetMapping("/processing-time-statistics")
    @ApiOperation("获取处理时间统计")
    public ResponseEntity<Map<String, Object>> getProcessingTimeStatistics(
            @ApiParam("用户ID") @RequestParam(required = false) String userId) {
        log.info("获取处理时间统计，用户ID: {}", userId);
        
        Map<String, Object> statistics = userTaskService.getProcessingTimeStatistics(userId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取创建趋势统计
     * 
     * @param days 天数
     * @return 创建趋势统计
     */
    @GetMapping("/creation-trend")
    @ApiOperation("获取创建趋势统计")
    public ResponseEntity<Map<String, Object>> getCreationTrendStatistics(
            @ApiParam("天数") @RequestParam(defaultValue = "30") int days) {
        log.info("获取创建趋势统计，天数: {}", days);
        
        Map<String, Object> statistics = userTaskService.getCreationTrendStatistics(days);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取用户效率统计
     * 
     * @param userId 用户ID
     * @return 用户效率统计
     */
    @GetMapping("/user-efficiency")
    @ApiOperation("获取用户效率统计")
    public ResponseEntity<Map<String, Object>> getUserEfficiencyStatistics(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("获取用户效率统计: {}", userId);
        
        Map<String, Object> statistics = userTaskService.getUserEfficiencyStatistics(userId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取任务类型分布统计
     * 
     * @return 任务类型分布统计
     */
    @GetMapping("/type-distribution")
    @ApiOperation("获取任务类型分布统计")
    public ResponseEntity<Map<String, Object>> getTaskTypeDistributionStatistics() {
        log.info("获取任务类型分布统计");
        
        Map<String, Object> statistics = userTaskService.getTaskTypeDistributionStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 搜索用户任务
     * 
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    @ApiOperation("搜索用户任务")
    public ResponseEntity<List<UserTaskEntity>> searchTasks(
            @ApiParam("关键词") @RequestParam String keyword) {
        log.info("搜索用户任务: {}", keyword);
        
        List<UserTaskEntity> tasks = userTaskService.searchTasks(keyword);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取需要提醒的任务
     * 
     * @return 需要提醒的任务列表
     */
    @GetMapping("/reminder")
    @ApiOperation("获取需要提醒的任务")
    public ResponseEntity<List<UserTaskEntity>> getTasksForReminder() {
        log.info("获取需要提醒的任务");
        
        List<UserTaskEntity> tasks = userTaskService.getTasksForReminder();
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取需要清理的任务
     * 
     * @param days 保留天数
     * @return 需要清理的任务列表
     */
    @GetMapping("/cleanup")
    @ApiOperation("获取需要清理的任务")
    public ResponseEntity<List<UserTaskEntity>> getTasksForCleanup(
            @ApiParam("保留天数") @RequestParam(defaultValue = "90") int days) {
        log.info("获取需要清理的任务，保留天数: {}", days);
        
        List<UserTaskEntity> tasks = userTaskService.getTasksForCleanup(days);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 检查用户是否可以处理任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 是否可以处理
     */
    @GetMapping("/can-handle")
    @ApiOperation("检查用户是否可以处理任务")
    public ResponseEntity<Boolean> canUserHandleTask(
            @ApiParam("用户ID") @RequestParam String userId,
            @ApiParam("任务ID") @RequestParam String taskId) {
        log.info("检查用户是否可以处理任务: {} - {}", userId, taskId);
        
        boolean canHandle = userTaskService.canUserHandleTask(userId, taskId);
        
        return ResponseEntity.ok(canHandle);
    }

    /**
     * 获取委派历史
     * 
     * @param taskId 任务ID
     * @return 委派历史
     */
    @GetMapping("/{taskId}/delegation-history")
    @ApiOperation("获取委派历史")
    public ResponseEntity<List<Map<String, Object>>> getDelegationHistory(
            @ApiParam("任务ID") @PathVariable String taskId) {
        log.info("获取委派历史: {}", taskId);
        
        List<Map<String, Object>> history = userTaskService.getDelegationHistory(taskId);
        
        return ResponseEntity.ok(history);
    }

    /**
     * 获取活跃任务数量
     * 
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    @GetMapping("/active-count")
    @ApiOperation("获取活跃任务数量")
    public ResponseEntity<Long> getActiveTaskCount(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("获取活跃任务数量: {}", userId);
        
        long count = userTaskService.getActiveTaskCount(userId);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 分配任务
     * 
     * @param taskId 任务ID
     * @param request 分配请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/assign")
    @ApiOperation("分配任务")
    public ResponseEntity<Void> assignTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody AssignTaskRequest request) {
        log.info("分配任务: {} -> {}", taskId, request.getAssignee());
        
        boolean result = userTaskService.assignTask(taskId, request.getAssignee());
        
        if (result) {
            log.info("任务分配成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务分配失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 认领任务
     * 
     * @param taskId 任务ID
     * @param request 认领请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/claim")
    @ApiOperation("认领任务")
    public ResponseEntity<Void> claimTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody ClaimTaskRequest request) {
        log.info("认领任务: {} - {}", taskId, request.getUserId());
        
        boolean result = userTaskService.claimTask(taskId, request.getUserId());
        
        if (result) {
            log.info("任务认领成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务认领失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 释放任务
     * 
     * @param taskId 任务ID
     * @param request 释放请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/release")
    @ApiOperation("释放任务")
    public ResponseEntity<Void> releaseTask(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody ReleaseTaskRequest request) {
        log.info("释放任务: {} - {}", taskId, request.getUserId());
        
        boolean result = userTaskService.releaseTask(taskId, request.getUserId());
        
        if (result) {
            log.info("任务释放成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务释放失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取任务表单数据
     * 
     * @param taskId 任务ID
     * @return 表单数据
     */
    @GetMapping("/{taskId}/form-data")
    @ApiOperation("获取任务表单数据")
    public ResponseEntity<Map<String, Object>> getTaskFormData(
            @ApiParam("任务ID") @PathVariable String taskId) {
        log.info("获取任务表单数据: {}", taskId);
        
        Map<String, Object> formData = userTaskService.getTaskFormData(taskId);
        
        return ResponseEntity.ok(formData);
    }

    /**
     * 设置任务表单数据
     * 
     * @param taskId 任务ID
     * @param request 设置表单数据请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/form-data")
    @ApiOperation("设置任务表单数据")
    public ResponseEntity<Void> setTaskFormData(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody SetTaskFormDataRequest request) {
        log.info("设置任务表单数据: {}", taskId);
        
        boolean result = userTaskService.setTaskFormData(taskId, request.getFormData());
        
        if (result) {
            log.info("任务表单数据设置成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务表单数据设置失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 添加任务候选用户
     * 
     * @param taskId 任务ID
     * @param request 添加候选用户请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/candidate-users")
    @ApiOperation("添加任务候选用户")
    public ResponseEntity<Void> addTaskCandidateUser(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody AddCandidateUserRequest request) {
        log.info("添加任务候选用户: {} - {}", taskId, request.getUserId());
        
        boolean result = userTaskService.addTaskCandidateUser(taskId, request.getUserId());
        
        if (result) {
            log.info("任务候选用户添加成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务候选用户添加失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 移除任务候选用户
     * 
     * @param taskId 任务ID
     * @param request 移除候选用户请求
     * @return 操作结果
     */
    @DeleteMapping("/{taskId}/candidate-users")
    @ApiOperation("移除任务候选用户")
    public ResponseEntity<Void> removeTaskCandidateUser(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody RemoveCandidateUserRequest request) {
        log.info("移除任务候选用户: {} - {}", taskId, request.getUserId());
        
        boolean result = userTaskService.removeTaskCandidateUser(taskId, request.getUserId());
        
        if (result) {
            log.info("任务候选用户移除成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务候选用户移除失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 添加任务候选组
     * 
     * @param taskId 任务ID
     * @param request 添加候选组请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/candidate-groups")
    @ApiOperation("添加任务候选组")
    public ResponseEntity<Void> addTaskCandidateGroup(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody AddCandidateGroupRequest request) {
        log.info("添加任务候选组: {} - {}", taskId, request.getGroupId());
        
        boolean result = userTaskService.addTaskCandidateGroup(taskId, request.getGroupId());
        
        if (result) {
            log.info("任务候选组添加成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务候选组添加失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 移除任务候选组
     * 
     * @param taskId 任务ID
     * @param request 移除候选组请求
     * @return 操作结果
     */
    @DeleteMapping("/{taskId}/candidate-groups")
    @ApiOperation("移除任务候选组")
    public ResponseEntity<Void> removeTaskCandidateGroup(
            @ApiParam("任务ID") @PathVariable String taskId,
            @Valid @RequestBody RemoveCandidateGroupRequest request) {
        log.info("移除任务候选组: {} - {}", taskId, request.getGroupId());
        
        boolean result = userTaskService.removeTaskCandidateGroup(taskId, request.getGroupId());
        
        if (result) {
            log.info("任务候选组移除成功: {}", taskId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("任务候选组移除失败: {}", taskId);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 请求和响应类 ====================

    /**
     * 创建用户任务请求
     */
    @lombok.Data
    public static class CreateUserTaskRequest {
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("步骤ID")
        private String stepId;
        
        @ApiParam("任务名称")
        private String taskName;
        
        @ApiParam("任务描述")
        private String taskDescription;
        
        @ApiParam("分配人")
        private String assignee;
        
        @ApiParam("候选用户")
        private String candidateUsers;
        
        @ApiParam("候选组")
        private String candidateGroups;
        
        @ApiParam("优先级")
        private UserTaskEntity.Priority priority;
        
        @ApiParam("到期时间")
        private LocalDateTime dueDate;
        
        @ApiParam("表单数据")
        private String formData;
    }

    /**
     * 查询用户任务请求
     */
    @lombok.Data
    public static class QueryUserTaskRequest {
        @ApiParam("当前页")
        private long current = 1;
        
        @ApiParam("页大小")
        private long size = 10;
        
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("步骤ID")
        private String stepId;
        
        @ApiParam("分配人")
        private String assignee;
        
        @ApiParam("状态")
        private UserTaskEntity.Status status;
        
        @ApiParam("优先级")
        private UserTaskEntity.Priority priority;
        
        @ApiParam("开始时间")
        private LocalDateTime startTime;
        
        @ApiParam("结束时间")
        private LocalDateTime endTime;
    }

    /**
     * 更新用户任务请求
     */
    @lombok.Data
    public static class UpdateUserTaskRequest {
        @ApiParam("任务名称")
        private String taskName;
        
        @ApiParam("任务描述")
        private String taskDescription;
        
        @ApiParam("分配人")
        private String assignee;
        
        @ApiParam("优先级")
        private UserTaskEntity.Priority priority;
        
        @ApiParam("到期时间")
        private LocalDateTime dueDate;
        
        @ApiParam("表单数据")
        private String formData;
    }

    /**
     * 完成用户任务请求
     */
    @lombok.Data
    public static class CompleteUserTaskRequest {
        @ApiParam("完成人")
        private String completedBy;
        
        @ApiParam("备注")
        private String comment;
        
        @ApiParam("表单数据")
        private String formData;
    }

    /**
     * 取消用户任务请求
     */
    @lombok.Data
    public static class CancelUserTaskRequest {
        @ApiParam("取消人")
        private String cancelledBy;
        
        @ApiParam("取消原因")
        private String reason;
    }

    /**
     * 委派用户任务请求
     */
    @lombok.Data
    public static class DelegateUserTaskRequest {
        @ApiParam("新分配人")
        private String newAssignee;
        
        @ApiParam("委派人")
        private String delegatedBy;
        
        @ApiParam("委派原因")
        private String reason;
    }

    /**
     * 收回用户任务请求
     */
    @lombok.Data
    public static class ReclaimUserTaskRequest {
        @ApiParam("收回人")
        private String reclaimedBy;
        
        @ApiParam("收回原因")
        private String reason;
    }

    /**
     * 更新任务到期时间请求
     */
    @lombok.Data
    public static class UpdateTaskDueDateRequest {
        @ApiParam("到期时间")
        private LocalDateTime dueDate;
    }

    /**
     * 更新任务优先级请求
     */
    @lombok.Data
    public static class UpdateTaskPriorityRequest {
        @ApiParam("优先级")
        private UserTaskEntity.Priority priority;
    }

    /**
     * 更新任务表单数据请求
     */
    @lombok.Data
    public static class UpdateTaskFormDataRequest {
        @ApiParam("表单数据")
        private String formData;
    }

    /**
     * 批量更新任务状态请求
     */
    @lombok.Data
    public static class BatchUpdateTaskStatusRequest {
        @ApiParam("任务ID列表")
        private List<String> taskIds;
        
        @ApiParam("状态")
        private UserTaskEntity.Status status;
    }

    /**
     * 分配任务请求
     */
    @lombok.Data
    public static class AssignTaskRequest {
        @ApiParam("分配人")
        private String assignee;
    }

    /**
     * 认领任务请求
     */
    @lombok.Data
    public static class ClaimTaskRequest {
        @ApiParam("用户ID")
        private String userId;
    }

    /**
     * 释放任务请求
     */
    @lombok.Data
    public static class ReleaseTaskRequest {
        @ApiParam("用户ID")
        private String userId;
    }

    /**
     * 设置任务表单数据请求
     */
    @lombok.Data
    public static class SetTaskFormDataRequest {
        @ApiParam("表单数据")
        private Map<String, Object> formData;
    }

    /**
     * 添加候选用户请求
     */
    @lombok.Data
    public static class AddCandidateUserRequest {
        @ApiParam("用户ID")
        private String userId;
    }

    /**
     * 移除候选用户请求
     */
    @lombok.Data
    public static class RemoveCandidateUserRequest {
        @ApiParam("用户ID")
        private String userId;
    }

    /**
     * 添加候选组请求
     */
    @lombok.Data
    public static class AddCandidateGroupRequest {
        @ApiParam("组ID")
        private String groupId;
    }

    /**
     * 移除候选组请求
     */
    @lombok.Data
    public static class RemoveCandidateGroupRequest {
        @ApiParam("组ID")
        private String groupId;
    }
}