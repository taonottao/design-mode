package com.tao.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.tao.workflow.entity.WorkflowVariableEntity;
import com.tao.workflow.service.WorkflowVariableService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 工作流变量控制器
 * 提供工作流变量的REST API接口
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/variables")
@RequiredArgsConstructor
@Api(tags = "工作流变量管理")
public class WorkflowVariableController {

    private final WorkflowVariableService workflowVariableService;

    /**
     * 创建工作流变量
     * 
     * @param request 创建请求
     * @return 创建的工作流变量
     */
    @PostMapping
    @ApiOperation("创建工作流变量")
    public ResponseEntity<WorkflowVariableEntity> createWorkflowVariable(
            @Valid @RequestBody CreateWorkflowVariableRequest request) {
        log.info("创建工作流变量: {}", request.getVariableName());
        
        WorkflowVariableEntity variable = new WorkflowVariableEntity();
        variable.setInstanceId(request.getInstanceId());
        variable.setStepId(request.getStepId());
        variable.setVariableName(request.getVariableName());
        variable.setVariableValue(request.getVariableValue());
        variable.setVariableType(request.getVariableType());
        variable.setScope(request.getScope());
        variable.setDescription(request.getDescription());
        
        WorkflowVariableEntity result = workflowVariableService.createWorkflowVariable(variable);
        
        log.info("工作流变量创建成功，变量ID: {}", result.getVariableId());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取工作流变量
     * 
     * @param variableId 变量ID
     * @return 工作流变量
     */
    @GetMapping("/{variableId}")
    @ApiOperation("根据ID获取工作流变量")
    public ResponseEntity<WorkflowVariableEntity> getWorkflowVariableById(
            @ApiParam("变量ID") @PathVariable String variableId) {
        log.info("获取工作流变量: {}", variableId);
        
        WorkflowVariableEntity variable = workflowVariableService.getWorkflowVariableById(variableId);
        
        if (variable == null) {
            log.warn("工作流变量不存在: {}", variableId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(variable);
    }

    /**
     * 根据实例ID获取变量列表
     * 
     * @param instanceId 实例ID
     * @return 变量列表
     */
    @GetMapping("/by-instance/{instanceId}")
    @ApiOperation("根据实例ID获取变量列表")
    public ResponseEntity<List<WorkflowVariableEntity>> getVariablesByInstanceId(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例变量列表: {}", instanceId);
        
        List<WorkflowVariableEntity> variables = workflowVariableService.getVariablesByInstanceId(instanceId);
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 根据步骤ID获取变量列表
     * 
     * @param stepId 步骤ID
     * @return 变量列表
     */
    @GetMapping("/by-step/{stepId}")
    @ApiOperation("根据步骤ID获取变量列表")
    public ResponseEntity<List<WorkflowVariableEntity>> getVariablesByStepId(
            @ApiParam("步骤ID") @PathVariable String stepId) {
        log.info("获取步骤变量列表: {}", stepId);
        
        List<WorkflowVariableEntity> variables = workflowVariableService.getVariablesByStepId(stepId);
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 根据名称获取变量
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 工作流变量
     */
    @GetMapping("/by-name")
    @ApiOperation("根据名称获取变量")
    public ResponseEntity<WorkflowVariableEntity> getVariableByName(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("根据名称获取变量: {} - {}", instanceId, variableName);
        
        WorkflowVariableEntity variable = workflowVariableService.getVariableByName(instanceId, variableName);
        
        if (variable == null) {
            log.warn("工作流变量不存在: {} - {}", instanceId, variableName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(variable);
    }

    /**
     * 设置实例变量
     * 
     * @param instanceId 实例ID
     * @param request 设置变量请求
     * @return 操作结果
     */
    @PostMapping("/instance/{instanceId}")
    @ApiOperation("设置实例变量")
    public ResponseEntity<Void> setInstanceVariable(
            @ApiParam("实例ID") @PathVariable String instanceId,
            @Valid @RequestBody SetVariableRequest request) {
        log.info("设置实例变量: {} - {}", instanceId, request.getVariableName());
        
        boolean result = workflowVariableService.setInstanceVariable(
                instanceId, request.getVariableName(), request.getVariableValue(), 
                request.getVariableType());
        
        if (result) {
            log.info("实例变量设置成功: {} - {}", instanceId, request.getVariableName());
            return ResponseEntity.ok().build();
        } else {
            log.warn("实例变量设置失败: {} - {}", instanceId, request.getVariableName());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 设置步骤变量
     * 
     * @param stepId 步骤ID
     * @param request 设置变量请求
     * @return 操作结果
     */
    @PostMapping("/step/{stepId}")
    @ApiOperation("设置步骤变量")
    public ResponseEntity<Void> setStepVariable(
            @ApiParam("步骤ID") @PathVariable String stepId,
            @Valid @RequestBody SetVariableRequest request) {
        log.info("设置步骤变量: {} - {}", stepId, request.getVariableName());
        
        boolean result = workflowVariableService.setStepVariable(
                stepId, request.getVariableName(), request.getVariableValue(), 
                request.getVariableType());
        
        if (result) {
            log.info("步骤变量设置成功: {} - {}", stepId, request.getVariableName());
            return ResponseEntity.ok().build();
        } else {
            log.warn("步骤变量设置失败: {} - {}", stepId, request.getVariableName());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 设置全局变量
     * 
     * @param request 设置变量请求
     * @return 操作结果
     */
    @PostMapping("/global")
    @ApiOperation("设置全局变量")
    public ResponseEntity<Void> setGlobalVariable(
            @Valid @RequestBody SetVariableRequest request) {
        log.info("设置全局变量: {}", request.getVariableName());
        
        boolean result = workflowVariableService.setGlobalVariable(
                request.getVariableName(), request.getVariableValue(), 
                request.getVariableType());
        
        if (result) {
            log.info("全局变量设置成功: {}", request.getVariableName());
            return ResponseEntity.ok().build();
        } else {
            log.warn("全局变量设置失败: {}", request.getVariableName());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新变量值
     * 
     * @param variableId 变量ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{variableId}/value")
    @ApiOperation("更新变量值")
    public ResponseEntity<Void> updateVariableValue(
            @ApiParam("变量ID") @PathVariable String variableId,
            @Valid @RequestBody UpdateVariableValueRequest request) {
        log.info("更新变量值: {} -> {}", variableId, request.getVariableValue());
        
        boolean result = workflowVariableService.updateVariableValue(variableId, request.getVariableValue());
        
        if (result) {
            log.info("变量值更新成功: {}", variableId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("变量值更新失败: {}", variableId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除工作流变量
     * 
     * @param variableId 变量ID
     * @return 操作结果
     */
    @DeleteMapping("/{variableId}")
    @ApiOperation("删除工作流变量")
    public ResponseEntity<Void> deleteWorkflowVariable(
            @ApiParam("变量ID") @PathVariable String variableId) {
        log.info("删除工作流变量: {}", variableId);
        
        boolean result = workflowVariableService.deleteWorkflowVariable(variableId);
        
        if (result) {
            log.info("工作流变量删除成功: {}", variableId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流变量删除失败: {}", variableId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除实例变量
     * 
     * @param instanceId 实例ID
     * @return 删除数量
     */
    @DeleteMapping("/instance/{instanceId}")
    @ApiOperation("删除实例变量")
    public ResponseEntity<Integer> deleteVariablesByInstanceId(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("删除实例变量: {}", instanceId);
        
        int count = workflowVariableService.deleteVariablesByInstanceId(instanceId);
        
        log.info("实例变量删除完成，删除数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 删除步骤变量
     * 
     * @param stepId 步骤ID
     * @return 删除数量
     */
    @DeleteMapping("/step/{stepId}")
    @ApiOperation("删除步骤变量")
    public ResponseEntity<Integer> deleteVariablesByStepId(
            @ApiParam("步骤ID") @PathVariable String stepId) {
        log.info("删除步骤变量: {}", stepId);
        
        int count = workflowVariableService.deleteVariablesByStepId(stepId);
        
        log.info("步骤变量删除完成，删除数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 批量设置变量
     * 
     * @param request 批量设置请求
     * @return 设置数量
     */
    @PostMapping("/batch")
    @ApiOperation("批量设置变量")
    public ResponseEntity<Integer> batchSetVariables(
            @Valid @RequestBody BatchSetVariablesRequest request) {
        log.info("批量设置变量: {} 个变量", request.getVariables().size());
        
        int count = workflowVariableService.batchSetVariables(
                request.getInstanceId(), request.getStepId(), request.getVariables());
        
        log.info("批量设置变量完成，设置数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取实例变量映射
     * 
     * @param instanceId 实例ID
     * @return 变量映射
     */
    @GetMapping("/instance/{instanceId}/map")
    @ApiOperation("获取实例变量映射")
    public ResponseEntity<Map<String, Object>> getInstanceVariablesAsMap(
            @ApiParam("实例ID") @PathVariable String instanceId) {
        log.info("获取实例变量映射: {}", instanceId);
        
        Map<String, Object> variables = workflowVariableService.getInstanceVariablesAsMap(instanceId);
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 获取步骤变量映射
     * 
     * @param stepId 步骤ID
     * @return 变量映射
     */
    @GetMapping("/step/{stepId}/map")
    @ApiOperation("获取步骤变量映射")
    public ResponseEntity<Map<String, Object>> getStepVariablesAsMap(
            @ApiParam("步骤ID") @PathVariable String stepId) {
        log.info("获取步骤变量映射: {}", stepId);
        
        Map<String, Object> variables = workflowVariableService.getStepVariablesAsMap(stepId);
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 获取全局变量映射
     * 
     * @return 变量映射
     */
    @GetMapping("/global/map")
    @ApiOperation("获取全局变量映射")
    public ResponseEntity<Map<String, Object>> getGlobalVariablesAsMap() {
        log.info("获取全局变量映射");
        
        Map<String, Object> variables = workflowVariableService.getGlobalVariablesAsMap();
        
        return ResponseEntity.ok(variables);
    }

    /**
     * 获取字符串类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 字符串值
     */
    @GetMapping("/string")
    @ApiOperation("获取字符串类型变量值")
    public ResponseEntity<String> getVariableAsString(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取字符串类型变量值: {} - {}", instanceId, variableName);
        
        String value = workflowVariableService.getVariableAsString(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 获取整数类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 整数值
     */
    @GetMapping("/integer")
    @ApiOperation("获取整数类型变量值")
    public ResponseEntity<Integer> getVariableAsInteger(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取整数类型变量值: {} - {}", instanceId, variableName);
        
        Integer value = workflowVariableService.getVariableAsInteger(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 获取长整数类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 长整数值
     */
    @GetMapping("/long")
    @ApiOperation("获取长整数类型变量值")
    public ResponseEntity<Long> getVariableAsLong(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取长整数类型变量值: {} - {}", instanceId, variableName);
        
        Long value = workflowVariableService.getVariableAsLong(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 获取双精度类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 双精度值
     */
    @GetMapping("/double")
    @ApiOperation("获取双精度类型变量值")
    public ResponseEntity<Double> getVariableAsDouble(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取双精度类型变量值: {} - {}", instanceId, variableName);
        
        Double value = workflowVariableService.getVariableAsDouble(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 获取布尔类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 布尔值
     */
    @GetMapping("/boolean")
    @ApiOperation("获取布尔类型变量值")
    public ResponseEntity<Boolean> getVariableAsBoolean(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取布尔类型变量值: {} - {}", instanceId, variableName);
        
        Boolean value = workflowVariableService.getVariableAsBoolean(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 获取JSON类型变量值
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return JSON对象
     */
    @GetMapping("/json")
    @ApiOperation("获取JSON类型变量值")
    public ResponseEntity<Map<String, Object>> getVariableAsJson(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取JSON类型变量值: {} - {}", instanceId, variableName);
        
        Map<String, Object> value = workflowVariableService.getVariableAsJson(instanceId, variableName);
        
        return ResponseEntity.ok(value);
    }

    /**
     * 检查变量是否存在
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 是否存在
     */
    @GetMapping("/exists")
    @ApiOperation("检查变量是否存在")
    public ResponseEntity<Boolean> variableExists(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("检查变量是否存在: {} - {}", instanceId, variableName);
        
        boolean exists = workflowVariableService.variableExists(instanceId, variableName);
        
        return ResponseEntity.ok(exists);
    }

    /**
     * 复制变量
     * 
     * @param request 复制变量请求
     * @return 操作结果
     */
    @PostMapping("/copy")
    @ApiOperation("复制变量")
    public ResponseEntity<Void> copyVariables(
            @Valid @RequestBody CopyVariablesRequest request) {
        log.info("复制变量: {} -> {}", request.getSourceInstanceId(), request.getTargetInstanceId());
        
        boolean result = workflowVariableService.copyVariables(
                request.getSourceInstanceId(), request.getTargetInstanceId());
        
        if (result) {
            log.info("变量复制成功: {} -> {}", request.getSourceInstanceId(), request.getTargetInstanceId());
            return ResponseEntity.ok().build();
        } else {
            log.warn("变量复制失败: {} -> {}", request.getSourceInstanceId(), request.getTargetInstanceId());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取变量统计信息
     * 
     * @param instanceId 实例ID（可选）
     * @return 变量统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取变量统计信息")
    public ResponseEntity<Map<String, Object>> getVariableStatistics(
            @ApiParam("实例ID") @RequestParam(required = false) String instanceId) {
        log.info("获取变量统计信息，实例ID: {}", instanceId);
        
        Map<String, Object> statistics = workflowVariableService.getVariableStatistics(instanceId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 根据类型统计变量数量
     * 
     * @param variableType 变量类型
     * @return 变量数量
     */
    @GetMapping("/count-by-type")
    @ApiOperation("根据类型统计变量数量")
    public ResponseEntity<Long> countVariablesByType(
            @ApiParam("变量类型") @RequestParam String variableType) {
        log.info("统计变量数量，类型: {}", variableType);
        
        long count = workflowVariableService.countVariablesByType(variableType);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 根据作用域统计变量数量
     * 
     * @param scope 作用域
     * @return 变量数量
     */
    @GetMapping("/count-by-scope")
    @ApiOperation("根据作用域统计变量数量")
    public ResponseEntity<Long> countVariablesByScope(
            @ApiParam("作用域") @RequestParam WorkflowVariableEntity.Scope scope) {
        log.info("统计变量数量，作用域: {}", scope);
        
        long count = workflowVariableService.countVariablesByScope(scope);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 清理过期变量
     * 
     * @param days 保留天数
     * @return 清理数量
     */
    @PostMapping("/cleanup")
    @ApiOperation("清理过期变量")
    public ResponseEntity<Integer> cleanupExpiredVariables(
            @ApiParam("保留天数") @RequestParam(defaultValue = "90") int days) {
        log.info("清理过期变量，保留天数: {}", days);
        
        int count = workflowVariableService.cleanupExpiredVariables(days);
        
        log.info("清理过期变量完成，清理数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取变量历史
     * 
     * @param instanceId 实例ID
     * @param variableName 变量名称
     * @return 变量历史
     */
    @GetMapping("/history")
    @ApiOperation("获取变量历史")
    public ResponseEntity<List<Map<String, Object>>> getVariableHistory(
            @ApiParam("实例ID") @RequestParam String instanceId,
            @ApiParam("变量名称") @RequestParam String variableName) {
        log.info("获取变量历史: {} - {}", instanceId, variableName);
        
        List<Map<String, Object>> history = workflowVariableService.getVariableHistory(instanceId, variableName);
        
        return ResponseEntity.ok(history);
    }

    /**
     * 导出变量
     * 
     * @param instanceId 实例ID
     * @return 导出数据
     */
    @GetMapping("/export")
    @ApiOperation("导出变量")
    public ResponseEntity<Map<String, Object>> exportVariables(
            @ApiParam("实例ID") @RequestParam String instanceId) {
        log.info("导出变量: {}", instanceId);
        
        Map<String, Object> exportData = workflowVariableService.exportVariables(instanceId);
        
        return ResponseEntity.ok(exportData);
    }

    /**
     * 导入变量
     * 
     * @param request 导入请求
     * @return 导入数量
     */
    @PostMapping("/import")
    @ApiOperation("导入变量")
    public ResponseEntity<Integer> importVariables(
            @Valid @RequestBody ImportVariablesRequest request) {
        log.info("导入变量: {}", request.getInstanceId());
        
        int count = workflowVariableService.importVariables(request.getInstanceId(), request.getVariableData());
        
        log.info("导入变量完成，导入数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 合并变量
     * 
     * @param request 合并变量请求
     * @return 操作结果
     */
    @PostMapping("/merge")
    @ApiOperation("合并变量")
    public ResponseEntity<Void> mergeVariables(
            @Valid @RequestBody MergeVariablesRequest request) {
        log.info("合并变量: {} + {}", request.getInstanceId(), request.getVariables().size());
        
        boolean result = workflowVariableService.mergeVariables(request.getInstanceId(), request.getVariables());
        
        if (result) {
            log.info("变量合并成功: {}", request.getInstanceId());
            return ResponseEntity.ok().build();
        } else {
            log.warn("变量合并失败: {}", request.getInstanceId());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成唯一键
     * 
     * @param prefix 前缀
     * @return 唯一键
     */
    @GetMapping("/generate-key")
    @ApiOperation("生成唯一键")
    public ResponseEntity<String> generateUniqueKey(
            @ApiParam("前缀") @RequestParam(required = false) String prefix) {
        log.info("生成唯一键，前缀: {}", prefix);
        
        String uniqueKey = workflowVariableService.generateUniqueKey(prefix);
        
        return ResponseEntity.ok(uniqueKey);
    }

    /**
     * 验证变量值格式
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    @ApiOperation("验证变量值格式")
    public ResponseEntity<Boolean> validateVariableValue(
            @Valid @RequestBody ValidateVariableRequest request) {
        log.info("验证变量值格式: {} - {}", request.getVariableType(), request.getVariableValue());
        
        boolean isValid = workflowVariableService.validateVariableValue(
                request.getVariableValue(), request.getVariableType());
        
        return ResponseEntity.ok(isValid);
    }

    /**
     * 转换变量类型
     * 
     * @param variableId 变量ID
     * @param request 转换请求
     * @return 操作结果
     */
    @PostMapping("/{variableId}/convert")
    @ApiOperation("转换变量类型")
    public ResponseEntity<Void> convertVariableType(
            @ApiParam("变量ID") @PathVariable String variableId,
            @Valid @RequestBody ConvertVariableTypeRequest request) {
        log.info("转换变量类型: {} -> {}", variableId, request.getTargetType());
        
        boolean result = workflowVariableService.convertVariableType(variableId, request.getTargetType());
        
        if (result) {
            log.info("变量类型转换成功: {}", variableId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("变量类型转换失败: {}", variableId);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 请求和响应类 ====================

    /**
     * 创建工作流变量请求
     */
    @lombok.Data
    public static class CreateWorkflowVariableRequest {
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("步骤ID")
        private String stepId;
        
        @ApiParam("变量名称")
        private String variableName;
        
        @ApiParam("变量值")
        private String variableValue;
        
        @ApiParam("变量类型")
        private String variableType;
        
        @ApiParam("作用域")
        private WorkflowVariableEntity.Scope scope;
        
        @ApiParam("描述")
        private String description;
    }

    /**
     * 设置变量请求
     */
    @lombok.Data
    public static class SetVariableRequest {
        @ApiParam("变量名称")
        private String variableName;
        
        @ApiParam("变量值")
        private String variableValue;
        
        @ApiParam("变量类型")
        private String variableType;
    }

    /**
     * 更新变量值请求
     */
    @lombok.Data
    public static class UpdateVariableValueRequest {
        @ApiParam("变量值")
        private String variableValue;
    }

    /**
     * 批量设置变量请求
     */
    @lombok.Data
    public static class BatchSetVariablesRequest {
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("步骤ID")
        private String stepId;
        
        @ApiParam("变量映射")
        private Map<String, Object> variables;
    }

    /**
     * 复制变量请求
     */
    @lombok.Data
    public static class CopyVariablesRequest {
        @ApiParam("源实例ID")
        private String sourceInstanceId;
        
        @ApiParam("目标实例ID")
        private String targetInstanceId;
    }

    /**
     * 导入变量请求
     */
    @lombok.Data
    public static class ImportVariablesRequest {
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("变量数据")
        private Map<String, Object> variableData;
    }

    /**
     * 合并变量请求
     */
    @lombok.Data
    public static class MergeVariablesRequest {
        @ApiParam("实例ID")
        private String instanceId;
        
        @ApiParam("变量映射")
        private Map<String, Object> variables;
    }

    /**
     * 验证变量请求
     */
    @lombok.Data
    public static class ValidateVariableRequest {
        @ApiParam("变量值")
        private String variableValue;
        
        @ApiParam("变量类型")
        private String variableType;
    }

    /**
     * 转换变量类型请求
     */
    @lombok.Data
    public static class ConvertVariableTypeRequest {
        @ApiParam("目标类型")
        private String targetType;
    }
}