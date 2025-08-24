package com.tao.workflow.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体类
 * 用于表示请假申请的业务数据
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@Accessors(chain = true)
public class LeaveRequest {

    /**
     * 请假申请ID
     */
    private String requestId;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请人部门
     */
    private String department;

    /**
     * 请假类型
     */
    private LeaveType leaveType;

    /**
     * 请假开始日期
     */
    private LocalDate startDate;

    /**
     * 请假结束日期
     */
    private LocalDate endDate;

    /**
     * 请假天数
     */
    private Integer leaveDays;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 工作交接说明
     */
    private String workHandover;

    /**
     * 申请时间
     */
    private LocalDateTime applicationTime;

    /**
     * 当前审批状态
     */
    private ApprovalStatus status;

    /**
     * 直属领导ID
     */
    private String directManagerId;

    /**
     * 直属领导姓名
     */
    private String directManagerName;

    /**
     * 部门经理ID
     */
    private String departmentManagerId;

    /**
     * 部门经理姓名
     */
    private String departmentManagerName;

    /**
     * HR负责人ID
     */
    private String hrManagerId;

    /**
     * HR负责人姓名
     */
    private String hrManagerName;

    /**
     * 总经理ID（长期请假需要）
     */
    private String generalManagerId;

    /**
     * 总经理姓名
     */
    private String generalManagerName;

    /**
     * 直属领导审批意见
     */
    private String directManagerComment;

    /**
     * 直属领导审批时间
     */
    private LocalDateTime directManagerApprovalTime;

    /**
     * 部门经理审批意见
     */
    private String departmentManagerComment;

    /**
     * 部门经理审批时间
     */
    private LocalDateTime departmentManagerApprovalTime;

    /**
     * HR审批意见
     */
    private String hrComment;

    /**
     * HR审批时间
     */
    private LocalDateTime hrApprovalTime;

    /**
     * 总经理审批意见
     */
    private String generalManagerComment;

    /**
     * 总经理审批时间
     */
    private LocalDateTime generalManagerApprovalTime;

    /**
     * 最终审批结果
     */
    private ApprovalResult finalResult;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 请假类型枚举
     */
    public enum LeaveType {
        /**
         * 年假
         */
        ANNUAL("年假"),
        
        /**
         * 病假
         */
        SICK("病假"),
        
        /**
         * 事假
         */
        PERSONAL("事假"),
        
        /**
         * 婚假
         */
        MARRIAGE("婚假"),
        
        /**
         * 产假
         */
        MATERNITY("产假"),
        
        /**
         * 陪产假
         */
        PATERNITY("陪产假"),
        
        /**
         * 丧假
         */
        BEREAVEMENT("丧假"),
        
        /**
         * 调休
         */
        COMPENSATORY("调休"),
        
        /**
         * 其他
         */
        OTHER("其他");

        private final String description;

        LeaveType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 审批状态枚举
     */
    public enum ApprovalStatus {
        /**
         * 待提交
         */
        DRAFT("待提交"),
        
        /**
         * 待直属领导审批
         */
        PENDING_DIRECT_MANAGER("待直属领导审批"),
        
        /**
         * 待部门经理审批
         */
        PENDING_DEPARTMENT_MANAGER("待部门经理审批"),
        
        /**
         * 待HR审批
         */
        PENDING_HR("待HR审批"),
        
        /**
         * 待总经理审批
         */
        PENDING_GENERAL_MANAGER("待总经理审批"),
        
        /**
         * 审批通过
         */
        APPROVED("审批通过"),
        
        /**
         * 审批拒绝
         */
        REJECTED("审批拒绝"),
        
        /**
         * 已撤销
         */
        CANCELLED("已撤销");

        private final String description;

        ApprovalStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 审批结果枚举
     */
    public enum ApprovalResult {
        /**
         * 同意
         */
        APPROVE("同意"),
        
        /**
         * 拒绝
         */
        REJECT("拒绝"),
        
        /**
         * 待审批
         */
        PENDING("待审批");

        private final String description;

        ApprovalResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取请假天数（自动计算）
     * 
     * @return 请假天数
     */
    public int calculateLeaveDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        // 简单计算，实际项目中可能需要考虑工作日、节假日等
        return (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }

    /**
     * 判断是否为长期请假（超过7天）
     * 
     * @return 是否为长期请假
     */
    public boolean isLongTermLeave() {
        return calculateLeaveDays() > 7;
    }

    /**
     * 判断是否需要总经理审批
     * 长期请假（超过7天）或特殊假期（婚假、产假、陪产假、丧假）需要总经理审批
     * 
     * @return 是否需要总经理审批
     */
    public boolean requiresGeneralManagerApproval() {
        return isLongTermLeave() || 
               leaveType == LeaveType.MARRIAGE || 
               leaveType == LeaveType.MATERNITY || 
               leaveType == LeaveType.PATERNITY || 
               leaveType == LeaveType.BEREAVEMENT;
    }

    /**
     * 获取当前审批环节描述
     * 
     * @return 当前审批环节描述
     */
    public String getCurrentApprovalStage() {
        if (status == null) {
            return "未知状态";
        }
        return status.getDescription();
    }

    /**
     * 检查是否可以撤销
     * 只有在待审批状态下才能撤销
     * 
     * @return 是否可以撤销
     */
    public boolean canCancel() {
        return status != null && 
               status != ApprovalStatus.APPROVED && 
               status != ApprovalStatus.REJECTED && 
               status != ApprovalStatus.CANCELLED;
    }

    /**
     * 检查是否已完成审批
     * 
     * @return 是否已完成审批
     */
    public boolean isCompleted() {
        return status == ApprovalStatus.APPROVED || 
               status == ApprovalStatus.REJECTED || 
               status == ApprovalStatus.CANCELLED;
    }
}