package com.tao.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流定义实体类
 * 对应数据库表：workflow_definition
 * 存储工作流的元数据和定义信息
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true) // 支持链式调用
@TableName("workflow_definition")
public class WorkflowDefinitionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流定义ID
     * 主键，使用字符串类型便于自定义ID规则
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 工作流名称
     * 业务层面的工作流名称，如"请假审批流程"
     */
    @TableField("name")
    private String name;

    /**
     * 工作流描述
     * 详细描述工作流的用途和业务场景
     */
    @TableField("description")
    private String description;

    /**
     * 工作流版本
     * 支持工作流的版本管理，默认为"1.0"
     */
    @TableField("version")
    private String version;

    /**
     * 工作流状态
     * ACTIVE: 激活状态，可以创建新实例
     * INACTIVE: 非激活状态，不能创建新实例
     * DEPRECATED: 已废弃，建议使用新版本
     */
    @TableField("status")
    private String status;

    /**
     * 工作流定义JSON
     * 存储完整的工作流定义信息，包括步骤、连接、配置等
     * 使用JSON格式便于灵活扩展
     */
    @TableField("definition_json")
    private String definitionJson;

    /**
     * 创建人
     * 记录工作流定义的创建者
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 创建时间
     * 自动填充创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 更新人
     * 记录最后更新工作流定义的用户
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * 更新时间
     * 自动填充更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 工作流定义状态枚举
     * 定义工作流的生命周期状态
     */
    public enum Status {
        /**
         * 激活状态 - 可以创建新的工作流实例
         */
        ACTIVE("ACTIVE", "激活"),
        
        /**
         * 非激活状态 - 不能创建新的工作流实例，但现有实例可以继续执行
         */
        INACTIVE("INACTIVE", "非激活"),
        
        /**
         * 已废弃状态 - 建议使用新版本，不推荐使用
         */
        DEPRECATED("DEPRECATED", "已废弃");

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
     * 检查工作流定义是否处于激活状态
     * 
     * @return true表示激活状态，false表示非激活状态
     */
    public boolean isActive() {
        return Status.ACTIVE.getCode().equals(this.status);
    }

    /**
     * 检查工作流定义是否已废弃
     * 
     * @return true表示已废弃，false表示未废弃
     */
    public boolean isDeprecated() {
        return Status.DEPRECATED.getCode().equals(this.status);
    }

    /**
     * 获取工作流的唯一标识
     * 由名称和版本组成，用于区分不同版本的同名工作流
     * 
     * @return 格式为"name:version"的唯一标识
     */
    public String getUniqueKey() {
        return this.name + ":" + this.version;
    }
}