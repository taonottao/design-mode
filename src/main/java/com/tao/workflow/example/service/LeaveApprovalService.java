package com.tao.workflow.example.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tao.workflow.core.WorkflowEngine;
import com.tao.workflow.entity.WorkflowDefinitionEntity;
import com.tao.workflow.entity.WorkflowInstanceEntity;
import com.tao.workflow.entity.UserTaskEntity;
import com.tao.workflow.example.entity.LeaveRequest;
import com.tao.workflow.example.factory.LeaveApprovalWorkflowFactory;
import com.tao.workflow.service.WorkflowDefinitionService;
import com.tao.workflow.service.WorkflowInstanceService;
import com.tao.workflow.service.UserTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假审批服务
 * 提供完整的请假申请和审批业务逻辑
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApprovalService {

    private final WorkflowEngine workflowEngine;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowInstanceService workflowInstanceService;
    private final UserTaskService userTaskService;
    private final LeaveApprovalWorkflowFactory workflowFactory;

    /**
     * 提交请假申请
     * 
     * @param leaveRequest 请假申请
     * @return 工作流实例ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String submitLeaveRequest(LeaveRequest leaveRequest) {
        log.info("提交请假申请，申请人: {}，请假类型: {}，请假天数: {}", 
                leaveRequest.getApplicantName(), leaveRequest.getLeaveType(), leaveRequest.getLeaveDays());
        
        try {
            // 1. 验证请假申请数据
            validateLeaveRequest(leaveRequest);
            
            // 2. 生成请假申请ID
            if (StrUtil.isBlank(leaveRequest.getRequestId())) {
                leaveRequest.setRequestId(IdUtil.simpleUUID());
            }
            
            // 3. 设置申请时间
            leaveRequest.setApplicationTime(LocalDateTime.now());
            leaveRequest.setCreateTime(LocalDateTime.now());
            leaveRequest.setUpdateTime(LocalDateTime.now());
            
            // 4. 计算请假天数
            int leaveDays = leaveRequest.calculateLeaveDays();
            leaveRequest.setLeaveDays(leaveDays);
            
            // 5. 选择合适的工作流
            WorkflowDefinitionEntity workflowDefinition = workflowFactory.selectWorkflowForLeaveRequest(leaveRequest);
            
            // 6. 保存工作流定义（如果不存在）
            WorkflowDefinitionEntity existingDefinition = workflowDefinitionService.getLatestWorkflowDefinition(
                    workflowDefinition.getWorkflowName());
            if (existingDefinition == null) {
                workflowDefinition = workflowDefinitionService.createWorkflowDefinition(workflowDefinition);
            } else {
                workflowDefinition = existingDefinition;
            }
            
            // 7. 准备工作流上下文
            Map<String, Object> context = new HashMap<>();
            context.put("leaveRequest", leaveRequest);
            context.put("leaveRequestJson", JSONUtil.toJsonStr(leaveRequest));
            context.put("applicantId", leaveRequest.getApplicantId());
            context.put("applicantName", leaveRequest.getApplicantName());
            context.put("department", leaveRequest.getDepartment());
            context.put("leaveType", leaveRequest.getLeaveType().name());
            context.put("leaveDays", leaveRequest.getLeaveDays());
            context.put("startDate", leaveRequest.getStartDate().toString());
            context.put("endDate", leaveRequest.getEndDate().toString());
            context.put("reason", leaveRequest.getReason());
            
            // 8. 启动工作流实例
            WorkflowInstanceEntity instance = workflowEngine.startWorkflow(
                    workflowDefinition.getDefinitionId(),
                    leaveRequest.getApplicantId(),
                    "请假申请: " + leaveRequest.getApplicantName() + " - " + leaveRequest.getLeaveType().getDescription(),
                    context
            );
            
            log.info("请假申请提交成功，工作流实例ID: {}，请假申请ID: {}", 
                    instance.getInstanceId(), leaveRequest.getRequestId());
            
            return instance.getInstanceId();
            
        } catch (Exception e) {
            log.error("提交请假申请失败: {}", e.getMessage(), e);
            throw new RuntimeException("提交请假申请失败: " + e.getMessage(), e);
        }
    }

    /**
     * 审批请假申请
     * 
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param approvalResult 审批结果（APPROVE/REJECT）
     * @param approvalComment 审批意见
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveLeaveRequest(String taskId, String approverId, String approvalResult, String approvalComment) {
        log.info("审批请假申请，任务ID: {}，审批人: {}，审批结果: {}", taskId, approverId, approvalResult);
        
        try {
            // 1. 获取用户任务
            UserTaskEntity userTask = userTaskService.getUserTaskById(taskId);
            if (userTask == null) {
                log.error("用户任务不存在: {}", taskId);
                return false;
            }
            
            // 2. 验证审批人权限
            if (!canUserApproveTask(userTask, approverId)) {
                log.error("用户无权限审批此任务: {} - {}", taskId, approverId);
                return false;
            }
            
            // 3. 准备审批数据
            Map<String, Object> approvalData = new HashMap<>();
            approvalData.put("approvalResult", approvalResult);
            approvalData.put("approvalComment", StrUtil.isBlank(approvalComment) ? "" : approvalComment);
            approvalData.put("approverId", approverId);
            approvalData.put("approvalTime", LocalDateTime.now().toString());
            
            // 4. 完成用户任务
            boolean result = userTaskService.completeUserTask(taskId, approverId, approvalData);
            
            if (result) {
                log.info("请假申请审批完成，任务ID: {}，审批结果: {}", taskId, approvalResult);
            } else {
                log.error("请假申请审批失败，任务ID: {}", taskId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("审批请假申请失败: {}", e.getMessage(), e);
            throw new RuntimeException("审批请假申请失败: " + e.getMessage(), e);
        }
    }

    /**
     * 撤销请假申请
     * 
     * @param instanceId 工作流实例ID
     * @param applicantId 申请人ID
     * @param reason 撤销原因
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelLeaveRequest(String instanceId, String applicantId, String reason) {
        log.info("撤销请假申请，实例ID: {}，申请人: {}，撤销原因: {}", instanceId, applicantId, reason);
        
        try {
            // 1. 获取工作流实例
            WorkflowInstanceEntity instance = workflowInstanceService.getWorkflowInstanceById(instanceId);
            if (instance == null) {
                log.error("工作流实例不存在: {}", instanceId);
                return false;
            }
            
            // 2. 验证申请人权限
            if (!applicantId.equals(instance.getStartUserId())) {
                log.error("只有申请人才能撤销请假申请: {} - {}", instanceId, applicantId);
                return false;
            }
            
            // 3. 检查实例状态
            if (instance.getStatus() != WorkflowInstanceEntity.Status.RUNNING) {
                log.error("只能撤销运行中的请假申请: {} - {}", instanceId, instance.getStatus());
                return false;
            }
            
            // 4. 取消工作流实例
            boolean result = workflowInstanceService.cancelWorkflowInstance(instanceId, applicantId, reason);
            
            if (result) {
                log.info("请假申请撤销成功，实例ID: {}", instanceId);
            } else {
                log.error("请假申请撤销失败，实例ID: {}", instanceId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("撤销请假申请失败: {}", e.getMessage(), e);
            throw new RuntimeException("撤销请假申请失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取请假申请详情
     * 
     * @param instanceId 工作流实例ID
     * @return 请假申请
     */
    public LeaveRequest getLeaveRequestByInstanceId(String instanceId) {
        log.info("获取请假申请详情，实例ID: {}", instanceId);
        
        try {
            // 1. 获取工作流实例
            WorkflowInstanceEntity instance = workflowInstanceService.getWorkflowInstanceById(instanceId);
            if (instance == null) {
                log.error("工作流实例不存在: {}", instanceId);
                return null;
            }
            
            // 2. 从上下文中获取请假申请数据
            Map<String, Object> contextData = instance.getContextData();
            if (contextData == null || !contextData.containsKey("leaveRequestJson")) {
                log.error("工作流实例中没有请假申请数据: {}", instanceId);
                return null;
            }
            
            String leaveRequestJson = (String) contextData.get("leaveRequestJson");
            LeaveRequest leaveRequest = JSONUtil.toBean(leaveRequestJson, LeaveRequest.class);
            
            // 3. 更新状态信息
            updateLeaveRequestStatus(leaveRequest, instance);
            
            return leaveRequest;
            
        } catch (Exception e) {
            log.error("获取请假申请详情失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取用户的待审批任务
     * 
     * @param userId 用户ID
     * @return 待审批任务列表
     */
    public List<UserTaskEntity> getPendingApprovalTasks(String userId) {
        log.info("获取用户待审批任务，用户ID: {}", userId);
        
        try {
            return userTaskService.getPendingUserTasksByUser(userId);
        } catch (Exception e) {
            log.error("获取用户待审批任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户待审批任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户的请假申请历史
     * 
     * @param applicantId 申请人ID
     * @return 请假申请历史
     */
    public List<WorkflowInstanceEntity> getLeaveRequestHistory(String applicantId) {
        log.info("获取用户请假申请历史，申请人ID: {}", applicantId);
        
        try {
            return workflowInstanceService.getWorkflowInstancesByStarter(applicantId);
        } catch (Exception e) {
            log.error("获取用户请假申请历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户请假申请历史失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证请假申请数据
     * 
     * @param leaveRequest 请假申请
     */
    private void validateLeaveRequest(LeaveRequest leaveRequest) {
        if (leaveRequest == null) {
            throw new IllegalArgumentException("请假申请不能为空");
        }
        
        if (StrUtil.isBlank(leaveRequest.getApplicantId())) {
            throw new IllegalArgumentException("申请人ID不能为空");
        }
        
        if (StrUtil.isBlank(leaveRequest.getApplicantName())) {
            throw new IllegalArgumentException("申请人姓名不能为空");
        }
        
        if (leaveRequest.getLeaveType() == null) {
            throw new IllegalArgumentException("请假类型不能为空");
        }
        
        if (leaveRequest.getStartDate() == null) {
            throw new IllegalArgumentException("请假开始日期不能为空");
        }
        
        if (leaveRequest.getEndDate() == null) {
            throw new IllegalArgumentException("请假结束日期不能为空");
        }
        
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new IllegalArgumentException("请假开始日期不能晚于结束日期");
        }
        
        if (StrUtil.isBlank(leaveRequest.getReason())) {
            throw new IllegalArgumentException("请假原因不能为空");
        }
        
        if (StrUtil.isBlank(leaveRequest.getDirectManagerId())) {
            throw new IllegalArgumentException("直属领导ID不能为空");
        }
        
        // 根据请假天数验证必要的审批人信息
        int leaveDays = leaveRequest.calculateLeaveDays();
        if (leaveDays > 3 && StrUtil.isBlank(leaveRequest.getDepartmentManagerId())) {
            throw new IllegalArgumentException("超过3天的请假需要提供部门经理信息");
        }
        
        if (leaveDays > 5 && StrUtil.isBlank(leaveRequest.getHrManagerId())) {
            throw new IllegalArgumentException("超过5天的请假需要提供HR负责人信息");
        }
        
        if (leaveRequest.requiresGeneralManagerApproval() && StrUtil.isBlank(leaveRequest.getGeneralManagerId())) {
            throw new IllegalArgumentException("长期请假或特殊假期需要提供总经理信息");
        }
    }

    /**
     * 检查用户是否有权限审批指定任务
     * 
     * @param userTask 用户任务
     * @param userId 用户ID
     * @return 是否有权限
     */
    private boolean canUserApproveTask(UserTaskEntity userTask, String userId) {
        // 检查是否为指定的审批人
        if (userId.equals(userTask.getAssignee())) {
            return true;
        }
        
        // 检查是否在候选用户列表中
        if (userTask.getCandidateUsers() != null && userTask.getCandidateUsers().contains(userId)) {
            return true;
        }
        
        // 可以在这里添加更多的权限检查逻辑，比如检查用户组、角色等
        
        return false;
    }

    /**
     * 更新请假申请的状态信息
     * 
     * @param leaveRequest 请假申请
     * @param instance 工作流实例
     */
    private void updateLeaveRequestStatus(LeaveRequest leaveRequest, WorkflowInstanceEntity instance) {
        // 根据工作流实例状态更新请假申请状态
        switch (instance.getStatus()) {
            case RUNNING:
                // 根据当前步骤更新状态
                updateStatusByCurrentStep(leaveRequest, instance);
                break;
            case COMPLETED:
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.APPROVED);
                leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.APPROVE);
                break;
            case CANCELLED:
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.CANCELLED);
                break;
            case TERMINATED:
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.REJECTED);
                leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.REJECT);
                break;
            default:
                break;
        }
        
        leaveRequest.setUpdateTime(instance.getUpdateTime());
    }

    /**
     * 根据当前步骤更新请假申请状态
     * 
     * @param leaveRequest 请假申请
     * @param instance 工作流实例
     */
    private void updateStatusByCurrentStep(LeaveRequest leaveRequest, WorkflowInstanceEntity instance) {
        String currentStepType = instance.getCurrentStepType();
        
        if (StrUtil.isBlank(currentStepType)) {
            return;
        }
        
        switch (currentStepType) {
            case "DIRECT_MANAGER_APPROVAL":
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_DIRECT_MANAGER);
                break;
            case "DEPARTMENT_MANAGER_APPROVAL":
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_DEPARTMENT_MANAGER);
                break;
            case "HR_APPROVAL":
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_HR);
                break;
            case "GENERAL_MANAGER_APPROVAL":
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_GENERAL_MANAGER);
                break;
            default:
                break;
        }
    }
}