package com.tao.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * 用户任务实体类
 * 对应数据库表：user_task
 * 存储需要人工处理的工作流任务信息
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true) // 支持链式调用
@TableName("user_task")
public class UserTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户任务ID
     * 主键，每个用户任务的唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 工作流实例ID
     * 关联到workflow_instance表的主键
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 步骤ID
     * 工作流定义中步骤的唯一标识
     */
    @TableField("step_id")
    private String stepId;

    /**
     * 任务名称
     * 用户任务的显示名称
     */
    @TableField("name")
    private String name;

    /**
     * 任务描述
     * 详细描述任务的内容和要求
     */
    @TableField("description")
    private String description;

    /**
     * 表单键
     * 关联到前端表单的标识，用于渲染对应的表单
     */
    @TableField("form_key")
    private String formKey;

    /**
     * 表单数据
     * 任务相关的表单数据，JSON格式
     */
    @TableField("form_data")
    private String formData;

    /**
     * 任务分配人
     * 具体负责处理该任务的用户ID
     */
    @TableField("assignee")
    private String assignee;

    /**
     * 候选用户列表
     * 可以处理该任务的候选用户ID列表，JSON数组格式
     */
    @TableField("candidate_users")
    private String candidateUsers;

    /**
     * 候选用户组列表
     * 可以处理该任务的候选用户组列表，JSON数组格式
     */
    @TableField("candidate_groups")
    private String candidateGroups;

    /**
     * 任务优先级
     * 数值越大优先级越高，默认为50
     * 0-30: 低优先级
     * 31-70: 中等优先级
     * 71-100: 高优先级
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 任务状态
     * CREATED: 已创建
     * ASSIGNED: 已分配
     * IN_PROGRESS: 处理中
     * COMPLETED: 已完成
     * CANCELLED: 已取消
     * DELEGATED: 已委派
     * RECLAIMED: 已收回
     */
    @TableField("status")
    private String status;

    /**
     * 截止时间
     * 任务需要完成的截止时间
     */
    @TableField("due_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dueDate;

    /**
     * 创建人
     * 创建该任务的用户ID
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 创建时间
     * 自动填充创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 完成人
     * 完成该任务的用户ID
     */
    @TableField("completed_by")
    private String completedBy;

    /**
     * 完成时间
     * 任务完成的时间
     */
    @TableField("completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    /**
     * 委派人
     * 委派该任务的用户ID
     */
    @TableField("delegated_by")
    private String delegatedBy;

    /**
     * 委派时间
     * 任务被委派的时间
     */
    @TableField("delegated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime delegatedTime;

    /**
     * 委派原因
     * 委派任务的原因说明
     */
    @TableField("delegation_reason")
    private String delegationReason;

    /**
     * 收回人
     * 收回委派任务的用户ID
     */
    @TableField("reclaimed_by")
    private String reclaimedBy;

    /**
     * 收回时间
     * 任务被收回的时间
     */
    @TableField("reclaimed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reclaimedTime;

    /**
     * 用户任务状态枚举
     * 定义用户任务的生命周期状态
     */
    public enum Status {
        /**
         * 已创建 - 任务已创建但尚未分配给具体用户
         */
        CREATED("CREATED", "已创建"),
        
        /**
         * 已分配 - 任务已分配给具体用户但尚未开始处理
         */
        ASSIGNED("ASSIGNED", "已分配"),
        
        /**
         * 处理中 - 用户正在处理该任务
         */
        IN_PROGRESS("IN_PROGRESS", "处理中"),
        
        /**
         * 已完成 - 任务已成功完成
         */
        COMPLETED("COMPLETED", "已完成"),
        
        /**
         * 已取消 - 任务被取消，不再需要处理
         */
        CANCELLED("CANCELLED", "已取消"),
        
        /**
         * 已委派 - 任务被委派给其他用户处理
         */
        DELEGATED("DELEGATED", "已委派"),
        
        /**
         * 已收回 - 委派的任务被收回
         */
        RECLAIMED("RECLAIMED", "已收回");

        private final String code;
        private final String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据状态码获取枚举值
         * 
         * @param code 状态码
         * @return 对应的枚举值，如果不存在则返回null
         */
        public static Status fromCode(String code) {
            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 任务优先级枚举
     * 定义任务的优先级级别
     */
    public enum Priority {
        /**
         * 低优先级 (0-30)
         */
        LOW(25, "低优先级"),
        
        /**
         * 中等优先级 (31-70)
         */
        MEDIUM(50, "中等优先级"),
        
        /**
         * 高优先级 (71-100)
         */
        HIGH(80, "高优先级"),
        
        /**
         * 紧急优先级 (90-100)
         */
        URGENT(95, "紧急优先级");

        private final int value;
        private final String description;

        Priority(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据优先级数值获取对应的枚举
         * 
         * @param value 优先级数值
         * @return 对应的优先级枚举
         */
        public static Priority fromValue(int value) {
            if (value <= 30) {
                return LOW;
            } else if (value <= 70) {
                return MEDIUM;
            } else if (value <= 89) {
                return HIGH;
            } else {
                return URGENT;
            }
        }
    }

    /**
     * 检查任务是否已完成
     * 
     * @return true表示已完成，false表示未完成
     */
    public boolean isCompleted() {
        return Status.COMPLETED.getCode().equals(this.status);
    }

    /**
     * 检查任务是否已取消
     * 
     * @return true表示已取消，false表示未取消
     */
    public boolean isCancelled() {
        return Status.CANCELLED.getCode().equals(this.status);
    }

    /**
     * 检查任务是否处于活跃状态（可以被处理）
     * 
     * @return true表示活跃状态，false表示非活跃状态
     */
    public boolean isActive() {
        return Status.CREATED.getCode().equals(this.status) ||
               Status.ASSIGNED.getCode().equals(this.status) ||
               Status.IN_PROGRESS.getCode().equals(this.status);
    }

    /**
     * 检查任务是否已委派
     * 
     * @return true表示已委派，false表示未委派
     */
    public boolean isDelegated() {
        return Status.DELEGATED.getCode().equals(this.status);
    }

    /**
     * 检查任务是否已分配给具体用户
     * 
     * @return true表示已分配，false表示未分配
     */
    public boolean isAssigned() {
        return assignee != null && !assignee.trim().isEmpty();
    }

    /**
     * 检查任务是否已过期
     * 
     * @return true表示已过期，false表示未过期
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && isActive();
    }

    /**
     * 检查指定用户是否可以处理该任务
     * 用户可以处理任务的条件：
     * 1. 任务已分配给该用户
     * 2. 用户在候选用户列表中
     * 3. 用户属于候选用户组
     * 
     * @param userId 用户ID
     * @param userGroups 用户所属的用户组列表
     * @return true表示可以处理，false表示不可以处理
     */
    public boolean canBeHandledBy(String userId, java.util.List<String> userGroups) {
        // 检查是否分配给该用户
        if (userId.equals(assignee)) {
            return true;
        }
        
        // 检查是否在候选用户列表中
        if (candidateUsers != null && candidateUsers.contains(userId)) {
            return true;
        }
        
        // 检查用户组是否在候选用户组列表中
        if (candidateGroups != null && userGroups != null) {
            for (String group : userGroups) {
                if (candidateGroups.contains(group)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 计算任务的处理时长
     * 
     * @return 处理时长，如果尚未完成则返回null
     */
    public Duration getProcessingDuration() {
        if (createdTime == null || completedTime == null) {
            return null;
        }
        return Duration.between(createdTime, completedTime);
    }

    /**
     * 计算任务距离截止时间的剩余时间
     * 
     * @return 剩余时间，如果没有截止时间或已过期则返回null
     */
    public Duration getTimeUntilDue() {
        if (dueDate == null) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(dueDate)) {
            return null; // 已过期
        }
        
        return Duration.between(now, dueDate);
    }

    /**
     * 获取任务优先级的描述
     * 
     * @return 优先级描述
     */
    public String getPriorityDescription() {
        if (priority == null) {
            return Priority.MEDIUM.getDescription();
        }
        return Priority.fromValue(priority).getDescription();
    }

    /**
     * 获取任务的显示标题
     * 包含任务名称和优先级信息
     * 
     * @return 显示标题
     */
    public String getDisplayTitle() {
        String priorityDesc = getPriorityDescription();
        return String.format("%s [%s]", name, priorityDesc);
    }

    /**
     * 获取任务的状态描述
     * 
     * @return 状态描述
     */
    public String getStatusDescription() {
        Status statusEnum = Status.fromCode(this.status);
        return statusEnum != null ? statusEnum.getDescription() : "未知状态";
    }
}