package com.tao.workflow.service;

import com.tao.workflow.entity.WorkflowVariableEntity;

import java.util.List;
import java.util.Map;

/**
 * 工作流变量服务接口
 * 提供工作流变量的核心业务操作
 * 
 * @author tao
 * @since 2024-01-15
 */
public interface WorkflowVariableService {

    /**
     * 创建工作流变量
     * 
     * @param variable 工作流变量实体
     * @return 创建成功的工作流变量
     */
    WorkflowVariableEntity createVariable(WorkflowVariableEntity variable);

    /**
     * 根据ID获取工作流变量
     * 
     * @param variableId 变量ID
     * @return 工作流变量，如果不存在则返回null
     */
    WorkflowVariableEntity getVariableById(String variableId);

    /**
     * 根据实例ID获取所有变量
     * 
     * @param instanceId 实例ID
     * @return 变量列表
     */
    List<WorkflowVariableEntity> getVariablesByInstanceId(String instanceId);

    /**
     * 根据实例ID和作用域获取变量
     * 
     * @param instanceId 实例ID
     * @param scope 作用域
     * @return 变量列表
     */
    List<WorkflowVariableEntity> getVariablesByInstanceIdAndScope(String instanceId, WorkflowVariableEntity.Scope scope);

    /**
     * 根据步骤ID获取变量
     * 
     * @param stepId 步骤ID
     * @return 变量列表
     */
    List<WorkflowVariableEntity> getVariablesByStepId(String stepId);

    /**
     * 根据实例ID和变量名获取变量
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 工作流变量，如果不存在则返回null
     */
    WorkflowVariableEntity getVariableByInstanceIdAndName(String instanceId, String variableName);

    /**
     * 根据步骤ID和变量名获取变量
     * 
     * @param stepId 步骤ID
     * @param variableName 变量名
     * @return 工作流变量，如果不存在则返回null
     */
    WorkflowVariableEntity getVariableByStepIdAndName(String stepId, String variableName);

    /**
     * 设置实例级变量
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param value 变量值
     * @param type 变量类型
     * @return 设置成功的变量
     */
    WorkflowVariableEntity setInstanceVariable(String instanceId, String variableName, String value, WorkflowVariableEntity.VariableType type);

    /**
     * 设置步骤级变量
     * 
     * @param instanceId 实例ID
     * @param stepId 步骤ID
     * @param variableName 变量名
     * @param value 变量值
     * @param type 变量类型
     * @return 设置成功的变量
     */
    WorkflowVariableEntity setStepVariable(String instanceId, String stepId, String variableName, String value, WorkflowVariableEntity.VariableType type);

    /**
     * 设置全局变量
     * 
     * @param variableName 变量名
     * @param value 变量值
     * @param type 变量类型
     * @return 设置成功的变量
     */
    WorkflowVariableEntity setGlobalVariable(String variableName, String value, WorkflowVariableEntity.VariableType type);

    /**
     * 更新变量值
     * 
     * @param variableId 变量ID
     * @param value 新的变量值
     * @return 是否更新成功
     */
    boolean updateVariableValue(String variableId, String value);

    /**
     * 更新变量
     * 
     * @param variable 要更新的变量
     * @return 更新后的变量
     */
    WorkflowVariableEntity updateVariable(WorkflowVariableEntity variable);

    /**
     * 删除变量
     * 
     * @param variableId 变量ID
     * @return 是否删除成功
     */
    boolean deleteVariable(String variableId);

    /**
     * 删除实例的所有变量
     * 
     * @param instanceId 实例ID
     * @return 删除的变量数量
     */
    int deleteVariablesByInstanceId(String instanceId);

    /**
     * 删除步骤的所有变量
     * 
     * @param stepId 步骤ID
     * @return 删除的变量数量
     */
    int deleteVariablesByStepId(String stepId);

    /**
     * 批量设置变量
     * 
     * @param instanceId 实例ID
     * @param variables 变量映射（变量名 -> 变量值）
     * @param scope 作用域
     * @param stepId 步骤ID（当作用域为STEP时必填）
     * @return 设置成功的变量列表
     */
    List<WorkflowVariableEntity> batchSetVariables(String instanceId, Map<String, Object> variables, WorkflowVariableEntity.Scope scope, String stepId);

    /**
     * 获取实例的所有变量作为Map
     * 
     * @param instanceId 实例ID
     * @return 变量映射（变量名 -> 变量值）
     */
    Map<String, Object> getInstanceVariablesAsMap(String instanceId);

    /**
     * 获取步骤的所有变量作为Map
     * 
     * @param stepId 步骤ID
     * @return 变量映射（变量名 -> 变量值）
     */
    Map<String, Object> getStepVariablesAsMap(String stepId);

