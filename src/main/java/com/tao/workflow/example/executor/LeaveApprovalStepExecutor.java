package com.tao.workflow.example.executor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tao.workflow.core.StepExecutor;
import com.tao.workflow.entity.WorkflowInstanceEntity;
import com.tao.workflow.entity.WorkflowStepEntity;
import com.tao.workflow.example.entity.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 请假审批步骤执行器
 * 实现请假审批流程中各个步骤的具体执行逻辑
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Component
public class LeaveApprovalStepExecutor implements StepExecutor {

    /**
     * 执行工作流步骤
     * 
     * @param instance 工作流实例
     * @param step 当前步骤
     * @param context 执行上下文
     * @return 执行结果
     */
    @Override
    public ExecutionResult execute(WorkflowInstanceEntity instance, WorkflowStepEntity step, Map<String, Object> context) {
        log.info("执行请假审批步骤: {} - {}", step.getStepName(), step.getStepType());
        
        try {
            // 从上下文中获取请假申请数据
            LeaveRequest leaveRequest = getLeaveRequestFromContext(context);
            if (leaveRequest == null) {
                log.error("无法从上下文中获取请假申请数据");
                return ExecutionResult.failure("请假申请数据不存在");
            }
            
            // 根据步骤类型执行相应的逻辑
            switch (step.getStepType()) {
                case "START":
                    return executeStartStep(leaveRequest, context);
                case "DIRECT_MANAGER_APPROVAL":
                    return executeDirectManagerApproval(leaveRequest, context);
                case "DEPARTMENT_MANAGER_APPROVAL":
                    return executeDepartmentManagerApproval(leaveRequest, context);
                case "HR_APPROVAL":
                    return executeHrApproval(leaveRequest, context);
                case "GENERAL_MANAGER_APPROVAL":
                    return executeGeneralManagerApproval(leaveRequest, context);
                case "END":
                    return executeEndStep(leaveRequest, context);
                default:
                    log.warn("未知的步骤类型: {}", step.getStepType());
                    return ExecutionResult.failure("未知的步骤类型: " + step.getStepType());
            }
        } catch (Exception e) {
            log.error("执行请假审批步骤失败: {}", e.getMessage(), e);
            return ExecutionResult.failure("步骤执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行开始步骤
     * 初始化请假申请，设置初始状态
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeStartStep(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行开始步骤，申请人: {}", leaveRequest.getApplicantName());
        
        // 设置申请时间
        leaveRequest.setApplicationTime(LocalDateTime.now());
        leaveRequest.setCreateTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        // 计算请假天数
        int leaveDays = leaveRequest.calculateLeaveDays();
        leaveRequest.setLeaveDays(leaveDays);
        
        // 设置初始状态为待直属领导审批
        leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_DIRECT_MANAGER);
        leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.PENDING);
        
        // 更新上下文
        updateLeaveRequestInContext(context, leaveRequest);
        
        log.info("请假申请初始化完成，请假天数: {} 天，状态: {}", leaveDays, leaveRequest.getStatus().getDescription());
        
        return ExecutionResult.success("请假申请提交成功，等待直属领导审批");
    }

    /**
     * 执行直属领导审批步骤
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeDirectManagerApproval(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行直属领导审批步骤，审批人: {}", leaveRequest.getDirectManagerName());
        
        // 从上下文中获取审批结果
        String approvalResult = (String) context.get("approvalResult");
        String approvalComment = (String) context.get("approvalComment");
        
        if (StrUtil.isBlank(approvalResult)) {
            log.warn("直属领导审批结果为空");
            return ExecutionResult.waiting("等待直属领导审批");
        }
        
        // 记录审批信息
        leaveRequest.setDirectManagerComment(approvalComment);
        leaveRequest.setDirectManagerApprovalTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        if ("APPROVE".equals(approvalResult)) {
            log.info("直属领导审批通过");
            
            // 判断下一步审批流程
            if (leaveRequest.getLeaveDays() <= 3) {
                // 3天以内的请假，直属领导审批后直接通过
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.APPROVED);
                leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.APPROVE);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("直属领导审批通过，请假申请已批准");
            } else {
                // 超过3天的请假，需要部门经理审批
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_DEPARTMENT_MANAGER);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("直属领导审批通过，转至部门经理审批");
            }
        } else if ("REJECT".equals(approvalResult)) {
            log.info("直属领导审批拒绝");
            
            leaveRequest.setStatus(LeaveRequest.ApprovalStatus.REJECTED);
            leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.REJECT);
            leaveRequest.setRejectionReason(approvalComment);
            
            updateLeaveRequestInContext(context, leaveRequest);
            return ExecutionResult.success("直属领导审批拒绝，请假申请被驳回");
        } else {
            log.warn("无效的审批结果: {}", approvalResult);
            return ExecutionResult.failure("无效的审批结果: " + approvalResult);
        }
    }

    /**
     * 执行部门经理审批步骤
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeDepartmentManagerApproval(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行部门经理审批步骤，审批人: {}", leaveRequest.getDepartmentManagerName());
        
        // 从上下文中获取审批结果
        String approvalResult = (String) context.get("approvalResult");
        String approvalComment = (String) context.get("approvalComment");
        
        if (StrUtil.isBlank(approvalResult)) {
            log.warn("部门经理审批结果为空");
            return ExecutionResult.waiting("等待部门经理审批");
        }
        
        // 记录审批信息
        leaveRequest.setDepartmentManagerComment(approvalComment);
        leaveRequest.setDepartmentManagerApprovalTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        if ("APPROVE".equals(approvalResult)) {
            log.info("部门经理审批通过");
            
            // 判断是否需要HR审批
            if (leaveRequest.requiresGeneralManagerApproval()) {
                // 需要总经理审批的情况，先转HR审批
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_HR);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("部门经理审批通过，转至HR审批");
            } else if (leaveRequest.getLeaveDays() > 5) {
                // 超过5天但不需要总经理审批的，需要HR审批
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_HR);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("部门经理审批通过，转至HR审批");
            } else {
                // 3-5天的请假，部门经理审批后直接通过
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.APPROVED);
                leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.APPROVE);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("部门经理审批通过，请假申请已批准");
            }
        } else if ("REJECT".equals(approvalResult)) {
            log.info("部门经理审批拒绝");
            
            leaveRequest.setStatus(LeaveRequest.ApprovalStatus.REJECTED);
            leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.REJECT);
            leaveRequest.setRejectionReason(approvalComment);
            
            updateLeaveRequestInContext(context, leaveRequest);
            return ExecutionResult.success("部门经理审批拒绝，请假申请被驳回");
        } else {
            log.warn("无效的审批结果: {}", approvalResult);
            return ExecutionResult.failure("无效的审批结果: " + approvalResult);
        }
    }

    /**
     * 执行HR审批步骤
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeHrApproval(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行HR审批步骤，审批人: {}", leaveRequest.getHrManagerName());
        
        // 从上下文中获取审批结果
        String approvalResult = (String) context.get("approvalResult");
        String approvalComment = (String) context.get("approvalComment");
        
        if (StrUtil.isBlank(approvalResult)) {
            log.warn("HR审批结果为空");
            return ExecutionResult.waiting("等待HR审批");
        }
        
        // 记录审批信息
        leaveRequest.setHrComment(approvalComment);
        leaveRequest.setHrApprovalTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        if ("APPROVE".equals(approvalResult)) {
            log.info("HR审批通过");
            
            // 判断是否需要总经理审批
            if (leaveRequest.requiresGeneralManagerApproval()) {
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_GENERAL_MANAGER);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("HR审批通过，转至总经理审批");
            } else {
                // 不需要总经理审批，HR审批后直接通过
                leaveRequest.setStatus(LeaveRequest.ApprovalStatus.APPROVED);
                leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.APPROVE);
                
                updateLeaveRequestInContext(context, leaveRequest);
                return ExecutionResult.success("HR审批通过，请假申请已批准");
            }
        } else if ("REJECT".equals(approvalResult)) {
            log.info("HR审批拒绝");
            
            leaveRequest.setStatus(LeaveRequest.ApprovalStatus.REJECTED);
            leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.REJECT);
            leaveRequest.setRejectionReason(approvalComment);
            
            updateLeaveRequestInContext(context, leaveRequest);
            return ExecutionResult.success("HR审批拒绝，请假申请被驳回");
        } else {
            log.warn("无效的审批结果: {}", approvalResult);
            return ExecutionResult.failure("无效的审批结果: " + approvalResult);
        }
    }

    /**
     * 执行总经理审批步骤
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeGeneralManagerApproval(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行总经理审批步骤，审批人: {}", leaveRequest.getGeneralManagerName());
        
        // 从上下文中获取审批结果
        String approvalResult = (String) context.get("approvalResult");
        String approvalComment = (String) context.get("approvalComment");
        
        if (StrUtil.isBlank(approvalResult)) {
            log.warn("总经理审批结果为空");
            return ExecutionResult.waiting("等待总经理审批");
        }
        
        // 记录审批信息
        leaveRequest.setGeneralManagerComment(approvalComment);
        leaveRequest.setGeneralManagerApprovalTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        if ("APPROVE".equals(approvalResult)) {
            log.info("总经理审批通过");
            
            leaveRequest.setStatus(LeaveRequest.ApprovalStatus.APPROVED);
            leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.APPROVE);
            
            updateLeaveRequestInContext(context, leaveRequest);
            return ExecutionResult.success("总经理审批通过，请假申请已批准");
        } else if ("REJECT".equals(approvalResult)) {
            log.info("总经理审批拒绝");
            
            leaveRequest.setStatus(LeaveRequest.ApprovalStatus.REJECTED);
            leaveRequest.setFinalResult(LeaveRequest.ApprovalResult.REJECT);
            leaveRequest.setRejectionReason(approvalComment);
            
            updateLeaveRequestInContext(context, leaveRequest);
            return ExecutionResult.success("总经理审批拒绝，请假申请被驳回");
        } else {
            log.warn("无效的审批结果: {}", approvalResult);
            return ExecutionResult.failure("无效的审批结果: " + approvalResult);
        }
    }

    /**
     * 执行结束步骤
     * 完成请假审批流程
     * 
     * @param leaveRequest 请假申请
     * @param context 执行上下文
     * @return 执行结果
     */
    private ExecutionResult executeEndStep(LeaveRequest leaveRequest, Map<String, Object> context) {
        log.info("执行结束步骤，最终结果: {}", leaveRequest.getFinalResult().getDescription());
        
        // 设置最终更新时间
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        // 更新上下文
        updateLeaveRequestInContext(context, leaveRequest);
        
        // 根据最终结果返回相应的消息
        if (leaveRequest.getFinalResult() == LeaveRequest.ApprovalResult.APPROVE) {
            return ExecutionResult.success("请假申请审批完成，申请已通过");
        } else if (leaveRequest.getFinalResult() == LeaveRequest.ApprovalResult.REJECT) {
            return ExecutionResult.success("请假申请审批完成，申请被拒绝");
        } else {
            return ExecutionResult.success("请假申请审批流程结束");
        }
    }

    /**
     * 从上下文中获取请假申请数据
     * 
     * @param context 执行上下文
     * @return 请假申请
     */
    private LeaveRequest getLeaveRequestFromContext(Map<String, Object> context) {
        Object leaveRequestObj = context.get("leaveRequest");
        if (leaveRequestObj == null) {
            return null;
        }
        
        if (leaveRequestObj instanceof LeaveRequest) {
            return (LeaveRequest) leaveRequestObj;
        } else if (leaveRequestObj instanceof String) {
            // 如果是JSON字符串，则反序列化
            try {
                return JSONUtil.toBean((String) leaveRequestObj, LeaveRequest.class);
            } catch (Exception e) {
                log.error("反序列化请假申请数据失败: {}", e.getMessage(), e);
                return null;
            }
        } else {
            log.warn("无效的请假申请数据类型: {}", leaveRequestObj.getClass().getName());
            return null;
        }
    }

    /**
     * 更新上下文中的请假申请数据
     * 
     * @param context 执行上下文
     * @param leaveRequest 请假申请
     */
    private void updateLeaveRequestInContext(Map<String, Object> context, LeaveRequest leaveRequest) {
        context.put("leaveRequest", leaveRequest);
        context.put("leaveRequestJson", JSONUtil.toJsonStr(leaveRequest));
        context.put("currentStatus", leaveRequest.getStatus().name());
        context.put("finalResult", leaveRequest.getFinalResult().name());
    }

    /**
     * 检查是否支持指定的步骤类型
     * 
     * @param stepType 步骤类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String stepType) {
        return "START".equals(stepType) ||
               "DIRECT_MANAGER_APPROVAL".equals(stepType) ||
               "DEPARTMENT_MANAGER_APPROVAL".equals(stepType) ||
               "HR_APPROVAL".equals(stepType) ||
               "GENERAL_MANAGER_APPROVAL".equals(stepType) ||
               "END".equals(stepType);
    }
}