package com.tao.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tao.workflow.entity.WorkflowInstanceEntity;
import com.tao.workflow.service.WorkflowInstanceService;
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
 * 工作流实例控制器
 * 提供工作流实例的REST API接口
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/instances")
@RequiredArgsConstructor
@Api(tags = "工作流实例管理")
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    /**
     * 启动工作流实例
     * 
     * @param request 启动请求
     * @return 启动的工作流实例
     */
    @PostMapping("/start")
    @ApiOperation("启动工作流实例")
    public ResponseEntity<WorkflowInstanceEntity> startWorkflowInstance(
            @Valid @RequestBody StartWorkflowInstanceRequest request) {
        log.info("启动工作流实例: {}", request.getWorkflowId());
        
        WorkflowInstanceEntity instance = workflowInstanceService.startWorkflowInstance(
                request.getWorkflowId(), request.getStarter(), request.getBusinessKey(), 
                request.getVariables(), request.getTitle());
        
        if (instance == null) {
            log.warn("启动工作流实例失败，工作流不存在: {}", request.getWorkflowId());
            return ResponseEntity.badRequest().build();
        }
        
        log.info("工作流实例启动成功，实例ID: {}", instance.getInstanceId());
        return ResponseEntity.ok(instance);
    }

    /**
     * 根据ID获取工作流实例
     * 
     * @param instanceId 实例ID
     * @return 工作流实例
     */
    @GetMapping("/{instanceId}")
    @ApiOperation("根据ID获取工作流实例")
    public ResponseEntity<WorkflowInstanceEntity> getWorkflowInstanceById(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取工作流实例: {}", instanceId);
        
        WorkflowInstanceEntity instance = workflowInstanceService.getWorkflowInstanceById(instanceId);
        
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(instance);
    }

    /**
     * 根据工作流ID获取实例列表
     * 
     * @param workflowId 工作流ID
     * @return 实例列表
     */
    @GetMapping("/by-workflow/{workflowId}")
    @ApiOperation("根据工作流ID获取实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getInstancesByWorkflowId(
            @ApiParam("工作流ID") @PathVariable String workflowId) {
        log.info("获取工作流实例列表: {}", workflowId);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getInstancesByWorkflowId(workflowId);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 根据状态获取实例列表
     * 
     * @param status 状态
     * @return 实例列表
     */
    @GetMapping("/by-status")
    @ApiOperation("根据状态获取实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getInstancesByStatus(
            @ApiParam("状态") @RequestParam WorkflowInstanceEntity.Status status) {
        log.info("获取状态为 {} 的实例列表", status);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getInstancesByStatus(status);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 根据启动者获取实例列表
     * 
     * @param starter 启动者
     * @return 实例列表
     */
    @GetMapping("/by-starter")
    @ApiOperation("根据启动者获取实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getInstancesByStarter(
            @ApiParam("启动者") @RequestParam String starter) {
        log.info("获取启动者为 {} 的实例列表", starter);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getInstancesByStarter(starter);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取运行中的实例列表
     * 
     * @return 运行中的实例列表
     */
    @GetMapping("/running")
    @ApiOperation("获取运行中的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getRunningInstances() {
        log.info("获取运行中的实例列表");
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getRunningInstances();
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取等待中的实例列表
     * 
     * @return 等待中的实例列表
     */
    @GetMapping("/waiting")
    @ApiOperation("获取等待中的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getWaitingInstances() {
        log.info("获取等待中的实例列表");
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getWaitingInstances();
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取已完成的实例列表
     * 
     * @return 已完成的实例列表
     */
    @GetMapping("/completed")
    @ApiOperation("获取已完成的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getCompletedInstances() {
        log.info("获取已完成的实例列表");
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getCompletedInstances();
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 分页查询工作流实例
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    @ApiOperation("分页查询工作流实例")
    public ResponseEntity<IPage<WorkflowInstanceEntity>> getInstancesWithConditions(
            @Valid @RequestBody QueryWorkflowInstanceRequest request) {
        log.info("分页查询工作流实例，页码: {}, 页大小: {}", request.getCurrent(), request.getSize());
        
        IPage<WorkflowInstanceEntity> page = workflowInstanceService.getInstancesWithConditions(
                request.getCurrent(), request.getSize(), request.getWorkflowId(), 
                request.getStatus(), request.getStarter(), request.getBusinessKey());
        
        return ResponseEntity.ok(page);
    }

    /**
     * 更新工作流实例
     * 
     * @param instanceId 实例ID
     * @param request 更新请求
     * @return 更新后的工作流实例
     */
    @PutMapping("/{instanceId}")
    @ApiOperation("更新工作流实例")
    public ResponseEntity<WorkflowInstanceEntity> updateWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody UpdateWorkflowInstanceRequest request) {
        log.info("更新工作流实例: {}", instanceId);
        
        WorkflowInstanceEntity instance = workflowInstanceService.getWorkflowInstanceById(instanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return ResponseEntity.notFound().build();
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(request.getTitle())) {
            instance.setTitle(request.getTitle());
        }
        if (StrUtil.isNotBlank(request.getBusinessKey())) {
            instance.setBusinessKey(request.getBusinessKey());
        }
        
        WorkflowInstanceEntity result = workflowInstanceService.updateWorkflowInstance(instance);
        
        log.info("工作流实例更新成功: {}", instanceId);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新当前步骤
     * 
     * @param instanceId 实例ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/current-step")
    @ApiOperation("更新当前步骤")
    public ResponseEntity<Void> updateCurrentStep(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody UpdateCurrentStepRequest request) {
        log.info("更新实例当前步骤: {} -> {}", instanceId, request.getCurrentStep());
        
        boolean result = workflowInstanceService.updateCurrentStep(instanceId, request.getCurrentStep());
        
        if (result) {
            log.info("实例当前步骤更新成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例当前步骤更新失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新上下文数据
     * 
     * @param instanceId 实例ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/context")
    @ApiOperation("更新上下文数据")
    public ResponseEntity<Void> updateContextData(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody UpdateContextDataRequest request) {
        log.info("更新实例上下文数据: {}", instanceId);
        
        boolean result = workflowInstanceService.updateContextData(instanceId, request.getContextData());
        
        if (result) {
            log.info("实例上下文数据更新成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例上下文数据更新失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 完成工作流实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/complete")
    @ApiOperation("完成工作流实例")
    public ResponseEntity<Void> completeWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("完成工作流实例: {}", instanceId);
        
        boolean result = workflowInstanceService.completeWorkflowInstance(instanceId);
        
        if (result) {
            log.info("工作流实例完成成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流实例完成失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 暂停工作流实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/suspend")
    @ApiOperation("暂停工作流实例")
    public ResponseEntity<Void> suspendWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("暂停工作流实例: {}", instanceId);
        
        boolean result = workflowInstanceService.suspendWorkflowInstance(instanceId);
        
        if (result) {
            log.info("工作流实例暂停成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流实例暂停失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 恢复工作流实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/resume")
    @ApiOperation("恢复工作流实例")
    public ResponseEntity<Void> resumeWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("恢复工作流实例: {}", instanceId);
        
        boolean result = workflowInstanceService.resumeWorkflowInstance(instanceId);
        
        if (result) {
            log.info("工作流实例恢复成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流实例恢复失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 取消工作流实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/cancel")
    @ApiOperation("取消工作流实例")
    public ResponseEntity<Void> cancelWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("取消工作流实例: {}", instanceId);
        
        boolean result = workflowInstanceService.cancelWorkflowInstance(instanceId);
        
        if (result) {
            log.info("工作流实例取消成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流实例取消失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 终止工作流实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/terminate")
    @ApiOperation("终止工作流实例")
    public ResponseEntity<Void> terminateWorkflowInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("终止工作流实例: {}", instanceId);
        
        boolean result = workflowInstanceService.terminateWorkflowInstance(instanceId);
        
        if (result) {
            log.info("工作流实例终止成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流实例终止失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量更新实例状态
     * 
     * @param request 批量更新请求
     * @return 更新数量
     */
    @PostMapping("/batch-update-status")
    @ApiOperation("批量更新实例状态")
    public ResponseEntity<Integer> batchUpdateInstanceStatus(
            @Valid @RequestBody BatchUpdateInstanceStatusRequest request) {
        log.info("批量更新实例状态: {} 个实例", request.getInstanceIds().size());
        
        int count = workflowInstanceService.batchUpdateInstanceStatus(request.getInstanceIds(), request.getStatus());
        
        log.info("批量更新实例状态完成，更新数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 统计实例数量
     * 
     * @param status 状态（可选）
     * @return 实例数量
     */
    @GetMapping("/count")
    @ApiOperation("统计实例数量")
    public ResponseEntity<Long> countInstances(
            @ApiParam("状态") @RequestParam(required = false) WorkflowInstanceEntity.Status status) {
        log.info("统计实例数量，状态: {}", status);
        
        long count = workflowInstanceService.countInstances(status);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 根据工作流ID统计实例数量
     * 
     * @param workflowId 工作流ID
     * @return 实例数量
     */
    @GetMapping("/count-by-workflow")
    @ApiOperation("根据工作流ID统计实例数量")
    public ResponseEntity<Long> countInstancesByWorkflowId(
            @ApiParam("工作流ID") @RequestParam String workflowId) {
        log.info("统计工作流实例数量: {}", workflowId);
        
        long count = workflowInstanceService.countInstancesByWorkflowId(workflowId);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 根据用户统计实例数量
     * 
     * @param userId 用户ID
     * @return 实例数量
     */
    @GetMapping("/count-by-user")
    @ApiOperation("根据用户统计实例数量")
    public ResponseEntity<Long> countInstancesByUser(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("统计用户实例数量: {}", userId);
        
        long count = workflowInstanceService.countInstancesByUser(userId);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 获取最近的实例列表
     * 
     * @param limit 限制数量
     * @return 最近的实例列表
     */
    @GetMapping("/recent")
    @ApiOperation("获取最近的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getRecentInstances(
            @ApiParam("限制数量") @RequestParam(defaultValue = "10") int limit) {
        log.info("获取最近的实例列表，限制数量: {}", limit);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getRecentInstances(limit);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取长时间运行的实例列表
     * 
     * @param hours 小时数
     * @return 长时间运行的实例列表
     */
    @GetMapping("/long-running")
    @ApiOperation("获取长时间运行的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getLongRunningInstances(
            @ApiParam("小时数") @RequestParam(defaultValue = "24") int hours) {
        log.info("获取长时间运行的实例列表，小时数: {}", hours);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getLongRunningInstances(hours);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取超时的实例列表
     * 
     * @return 超时的实例列表
     */
    @GetMapping("/timeout")
    @ApiOperation("获取超时的实例列表")
    public ResponseEntity<List<WorkflowInstanceEntity>> getTimeoutInstances() {
        log.info("获取超时的实例列表");
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.getTimeoutInstances();
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 搜索工作流实例
     * 
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    @ApiOperation("搜索工作流实例")
    public ResponseEntity<List<WorkflowInstanceEntity>> searchInstances(
            @ApiParam("关键词") @RequestParam String keyword) {
        log.info("搜索工作流实例: {}", keyword);
        
        List<WorkflowInstanceEntity> instances = workflowInstanceService.searchInstances(keyword);
        
        return ResponseEntity.ok(instances);
    }

    /**
     * 获取执行统计信息
     * 
     * @param workflowId 工作流ID（可选）
     * @return 执行统计信息
     */
    @GetMapping("/execution-statistics")
    @ApiOperation("获取执行统计信息")
    public ResponseEntity<Map<String, Object>> getExecutionStatistics(
            @ApiParam("工作流ID") @RequestParam(required = false) String workflowId) {
        log.info("获取执行统计信息，工作流ID: {}", workflowId);
        
        Map<String, Object> statistics = workflowInstanceService.getExecutionStatistics(workflowId);
        
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
        
        Map<String, Object> statistics = workflowInstanceService.getCreationTrendStatistics(days);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取性能统计信息
     * 
     * @param workflowId 工作流ID（可选）
     * @return 性能统计信息
     */
    @GetMapping("/performance-statistics")
    @ApiOperation("获取性能统计信息")
    public ResponseEntity<Map<String, Object>> getPerformanceStatistics(
            @ApiParam("工作流ID") @RequestParam(required = false) String workflowId) {
        log.info("获取性能统计信息，工作流ID: {}", workflowId);
        
        Map<String, Object> statistics = workflowInstanceService.getPerformanceStatistics(workflowId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取用户实例统计
     * 
     * @param userId 用户ID
     * @return 用户实例统计
     */
    @GetMapping("/user-statistics")
    @ApiOperation("获取用户实例统计")
    public ResponseEntity<Map<String, Object>> getUserInstanceStatistics(
            @ApiParam("用户ID") @RequestParam String userId) {
        log.info("获取用户实例统计: {}", userId);
        
        Map<String, Object> statistics = workflowInstanceService.getUserInstanceStatistics(userId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 清理旧实例
     * 
     * @param days 保留天数
     * @return 清理数量
     */
    @PostMapping("/cleanup")
    @ApiOperation("清理旧实例")
    public ResponseEntity<Integer> cleanupOldInstances(
            @ApiParam("保留天数") @RequestParam(defaultValue = "90") int days) {
        log.info("清理旧实例，保留天数: {}", days);
        
        int count = workflowInstanceService.cleanupOldInstances(days);
        
        log.info("清理旧实例完成，清理数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 检查实例是否可以执行指定操作
     * 
     * @param instanceId 实例ID
     * @param operation 操作类型
     * @return 是否可以执行
     */
    @GetMapping("/{instanceId}/can-perform")
    @ApiOperation("检查实例是否可以执行指定操作")
    public ResponseEntity<Boolean> canPerformOperation(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @ApiParam("操作类型") @RequestParam String operation) {
        log.info("检查实例是否可以执行操作: {} - {}", instanceId, operation);
        
        boolean canPerform = workflowInstanceService.canPerformOperation(instanceId, operation);
        
        return ResponseEntity.ok(canPerform);
    }

    /**
     * 获取实例执行历史
     * 
     * @param instanceId 实例ID
     * @return 执行历史
     */
    @GetMapping("/{instanceId}/execution-history")
    @ApiOperation("获取实例执行历史")
    public ResponseEntity<List<Map<String, Object>>> getInstanceExecutionHistory(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例执行历史: {}", instanceId);
        
        List<Map<String, Object>> history = workflowInstanceService.getInstanceExecutionHistory(instanceId);
        
        return ResponseEntity.ok(history);
    }

    /**
     * 获取实例变量
     * 
     * @param instanceId 实例ID
     * @return 实例变量
     */
    @GetMapping("/{instanceId}/variables")
    @ApiOperation("获取实例变量")
    public ResponseEntity<Map<String, Object>> getInstanceVariables(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例变量: {}", instanceId);
        
        Map<String, Object> variables = workflowInstanceService.getInstanceVariables(instanceId);
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 设置实例变量
     * 
     * @param instanceId 实例ID
     * @param request 设置变量请求
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/variables")
    @ApiOperation("设置实例变量")
    public ResponseEntity<Void> setInstanceVariables(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody SetInstanceVariablesRequest request) {
        log.info("设置实例变量: {}", instanceId);
        
        boolean result = workflowInstanceService.setInstanceVariables(instanceId, request.getVariables());
        
        if (result) {
            log.info("实例变量设置成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例变量设置失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取实例当前任务
     * 
     * @param instanceId 实例ID
     * @return 当前任务列表
     */
    @GetMapping("/{instanceId}/current-tasks")
    @ApiOperation("获取实例当前任务")
    public ResponseEntity<List<Map<String, Object>>> getCurrentTasks(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例当前任务: {}", instanceId);
        
        List<Map<String, Object>> tasks = workflowInstanceService.getCurrentTasks(instanceId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * 重启失败的实例
     * 
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/restart")
    @ApiOperation("重启失败的实例")
    public ResponseEntity<Void> restartFailedInstance(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("重启失败的实例: {}", instanceId);
        
        boolean result = workflowInstanceService.restartFailedInstance(instanceId);
        
        if (result) {
            log.info("实例重启成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例重启失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 跳过当前步骤
     * 
     * @param instanceId 实例ID
     * @param request 跳过步骤请求
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/skip-step")
    @ApiOperation("跳过当前步骤")
    public ResponseEntity<Void> skipCurrentStep(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody SkipStepRequest request) {
        log.info("跳过当前步骤: {} - {}", instanceId, request.getReason());
        
        boolean result = workflowInstanceService.skipCurrentStep(instanceId, request.getReason());
        
        if (result) {
            log.info("步骤跳过成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("步骤跳过失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 回滚到指定步骤
     * 
     * @param instanceId 实例ID
     * @param request 回滚请求
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/rollback")
    @ApiOperation("回滚到指定步骤")
    public ResponseEntity<Void> rollbackToStep(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody RollbackRequest request) {
        log.info("回滚到指定步骤: {} -> {}", instanceId, request.getTargetStep());
        
        boolean result = workflowInstanceService.rollbackToStep(instanceId, request.getTargetStep(), request.getReason());
        
        if (result) {
            log.info("实例回滚成功: {}", instanceId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例回滚失败: {}", instanceId);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 请求和响应类 ====================

    /**
     * 启动工作流实例请求
     */
    @lombok.Data
    public static class StartWorkflowInstanceRequest {
        @ApiParam("工作流ID")
        private String workflowId;
        
        @ApiParam("启动者")
        private String starter;
        
        @ApiParam("业务键")
        private String businessKey;
        
        @ApiParam("变量")
        private Map<String, Object> variables;
        
        @ApiParam("标题")
        private String title;
    }

    /**
     * 查询工作流实例请求
     */
    @lombok.Data
    public static class QueryWorkflowInstanceRequest {
        @ApiParam("当前页")
        private long current = 1;
        
        @ApiParam("页大小")
        private long size = 10;
        
        @ApiParam("工作流ID")
        private String workflowId;
        
        @ApiParam("状态")
        private WorkflowInstanceEntity.Status status;
        
        @ApiParam("启动者")
        private String starter;
        
        @ApiParam("业务键")
        private String businessKey;
    }

    /**
     * 更新工作流实例请求
     */
    @lombok.Data
    public static class UpdateWorkflowInstanceRequest {
        @ApiParam("标题")
        private String title;
        
        @ApiParam("业务键")
        private String businessKey;
    }

    /**
     * 更新当前步骤请求
     */
    @lombok.Data
    public static class UpdateCurrentStepRequest {
        @ApiParam("当前步骤")
        private String currentStep;
    }

    /**
     * 更新上下文数据请求
     */
    @lombok.Data
    public static class UpdateContextDataRequest {
        @ApiParam("上下文数据")
        private String contextData;
    }

    /**
     * 批量更新实例状态请求
     */
    @lombok.Data
    public static class BatchUpdateInstanceStatusRequest {
        @ApiParam("实例ID列表")
        private List<String> instanceIds;
        
        @ApiParam("状态")
        private WorkflowInstanceEntity.Status status;
    }

    /**
     * 设置实例变量请求
     */
    @lombok.Data
    public static class SetInstanceVariablesRequest {
        @ApiParam("变量")
        private Map<String, Object> variables;
    }

    /**
     * 跳过步骤请求
     */
    @lombok.Data
    public static class SkipStepRequest {
        @ApiParam("跳过原因")
        private String reason;
    }

    /**
     * 回滚请求
     */
    @lombok.Data
    public static class RollbackRequest {
        @ApiParam("目标步骤")
        private String targetStep;
        
        @ApiParam("回滚原因")
        private String reason;
    }
}