package com.tao.workflow.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tao.workflow.entity.WorkflowDefinitionEntity;
import com.tao.workflow.mapper.WorkflowDefinitionMapper;
import com.tao.workflow.service.WorkflowDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义服务实现类
 * 提供工作流定义的核心业务操作实现
 * 
 * @author tao
 * @since 2024-01-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowDefinitionServiceImpl extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinitionEntity> 
        implements WorkflowDefinitionService {

    /**
     * 创建工作流定义
     * 
     * @param workflowDefinition 工作流定义实体
     * @return 创建成功的工作流定义
     */
    @Override
    public WorkflowDefinitionEntity createWorkflowDefinition(WorkflowDefinitionEntity workflowDefinition) {
        log.info("开始创建工作流定义: {}", workflowDefinition.getName());
        
        // 检查名称和版本是否已存在
        if (existsByNameAndVersion(workflowDefinition.getName(), workflowDefinition.getVersion())) {
            throw new IllegalArgumentException(
                String.format("工作流定义已存在: name=%s, version=%s", 
                    workflowDefinition.getName(), workflowDefinition.getVersion()));
        }
        
        // 设置ID和时间戳
        if (StrUtil.isBlank(workflowDefinition.getWorkflowId())) {
            workflowDefinition.setWorkflowId(IdUtil.fastSimpleUUID());
        }
        
        LocalDateTime now = LocalDateTime.now();
        workflowDefinition.setCreatedAt(now);
        workflowDefinition.setUpdatedAt(now);
        
        // 设置默认状态
        if (workflowDefinition.getStatus() == null) {
            workflowDefinition.setStatus(WorkflowDefinitionEntity.Status.ACTIVE);
        }
        
        // 验证工作流定义JSON
        Map<String, Object> validationResult = validateWorkflowDefinition(workflowDefinition.getDefinitionJson());
        if (!(Boolean) validationResult.get("valid")) {
            throw new IllegalArgumentException("工作流定义JSON格式无效: " + validationResult.get("error"));
        }
        
        // 保存到数据库
        boolean saved = save(workflowDefinition);
        if (!saved) {
            throw new RuntimeException("保存工作流定义失败");
        }
        
        log.info("工作流定义创建成功: id={}, name={}", workflowDefinition.getWorkflowId(), workflowDefinition.getName());
        return workflowDefinition;
    }

    /**
     * 根据ID获取工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 工作流定义，如果不存在则返回null
     */
    @Override
    public WorkflowDefinitionEntity getWorkflowDefinitionById(String workflowId) {
        if (StrUtil.isBlank(workflowId)) {
            return null;
        }
        
        return baseMapper.selectByWorkflowId(workflowId);
    }

    /**
     * 根据名称和版本获取工作流定义
     * 
     * @param name 工作流名称
     * @param version 版本号
     * @return 工作流定义，如果不存在则返回null
     */
    @Override
    public WorkflowDefinitionEntity getWorkflowDefinitionByNameAndVersion(String name, String version) {
        if (StrUtil.isBlank(name) || StrUtil.isBlank(version)) {
            return null;
        }
        
        return baseMapper.selectByNameAndVersion(name, version);
    }

    /**
     * 获取指定名称的最新版本工作流定义
     * 
     * @param name 工作流名称
     * @return 最新版本的工作流定义，如果不存在则返回null
     */
    @Override
    public WorkflowDefinitionEntity getLatestWorkflowDefinitionByName(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        
        return baseMapper.selectLatestByName(name);
    }

    /**
     * 获取所有激活状态的工作流定义
     * 
     * @return 激活状态的工作流定义列表
     */
    @Override
    public List<WorkflowDefinitionEntity> getActiveWorkflowDefinitions() {
        return baseMapper.selectActiveDefinitions();
    }

    /**
     * 分页查询工作流定义
     * 
     * @param page 分页参数
     * @param name 工作流名称（可选，模糊查询）
     * @param status 状态（可选）
     * @param createdBy 创建者（可选）
     * @param startDate 创建开始时间（可选）
     * @param endDate 创建结束时间（可选）
     * @return 分页结果
     */
    @Override
    public IPage<WorkflowDefinitionEntity> getWorkflowDefinitionsWithConditions(
            Page<WorkflowDefinitionEntity> page,
            String name,
            WorkflowDefinitionEntity.Status status,
            String createdBy,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        return baseMapper.selectWithConditions(page, name, status, createdBy, startDate, endDate);
    }

    /**
     * 更新工作流定义
     * 
     * @param workflowDefinition 要更新的工作流定义
     * @return 更新后的工作流定义
     */
    @Override
    public WorkflowDefinitionEntity updateWorkflowDefinition(WorkflowDefinitionEntity workflowDefinition) {
        log.info("开始更新工作流定义: {}", workflowDefinition.getWorkflowId());
        
        // 检查工作流定义是否存在
        WorkflowDefinitionEntity existing = getWorkflowDefinitionById(workflowDefinition.getWorkflowId());
        if (existing == null) {
            throw new IllegalArgumentException("工作流定义不存在: " + workflowDefinition.getWorkflowId());
        }
        
        // 验证工作流定义JSON（如果有更新）
        if (StrUtil.isNotBlank(workflowDefinition.getDefinitionJson())) {
            Map<String, Object> validationResult = validateWorkflowDefinition(workflowDefinition.getDefinitionJson());
            if (!(Boolean) validationResult.get("valid")) {
                throw new IllegalArgumentException("工作流定义JSON格式无效: " + validationResult.get("error"));
            }
        }
        
        // 更新时间戳
        workflowDefinition.setUpdatedAt(LocalDateTime.now());
        
        // 执行更新
        boolean updated = updateById(workflowDefinition);
        if (!updated) {
            throw new RuntimeException("更新工作流定义失败");
        }
        
        log.info("工作流定义更新成功: {}", workflowDefinition.getWorkflowId());
        return getWorkflowDefinitionById(workflowDefinition.getWorkflowId());
    }

    /**
     * 激活工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否激活成功
     */
    @Override
    public boolean activateWorkflowDefinition(String workflowId) {
        log.info("激活工作流定义: {}", workflowId);
        return updateWorkflowStatus(workflowId, WorkflowDefinitionEntity.Status.ACTIVE);
    }

    /**
     * 停用工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否停用成功
     */
    @Override
    public boolean deactivateWorkflowDefinition(String workflowId) {
        log.info("停用工作流定义: {}", workflowId);
        return updateWorkflowStatus(workflowId, WorkflowDefinitionEntity.Status.INACTIVE);
    }

    /**
     * 废弃工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否废弃成功
     */
    @Override
    public boolean deprecateWorkflowDefinition(String workflowId) {
        log.info("废弃工作流定义: {}", workflowId);
        return updateWorkflowStatus(workflowId, WorkflowDefinitionEntity.Status.DEPRECATED);
    }

    /**
     * 软删除工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteWorkflowDefinition(String workflowId) {
        log.info("删除工作流定义: {}", workflowId);
        
        // 检查是否有正在运行的实例
        // TODO: 添加检查逻辑
        
        int result = baseMapper.softDelete(workflowId);
        return result > 0;
    }

    /**
     * 恢复已删除的工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否恢复成功
     */
    @Override
    public boolean restoreWorkflowDefinition(String workflowId) {
        log.info("恢复工作流定义: {}", workflowId);
        
        int result = baseMapper.restore(workflowId);
        return result > 0;
    }

    /**
     * 批量更新工作流定义状态
     * 
     * @param workflowIds 工作流ID列表
     * @param status 目标状态
     * @return 更新成功的数量
     */
    @Override
    public int batchUpdateStatus(List<String> workflowIds, WorkflowDefinitionEntity.Status status) {
        if (workflowIds == null || workflowIds.isEmpty()) {
            return 0;
        }
        
        log.info("批量更新工作流定义状态: ids={}, status={}", workflowIds, status);
        return baseMapper.batchUpdateStatus(workflowIds, status);
    }

    /**
     * 检查工作流名称和版本是否已存在
     * 
     * @param name 工作流名称
     * @param version 版本号
     * @return 是否存在
     */
    @Override
    public boolean existsByNameAndVersion(String name, String version) {
        if (StrUtil.isBlank(name) || StrUtil.isBlank(version)) {
            return false;
        }
        
        return baseMapper.existsByNameAndVersion(name, version) > 0;
    }

    /**
     * 根据关键字搜索工作流定义
     * 
     * @param keyword 搜索关键字
     * @param page 分页参数
     * @return 搜索结果
     */
    @Override
    public IPage<WorkflowDefinitionEntity> searchWorkflowDefinitions(String keyword, Page<WorkflowDefinitionEntity> page) {
        if (StrUtil.isBlank(keyword)) {
            return page(page);
        }
        
        return baseMapper.searchByKeyword(page, keyword);
    }

    /**
     * 获取工作流定义的统计信息
     * 
     * @return 统计信息映射
     */
    @Override
    public Map<String, Object> getWorkflowDefinitionStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总数统计
        statistics.put("total", count());
        statistics.put("active", countByStatus(WorkflowDefinitionEntity.Status.ACTIVE));
        statistics.put("inactive", countByStatus(WorkflowDefinitionEntity.Status.INACTIVE));
        statistics.put("deprecated", countByStatus(WorkflowDefinitionEntity.Status.DEPRECATED));
        
        // 使用情况统计
        List<Map<String, Object>> usageStats = baseMapper.getUsageStatistics();
        statistics.put("usageStats", usageStats);
        
        return statistics;
    }

    /**
     * 获取指定创建者的工作流定义数量
     * 
     * @param createdBy 创建者
     * @return 工作流定义数量
     */
    @Override
    public long countByCreatedBy(String createdBy) {
        if (StrUtil.isBlank(createdBy)) {
            return 0;
        }
        
        return baseMapper.countByCreatedBy(createdBy);
    }

    /**
     * 获取指定状态的工作流定义数量
     * 
     * @param status 状态
     * @return 工作流定义数量
     */
    @Override
    public long countByStatus(WorkflowDefinitionEntity.Status status) {
        if (status == null) {
            return 0;
        }
        
        return baseMapper.countByStatus(status);
    }

    /**
     * 获取最近创建的工作流定义
     * 
     * @param limit 限制数量
     * @return 最近创建的工作流定义列表
     */
    @Override
    public List<WorkflowDefinitionEntity> getRecentWorkflowDefinitions(int limit) {
        return baseMapper.selectRecentDefinitions(limit);
    }

    /**
     * 获取即将过期的工作流定义
     * 
     * @param days 天数阈值
     * @return 即将过期的工作流定义列表
     */
    @Override
    public List<WorkflowDefinitionEntity> getExpiringWorkflowDefinitions(int days) {
        return baseMapper.selectExpiringDefinitions(days);
    }

    /**
     * 创建工作流定义的新版本
     * 
     * @param originalWorkflowId 原工作流ID
     * @param newVersion 新版本号
     * @param definitionJson 新的定义JSON
     * @param description 版本描述
     * @return 新版本的工作流定义
     */
    @Override
    public WorkflowDefinitionEntity createNewVersion(String originalWorkflowId, String newVersion, 
                                                    String definitionJson, String description) {
        log.info("创建工作流新版本: originalId={}, newVersion={}", originalWorkflowId, newVersion);
        
        // 获取原工作流定义
        WorkflowDefinitionEntity original = getWorkflowDefinitionById(originalWorkflowId);
        if (original == null) {
            throw new IllegalArgumentException("原工作流定义不存在: " + originalWorkflowId);
        }
        
        // 检查新版本是否已存在
        if (existsByNameAndVersion(original.getName(), newVersion)) {
            throw new IllegalArgumentException(
                String.format("工作流版本已存在: name=%s, version=%s", original.getName(), newVersion));
        }
        
        // 创建新版本
        WorkflowDefinitionEntity newVersionEntity = new WorkflowDefinitionEntity();
        newVersionEntity.setWorkflowId(IdUtil.fastSimpleUUID());
        newVersionEntity.setName(original.getName());
        newVersionEntity.setVersion(newVersion);
        newVersionEntity.setDescription(description);
        newVersionEntity.setDefinitionJson(definitionJson);
        newVersionEntity.setStatus(WorkflowDefinitionEntity.Status.ACTIVE);
        newVersionEntity.setCreatedBy(original.getCreatedBy());
        
        return createWorkflowDefinition(newVersionEntity);
    }

    /**
     * 获取指定工作流的所有版本
     * 
     * @param name 工作流名称
     * @return 所有版本的工作流定义列表
     */
    @Override
    public List<WorkflowDefinitionEntity> getAllVersionsByName(String name) {
        if (StrUtil.isBlank(name)) {
            return List.of();
        }
        
        return baseMapper.selectAllVersionsByName(name);
    }

    /**
     * 验证工作流定义的JSON格式
     * 
     * @param definitionJson 工作流定义JSON
     * @return 验证结果，包含是否有效和错误信息
     */
    @Override
    public Map<String, Object> validateWorkflowDefinition(String definitionJson) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (StrUtil.isBlank(definitionJson)) {
                result.put("valid", false);
                result.put("error", "工作流定义JSON不能为空");
                return result;
            }
            
            // 验证JSON格式
            JSONUtil.parseObj(definitionJson);
            
            // TODO: 添加更详细的工作流定义结构验证
            // 例如：检查必需字段、步骤连接性、条件表达式等
            
            result.put("valid", true);
            result.put("error", null);
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("error", "JSON格式错误: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 导出工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 导出的JSON字符串
     */
    @Override
    public String exportWorkflowDefinition(String workflowId) {
        WorkflowDefinitionEntity definition = getWorkflowDefinitionById(workflowId);
        if (definition == null) {
            throw new IllegalArgumentException("工作流定义不存在: " + workflowId);
        }
        
        // 创建导出对象，包含完整的工作流信息
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", definition.getName());
        exportData.put("version", definition.getVersion());
        exportData.put("description", definition.getDescription());
        exportData.put("definitionJson", definition.getDefinitionJson());
        exportData.put("exportedAt", LocalDateTime.now());
        
        return JSONUtil.toJsonStr(exportData);
    }

    /**
     * 导入工作流定义
     * 
     * @param definitionJson 工作流定义JSON
     * @param createdBy 创建者
     * @return 导入的工作流定义
     */
    @Override
    public WorkflowDefinitionEntity importWorkflowDefinition(String definitionJson, String createdBy) {
        log.info("导入工作流定义: createdBy={}", createdBy);
        
        try {
            // 解析导入数据
            Map<String, Object> importData = JSONUtil.parseObj(definitionJson).toBean(Map.class);
            
            // 创建工作流定义实体
            WorkflowDefinitionEntity definition = new WorkflowDefinitionEntity();
            definition.setName((String) importData.get("name"));
            definition.setVersion((String) importData.get("version"));
            definition.setDescription((String) importData.get("description"));
            definition.setDefinitionJson((String) importData.get("definitionJson"));
            definition.setCreatedBy(createdBy);
            
            return createWorkflowDefinition(definition);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("导入工作流定义失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制工作流定义
     * 
     * @param sourceWorkflowId 源工作流ID
     * @param newName 新工作流名称
     * @param newVersion 新版本号
     * @param createdBy 创建者
     * @return 复制的工作流定义
     */
    @Override
    public WorkflowDefinitionEntity copyWorkflowDefinition(String sourceWorkflowId, String newName, 
                                                          String newVersion, String createdBy) {
        log.info("复制工作流定义: sourceId={}, newName={}, newVersion={}", sourceWorkflowId, newName, newVersion);
        
        // 获取源工作流定义
        WorkflowDefinitionEntity source = getWorkflowDefinitionById(sourceWorkflowId);
        if (source == null) {
            throw new IllegalArgumentException("源工作流定义不存在: " + sourceWorkflowId);
        }
        
        // 创建复制的工作流定义
        WorkflowDefinitionEntity copy = new WorkflowDefinitionEntity();
        copy.setName(newName);
        copy.setVersion(newVersion);
        copy.setDescription("复制自: " + source.getName() + " v" + source.getVersion());
        copy.setDefinitionJson(source.getDefinitionJson());
        copy.setCreatedBy(createdBy);
        
        return createWorkflowDefinition(copy);
    }

    /**
     * 更新工作流状态的通用方法
     * 
     * @param workflowId 工作流ID
     * @param status 目标状态
     * @return 是否更新成功
     */
    private boolean updateWorkflowStatus(String workflowId, WorkflowDefinitionEntity.Status status) {
        WorkflowDefinitionEntity definition = getWorkflowDefinitionById(workflowId);
        if (definition == null) {
            throw new IllegalArgumentException("工作流定义不存在: " + workflowId);
        }
        
        definition.setStatus(status);
        definition.setUpdatedAt(LocalDateTime.now());
        
        return updateById(definition);
    }
}