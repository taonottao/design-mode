package com.tao.workflow.example.factory;

import com.tao.workflow.builder.WorkflowBuilder;
import com.tao.workflow.entity.WorkflowDefinitionEntity;
import com.tao.workflow.example.entity.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 请假审批工作流工厂
 * 用于构建请假审批工作流的定义
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Component
public class LeaveApprovalWorkflowFactory {

    /**
     * 创建请假审批工作流定义
     * 
     * @return 工作流定义
     */
    public WorkflowDefinitionEntity createLeaveApprovalWorkflow() {
        log.info("开始创建请假审批工作流定义");
        
        try {
            WorkflowDefinitionEntity workflow = WorkflowBuilder.create()
                    .name("请假审批流程")
                    .description("员工请假申请的审批工作流，支持多级审批")
                    .version("1.0.0")
                    .category("人事管理")
                    
                    // 开始节点
                    .startStep()
                        .name("提交申请")
                        .description("员工提交请假申请")
                        .stepType("START")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("formRequired", "true")
                        .addProperty("autoExecute", "true")
                    .endStep()
                    
                    // 直属领导审批节点
                    .userTaskStep()
                        .name("直属领导审批")
                        .description("直属领导审批请假申请")
                        .stepType("DIRECT_MANAGER_APPROVAL")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .assigneeExpression("${leaveRequest.directManagerId}")
                        .candidateGroups("DIRECT_MANAGER")
                        .dueDate("P1D") // 1天内完成
                        .priority("HIGH")
                        .addProperty("approvalRequired", "true")
                        .addProperty("commentRequired", "false")
                        .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                        .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                    .endStep()
                    
                    // 条件网关：判断是否需要部门经理审批
                    .exclusiveGateway()
                        .name("审批路径判断")
                        .description("根据请假天数判断审批路径")
                        .stepType("EXCLUSIVE_GATEWAY")
                        
                        // 条件1：3天以内，直接结束
                        .condition("${leaveRequest.leaveDays <= 3 && approvalResult == 'APPROVE'}")
                            .targetStep("审批完成")
                            .description("3天以内请假，直属领导审批通过后直接完成")
                        .endCondition()
                        
                        // 条件2：超过3天，需要部门经理审批
                        .condition("${leaveRequest.leaveDays > 3 && approvalResult == 'APPROVE'}")
                            .targetStep("部门经理审批")
                            .description("超过3天请假，需要部门经理审批")
                        .endCondition()
                        
                        // 条件3：直属领导拒绝，直接结束
                        .condition("${approvalResult == 'REJECT'}")
                            .targetStep("审批完成")
                            .description("直属领导拒绝，流程结束")
                        .endCondition()
                    .endGateway()
                    
                    // 部门经理审批节点
                    .userTaskStep()
                        .name("部门经理审批")
                        .description("部门经理审批请假申请")
                        .stepType("DEPARTMENT_MANAGER_APPROVAL")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .assigneeExpression("${leaveRequest.departmentManagerId}")
                        .candidateGroups("DEPARTMENT_MANAGER")
                        .dueDate("P2D") // 2天内完成
                        .priority("HIGH")
                        .addProperty("approvalRequired", "true")
                        .addProperty("commentRequired", "false")
                        .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                        .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                    .endStep()
                    
                    // 条件网关：判断是否需要HR审批
                    .exclusiveGateway()
                        .name("HR审批路径判断")
                        .description("根据请假天数和类型判断是否需要HR审批")
                        .stepType("EXCLUSIVE_GATEWAY")
                        
                        // 条件1：3-5天且部门经理通过，直接结束
                        .condition("${leaveRequest.leaveDays <= 5 && !leaveRequest.requiresGeneralManagerApproval() && approvalResult == 'APPROVE'}")
                            .targetStep("审批完成")
                            .description("3-5天请假，部门经理审批通过后直接完成")
                        .endCondition()
                        
                        // 条件2：超过5天或特殊假期，需要HR审批
                        .condition("${(leaveRequest.leaveDays > 5 || leaveRequest.requiresGeneralManagerApproval()) && approvalResult == 'APPROVE'}")
                            .targetStep("HR审批")
                            .description("超过5天或特殊假期，需要HR审批")
                        .endCondition()
                        
                        // 条件3：部门经理拒绝，直接结束
                        .condition("${approvalResult == 'REJECT'}")
                            .targetStep("审批完成")
                            .description("部门经理拒绝，流程结束")
                        .endCondition()
                    .endGateway()
                    
                    // HR审批节点
                    .userTaskStep()
                        .name("HR审批")
                        .description("HR审批请假申请")
                        .stepType("HR_APPROVAL")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .assigneeExpression("${leaveRequest.hrManagerId}")
                        .candidateGroups("HR_MANAGER")
                        .dueDate("P3D") // 3天内完成
                        .priority("MEDIUM")
                        .addProperty("approvalRequired", "true")
                        .addProperty("commentRequired", "false")
                        .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                        .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                    .endStep()
                    
                    // 条件网关：判断是否需要总经理审批
                    .exclusiveGateway()
                        .name("总经理审批路径判断")
                        .description("根据请假类型判断是否需要总经理审批")
                        .stepType("EXCLUSIVE_GATEWAY")
                        
                        // 条件1：不需要总经理审批且HR通过，直接结束
                        .condition("${!leaveRequest.requiresGeneralManagerApproval() && approvalResult == 'APPROVE'}")
                            .targetStep("审批完成")
                            .description("HR审批通过，无需总经理审批")
                        .endCondition()
                        
                        // 条件2：需要总经理审批且HR通过
                        .condition("${leaveRequest.requiresGeneralManagerApproval() && approvalResult == 'APPROVE'}")
                            .targetStep("总经理审批")
                            .description("需要总经理审批")
                        .endCondition()
                        
                        // 条件3：HR拒绝，直接结束
                        .condition("${approvalResult == 'REJECT'}")
                            .targetStep("审批完成")
                            .description("HR拒绝，流程结束")
                        .endCondition()
                    .endGateway()
                    
                    // 总经理审批节点
                    .userTaskStep()
                        .name("总经理审批")
                        .description("总经理审批请假申请（长期请假或特殊假期）")
                        .stepType("GENERAL_MANAGER_APPROVAL")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .assigneeExpression("${leaveRequest.generalManagerId}")
                        .candidateGroups("GENERAL_MANAGER")
                        .dueDate("P5D") // 5天内完成
                        .priority("HIGH")
                        .addProperty("approvalRequired", "true")
                        .addProperty("commentRequired", "false")
                        .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                        .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                    .endStep()
                    
                    // 结束节点
                    .endStep()
                        .name("审批完成")
                        .description("请假审批流程完成")
                        .stepType("END")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("autoExecute", "true")
                        .addProperty("notificationRequired", "true")
                    .endStep()
                    
                    .build();
            
            log.info("请假审批工作流定义创建完成，包含 {} 个步骤", workflow.getSteps().size());
            return workflow;
            
        } catch (Exception e) {
            log.error("创建请假审批工作流定义失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建请假审批工作流定义失败", e);
        }
    }