    /**
     * 获取全局变量作为Map
     * 
     * @return 变量映射（变量名 -> 变量值）
     */
    Map<String, Object> getGlobalVariablesAsMap();

    /**
     * 获取变量的字符串值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的字符串值
     */
    String getVariableAsString(String instanceId, String variableName, String defaultValue);

    /**
     * 获取变量的整数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的整数值
     */
    Integer getVariableAsInteger(String instanceId, String variableName, Integer defaultValue);

    /**
     * 获取变量的长整数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的长整数值
     */
    Long getVariableAsLong(String instanceId, String variableName, Long defaultValue);

    /**
     * 获取变量的双精度浮点数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的双精度浮点数值
     */
    Double getVariableAsDouble(String instanceId, String variableName, Double defaultValue);

    /**
     * 获取变量的布尔值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的布尔值
     */
    Boolean getVariableAsBoolean(String instanceId, String variableName, Boolean defaultValue);

    /**
     * 获取变量的JSON对象
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 变量的JSON对象
     */
    <T> T getVariableAsJson(String instanceId, String variableName, Class<T> clazz);

    /**
     * 检查变量是否存在
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 是否存在
     */
    boolean hasVariable(String instanceId, String variableName);

    /**
     * 检查步骤变量是否存在
     * 
     * @param stepId 步骤ID
     * @param variableName 变量名
     * @return 是否存在
     */
    boolean hasStepVariable(String stepId, String variableName);

    /**
     * 检查全局变量是否存在
     * 
     * @param variableName 变量名
     * @return 是否存在
     */
    boolean hasGlobalVariable(String variableName);

    /**
     * 复制变量到新实例
     * 
     * @param sourceInstanceId 源实例ID
     * @param targetInstanceId 目标实例ID
     * @param scope 作用域过滤（可选）
     * @return 复制的变量数量
     */
    int copyVariablesToInstance(String sourceInstanceId, String targetInstanceId, WorkflowVariableEntity.Scope scope);

    /**
     * 获取变量统计信息
     * 
     * @param instanceId 实例ID
     * @return 变量统计信息
     */
    Map<String, Object> getVariableStatistics(String instanceId);

    /**
     * 根据类型统计变量数量
     * 
     * @param type 变量类型
     * @return 变量数量
     */
    long countByType(WorkflowVariableEntity.VariableType type);

    /**
     * 根据作用域统计变量数量
     * 
     * @param scope 作用域
     * @return 变量数量
     */
    long countByScope(WorkflowVariableEntity.Scope scope);

    /**
     * 清理过期的变量
     * 
     * @param days 保留天数
     * @return 清理的变量数量
     */
    int cleanupExpiredVariables(int days);

    /**
     * 获取变量的历史记录
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 历史记录列表
     */
    List<Map<String, Object>> getVariableHistory(String instanceId, String variableName);

    /**
     * 导出实例变量
     * 
     * @param instanceId 实例ID
     * @return 变量的JSON字符串
     */
    String exportInstanceVariables(String instanceId);

    /**
     * 导入实例变量
     * 
     * @param instanceId 实例ID
     * @param variablesJson 变量的JSON字符串
     * @return 导入成功的变量数量
     */
    int importInstanceVariables(String instanceId, String variablesJson);

    /**
     * 合并变量
     * 将源变量合并到目标实例，如果目标实例已存在同名变量则覆盖
     * 
     * @param sourceInstanceId 源实例ID
     * @param targetInstanceId 目标实例ID
     * @param overwrite 是否覆盖已存在的变量
     * @return 合并的变量数量
     */
    int mergeVariables(String sourceInstanceId, String targetInstanceId, boolean overwrite);

    /**
     * 获取变量的唯一键
     * 
     * @param instanceId 实例ID
     * @param stepId 步骤ID（可选）
     * @param variableName 变量名
     * @param scope 作用域
     * @return 唯一键
     */
    String getVariableUniqueKey(String instanceId, String stepId, String variableName, WorkflowVariableEntity.Scope scope);

    /**
     * 验证变量值的格式
     * 
     * @param value 变量值
     * @param type 变量类型
     * @return 是否格式正确
     */
    boolean validateVariableValue(String value, WorkflowVariableEntity.VariableType type);

    /**
     * 转换变量值类型
     * 
     * @param value 原始值
     * @param fromType 原类型
     * @param toType 目标类型
     * @return 转换后的值
     */
    String convertVariableValue(String value, WorkflowVariableEntity.VariableType fromType, WorkflowVariableEntity.VariableType toType);
}