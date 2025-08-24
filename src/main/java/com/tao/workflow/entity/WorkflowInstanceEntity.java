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
 * 工作流实例实体类
 * 对应数据库表：workflow_instance
 * 存储工作流的执行实例信息
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true) // 支持链式调用
@TableName("workflow_instance")
public class WorkflowInstanceEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流实例ID
     * 主键，每个工作流实例的唯一标识
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 工作流定义ID
     * 关联到workflow_definition表的主键
     */
    @TableField("workflow_id")
    private String workflowId;

    /**
     * 工作流名称
     * 冗余字段，便于查询时不需要关联工作流定义表
     */
    @TableField("workflow_name")
    private String workflowName;

    /**
     * 工作流版本
     * 冗余字段，记录实例创建时使用的工作流版本
     */
    @TableField("workflow_version")
    private String workflowVersion;

    /**
     * 工作流实例状态
     * CREATED: 已创建，尚未开始执行
     * RUNNING: 正在执行中
     * WAITING: 等待中（如等待用户任务完成）
     * SUSPENDED: 已暂停
     * COMPLETED: 已完成
     * FAILED: 执行失败
     * CANCELLED: 已取消
     * TERMINATED: 已终止
     */
    @TableField("status")
    private String status;

    /**
     * 当前步骤ID
     * 记录工作流实例当前执行到的步骤
     */
    @TableField("current_step_id")
    private String currentStepId;

    /**
     * 上下文数据
     * 存储工作流实例执行过程中的变量和数据，JSON格式
     */
    @TableField("context_data")
    private String contextData;

    /**
     * 启动人
     * 记录启动工作流实例的用户
     */
    @TableField("started_by")
    private String startedBy;

    /**
     * 启动时间
     * 工作流实例开始执行的时间
     */
    @TableField("started_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startedTime;

    /**
     * 完成时间
     * 工作流实例执行完成的时间
     */
    @TableField("completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    /**
     * 创建时间
     * 自动填充创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     * 自动填充更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 工作流实例状态枚举
     * 定义工作流实例的生命周期状态
     */
    public enum Status {
        /**
         * 已创建 - 工作流实例已创建但尚未开始执行
         */
        CREATED("CREATED", "已创建"),
        
        /**
         * 运行中 - 工作流实例正在执行
         */
        RUNNING("RUNNING", "运行中"),
        
        /**
         * 等待中 - 工作流实例等待外部事件（如用户任务完成）
         */
        WAITING("WAITING", "等待中"),
        
        /**
         * 已暂停 - 工作流实例被手动暂停
         */
        SUSPENDED("SUSPENDED", "已暂停"),
        
        /**
         * 已完成 - 工作流实例成功执行完成
         */
        COMPLETED("COMPLETED", "已完成"),
        
        /**
         * 执行失败 - 工作流实例执行过程中发生错误
         */
        FAILED("FAILED", "执行失败"),
        
        /**
         * 已取消 - 工作流实例被用户取消
         */
        CANCELLED("CANCELLED", "已取消"),
        
        /**
         * 已终止 - 工作流实例被强制终止
         */
        TERMINATED("TERMINATED", "已终止");

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
     * 检查工作流实例是否正在运行
     * 
     * @return true表示正在运行，false表示未运行
     */
    public boolean isRunning() {
        return Status.RUNNING.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否已完成
     * 
     * @return true表示已完成，false表示未完成
     */
    public boolean isCompleted() {
        return Status.COMPLETED.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否已失败
     * 
     * @return true表示已失败，false表示未失败
     */
    public boolean isFailed() {
        return Status.FAILED.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否处于等待状态
     * 
     * @return true表示等待中，false表示非等待状态
     */
    public boolean isWaiting() {
        return Status.WAITING.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否已暂停
     * 
     * @return true表示已暂停，false表示未暂停
     */
    public boolean isSuspended() {
        return Status.SUSPENDED.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否已结束（完成、失败、取消或终止）
     * 
     * @return true表示已结束，false表示未结束
     */
    public boolean isFinished() {
        return isCompleted() || isFailed() || 
               Status.CANCELLED.getCode().equals(this.status) || 
               Status.TERMINATED.getCode().equals(this.status);
    }

    /**
     * 检查工作流实例是否可以被暂停
     * 只有运行中或等待中的实例可以被暂停
     * 
     * @return true表示可以暂停，false表示不可以暂停
     */
    public boolean canBeSuspended() {
        return isRunning() || isWaiting();
    }

    /**
     * 检查工作流实例是否可以被恢复
     * 只有暂停状态的实例可以被恢复
     * 
     * @return true表示可以恢复，false表示不可以恢复
     */
    public boolean canBeResumed() {
        return isSuspended();
    }

    /**
     * 计算工作流实例的执行时长
     * 
     * @return 执行时长，如果尚未开始或完成则返回null
     */
    public Duration getExecutionDuration() {
        if (startedTime == null) {
            return null;
        }
        
        LocalDateTime endTime = completedTime != null ? completedTime : LocalDateTime.now();
        return Duration.between(startedTime, endTime);
    }

    /**
     * 获取工作流实例的显示名称
     * 格式：工作流名称 (版本) - 实例ID前8位
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        String shortId = id != null && id.length() > 8 ? id.substring(0, 8) : id;
        return String.format("%s (%s) - %s", workflowName, workflowVersion, shortId);
    }
}