    /**
     * 创建简化版请假审批工作流（仅直属领导审批）
     * 适用于短期请假或特殊情况
     * 
     * @return 工作流定义
     */
    public WorkflowDefinitionEntity createSimpleLeaveApprovalWorkflow() {
        log.info("开始创建简化版请假审批工作流定义");
        
        try {
            WorkflowDefinitionEntity workflow = WorkflowBuilder.create()
                    .name("简化请假审批流程")
                    .description("简化的员工请假申请审批工作流，仅需直属领导审批")
                    .version("1.0.0")
                    .category("人事管理")
                    
                    // 开始节点
                    .startStep()
                        .name("提交申请")
                        .description("员工提交请假申请")
                        .stepType("START")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("formRequired", "true")
                        .addProperty("autoExecute", "true")
                    .endStep()
                    
                    // 直属领导审批节点
                    .userTaskStep()
                        .name("直属领导审批")
                        .description("直属领导审批请假申请")
                        .stepType("DIRECT_MANAGER_APPROVAL")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .assigneeExpression("${leaveRequest.directManagerId}")
                        .candidateGroups("DIRECT_MANAGER")
                        .dueDate("P1D") // 1天内完成
                        .priority("HIGH")
                        .addProperty("approvalRequired", "true")
                        .addProperty("commentRequired", "false")
                        .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                        .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                    .endStep()
                    
                    // 结束节点
                    .endStep()
                        .name("审批完成")
                        .description("请假审批流程完成")
                        .stepType("END")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("autoExecute", "true")
                        .addProperty("notificationRequired", "true")
                    .endStep()
                    
                    .build();
            
            log.info("简化版请假审批工作流定义创建完成，包含 {} 个步骤", workflow.getSteps().size());
            return workflow;
            
        } catch (Exception e) {
            log.error("创建简化版请假审批工作流定义失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建简化版请假审批工作流定义失败", e);
        }
    }

