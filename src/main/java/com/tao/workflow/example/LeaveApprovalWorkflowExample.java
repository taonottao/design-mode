package com.tao.workflow.example;

import com.tao.workflow.builder.WorkflowBuilder;
import com.tao.workflow.core.Workflow;
import com.tao.workflow.core.WorkflowInstance;
import com.tao.workflow.engine.WorkflowEngine;
import com.tao.workflow.engine.impl.DefaultWorkflowEngine;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 请假审批工作流示例
 * 演示如何使用工作流引擎创建和执行一个完整的请假审批流程
 * 
 * 流程说明：
 * 1. 员工提交请假申请
 * 2. 直属主管审批
 * 3. 根据请假天数判断是否需要HR审批
 * 4. HR审批（如果需要）
 * 5. 流程结束，发送通知
 * 
 * @author tao
 * @since 2024-01-15
 */
@Component
public class LeaveApprovalWorkflowExample {

    private final WorkflowEngine workflowEngine;

    public LeaveApprovalWorkflowExample() {
        this.workflowEngine = new DefaultWorkflowEngine();
    }

    /**
     * 创建请假审批工作流定义
     * 
     * @return 工作流定义
     */
    public Workflow createLeaveApprovalWorkflow() {
        return WorkflowBuilder.create("leave-approval", "请假审批流程")
                .description("员工请假申请审批流程，支持多级审批")
                .version("1.0")
                
                // 开始节点
                .startStep("start")
                    .name("流程开始")
                    .description("请假申请流程开始")
                    .next("submit-application")
                    .build()
                
                // 员工提交申请
                .userTaskStep("submit-application")
                    .name("提交请假申请")
                    .description("员工填写请假申请表单")
                    .formKey("leave-application-form")
                    .assignee("${applicant}")
                    .priority("MEDIUM")
                    .dueDate("${now() + 1d}")
                    .next("manager-approval")
                    .build()
                
                // 直属主管审批
                .userTaskStep("manager-approval")
                    .name("主管审批")
                    .description("直属主管审批请假申请")
                    .formKey("manager-approval-form")
                    .assignee("${applicant.manager}")
                    .priority("HIGH")
                    .dueDate("${now() + 2d}")
                    .next("check-leave-days")
                    .build()
                
                // 判断请假天数
                .conditionalStep("check-leave-days")
                    .name("检查请假天数")
                    .description("根据请假天数决定是否需要HR审批")
                    .condition("${leaveDays > 3}", "hr-approval")
                    .condition("${leaveDays <= 3}", "send-approval-notification")
                    .defaultNext("send-approval-notification")
                    .build()
                
                // HR审批
                .userTaskStep("hr-approval")
                    .name("HR审批")
                    .description("人力资源部门审批长期请假申请")
                    .formKey("hr-approval-form")
                    .candidateGroups("hr-group")
                    .priority("HIGH")
                    .dueDate("${now() + 3d}")
                    .next("send-approval-notification")
                    .build()
                
                // 发送审批通知
                .taskStep("send-approval-notification")
                    .name("发送审批通知")
                    .description("向申请人发送审批结果通知")
                    .taskType("email")
                    .parameter("to", "${applicant.email}")
                    .parameter("subject", "请假申请审批结果")
                    .parameter("template", "leave-approval-result")
                    .next("end")
                    .build()
                
                // 结束节点
                .endStep("end")
                    .name("流程结束")
                    .description("请假审批流程结束")
                    .build()
                
                .build();
    }

    /**
     * 创建请假申请拒绝流程
     * 当任何一个审批环节被拒绝时执行
     * 
     * @return 拒绝流程的工作流定义
     */
    public Workflow createLeaveRejectionWorkflow() {
        return WorkflowBuilder.create("leave-rejection", "请假申请拒绝流程")
                .description("处理请假申请被拒绝的情况")
                .version("1.0")
                
                .startStep("start")
                    .name("拒绝流程开始")
                    .next("send-rejection-notification")
                    .build()
                
                .taskStep("send-rejection-notification")
                    .name("发送拒绝通知")
                    .description("向申请人发送请假申请被拒绝的通知")
                    .taskType("email")
                    .parameter("to", "${applicant.email}")
                    .parameter("subject", "请假申请被拒绝")
                    .parameter("template", "leave-rejection-notice")
                    .next("end")
                    .build()
                
                .endStep("end")
                    .name("拒绝流程结束")
                    .build()
                
                .build();
    }

    /**
     * 启动请假审批流程
     * 
     * @param applicantId 申请人ID
     * @param managerId 主管ID
     * @param leaveDays 请假天数
     * @param leaveReason 请假原因
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工作流实例
     */
    public WorkflowInstance startLeaveApprovalProcess(
            String applicantId,
            String managerId,
            int leaveDays,
            String leaveReason,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        // 创建工作流定义
        Workflow workflow = createLeaveApprovalWorkflow();
        
        // 注册工作流定义到引擎
        workflowEngine.deployWorkflow(workflow);
        
        // 准备流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", applicantId);
        variables.put("applicant.manager", managerId);
        variables.put("applicant.email", applicantId + "@company.com");
        variables.put("leaveDays", leaveDays);
        variables.put("leaveReason", leaveReason);
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        variables.put("applicationDate", LocalDateTime.now());
        
        // 启动工作流实例
        return workflowEngine.startWorkflow(workflow.getId(), applicantId, variables);
    }

