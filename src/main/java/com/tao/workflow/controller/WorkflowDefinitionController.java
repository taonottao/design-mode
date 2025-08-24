package com.tao.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tao.workflow.entity.WorkflowDefinitionEntity;
import com.tao.workflow.service.WorkflowDefinitionService;
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
 * 工作流定义控制器
 * 提供工作流定义的REST API接口
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/definitions")
@RequiredArgsConstructor
@Api(tags = "工作流定义管理")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    /**
     * 创建工作流定义
     * 
     * @param request 创建请求
     * @return 创建成功的工作流定义
     */
    @PostMapping
    @ApiOperation("创建工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> createWorkflowDefinition(
            @Valid @RequestBody CreateWorkflowDefinitionRequest request) {
        log.info("创建工作流定义: {}", request.getWorkflowName());
        
        WorkflowDefinitionEntity definition = new WorkflowDefinitionEntity();
        definition.setWorkflowName(request.getWorkflowName());
        definition.setWorkflowKey(request.getWorkflowKey());
        definition.setVersion(request.getVersion());
        definition.setDescription(request.getDescription());
        definition.setWorkflowJson(request.getWorkflowJson());
        definition.setCreatedBy(request.getCreatedBy());
        definition.setCategory(request.getCategory());
        definition.setTags(request.getTags());
        
        WorkflowDefinitionEntity result = workflowDefinitionService.createWorkflowDefinition(definition);
        
        log.info("工作流定义创建成功，ID: {}", result.getDefinitionId());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取工作流定义
     * 
     * @param definitionId 定义ID
     * @return 工作流定义
     */
    @GetMapping("/{definitionId}")
    @ApiOperation("根据ID获取工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> getWorkflowDefinitionById(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("获取工作流定义: {}", definitionId);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.getWorkflowDefinitionById(definitionId);
        
        if (definition == null) {
            log.warn("工作流定义不存在: {}", definitionId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(definition);
    }

    /**
     * 根据名称和版本获取工作流定义
     * 
     * @param workflowName 工作流名称
     * @param version 版本号
     * @return 工作流定义
     */
    @GetMapping("/by-name-version")
    @ApiOperation("根据名称和版本获取工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> getWorkflowDefinitionByNameAndVersion(
            @ApiParam("工作流名称") @RequestParam String workflowName,
            @ApiParam("版本号") @RequestParam String version) {
        log.info("获取工作流定义: {} - {}", workflowName, version);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.getWorkflowDefinitionByNameAndVersion(workflowName, version);
        
        if (definition == null) {
            log.warn("工作流定义不存在: {} - {}", workflowName, version);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(definition);
    }

    /**
     * 获取最新版本的工作流定义
     * 
     * @param workflowName 工作流名称
     * @return 最新版本的工作流定义
     */
    @GetMapping("/latest")
    @ApiOperation("获取最新版本的工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> getLatestWorkflowDefinition(
            @ApiParam("工作流名称") @RequestParam String workflowName) {
        log.info("获取最新版本工作流定义: {}", workflowName);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.getLatestWorkflowDefinition(workflowName);
        
        if (definition == null) {
            log.warn("工作流定义不存在: {}", workflowName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(definition);
    }

    /**
     * 获取活跃的工作流定义列表
     * 
     * @return 活跃的工作流定义列表
     */
    @GetMapping("/active")
    @ApiOperation("获取活跃的工作流定义列表")
    public ResponseEntity<List<WorkflowDefinitionEntity>> getActiveWorkflowDefinitions() {
        log.info("获取活跃的工作流定义列表");
        
        List<WorkflowDefinitionEntity> definitions = workflowDefinitionService.getActiveWorkflowDefinitions();
        
        return ResponseEntity.ok(definitions);
    }

    /**
     * 分页查询工作流定义
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    @ApiOperation("分页查询工作流定义")
    public ResponseEntity<IPage<WorkflowDefinitionEntity>> getWorkflowDefinitionsWithConditions(
            @Valid @RequestBody QueryWorkflowDefinitionRequest request) {
        log.info("分页查询工作流定义，页码: {}, 页大小: {}", request.getCurrent(), request.getSize());
        
        IPage<WorkflowDefinitionEntity> page = workflowDefinitionService.getWorkflowDefinitionsWithConditions(
                request.getCurrent(), request.getSize(), request.getWorkflowName(), 
                request.getCategory(), request.getStatus(), request.getCreatedBy());
        
        return ResponseEntity.ok(page);
    }

    /**
     * 更新工作流定义
     * 
     * @param definitionId 定义ID
     * @param request 更新请求
     * @return 更新后的工作流定义
     */
    @PutMapping("/{definitionId}")
    @ApiOperation("更新工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> updateWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId,
            @Valid @RequestBody UpdateWorkflowDefinitionRequest request) {
        log.info("更新工作流定义: {}", definitionId);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.getWorkflowDefinitionById(definitionId);
        if (definition == null) {
            log.warn("工作流定义不存在: {}", definitionId);
            return ResponseEntity.notFound().build();
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(request.getDescription())) {
            definition.setDescription(request.getDescription());
        }
        if (StrUtil.isNotBlank(request.getWorkflowJson())) {
            definition.setWorkflowJson(request.getWorkflowJson());
        }
        if (StrUtil.isNotBlank(request.getCategory())) {
            definition.setCategory(request.getCategory());
        }
        if (StrUtil.isNotBlank(request.getTags())) {
            definition.setTags(request.getTags());
        }
        
        WorkflowDefinitionEntity result = workflowDefinitionService.updateWorkflowDefinition(definition);
        
        log.info("工作流定义更新成功: {}", definitionId);
        return ResponseEntity.ok(result);
    }

    /**
     * 激活工作流定义
     * 
     * @param definitionId 定义ID
     * @return 操作结果
     */
    @PostMapping("/{definitionId}/activate")
    @ApiOperation("激活工作流定义")
    public ResponseEntity<Void> activateWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("激活工作流定义: {}", definitionId);
        
        boolean result = workflowDefinitionService.activateWorkflowDefinition(definitionId);
        
        if (result) {
            log.info("工作流定义激活成功: {}", definitionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流定义激活失败: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 停用工作流定义
     * 
     * @param definitionId 定义ID
     * @return 操作结果
     */
    @PostMapping("/{definitionId}/deactivate")
    @ApiOperation("停用工作流定义")
    public ResponseEntity<Void> deactivateWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("停用工作流定义: {}", definitionId);
        
        boolean result = workflowDefinitionService.deactivateWorkflowDefinition(definitionId);
        
        if (result) {
            log.info("工作流定义停用成功: {}", definitionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流定义停用失败: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 弃用工作流定义
     * 
     * @param definitionId 定义ID
     * @return 操作结果
     */
    @PostMapping("/{definitionId}/deprecate")
    @ApiOperation("弃用工作流定义")
    public ResponseEntity<Void> deprecateWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("弃用工作流定义: {}", definitionId);
        
        boolean result = workflowDefinitionService.deprecateWorkflowDefinition(definitionId);
        
        if (result) {
            log.info("工作流定义弃用成功: {}", definitionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流定义弃用失败: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 软删除工作流定义
     * 
     * @param definitionId 定义ID
     * @return 操作结果
     */
    @DeleteMapping("/{definitionId}")
    @ApiOperation("软删除工作流定义")
    public ResponseEntity<Void> softDeleteWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("软删除工作流定义: {}", definitionId);
        
        boolean result = workflowDefinitionService.softDeleteWorkflowDefinition(definitionId);
        
        if (result) {
            log.info("工作流定义软删除成功: {}", definitionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流定义软删除失败: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 恢复已删除的工作流定义
     * 
     * @param definitionId 定义ID
     * @return 操作结果
     */
    @PostMapping("/{definitionId}/restore")
    @ApiOperation("恢复已删除的工作流定义")
    public ResponseEntity<Void> restoreWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("恢复工作流定义: {}", definitionId);
        
        boolean result = workflowDefinitionService.restoreWorkflowDefinition(definitionId);
        
        if (result) {
            log.info("工作流定义恢复成功: {}", definitionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("工作流定义恢复失败: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量更新工作流定义状态
     * 
     * @param request 批量更新请求
     * @return 更新数量
     */
    @PostMapping("/batch-update-status")
    @ApiOperation("批量更新工作流定义状态")
    public ResponseEntity<Integer> batchUpdateStatus(
            @Valid @RequestBody BatchUpdateStatusRequest request) {
        log.info("批量更新工作流定义状态: {} 个定义", request.getDefinitionIds().size());
        
        int count = workflowDefinitionService.batchUpdateStatus(request.getDefinitionIds(), request.getStatus());
        
        log.info("批量更新工作流定义状态完成，更新数量: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * 检查工作流定义是否存在
     * 
     * @param workflowName 工作流名称
     * @param version 版本号
     * @return 是否存在
     */
    @GetMapping("/exists")
    @ApiOperation("检查工作流定义是否存在")
    public ResponseEntity<Boolean> existsWorkflowDefinition(
            @ApiParam("工作流名称") @RequestParam String workflowName,
            @ApiParam("版本号") @RequestParam String version) {
        log.info("检查工作流定义是否存在: {} - {}", workflowName, version);
        
        boolean exists = workflowDefinitionService.existsWorkflowDefinition(workflowName, version);
        
        return ResponseEntity.ok(exists);
    }

    /**
     * 搜索工作流定义
     * 
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    @ApiOperation("搜索工作流定义")
    public ResponseEntity<List<WorkflowDefinitionEntity>> searchWorkflowDefinitions(
            @ApiParam("关键词") @RequestParam String keyword) {
        log.info("搜索工作流定义: {}", keyword);
        
        List<WorkflowDefinitionEntity> definitions = workflowDefinitionService.searchWorkflowDefinitions(keyword);
        
        return ResponseEntity.ok(definitions);
    }

    /**
     * 获取工作流定义统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取工作流定义统计信息")
    public ResponseEntity<Map<String, Object>> getWorkflowDefinitionStatistics() {
        log.info("获取工作流定义统计信息");
        
        Map<String, Object> statistics = workflowDefinitionService.getWorkflowDefinitionStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 根据创建者统计工作流定义数量
     * 
     * @param createdBy 创建者
     * @return 定义数量
     */
    @GetMapping("/count-by-creator")
    @ApiOperation("根据创建者统计工作流定义数量")
    public ResponseEntity<Long> countByCreator(
            @ApiParam("创建者") @RequestParam String createdBy) {
        log.info("统计创建者的工作流定义数量: {}", createdBy);
        
        long count = workflowDefinitionService.countByCreator(createdBy);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 根据状态统计工作流定义数量
     * 
     * @param status 状态
     * @return 定义数量
     */
    @GetMapping("/count-by-status")
    @ApiOperation("根据状态统计工作流定义数量")
    public ResponseEntity<Long> countByStatus(
            @ApiParam("状态") @RequestParam WorkflowDefinitionEntity.Status status) {
        log.info("统计状态的工作流定义数量: {}", status);
        
        long count = workflowDefinitionService.countByStatus(status);
        
        return ResponseEntity.ok(count);
    }

    /**
     * 获取最近创建的工作流定义
     * 
     * @param limit 限制数量
     * @return 最近创建的工作流定义列表
     */
    @GetMapping("/recent")
    @ApiOperation("获取最近创建的工作流定义")
    public ResponseEntity<List<WorkflowDefinitionEntity>> getRecentWorkflowDefinitions(
            @ApiParam("限制数量") @RequestParam(defaultValue = "10") int limit) {
        log.info("获取最近创建的工作流定义，限制数量: {}", limit);
        
        List<WorkflowDefinitionEntity> definitions = workflowDefinitionService.getRecentWorkflowDefinitions(limit);
        
        return ResponseEntity.ok(definitions);
    }

    /**
     * 获取即将过期的工作流定义
     * 
     * @param days 天数
     * @return 即将过期的工作流定义列表
     */
    @GetMapping("/expiring")
    @ApiOperation("获取即将过期的工作流定义")
    public ResponseEntity<List<WorkflowDefinitionEntity>> getExpiringWorkflowDefinitions(
            @ApiParam("天数") @RequestParam(defaultValue = "30") int days) {
        log.info("获取即将过期的工作流定义，天数: {}", days);
        
        List<WorkflowDefinitionEntity> definitions = workflowDefinitionService.getExpiringWorkflowDefinitions(days);
        
        return ResponseEntity.ok(definitions);
    }

    /**
     * 创建新版本的工作流定义
     * 
     * @param workflowName 工作流名称
     * @param request 创建新版本请求
     * @return 新版本的工作流定义
     */
    @PostMapping("/create-version")
    @ApiOperation("创建新版本的工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> createNewVersion(
            @ApiParam("工作流名称") @RequestParam String workflowName,
            @Valid @RequestBody CreateVersionRequest request) {
        log.info("创建新版本工作流定义: {}", workflowName);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.createNewVersion(
                workflowName, request.getWorkflowJson(), request.getCreatedBy());
        
        if (definition == null) {
            log.warn("创建新版本失败，工作流不存在: {}", workflowName);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("新版本工作流定义创建成功，版本: {}", definition.getVersion());
        return ResponseEntity.ok(definition);
    }

    /**
     * 获取工作流的所有版本
     * 
     * @param workflowName 工作流名称
     * @return 所有版本的工作流定义列表
     */
    @GetMapping("/versions")
    @ApiOperation("获取工作流的所有版本")
    public ResponseEntity<List<WorkflowDefinitionEntity>> getAllVersionsByName(
            @ApiParam("工作流名称") @RequestParam String workflowName) {
        log.info("获取工作流的所有版本: {}", workflowName);
        
        List<WorkflowDefinitionEntity> definitions = workflowDefinitionService.getAllVersionsByName(workflowName);
        
        return ResponseEntity.ok(definitions);
    }

    /**
     * 验证工作流JSON格式
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate-json")
    @ApiOperation("验证工作流JSON格式")
    public ResponseEntity<Boolean> validateWorkflowJson(
            @Valid @RequestBody ValidateJsonRequest request) {
        log.info("验证工作流JSON格式");
        
        boolean isValid = workflowDefinitionService.validateWorkflowJson(request.getWorkflowJson());
        
        return ResponseEntity.ok(isValid);
    }

    /**
     * 导出工作流定义
     * 
     * @param definitionId 定义ID
     * @return 导出的JSON字符串
     */
    @GetMapping("/{definitionId}/export")
    @ApiOperation("导出工作流定义")
    public ResponseEntity<String> exportWorkflowDefinition(
            @ApiParam("定义ID") @PathVariable String definitionId) {
        log.info("导出工作流定义: {}", definitionId);
        
        String exportJson = workflowDefinitionService.exportWorkflowDefinition(definitionId);
        
        if (exportJson == null) {
            log.warn("工作流定义不存在: {}", definitionId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(exportJson);
    }

    /**
     * 导入工作流定义
     * 
     * @param request 导入请求
     * @return 导入的工作流定义
     */
    @PostMapping("/import")
    @ApiOperation("导入工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> importWorkflowDefinition(
            @Valid @RequestBody ImportDefinitionRequest request) {
        log.info("导入工作流定义");
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.importWorkflowDefinition(
                request.getDefinitionJson(), request.getCreatedBy());
        
        if (definition == null) {
            log.warn("导入工作流定义失败");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("工作流定义导入成功，ID: {}", definition.getDefinitionId());
        return ResponseEntity.ok(definition);
    }

    /**
     * 复制工作流定义
     * 
     * @param definitionId 源定义ID
     * @param request 复制请求
     * @return 复制的工作流定义
     */
    @PostMapping("/{definitionId}/copy")
    @ApiOperation("复制工作流定义")
    public ResponseEntity<WorkflowDefinitionEntity> copyWorkflowDefinition(
            @ApiParam("源定义ID") @PathVariable String definitionId,
            @Valid @RequestBody CopyDefinitionRequest request) {
        log.info("复制工作流定义: {}", definitionId);
        
        WorkflowDefinitionEntity definition = workflowDefinitionService.copyWorkflowDefinition(
                definitionId, request.getNewWorkflowName(), request.getCreatedBy());
        
        if (definition == null) {
            log.warn("复制工作流定义失败，源定义不存在: {}", definitionId);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("工作流定义复制成功，新定义ID: {}", definition.getDefinitionId());
        return ResponseEntity.ok(definition);
    }

    // ==================== 请求和响应类 ====================

    /**
     * 创建工作流定义请求
     */
    @lombok.Data
    public static class CreateWorkflowDefinitionRequest {
        @ApiParam("工作流名称")
        private String workflowName;
        
        @ApiParam("工作流键")
        private String workflowKey;
        
        @ApiParam("版本号")
        private String version;
        
        @ApiParam("描述")
        private String description;
        
        @ApiParam("工作流JSON")
        private String workflowJson;
        
        @ApiParam("创建者")
        private String createdBy;
        
        @ApiParam("分类")
        private String category;
        
        @ApiParam("标签")
        private String tags;
    }

    /**
     * 查询工作流定义请求
     */
    @lombok.Data
    public static class QueryWorkflowDefinitionRequest {
        @ApiParam("当前页")
        private long current = 1;
        
        @ApiParam("页大小")
        private long size = 10;
        
        @ApiParam("工作流名称")
        private String workflowName;
        
        @ApiParam("分类")
        private String category;
        
        @ApiParam("状态")
        private WorkflowDefinitionEntity.Status status;
        
        @ApiParam("创建者")
        private String createdBy;
    }

    /**
     * 更新工作流定义请求
     */
    @lombok.Data
    public static class UpdateWorkflowDefinitionRequest {
        @ApiParam("描述")
        private String description;
        
        @ApiParam("工作流JSON")
        private String workflowJson;
        
        @ApiParam("分类")
        private String category;
        
        @ApiParam("标签")
        private String tags;
    }

    /**
     * 批量更新状态请求
     */
    @lombok.Data
    public static class BatchUpdateStatusRequest {
        @ApiParam("定义ID列表")
        private List<String> definitionIds;
        
        @ApiParam("状态")
        private WorkflowDefinitionEntity.Status status;
    }

    /**
     * 创建新版本请求
     */
    @lombok.Data
    public static class CreateVersionRequest {
        @ApiParam("工作流JSON")
        private String workflowJson;
        
        @ApiParam("创建者")
        private String createdBy;
    }

    /**
     * 验证JSON请求
     */
    @lombok.Data
    public static class ValidateJsonRequest {
        @ApiParam("工作流JSON")
        private String workflowJson;
    }

    /**
     * 导入定义请求
     */
    @lombok.Data
    public static class ImportDefinitionRequest {
        @ApiParam("定义JSON")
        private String definitionJson;
        
        @ApiParam("创建者")
        private String createdBy;
    }

    /**
     * 复制定义请求
     */
    @lombok.Data
    public static class CopyDefinitionRequest {
        @ApiParam("新工作流名称")
        private String newWorkflowName;
        
        @ApiParam("创建者")
        private String createdBy;
    }
}