    /**
     * 创建紧急请假审批工作流
     * 适用于紧急情况下的请假申请，简化审批流程
     * 
     * @return 工作流定义
     */
    public WorkflowDefinitionEntity createEmergencyLeaveApprovalWorkflow() {
        log.info("开始创建紧急请假审批工作流定义");
        
        try {
            WorkflowDefinitionEntity workflow = WorkflowBuilder.create()
                    .name("紧急请假审批流程")
                    .description("紧急情况下的员工请假申请审批工作流，优先处理")
                    .version("1.0.0")
                    .category("人事管理")
                    
                    // 开始节点
                    .startStep()
                        .name("提交紧急申请")
                        .description("员工提交紧急请假申请")
                        .stepType("START")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("formRequired", "true")
                        .addProperty("autoExecute", "true")
                        .addProperty("emergency", "true")
                    .endStep()
                    
                    // 并行网关：同时通知直属领导和HR
                    .parallelGateway()
                        .name("并行审批")
                        .description("同时进行直属领导和HR审批")
                        .stepType("PARALLEL_GATEWAY")
                        
                        // 分支1：直属领导审批
                        .parallelBranch()
                            .userTaskStep()
                                .name("直属领导紧急审批")
                                .description("直属领导紧急审批请假申请")
                                .stepType("DIRECT_MANAGER_APPROVAL")
                                .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                                .assigneeExpression("${leaveRequest.directManagerId}")
                                .candidateGroups("DIRECT_MANAGER")
                                .dueDate("PT4H") // 4小时内完成
                                .priority("URGENT")
                                .addProperty("approvalRequired", "true")
                                .addProperty("emergency", "true")
                                .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                                .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                            .endStep()
                        .endBranch()
                        
                        // 分支2：HR审批
                        .parallelBranch()
                            .userTaskStep()
                                .name("HR紧急审批")
                                .description("HR紧急审批请假申请")
                                .stepType("HR_APPROVAL")
                                .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                                .assigneeExpression("${leaveRequest.hrManagerId}")
                                .candidateGroups("HR_MANAGER")
                                .dueDate("PT4H") // 4小时内完成
                                .priority("URGENT")
                                .addProperty("approvalRequired", "true")
                                .addProperty("emergency", "true")
                                .addFormField("approvalResult", "审批结果", "ENUM", "APPROVE,REJECT", true)
                                .addFormField("approvalComment", "审批意见", "TEXT", null, false)
                            .endStep()
                        .endBranch()
                    .endGateway()
                    
                    // 汇聚网关：等待所有审批完成
                    .parallelGateway()
                        .name("审批汇聚")
                        .description("等待所有审批完成")
                        .stepType("PARALLEL_GATEWAY")
                        .gatewayType("CONVERGING")
                    .endGateway()
                    
                    // 结束节点
                    .endStep()
                        .name("紧急审批完成")
                        .description("紧急请假审批流程完成")
                        .stepType("END")
                        .executorClass("com.tao.workflow.example.executor.LeaveApprovalStepExecutor")
                        .addProperty("autoExecute", "true")
                        .addProperty("notificationRequired", "true")
                        .addProperty("emergency", "true")
                    .endStep()
                    
                    .build();
            
            log.info("紧急请假审批工作流定义创建完成，包含 {} 个步骤", workflow.getSteps().size());
            return workflow;
            
        } catch (Exception e) {
            log.error("创建紧急请假审批工作流定义失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建紧急请假审批工作流定义失败", e);
        }
    }

    /**
     * 根据请假申请自动选择合适的工作流
     * 
     * @param leaveRequest 请假申请
     * @return 工作流定义
     */
    public WorkflowDefinitionEntity selectWorkflowForLeaveRequest(LeaveRequest leaveRequest) {
        log.info("为请假申请选择合适的工作流，申请人: {}，请假天数: {}，请假类型: {}", 
                leaveRequest.getApplicantName(), leaveRequest.getLeaveDays(), leaveRequest.getLeaveType());
        
        // 判断是否为紧急请假（可以根据业务规则定义）
        boolean isEmergency = isEmergencyLeave(leaveRequest);
        
        if (isEmergency) {
            log.info("选择紧急请假审批流程");
            return createEmergencyLeaveApprovalWorkflow();
        } else if (leaveRequest.getLeaveDays() <= 1) {
            log.info("选择简化请假审批流程");
            return createSimpleLeaveApprovalWorkflow();
        } else {
            log.info("选择标准请假审批流程");
            return createLeaveApprovalWorkflow();
        }
    }

    /**
     * 判断是否为紧急请假
     * 
     * @param leaveRequest 请假申请
     * @return 是否为紧急请假
     */
    private boolean isEmergency(LeaveRequest leaveRequest) {
        // 病假且当天申请当天生效
        if (leaveRequest.getLeaveType() == LeaveRequest.LeaveType.SICK &&
            leaveRequest.getStartDate().equals(leaveRequest.getApplicationTime().toLocalDate())) {
            return true;
        }
        
        // 丧假
        if (leaveRequest.getLeaveType() == LeaveRequest.LeaveType.BEREAVEMENT) {
            return true;
        }
        
        // 其他紧急情况可以在这里添加判断逻辑
        
        return false;
    }
}