    /**
     * 主管审批请假申请
     * 
     * @param instanceId 工作流实例ID
     * @param managerId 主管ID
     * @param approved 是否批准
     * @param comments 审批意见
     */
    public void managerApproval(String instanceId, String managerId, boolean approved, String comments) {
        // 准备审批结果数据
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("managerApproved", approved);
        taskData.put("managerComments", comments);
        taskData.put("managerApprovalDate", LocalDateTime.now());
        
        if (approved) {
            // 批准：继续流程
            workflowEngine.completeUserTask(instanceId, "manager-approval", managerId, taskData);
        } else {
            // 拒绝：启动拒绝流程
            startRejectionProcess(instanceId, "主管拒绝：" + comments);
        }
    }

    /**
     * HR审批请假申请
     * 
     * @param instanceId 工作流实例ID
     * @param hrUserId HR用户ID
     * @param approved 是否批准
     * @param comments 审批意见
     */
    public void hrApproval(String instanceId, String hrUserId, boolean approved, String comments) {
        // 准备审批结果数据
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("hrApproved", approved);
        taskData.put("hrComments", comments);
        taskData.put("hrApprovalDate", LocalDateTime.now());
        
        if (approved) {
            // 批准：继续流程
            workflowEngine.completeUserTask(instanceId, "hr-approval", hrUserId, taskData);
        } else {
            // 拒绝：启动拒绝流程
            startRejectionProcess(instanceId, "HR拒绝：" + comments);
        }
    }

    /**
     * 启动拒绝流程
     * 
     * @param originalInstanceId 原始流程实例ID
     * @param rejectionReason 拒绝原因
     */
    private void startRejectionProcess(String originalInstanceId, String rejectionReason) {
        // 终止原始流程
        workflowEngine.terminateWorkflow(originalInstanceId);
        
        // 获取原始流程的变量
        WorkflowInstance originalInstance = workflowEngine.getWorkflowInstance(originalInstanceId);
        Map<String, Object> originalVariables = originalInstance.getVariables();
        
        // 创建拒绝流程的变量
        Map<String, Object> rejectionVariables = new HashMap<>(originalVariables);
        rejectionVariables.put("rejectionReason", rejectionReason);
        rejectionVariables.put("rejectionDate", LocalDateTime.now());
        rejectionVariables.put("originalInstanceId", originalInstanceId);
        
        // 创建并部署拒绝流程
        Workflow rejectionWorkflow = createLeaveRejectionWorkflow();
        workflowEngine.deployWorkflow(rejectionWorkflow);
        
        // 启动拒绝流程
        String applicant = (String) originalVariables.get("applicant");
        workflowEngine.startWorkflow(rejectionWorkflow.getId(), applicant, rejectionVariables);
    }

    /**
     * 获取用户的待办任务
     * 
     * @param userId 用户ID
     * @return 待办任务列表
     */
    public Map<String, Object> getUserPendingTasks(String userId) {
        // 这里应该调用UserTaskService来获取用户的待办任务
        // 为了示例，我们返回一个模拟的结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("pendingTasks", "调用UserTaskService获取待办任务");
        return result;
    }

    /**
     * 查询流程实例状态
     * 
     * @param instanceId 实例ID
     * @return 实例状态信息
     */
    public Map<String, Object> getInstanceStatus(String instanceId) {
        WorkflowInstance instance = workflowEngine.getWorkflowInstance(instanceId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("instanceId", instance.getId());
        status.put("workflowName", instance.getWorkflowName());
        status.put("status", instance.getStatus());
        status.put("currentStep", instance.getCurrentStepId());
        status.put("startedBy", instance.getStartedBy());
        status.put("startedAt", instance.getStartedAt());
        status.put("variables", instance.getVariables());
        
        return status;
    }

    /**
     * 演示完整的请假审批流程
     * 这个方法展示了如何使用工作流引擎执行一个完整的业务流程
     */
    public void demonstrateLeaveApprovalProcess() {
        System.out.println("=== 请假审批流程演示 ===");
        
        // 1. 启动请假申请
        System.out.println("1. 员工张三提交请假申请...");
        WorkflowInstance instance = startLeaveApprovalProcess(
                "zhangsan",
                "manager001",
                5, // 5天假期，需要HR审批
                "家庭事务",
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().plusDays(12)
        );
        
        System.out.println("   流程实例ID: " + instance.getId());
        System.out.println("   当前状态: " + instance.getStatus());
        System.out.println("   当前步骤: " + instance.getCurrentStepId());
        
        // 2. 主管审批
        System.out.println("\n2. 主管审批请假申请...");
        managerApproval(instance.getId(), "manager001", true, "同意请假，注意工作交接");
        
        // 3. HR审批（因为超过3天）
        System.out.println("\n3. HR审批长期请假申请...");
        hrApproval(instance.getId(), "hr001", true, "批准长期请假申请");
        
        // 4. 查看最终状态
        System.out.println("\n4. 流程执行完成，查看最终状态...");
        Map<String, Object> finalStatus = getInstanceStatus(instance.getId());
        System.out.println("   最终状态: " + finalStatus);
        
        System.out.println("\n=== 演示完成 ===");
    }

    /**
     * 演示请假申请被拒绝的情况
     */
    public void demonstrateLeaveRejectionProcess() {
        System.out.println("=== 请假申请拒绝流程演示 ===");
        
        // 1. 启动请假申请
        System.out.println("1. 员工李四提交请假申请...");
        WorkflowInstance instance = startLeaveApprovalProcess(
                "lisi",
                "manager002",
                2, // 2天假期
                "个人事务",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(5)
        );
        
        System.out.println("   流程实例ID: " + instance.getId());
        
        // 2. 主管拒绝申请
        System.out.println("\n2. 主管拒绝请假申请...");
        managerApproval(instance.getId(), "manager002", false, "当前项目紧急，无法批准请假");
        
        System.out.println("\n=== 拒绝流程演示完成 ===");
    }
}