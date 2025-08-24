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
 * 工作流执行历史实体类
 * 对应数据库表：workflow_execution_history
 * 记录工作流中每个步骤的执行情况和历史信息
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true) // 支持链式调用
@TableName("workflow_execution_history")
public class WorkflowExecutionHistoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行历史记录ID
     * 主键，每条执行历史记录的唯一标识
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
     * 步骤名称
     * 冗余字段，便于查询时直接显示步骤名称
     */
    @TableField("step_name")
    private String stepName;

    /**
     * 步骤类型
     * START: 开始步骤
     * END: 结束步骤
     * USER_TASK: 用户任务步骤
     * TASK: 自动任务步骤
     * CONDITIONAL: 条件判断步骤
     * PARALLEL: 并行处理步骤
     */
    @TableField("step_type")
    private String stepType;

    /**
     * 执行状态
     * SUCCESS: 执行成功
     * FAILED: 执行失败
     * WAITING: 等待中
     * SKIPPED: 已跳过
     * TIMEOUT: 执行超时
     * RETRY: 重试中
     */
    @TableField("status")
    private String status;

    /**
     * 执行器名称
     * 执行该步骤的执行器类名或标识
     */
    @TableField("executor_name")
    private String executorName;

    /**
     * 输入数据
     * 步骤执行时的输入参数，JSON格式
     */
    @TableField("input_data")
    private String inputData;

    /**
     * 输出数据
     * 步骤执行完成后的输出结果，JSON格式
     */
    @TableField("output_data")
    private String outputData;

    /**
     * 错误信息
     * 步骤执行失败时的错误详情
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 开始执行时间
     * 步骤开始执行的时间
     */
    @TableField("started_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startedTime;

    /**
     * 完成执行时间
     * 步骤执行完成的时间
     */
    @TableField("completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    /**
     * 执行耗时
     * 步骤执行的总耗时，单位：毫秒
     */
    @TableField("execution_time")
    private Long executionTime;

    /**
     * 重试次数
     * 步骤执行失败后的重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 创建时间
     * 自动填充创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 步骤执行状态枚举
     * 定义步骤执行的各种状态
     */
    public enum Status {
        /**
         * 执行成功
         */
        SUCCESS("SUCCESS", "执行成功"),
        
        /**
         * 执行失败
         */
        FAILED("FAILED", "执行失败"),
        
        /**
         * 等待中
         */
        WAITING("WAITING", "等待中"),
        
        /**
         * 已跳过
         */
        SKIPPED("SKIPPED", "已跳过"),
        
        /**
         * 执行超时
         */
        TIMEOUT("TIMEOUT", "执行超时"),
        
        /**
         * 重试中
         */
        RETRY("RETRY", "重试中");

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
     * 步骤类型枚举
     * 定义工作流中支持的步骤类型
     */
    public enum StepType {
        /**
         * 开始步骤
         */
        START("START", "开始步骤"),
        
        /**
         * 结束步骤
         */
        END("END", "结束步骤"),
        
        /**
         * 用户任务步骤
         */
        USER_TASK("USER_TASK", "用户任务步骤"),
        
        /**
         * 自动任务步骤
         */
        TASK("TASK", "自动任务步骤"),
        
        /**
         * 条件判断步骤
         */
        CONDITIONAL("CONDITIONAL", "条件判断步骤"),
        
        /**
         * 并行处理步骤
         */
        PARALLEL("PARALLEL", "并行处理步骤");

        private final String code;
        private final String description;

        StepType(String code, String description) {
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
         * 根据类型码获取枚举值
         * 
         * @param code 类型码
         * @return 对应的枚举值，如果不存在则返回null
         */
        public static StepType fromCode(String code) {
            for (StepType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 检查步骤是否执行成功
     * 
     * @return true表示执行成功，false表示未成功
     */
    public boolean isSuccess() {
        return Status.SUCCESS.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否执行失败
     * 
     * @return true表示执行失败，false表示未失败
     */
    public boolean isFailed() {
        return Status.FAILED.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否正在等待
     * 
     * @return true表示等待中，false表示非等待状态
     */
    public boolean isWaiting() {
        return Status.WAITING.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否被跳过
     * 
     * @return true表示已跳过，false表示未跳过
     */
    public boolean isSkipped() {
        return Status.SKIPPED.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否执行超时
     * 
     * @return true表示执行超时，false表示未超时
     */
    public boolean isTimeout() {
        return Status.TIMEOUT.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否正在重试
     * 
     * @return true表示重试中，false表示非重试状态
     */
    public boolean isRetrying() {
        return Status.RETRY.getCode().equals(this.status);
    }

    /**
     * 检查步骤是否已完成（成功或失败）
     * 
     * @return true表示已完成，false表示未完成
     */
    public boolean isCompleted() {
        return isSuccess() || isFailed() || isSkipped() || isTimeout();
    }

    /**
     * 计算步骤的实际执行时长
     * 如果executionTime字段有值则直接返回，否则根据开始和结束时间计算
     * 
     * @return 执行时长（毫秒），如果无法计算则返回null
     */
    public Long getActualExecutionTime() {
        if (executionTime != null) {
            return executionTime;
        }
        
        if (startedTime != null && completedTime != null) {
            Duration duration = Duration.between(startedTime, completedTime);
            return duration.toMillis();
        }
        
        return null;
    }

    /**
     * 获取执行时长的可读格式
     * 
     * @return 格式化的执行时长字符串，如"1.23秒"、"2分30秒"
     */
    public String getFormattedExecutionTime() {
        Long actualTime = getActualExecutionTime();
        if (actualTime == null) {
            return "未知";
        }
        
        if (actualTime < 1000) {
            return actualTime + "毫秒";
        } else if (actualTime < 60000) {
            return String.format("%.2f秒", actualTime / 1000.0);
        } else {
            long minutes = actualTime / 60000;
            long seconds = (actualTime % 60000) / 1000;
            return minutes + "分" + seconds + "秒";
        }
    }

    /**
     * 检查是否有重试记录
     * 
     * @return true表示有重试记录，false表示无重试
     */
    public boolean hasRetries() {
        return retryCount != null && retryCount > 0;
    }

    /**
     * 获取步骤的显示标题
     * 格式：步骤名称 (步骤类型)
     * 
     * @return 显示标题
     */
    public String getDisplayTitle() {
        return String.format("%s (%s)", stepName, stepType);
    }
}