package com.tao.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tao.workflow.entity.WorkflowVariableEntity;
import com.tao.workflow.mapper.WorkflowVariableMapper;
import com.tao.workflow.service.WorkflowVariableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流变量服务实现类
 * 提供工作流变量的具体业务逻辑实现
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowVariableServiceImpl extends ServiceImpl<WorkflowVariableMapper, WorkflowVariableEntity> implements WorkflowVariableService {

    private final WorkflowVariableMapper workflowVariableMapper;

    /**
     * 创建工作流变量
     * 
     * @param variable 工作流变量实体
     * @return 创建成功的工作流变量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVariableEntity createVariable(WorkflowVariableEntity variable) {
        log.info("创建工作流变量: {}", variable.getVariableName());
        
        // 设置默认值
        if (variable.getCreatedTime() == null) {
            variable.setCreatedTime(LocalDateTime.now());
        }
        if (variable.getUpdatedTime() == null) {
            variable.setUpdatedTime(LocalDateTime.now());
        }
        
        // 验证变量值格式
        if (!validateVariableValue(variable.getVariableValue(), variable.getVariableType())) {
            throw new IllegalArgumentException("变量值格式不正确: " + variable.getVariableValue());
        }
        
        // 保存变量
        save(variable);
        
        log.info("工作流变量创建成功，变量ID: {}", variable.getVariableId());
        return variable;
    }

    /**
     * 根据ID获取工作流变量
     * 
     * @param variableId 变量ID
     * @return 工作流变量，如果不存在则返回null
     */
    @Override
    public WorkflowVariableEntity getVariableById(String variableId) {
        if (StrUtil.isBlank(variableId)) {
            return null;
        }
        return getById(variableId);
    }

    /**
     * 根据实例ID获取所有变量
     * 
     * @param instanceId 实例ID
     * @return 变量列表
     */
    @Override
    public List<WorkflowVariableEntity> getVariablesByInstanceId(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getInstanceId, instanceId)
                .orderByAsc(WorkflowVariableEntity::getVariableName);
        
        return list(wrapper);
    }

    /**
     * 根据实例ID和作用域获取变量
     * 
     * @param instanceId 实例ID
     * @param scope 作用域
     * @return 变量列表
     */
    @Override
    public List<WorkflowVariableEntity> getVariablesByInstanceIdAndScope(String instanceId, WorkflowVariableEntity.Scope scope) {
        if (StrUtil.isBlank(instanceId) || scope == null) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getInstanceId, instanceId)
                .eq(WorkflowVariableEntity::getScope, scope)
                .orderByAsc(WorkflowVariableEntity::getVariableName);
        
        return list(wrapper);
    }

    /**
     * 根据步骤ID获取变量
     * 
     * @param stepId 步骤ID
     * @return 变量列表
     */
    @Override
    public List<WorkflowVariableEntity> getVariablesByStepId(String stepId) {
        if (StrUtil.isBlank(stepId)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getStepId, stepId)
                .eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.STEP)
                .orderByAsc(WorkflowVariableEntity::getVariableName);
        
        return list(wrapper);
    }

    /**
     * 根据实例ID和变量名获取变量
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 工作流变量，如果不存在则返回null
     */
    @Override
    public WorkflowVariableEntity getVariableByInstanceIdAndName(String instanceId, String variableName) {
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(variableName)) {
            return null;
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getInstanceId, instanceId)
                .eq(WorkflowVariableEntity::getVariableName, variableName)
                .in(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.INSTANCE, WorkflowVariableEntity.Scope.GLOBAL)
                .orderByDesc(WorkflowVariableEntity::getScope); // 优先返回实例级变量
        
        List<WorkflowVariableEntity> variables = list(wrapper);
        return CollUtil.isNotEmpty(variables) ? variables.get(0) : null;
    }

    /**
     * 根据步骤ID和变量名获取变量
     * 
     * @param stepId 步骤ID
     * @param variableName 变量名
     * @return 工作流变量，如果不存在则返回null
     */
    @Override
    public WorkflowVariableEntity getVariableByStepIdAndName(String stepId, String variableName) {
        if (StrUtil.isBlank(stepId) || StrUtil.isBlank(variableName)) {
            return null;
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getStepId, stepId)
                .eq(WorkflowVariableEntity::getVariableName, variableName)
                .eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.STEP);
        
        return getOne(wrapper);
    }

    /**
     * 设置实例级变量
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param value 变量值
     * @param type 变量类型
     * @return 设置成功的变量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVariableEntity setInstanceVariable(String instanceId, String variableName, String value, WorkflowVariableEntity.VariableType type) {
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(variableName)) {
            throw new IllegalArgumentException("实例ID和变量名不能为空");
        }
        
        log.info("设置实例变量: {} = {}, 实例: {}", variableName, value, instanceId);
        
        // 查找已存在的变量
        WorkflowVariableEntity existingVariable = getVariableByInstanceIdAndName(instanceId, variableName);
        
        if (existingVariable != null && existingVariable.getScope() == WorkflowVariableEntity.Scope.INSTANCE) {
            // 更新已存在的实例变量
            existingVariable.setVariableValue(value);
            existingVariable.setVariableType(type);
            existingVariable.setUpdatedTime(LocalDateTime.now());
            updateById(existingVariable);
            return existingVariable;
        } else {
            // 创建新的实例变量
            WorkflowVariableEntity variable = new WorkflowVariableEntity();
            variable.setInstanceId(instanceId);
            variable.setVariableName(variableName);
            variable.setVariableValue(value);
            variable.setVariableType(type);
            variable.setScope(WorkflowVariableEntity.Scope.INSTANCE);
            return createVariable(variable);
        }
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVariableEntity setStepVariable(String instanceId, String stepId, String variableName, String value, WorkflowVariableEntity.VariableType type) {
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(stepId) || StrUtil.isBlank(variableName)) {
            throw new IllegalArgumentException("实例ID、步骤ID和变量名不能为空");
        }
        
        log.info("设置步骤变量: {} = {}, 步骤: {}", variableName, value, stepId);
        
        // 查找已存在的步骤变量
        WorkflowVariableEntity existingVariable = getVariableByStepIdAndName(stepId, variableName);
        
        if (existingVariable != null) {
            // 更新已存在的步骤变量
            existingVariable.setVariableValue(value);
            existingVariable.setVariableType(type);
            existingVariable.setUpdatedTime(LocalDateTime.now());
            updateById(existingVariable);
            return existingVariable;
        } else {
            // 创建新的步骤变量
            WorkflowVariableEntity variable = new WorkflowVariableEntity();
            variable.setInstanceId(instanceId);
            variable.setStepId(stepId);
            variable.setVariableName(variableName);
            variable.setVariableValue(value);
            variable.setVariableType(type);
            variable.setScope(WorkflowVariableEntity.Scope.STEP);
            return createVariable(variable);
        }
    }

    /**
     * 设置全局变量
     * 
     * @param variableName 变量名
     * @param value 变量值
     * @param type 变量类型
     * @return 设置成功的变量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVariableEntity setGlobalVariable(String variableName, String value, WorkflowVariableEntity.VariableType type) {
        if (StrUtil.isBlank(variableName)) {
            throw new IllegalArgumentException("变量名不能为空");
        }
        
        log.info("设置全局变量: {} = {}", variableName, value);
        
        // 查找已存在的全局变量
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getVariableName, variableName)
                .eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.GLOBAL);
        
        WorkflowVariableEntity existingVariable = getOne(wrapper);
        
        if (existingVariable != null) {
            // 更新已存在的全局变量
            existingVariable.setVariableValue(value);
            existingVariable.setVariableType(type);
            existingVariable.setUpdatedTime(LocalDateTime.now());
            updateById(existingVariable);
            return existingVariable;
        } else {
            // 创建新的全局变量
            WorkflowVariableEntity variable = new WorkflowVariableEntity();
            variable.setVariableName(variableName);
            variable.setVariableValue(value);
            variable.setVariableType(type);
            variable.setScope(WorkflowVariableEntity.Scope.GLOBAL);
            return createVariable(variable);
        }
    }

    /**
     * 更新变量值
     * 
     * @param variableId 变量ID
     * @param value 新的变量值
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVariableValue(String variableId, String value) {
        if (StrUtil.isBlank(variableId)) {
            return false;
        }
        
        log.info("更新变量值: {}, 新值: {}", variableId, value);
        
        WorkflowVariableEntity variable = getById(variableId);
        if (variable == null) {
            log.warn("变量不存在: {}", variableId);
            return false;
        }
        
        // 验证变量值格式
        if (!validateVariableValue(value, variable.getVariableType())) {
            log.warn("变量值格式不正确: {}", value);
            return false;
        }
        
        variable.setVariableValue(value);
        variable.setUpdatedTime(LocalDateTime.now());
        
        boolean result = updateById(variable);
        
        if (result) {
            log.info("变量值更新成功: {}", variableId);
        } else {
            log.warn("变量值更新失败: {}", variableId);
        }
        
        return result;
    }

    /**
     * 更新变量
     * 
     * @param variable 要更新的变量
     * @return 更新后的变量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVariableEntity updateVariable(WorkflowVariableEntity variable) {
        if (variable == null || StrUtil.isBlank(variable.getVariableId())) {
            throw new IllegalArgumentException("变量或变量ID不能为空");
        }
        
        log.info("更新工作流变量: {}", variable.getVariableId());
        
        // 验证变量值格式
        if (!validateVariableValue(variable.getVariableValue(), variable.getVariableType())) {
            throw new IllegalArgumentException("变量值格式不正确: " + variable.getVariableValue());
        }
        
        // 设置更新时间
        variable.setUpdatedTime(LocalDateTime.now());
        
        // 更新变量
        updateById(variable);
        
        log.info("工作流变量更新成功: {}", variable.getVariableId());
        return variable;
    }

    /**
     * 删除变量
     * 
     * @param variableId 变量ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVariable(String variableId) {
        if (StrUtil.isBlank(variableId)) {
            return false;
        }
        
        log.info("删除工作流变量: {}", variableId);
        
        boolean result = removeById(variableId);
        
        if (result) {
            log.info("工作流变量删除成功: {}", variableId);
        } else {
            log.warn("工作流变量删除失败: {}", variableId);
        }
        
        return result;
    }

    /**
     * 删除实例的所有变量
     * 
     * @param instanceId 实例ID
     * @return 删除的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteVariablesByInstanceId(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return 0;
        }
        
        log.info("删除实例的所有变量: {}", instanceId);
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getInstanceId, instanceId);
        
        List<WorkflowVariableEntity> variables = list(wrapper);
        int count = variables.size();
        
        if (count > 0) {
            remove(wrapper);
            log.info("删除实例变量完成，删除数量: {}", count);
        }
        
        return count;
    }

    /**
     * 删除步骤的所有变量
     * 
     * @param stepId 步骤ID
     * @return 删除的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteVariablesByStepId(String stepId) {
        if (StrUtil.isBlank(stepId)) {
            return 0;
        }
        
        log.info("删除步骤的所有变量: {}", stepId);
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getStepId, stepId)
                .eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.STEP);
        
        List<WorkflowVariableEntity> variables = list(wrapper);
        int count = variables.size();
        
        if (count > 0) {
            remove(wrapper);
            log.info("删除步骤变量完成，删除数量: {}", count);
        }
        
        return count;
    }

    /**
     * 批量设置变量
     * 
     * @param instanceId 实例ID
     * @param variables 变量映射（变量名 -> 变量值）
     * @param scope 作用域
     * @param stepId 步骤ID（当作用域为STEP时必填）
     * @return 设置成功的变量列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WorkflowVariableEntity> batchSetVariables(String instanceId, Map<String, Object> variables, WorkflowVariableEntity.Scope scope, String stepId) {
        if (StrUtil.isBlank(instanceId) || variables == null || variables.isEmpty() || scope == null) {
            return Collections.emptyList();
        }
        
        if (scope == WorkflowVariableEntity.Scope.STEP && StrUtil.isBlank(stepId)) {
            throw new IllegalArgumentException("设置步骤变量时步骤ID不能为空");
        }
        
        log.info("批量设置变量: {} 个变量, 作用域: {}", variables.size(), scope);
        
        List<WorkflowVariableEntity> result = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            Object value = entry.getValue();
            
            if (StrUtil.isBlank(variableName)) {
                continue;
            }
            
            // 推断变量类型
            WorkflowVariableEntity.VariableType type = inferVariableType(value);
            String valueStr = value != null ? value.toString() : null;
            
            WorkflowVariableEntity variable;
            switch (scope) {
                case INSTANCE:
                    variable = setInstanceVariable(instanceId, variableName, valueStr, type);
                    break;
                case STEP:
                    variable = setStepVariable(instanceId, stepId, variableName, valueStr, type);
                    break;
                case GLOBAL:
                    variable = setGlobalVariable(variableName, valueStr, type);
                    break;
                default:
                    continue;
            }
            
            if (variable != null) {
                result.add(variable);
            }
        }
        
        log.info("批量设置变量完成，成功设置: {} 个变量", result.size());
        return result;
    }

    /**
     * 获取实例的所有变量作为Map
     * 
     * @param instanceId 实例ID
     * @return 变量映射（变量名 -> 变量值）
     */
    @Override
    public Map<String, Object> getInstanceVariablesAsMap(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return Collections.emptyMap();
        }
        
        List<WorkflowVariableEntity> variables = getVariablesByInstanceId(instanceId);
        
        return variables.stream()
                .collect(Collectors.toMap(
                        WorkflowVariableEntity::getVariableName,
                        this::convertVariableValue,
                        (existing, replacement) -> existing // 保留第一个值
                ));
    }

    /**
     * 获取步骤的所有变量作为Map
     * 
     * @param stepId 步骤ID
     * @return 变量映射（变量名 -> 变量值）
     */
    @Override
    public Map<String, Object> getStepVariablesAsMap(String stepId) {
        if (StrUtil.isBlank(stepId)) {
            return Collections.emptyMap();
        }
        
        List<WorkflowVariableEntity> variables = getVariablesByStepId(stepId);
        
        return variables.stream()
                .collect(Collectors.toMap(
                        WorkflowVariableEntity::getVariableName,
                        this::convertVariableValue
                ));
    }

    /**
     * 获取全局变量作为Map
     * 
     * @return 变量映射（变量名 -> 变量值）
     */
    @Override
    public Map<String, Object> getGlobalVariablesAsMap() {
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.GLOBAL);
        
        List<WorkflowVariableEntity> variables = list(wrapper);
        
        return variables.stream()
                .collect(Collectors.toMap(
                        WorkflowVariableEntity::getVariableName,
                        this::convertVariableValue
                ));
    }

    /**
     * 获取变量的字符串值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的字符串值
     */
    @Override
    public String getVariableAsString(String instanceId, String variableName, String defaultValue) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        return variable != null ? variable.getVariableValue() : defaultValue;
    }

    /**
     * 获取变量的整数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的整数值
     */
    @Override
    public Integer getVariableAsInteger(String instanceId, String variableName, Integer defaultValue) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable != null) {
            return variable.getAsInteger();
        }
        return defaultValue;
    }

    /**
     * 获取变量的长整数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的长整数值
     */
    @Override
    public Long getVariableAsLong(String instanceId, String variableName, Long defaultValue) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable != null) {
            return variable.getAsLong();
        }
        return defaultValue;
    }

    /**
     * 获取变量的双精度浮点数值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的双精度浮点数值
     */
    @Override
    public Double getVariableAsDouble(String instanceId, String variableName, Double defaultValue) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable != null) {
            return variable.getAsDouble();
        }
        return defaultValue;
    }

    /**
     * 获取变量的布尔值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param defaultValue 默认值
     * @return 变量的布尔值
     */
    @Override
    public Boolean getVariableAsBoolean(String instanceId, String variableName, Boolean defaultValue) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable != null) {
            return variable.getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * 获取变量的JSON对象
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 变量的JSON对象
     */
    @Override
    public <T> T getVariableAsJson(String instanceId, String variableName, Class<T> clazz) {
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable != null) {
            return variable.getAsJson(clazz);
        }
        return null;
    }

    /**
     * 检查变量是否存在
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 是否存在
     */
    @Override
    public boolean hasVariable(String instanceId, String variableName) {
        return getVariableByInstanceIdAndName(instanceId, variableName) != null;
    }

    /**
     * 检查步骤变量是否存在
     * 
     * @param stepId 步骤ID
     * @param variableName 变量名
     * @return 是否存在
     */
    @Override
    public boolean hasStepVariable(String stepId, String variableName) {
        return getVariableByStepIdAndName(stepId, variableName) != null;
    }

    /**
     * 检查全局变量是否存在
     * 
     * @param variableName 变量名
     * @return 是否存在
     */
    @Override
    public boolean hasGlobalVariable(String variableName) {
        if (StrUtil.isBlank(variableName)) {
            return false;
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getVariableName, variableName)
                .eq(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.GLOBAL);
        
        return count(wrapper) > 0;
    }

    /**
     * 复制变量到新实例
     * 
     * @param sourceInstanceId 源实例ID
     * @param targetInstanceId 目标实例ID
     * @param scope 作用域过滤（可选）
     * @return 复制的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyVariablesToInstance(String sourceInstanceId, String targetInstanceId, WorkflowVariableEntity.Scope scope) {
        if (StrUtil.isBlank(sourceInstanceId) || StrUtil.isBlank(targetInstanceId)) {
            return 0;
        }
        
        log.info("复制变量从实例 {} 到实例 {}", sourceInstanceId, targetInstanceId);
        
        List<WorkflowVariableEntity> sourceVariables;
        if (scope != null) {
            sourceVariables = getVariablesByInstanceIdAndScope(sourceInstanceId, scope);
        } else {
            sourceVariables = getVariablesByInstanceId(sourceInstanceId);
        }
        
        int count = 0;
        for (WorkflowVariableEntity sourceVariable : sourceVariables) {
            // 跳过全局变量
            if (sourceVariable.getScope() == WorkflowVariableEntity.Scope.GLOBAL) {
                continue;
            }
            
            WorkflowVariableEntity newVariable = new WorkflowVariableEntity();
            newVariable.setInstanceId(targetInstanceId);
            newVariable.setStepId(sourceVariable.getStepId());
            newVariable.setVariableName(sourceVariable.getVariableName());
            newVariable.setVariableValue(sourceVariable.getVariableValue());
            newVariable.setVariableType(sourceVariable.getVariableType());
            newVariable.setScope(sourceVariable.getScope());
            
            createVariable(newVariable);
            count++;
        }
        
        log.info("变量复制完成，复制数量: {}", count);
        return count;
    }

    /**
     * 获取变量统计信息
     * 
     * @param instanceId 实例ID
     * @return 变量统计信息
     */
    @Override
    public Map<String, Object> getVariableStatistics(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return Collections.emptyMap();
        }
        
        List<WorkflowVariableEntity> variables = getVariablesByInstanceId(instanceId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", variables.size());
        
        // 按作用域统计
        Map<WorkflowVariableEntity.Scope, Long> scopeCount = variables.stream()
                .collect(Collectors.groupingBy(WorkflowVariableEntity::getScope, Collectors.counting()));
        statistics.put("scopeCount", scopeCount);
        
        // 按类型统计
        Map<WorkflowVariableEntity.VariableType, Long> typeCount = variables.stream()
                .collect(Collectors.groupingBy(WorkflowVariableEntity::getVariableType, Collectors.counting()));
        statistics.put("typeCount", typeCount);
        
        return statistics;
    }

    /**
     * 根据类型统计变量数量
     * 
     * @param type 变量类型
     * @return 变量数量
     */
    @Override
    public long countByType(WorkflowVariableEntity.VariableType type) {
        if (type == null) {
            return 0;
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getVariableType, type);
        
        return count(wrapper);
    }

    /**
     * 根据作用域统计变量数量
     * 
     * @param scope 作用域
     * @return 变量数量
     */
    @Override
    public long countByScope(WorkflowVariableEntity.Scope scope) {
        if (scope == null) {
            return 0;
        }
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowVariableEntity::getScope, scope);
        
        return count(wrapper);
    }

    /**
     * 清理过期的变量
     * 
     * @param days 保留天数
     * @return 清理的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredVariables(int days) {
        if (days <= 0) {
            return 0;
        }
        
        log.info("清理 {} 天前的过期变量", days);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        
        LambdaQueryWrapper<WorkflowVariableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(WorkflowVariableEntity::getCreatedTime, cutoffTime)
                .ne(WorkflowVariableEntity::getScope, WorkflowVariableEntity.Scope.GLOBAL); // 不删除全局变量
        
        List<WorkflowVariableEntity> expiredVariables = list(wrapper);
        int count = expiredVariables.size();
        
        if (count > 0) {
            remove(wrapper);
            log.info("清理过期变量完成，清理数量: {}", count);
        }
        
        return count;
    }

    /**
     * 获取变量的历史记录
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名
     * @return 历史记录列表
     */
    @Override
    public List<Map<String, Object>> getVariableHistory(String instanceId, String variableName) {
        // 这里简化实现，实际项目中可能需要单独的历史记录表
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(variableName)) {
            return Collections.emptyList();
        }
        
        WorkflowVariableEntity variable = getVariableByInstanceIdAndName(instanceId, variableName);
        if (variable == null) {
            return Collections.emptyList();
        }
        
        Map<String, Object> record = new HashMap<>();
        record.put("variableId", variable.getVariableId());
        record.put("variableName", variable.getVariableName());
        record.put("variableValue", variable.getVariableValue());
        record.put("variableType", variable.getVariableType());
        record.put("scope", variable.getScope());
        record.put("createdTime", variable.getCreatedTime());
        record.put("updatedTime", variable.getUpdatedTime());
        
        return Collections.singletonList(record);
    }

    /**
     * 导出实例变量
     * 
     * @param instanceId 实例ID
     * @return 变量的JSON字符串
     */
    @Override
    public String exportInstanceVariables(String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            return "{}";
        }
        
        Map<String, Object> variables = getInstanceVariablesAsMap(instanceId);
        return JSONUtil.toJsonStr(variables);
    }

    /**
     * 导入实例变量
     * 
     * @param instanceId 实例ID
     * @param variablesJson 变量的JSON字符串
     * @return 导入成功的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importInstanceVariables(String instanceId, String variablesJson) {
        if (StrUtil.isBlank(instanceId) || StrUtil.isBlank(variablesJson)) {
            return 0;
        }
        
        try {
            Map<String, Object> variables = JSONUtil.toBean(variablesJson, Map.class);
            List<WorkflowVariableEntity> result = batchSetVariables(instanceId, variables, WorkflowVariableEntity.Scope.INSTANCE, null);
            return result.size();
        } catch (Exception e) {
            log.error("导入实例变量失败: {}", instanceId, e);
            return 0;
        }
    }

    /**
     * 合并变量
     * 将源变量合并到目标实例，如果目标实例已存在同名变量则覆盖
     * 
     * @param sourceInstanceId 源实例ID
     * @param targetInstanceId 目标实例ID
     * @param overwrite 是否覆盖已存在的变量
     * @return 合并的变量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int mergeVariables(String sourceInstanceId, String targetInstanceId, boolean overwrite) {
        if (StrUtil.isBlank(sourceInstanceId) || StrUtil.isBlank(targetInstanceId)) {
            return 0;
        }
        
        log.info("合并变量从实例 {} 到实例 {}, 覆盖模式: {}", sourceInstanceId, targetInstanceId, overwrite);
        
        List<WorkflowVariableEntity> sourceVariables = getVariablesByInstanceId(sourceInstanceId);
        Map<String, Object> targetVariables = getInstanceVariablesAsMap(targetInstanceId);
        
        int count = 0;
        for (WorkflowVariableEntity sourceVariable : sourceVariables) {
            // 跳过全局变量
            if (sourceVariable.getScope() == WorkflowVariableEntity.Scope.GLOBAL) {
                continue;
            }
            
            String variableName = sourceVariable.getVariableName();
            
            // 检查是否已存在
            if (!overwrite && targetVariables.containsKey(variableName)) {
                continue;
            }
            
            // 设置变量
            if (sourceVariable.getScope() == WorkflowVariableEntity.Scope.INSTANCE) {
                setInstanceVariable(targetInstanceId, variableName, sourceVariable.getVariableValue(), sourceVariable.getVariableType());
                count++;
            } else if (sourceVariable.getScope() == WorkflowVariableEntity.Scope.STEP) {
                setStepVariable(targetInstanceId, sourceVariable.getStepId(), variableName, sourceVariable.getVariableValue(), sourceVariable.getVariableType());
                count++;
            }
        }
        
        log.info("变量合并完成，合并数量: {}", count);
        return count;
    }

    /**
     * 获取变量的唯一键
     * 
     * @param instanceId 实例ID
     * @param stepId 步骤ID（可选）
     * @param variableName 变量名
     * @param scope 作用域
     * @return 唯一键
     */
    @Override
    public String getVariableUniqueKey(String instanceId, String stepId, String variableName, WorkflowVariableEntity.Scope scope) {
        switch (scope) {
            case GLOBAL:
                return "GLOBAL:" + variableName;
            case INSTANCE:
                return instanceId + ":INSTANCE:" + variableName;
            case STEP:
                return instanceId + ":STEP:" + stepId + ":" + variableName;
            default:
                return variableName;
        }
    }

    /**
     * 验证变量值的格式
     * 
     * @param value 变量值
     * @param type 变量类型
     * @return 是否格式正确
     */
    @Override
    public boolean validateVariableValue(String value, WorkflowVariableEntity.VariableType type) {
        if (value == null || type == null) {
            return true; // 允许空值
        }
        
        try {
            switch (type) {
                case STRING:
                    return true; // 字符串总是有效的
                case INTEGER:
                    Integer.parseInt(value);
                    return true;
                case LONG:
                    Long.parseLong(value);
                    return true;
                case DOUBLE:
                    Double.parseDouble(value);
                    return true;
                case BOOLEAN:
                    // 接受 true/false, 1/0, yes/no
                    String lowerValue = value.toLowerCase();
                    return "true".equals(lowerValue) || "false".equals(lowerValue) ||
                           "1".equals(value) || "0".equals(value) ||
                           "yes".equals(lowerValue) || "no".equals(lowerValue);
                case JSON:
                    JSONUtil.parseObj(value); // 验证JSON格式
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转换变量值类型
     * 
     * @param value 原始值
     * @param fromType 原类型
     * @param toType 目标类型
     * @return 转换后的值
     */
    @Override
    public String convertVariableValue(String value, WorkflowVariableEntity.VariableType fromType, WorkflowVariableEntity.VariableType toType) {
        if (value == null || fromType == toType) {
            return value;
        }
        
        try {
            // 简化的类型转换逻辑
            switch (toType) {
                case STRING:
                    return value;
                case INTEGER:
                    if (fromType == WorkflowVariableEntity.VariableType.DOUBLE) {
                        return String.valueOf(Double.valueOf(value).intValue());
                    } else if (fromType == WorkflowVariableEntity.VariableType.LONG) {
                        return String.valueOf(Long.valueOf(value).intValue());
                    }
                    return value;
                case LONG:
                    if (fromType == WorkflowVariableEntity.VariableType.INTEGER) {
                        return String.valueOf(Integer.valueOf(value).longValue());
                    } else if (fromType == WorkflowVariableEntity.VariableType.DOUBLE) {
                        return String.valueOf(Double.valueOf(value).longValue());
                    }
                    return value;
                case DOUBLE:
                    if (fromType == WorkflowVariableEntity.VariableType.INTEGER) {
                        return String.valueOf(Integer.valueOf(value).doubleValue());
                    } else if (fromType == WorkflowVariableEntity.VariableType.LONG) {
                        return String.valueOf(Long.valueOf(value).doubleValue());
                    }
                    return value;
                case BOOLEAN:
                    if (fromType == WorkflowVariableEntity.VariableType.INTEGER) {
                        return String.valueOf(Integer.valueOf(value) != 0);
                    } else if (fromType == WorkflowVariableEntity.VariableType.STRING) {
                        String lowerValue = value.toLowerCase();
                        if ("yes".equals(lowerValue) || "1".equals(value)) {
                            return "true";
                        } else if ("no".equals(lowerValue) || "0".equals(value)) {
                            return "false";
                        }
                    }
                    return value;
                case JSON:
                    if (fromType == WorkflowVariableEntity.VariableType.STRING) {
                        // 尝试将字符串包装为JSON
                        return JSONUtil.toJsonStr(value);
                    }
                    return value;
                default:
                    return value;
            }
        } catch (Exception e) {
            log.warn("变量值类型转换失败: {} -> {}", fromType, toType, e);
            return value;
        }
    }

    /**
     * 推断变量类型
     * 
     * @param value 变量值
     * @return 推断的变量类型
     */
    private WorkflowVariableEntity.VariableType inferVariableType(Object value) {
        if (value == null) {
            return WorkflowVariableEntity.VariableType.STRING;
        }
        
        if (value instanceof Integer) {
            return WorkflowVariableEntity.VariableType.INTEGER;
        } else if (value instanceof Long) {
            return WorkflowVariableEntity.VariableType.LONG;
        } else if (value instanceof Double || value instanceof Float) {
            return WorkflowVariableEntity.VariableType.DOUBLE;
        } else if (value instanceof Boolean) {
            return WorkflowVariableEntity.VariableType.BOOLEAN;
        } else if (value instanceof Map || value instanceof List) {
            return WorkflowVariableEntity.VariableType.JSON;
        } else {
            String strValue = value.toString();
            
            // 尝试推断数字类型
            try {
                Integer.parseInt(strValue);
                return WorkflowVariableEntity.VariableType.INTEGER;
            } catch (NumberFormatException ignored) {
            }
            
            try {
                Long.parseLong(strValue);
                return WorkflowVariableEntity.VariableType.LONG;
            } catch (NumberFormatException ignored) {
            }
            
            try {
                Double.parseDouble(strValue);
                return WorkflowVariableEntity.VariableType.DOUBLE;
            } catch (NumberFormatException ignored) {
            }
            
            // 尝试推断布尔类型
            String lowerValue = strValue.toLowerCase();
            if ("true".equals(lowerValue) || "false".equals(lowerValue) ||
                "1".equals(strValue) || "0".equals(strValue) ||
                "yes".equals(lowerValue) || "no".equals(lowerValue)) {
                return WorkflowVariableEntity.VariableType.BOOLEAN;
            }
            
            // 尝试推断JSON类型
            try {
                JSONUtil.parseObj(strValue);
                return WorkflowVariableEntity.VariableType.JSON;
            } catch (Exception ignored) {
            }
            
            // 默认为字符串类型
            return WorkflowVariableEntity.VariableType.STRING;
        }
    }

    /**
     * 转换变量值为对应的Java对象
     * 
     * @param variable 工作流变量
     * @return 转换后的值
     */
    private Object convertVariableValue(WorkflowVariableEntity variable) {
        if (variable == null || variable.getVariableValue() == null) {
            return null;
        }
        
        try {
            switch (variable.getVariableType()) {
                case INTEGER:
                    return variable.getAsInteger();
                case LONG:
                    return variable.getAsLong();
                case DOUBLE:
                    return variable.getAsDouble();
                case BOOLEAN:
                    return variable.getAsBoolean();
                case JSON:
                    return variable.getAsJson(Object.class);
                case STRING:
                default:
                    return variable.getVariableValue();
            }
        } catch (Exception e) {
            log.warn("变量值转换失败: {}", variable.getVariableId(), e);
            return variable.getVariableValue();
        }
    }
}