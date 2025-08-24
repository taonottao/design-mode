package com.tao.workflow.example.controller;

import cn.hutool.core.util.StrUtil;
import com.tao.workflow.common.ApiResponse;
import com.tao.workflow.entity.UserTaskEntity;
import com.tao.workflow.entity.WorkflowInstanceEntity;
import com.tao.workflow.example.entity.LeaveRequest;
import com.tao.workflow.example.service.LeaveApprovalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/**
 * 请假审批控制器
 * 提供请假申请和审批相关的REST API接口
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
@Api(tags = "请假审批管理")
@Validated
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    /**
     * 提交请假申请
     * 
     * @param request 请假申请请求
     * @return 工作流实例ID
     */
    @PostMapping("/submit")
    @ApiOperation("提交请假申请")
    public ApiResponse<String> submitLeaveRequest(
            @Valid @RequestBody SubmitLeaveRequestRequest request) {
        
        log.info("收到请假申请提交请求: {}", request.getApplicantName());
        
        try {
            // 构建请假申请对象
            LeaveRequest leaveRequest = buildLeaveRequest(request);
            
            // 提交请假申请
            String instanceId = leaveApprovalService.submitLeaveRequest(leaveRequest);
            
            return ApiResponse.success(instanceId, "请假申请提交成功");
            
        } catch (Exception e) {
            log.error("提交请假申请失败: {}", e.getMessage(), e);
            return ApiResponse.error("提交请假申请失败: " + e.getMessage());
        }
    }

    /**
     * 审批请假申请
     * 
     * @param request 审批请求
     * @return 审批结果
     */
    @PostMapping("/approve")
    @ApiOperation("审批请假申请")
    public ApiResponse<Boolean> approveLeaveRequest(
            @Valid @RequestBody ApproveLeaveRequestRequest request) {
        
        log.info("收到请假审批请求，任务ID: {}，审批人: {}", request.getTaskId(), request.getApproverId());
        
        try {
            boolean result = leaveApprovalService.approveLeaveRequest(
                    request.getTaskId(),
                    request.getApproverId(),
                    request.getApprovalResult(),
                    request.getApprovalComment()
            );
            
            if (result) {
                return ApiResponse.success(true, "审批完成");
            } else {
                return ApiResponse.error("审批失败");
            }
            
        } catch (Exception e) {
            log.error("审批请假申请失败: {}", e.getMessage(), e);
            return ApiResponse.error("审批失败: " + e.getMessage());
        }
    }

    /**
     * 撤销请假申请
     * 
     * @param request 撤销请求
     * @return 撤销结果
     */
    @PostMapping("/cancel")
    @ApiOperation("撤销请假申请")
    public ApiResponse<Boolean> cancelLeaveRequest(
            @Valid @RequestBody CancelLeaveRequestRequest request) {
        
        log.info("收到请假撤销请求，实例ID: {}，申请人: {}", request.getInstanceId(), request.getApplicantId());
        
        try {
            boolean result = leaveApprovalService.cancelLeaveRequest(
                    request.getInstanceId(),
                    request.getApplicantId(),
                    request.getReason()
            );
            
            if (result) {
                return ApiResponse.success(true, "撤销成功");
            } else {
                return ApiResponse.error("撤销失败");
            }
            
        } catch (Exception e) {
            log.error("撤销请假申请失败: {}", e.getMessage(), e);
            return ApiResponse.error("撤销失败: " + e.getMessage());
        }
    }

    /**
     * 获取请假申请详情
     * 
     * @param instanceId 工作流实例ID
     * @return 请假申请详情
     */
    @GetMapping("/detail")
    @ApiOperation("获取请假申请详情")
    public ApiResponse<LeaveRequest> getLeaveRequestDetail(
            @ApiParam("工作流实例ID") @RequestParam @NotBlank String instanceId) {
        
        log.info("获取请假申请详情，实例ID: {}", instanceId);
        
        try {
            LeaveRequest leaveRequest = leaveApprovalService.getLeaveRequestByInstanceId(instanceId);
            
            if (leaveRequest != null) {
                return ApiResponse.success(leaveRequest, "获取成功");
            } else {
                return ApiResponse.error("请假申请不存在");
            }
            
        } catch (Exception e) {
            log.error("获取请假申请详情失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的待审批任务
     * 
     * @param userId 用户ID
     * @return 待审批任务列表
     */
    @GetMapping("/pending-tasks")
    @ApiOperation("获取用户的待审批任务")
    public ApiResponse<List<UserTaskEntity>> getPendingApprovalTasks(
            @ApiParam("用户ID") @RequestParam @NotBlank String userId) {
        
        log.info("获取用户待审批任务，用户ID: {}", userId);
        
        try {
            List<UserTaskEntity> tasks = leaveApprovalService.getPendingApprovalTasks(userId);
            return ApiResponse.success(tasks, "获取成功");
            
        } catch (Exception e) {
            log.error("获取用户待审批任务失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的请假申请历史
     * 
     * @param applicantId 申请人ID
     * @return 请假申请历史
     */
    @GetMapping("/history")
    @ApiOperation("获取用户的请假申请历史")
    public ApiResponse<List<WorkflowInstanceEntity>> getLeaveRequestHistory(
            @ApiParam("申请人ID") @RequestParam @NotBlank String applicantId) {
        
        log.info("获取用户请假申请历史，申请人ID: {}", applicantId);
        
        try {
            List<WorkflowInstanceEntity> history = leaveApprovalService.getLeaveRequestHistory(applicantId);
            return ApiResponse.success(history, "获取成功");
            
        } catch (Exception e) {
            log.error("获取用户请假申请历史失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 构建请假申请对象
     * 
     * @param request 请假申请请求
     * @return 请假申请对象
     */
    private LeaveRequest buildLeaveRequest(SubmitLeaveRequestRequest request) {
        LeaveRequest leaveRequest = new LeaveRequest();
        
        // 基本信息
        leaveRequest.setApplicantId(request.getApplicantId());
        leaveRequest.setApplicantName(request.getApplicantName());
        leaveRequest.setDepartment(request.getDepartment());
        leaveRequest.setPosition(request.getPosition());
        leaveRequest.setEmployeeNumber(request.getEmployeeNumber());
        
        // 请假信息
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.valueOf(request.getLeaveType()));
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setEmergency(request.isEmergency());
        
        // 联系信息
        leaveRequest.setEmergencyContact(request.getEmergencyContact());
        leaveRequest.setEmergencyPhone(request.getEmergencyPhone());
        
        // 工作交接
        leaveRequest.setWorkHandover(request.getWorkHandover());
        
        // 审批人信息
        leaveRequest.setDirectManagerId(request.getDirectManagerId());
        leaveRequest.setDirectManagerName(request.getDirectManagerName());
        
        if (StrUtil.isNotBlank(request.getDepartmentManagerId())) {
            leaveRequest.setDepartmentManagerId(request.getDepartmentManagerId());
            leaveRequest.setDepartmentManagerName(request.getDepartmentManagerName());
        }
        
        if (StrUtil.isNotBlank(request.getHrManagerId())) {
            leaveRequest.setHrManagerId(request.getHrManagerId());
            leaveRequest.setHrManagerName(request.getHrManagerName());
        }
        
        if (StrUtil.isNotBlank(request.getGeneralManagerId())) {
            leaveRequest.setGeneralManagerId(request.getGeneralManagerId());
            leaveRequest.setGeneralManagerName(request.getGeneralManagerName());
        }
        
        // 初始状态
        leaveRequest.setStatus(LeaveRequest.ApprovalStatus.PENDING_DIRECT_MANAGER);
        
        return leaveRequest;
    }

    /**
     * 提交请假申请请求
     */
    @Data
    public static class SubmitLeaveRequestRequest {
        
        @NotBlank(message = "申请人ID不能为空")
        @ApiParam("申请人ID")
        private String applicantId;
        
        @NotBlank(message = "申请人姓名不能为空")
        @ApiParam("申请人姓名")
        private String applicantName;
        
        @NotBlank(message = "部门不能为空")
        @ApiParam("部门")
        private String department;
        
        @ApiParam("职位")
        private String position;
        
        @ApiParam("员工编号")
        private String employeeNumber;
        
        @NotBlank(message = "请假类型不能为空")
        @ApiParam("请假类型")
        private String leaveType;
        
        @ApiParam("请假开始日期")
        private LocalDate startDate;
        
        @ApiParam("请假结束日期")
        private LocalDate endDate;
        
        @NotBlank(message = "请假原因不能为空")
        @ApiParam("请假原因")
        private String reason;
        
        @ApiParam("是否紧急")
        private boolean emergency = false;
        
        @ApiParam("紧急联系人")
        private String emergencyContact;
        
        @ApiParam("紧急联系电话")
        private String emergencyPhone;
        
        @ApiParam("工作交接说明")
        private String workHandover;
        
        @NotBlank(message = "直属领导ID不能为空")
        @ApiParam("直属领导ID")
        private String directManagerId;
        
        @NotBlank(message = "直属领导姓名不能为空")
        @ApiParam("直属领导姓名")
        private String directManagerName;
        
        @ApiParam("部门经理ID")
        private String departmentManagerId;
        
        @ApiParam("部门经理姓名")
        private String departmentManagerName;
        
        @ApiParam("HR负责人ID")
        private String hrManagerId;
        
        @ApiParam("HR负责人姓名")
        private String hrManagerName;
        
        @ApiParam("总经理ID")
        private String generalManagerId;
        
        @ApiParam("总经理姓名")
        private String generalManagerName;
    }

    /**
     * 审批请假申请请求
     */
    @Data
    public static class ApproveLeaveRequestRequest {
        
        @NotBlank(message = "任务ID不能为空")
        @ApiParam("任务ID")
        private String taskId;
        
        @NotBlank(message = "审批人ID不能为空")
        @ApiParam("审批人ID")
        private String approverId;
        
        @NotBlank(message = "审批结果不能为空")
        @ApiParam("审批结果（APPROVE/REJECT）")
        private String approvalResult;
        
        @ApiParam("审批意见")
        private String approvalComment;
    }

    /**
     * 撤销请假申请请求
     */
    @Data
    public static class CancelLeaveRequestRequest {
        
        @NotBlank(message = "工作流实例ID不能为空")
        @ApiParam("工作流实例ID")
        private String instanceId;
        
        @NotBlank(message = "申请人ID不能为空")
        @ApiParam("申请人ID")
        private String applicantId;
        
        @ApiParam("撤销原因")
        private String reason;
    }
}