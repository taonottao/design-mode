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

/**
 * 工作流变量实体类
 * 对应数据库表：workflow_variable
 * 存储工作流执行过程中的变量和数据
 * 
 * @author tao
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true) // 支持链式调用
@TableName("workflow_variable")
public class WorkflowVariableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变量ID
     * 主键，每个工作流变量的唯一标识
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
     * 变量名称
     * 变量的唯一标识名称，在同一作用域内不能重复
     */
    @TableField("variable_name")
    private String variableName;

    /**
     * 变量类型
     * STRING: 字符串类型
     * INTEGER: 整数类型
     * LONG: 长整数类型
     * DOUBLE: 双精度浮点数类型
     * BOOLEAN: 布尔类型
     * DATE: 日期类型
     * DATETIME: 日期时间类型
     * JSON: JSON对象类型
     * ARRAY: 数组类型
     * OBJECT: 复杂对象类型
     */
    @TableField("variable_type")
    private String variableType;

    /**
     * 变量值
     * 存储变量的实际值，统一使用字符串格式存储
     * 复杂类型（如JSON、OBJECT）会序列化为字符串
     */
    @TableField("variable_value")
    private String variableValue;

    /**
     * 变量作用域
     * INSTANCE: 实例级别，整个工作流实例都可以访问
     * STEP: 步骤级别，只在特定步骤内有效
     * GLOBAL: 全局级别，跨工作流实例共享
     */
    @TableField("scope")
    private String scope;

    /**
     * 步骤ID
     * 当作用域为STEP时，指定变量所属的步骤ID
     */
    @TableField("step_id")
    private String stepId;

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
     * 变量类型枚举
     * 定义工作流变量支持的数据类型
     */
    public enum VariableType {
        /**
         * 字符串类型
         */
        STRING("STRING", "字符串", String.class),
        
        /**
         * 整数类型
         */
        INTEGER("INTEGER", "整数", Integer.class),
        
        /**
         * 长整数类型
         */
        LONG("LONG", "长整数", Long.class),
        
        /**
         * 双精度浮点数类型
         */
        DOUBLE("DOUBLE", "双精度浮点数", Double.class),
        
        /**
         * 布尔类型
         */
        BOOLEAN("BOOLEAN", "布尔值", Boolean.class),
        
        /**
         * 日期类型
         */
        DATE("DATE", "日期", java.time.LocalDate.class),
        
        /**
         * 日期时间类型
         */
        DATETIME("DATETIME", "日期时间", java.time.LocalDateTime.class),
        
        /**
         * JSON对象类型
         */
        JSON("JSON", "JSON对象", Object.class),
        
        /**
         * 数组类型
         */
        ARRAY("ARRAY", "数组", java.util.List.class),
        
        /**
         * 复杂对象类型
         */
        OBJECT("OBJECT", "对象", Object.class);

        private final String code;
        private final String description;
        private final Class<?> javaType;

        VariableType(String code, String description, Class<?> javaType) {
            this.code = code;
            this.description = description;
            this.javaType = javaType;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public Class<?> getJavaType() {
            return javaType;
        }

        /**
         * 根据类型码获取枚举值
         * 
         * @param code 类型码
         * @return 对应的枚举值，如果不存在则返回null
         */
        public static VariableType fromCode(String code) {
            for (VariableType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * 根据Java类型推断变量类型
         * 
         * @param javaType Java类型
         * @return 对应的变量类型枚举
         */
        public static VariableType fromJavaType(Class<?> javaType) {
            if (javaType == String.class) {
                return STRING;
            } else if (javaType == Integer.class || javaType == int.class) {
                return INTEGER;
            } else if (javaType == Long.class || javaType == long.class) {
                return LONG;
            } else if (javaType == Double.class || javaType == double.class || 
                      javaType == Float.class || javaType == float.class) {
                return DOUBLE;
            } else if (javaType == Boolean.class || javaType == boolean.class) {
                return BOOLEAN;
            } else if (javaType == java.time.LocalDate.class || javaType == java.util.Date.class) {
                return DATE;
            } else if (javaType == java.time.LocalDateTime.class) {
                return DATETIME;
            } else if (java.util.List.class.isAssignableFrom(javaType) || javaType.isArray()) {
                return ARRAY;
            } else {
                return OBJECT;
            }
        }
    }

    /**
     * 变量作用域枚举
     * 定义变量的可见性和生命周期
     */
    public enum Scope {
        /**
         * 实例级别 - 整个工作流实例都可以访问
         */
        INSTANCE("INSTANCE", "实例级别"),
        
        /**
         * 步骤级别 - 只在特定步骤内有效
         */
        STEP("STEP", "步骤级别"),
        
        /**
         * 全局级别 - 跨工作流实例共享
         */
        GLOBAL("GLOBAL", "全局级别");

        private final String code;
        private final String description;

        Scope(String code, String description) {
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
         * 根据作用域码获取枚举值
         * 
         * @param code 作用域码
         * @return 对应的枚举值，如果不存在则返回null
         */
        public static Scope fromCode(String code) {
            for (Scope scope : values()) {
                if (scope.code.equals(code)) {
                    return scope;
                }
            }
            return null;
        }
    }

    /**
     * 检查变量是否为实例级别作用域
     * 
     * @return true表示实例级别，false表示其他级别
     */
    public boolean isInstanceScope() {
        return Scope.INSTANCE.getCode().equals(this.scope);
    }

    /**
     * 检查变量是否为步骤级别作用域
     * 
     * @return true表示步骤级别，false表示其他级别
     */
    public boolean isStepScope() {
        return Scope.STEP.getCode().equals(this.scope);
    }

    /**
     * 检查变量是否为全局级别作用域
     * 
     * @return true表示全局级别，false表示其他级别
     */
    public boolean isGlobalScope() {
        return Scope.GLOBAL.getCode().equals(this.scope);
    }

    /**
     * 获取变量的唯一键
     * 格式：实例ID:作用域:变量名[:步骤ID]
     * 
     * @return 变量的唯一标识键
     */
    public String getUniqueKey() {
        StringBuilder key = new StringBuilder();
        key.append(instanceId).append(":").append(scope).append(":").append(variableName);
        
        if (isStepScope() && stepId != null) {
            key.append(":").append(stepId);
        }
        
        return key.toString();
    }

    /**
     * 检查变量值是否为空
     * 
     * @return true表示值为空，false表示有值
     */
    public boolean isValueEmpty() {
        return variableValue == null || variableValue.trim().isEmpty();
    }

    /**
     * 获取变量类型的描述
     * 
     * @return 类型描述
     */
    public String getTypeDescription() {
        VariableType type = VariableType.fromCode(this.variableType);
        return type != null ? type.getDescription() : "未知类型";
    }

    /**
     * 获取变量作用域的描述
     * 
     * @return 作用域描述
     */
    public String getScopeDescription() {
        Scope scopeEnum = Scope.fromCode(this.scope);
        return scopeEnum != null ? scopeEnum.getDescription() : "未知作用域";
    }

    /**
     * 获取变量的显示名称
     * 格式：变量名 (类型) [作用域]
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return String.format("%s (%s) [%s]", 
                           variableName, 
                           getTypeDescription(), 
                           getScopeDescription());
    }

    /**
     * 检查变量是否可以在指定步骤中访问
     * 
     * @param targetStepId 目标步骤ID
     * @return true表示可以访问，false表示不可以访问
     */
    public boolean isAccessibleInStep(String targetStepId) {
        // 全局和实例级别的变量在所有步骤中都可以访问
        if (isGlobalScope() || isInstanceScope()) {
            return true;
        }
        
        // 步骤级别的变量只能在对应的步骤中访问
        if (isStepScope()) {
            return stepId != null && stepId.equals(targetStepId);
        }
        
        return false;
    }

    /**
     * 尝试将变量值转换为指定的Java类型
     * 
     * @param targetType 目标类型
     * @param <T> 泛型类型
     * @return 转换后的值，如果转换失败则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getValueAs(Class<T> targetType) {
        if (isValueEmpty()) {
            return null;
        }
        
        try {
            if (targetType == String.class) {
                return (T) variableValue;
            } else if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(variableValue);
            } else if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(variableValue);
            } else if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(variableValue);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return (T) Boolean.valueOf(variableValue);
            } else {
                // 对于复杂类型，这里可以扩展JSON反序列化逻辑
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}