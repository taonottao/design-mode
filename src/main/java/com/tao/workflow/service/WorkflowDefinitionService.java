package com.tao.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.WorkflowDefinitionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义服务接口
 * 提供工作流定义的核心业务操作
 * 
 * @author tao
 * @since 2024-01-15
 */
public interface WorkflowDefinitionService {

    /**
     * 创建工作流定义
     * 
     * @param workflowDefinition 工作流定义实体
     * @return 创建成功的工作流定义
     */
    WorkflowDefinitionEntity createWorkflowDefinition(WorkflowDefinitionEntity workflowDefinition);

    /**
     * 根据ID获取工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 工作流定义，如果不存在则返回null
     */
    WorkflowDefinitionEntity getWorkflowDefinitionById(String workflowId);

    /**
     * 根据名称和版本获取工作流定义
     * 
     * @param name 工作流名称
     * @param version 版本号
     * @return 工作流定义，如果不存在则返回null
     */
    WorkflowDefinitionEntity getWorkflowDefinitionByNameAndVersion(String name, String version);

    /**
     * 获取指定名称的最新版本工作流定义
     * 
     * @param name 工作流名称
     * @return 最新版本的工作流定义，如果不存在则返回null
     */
    WorkflowDefinitionEntity getLatestWorkflowDefinitionByName(String name);

    /**
     * 获取所有激活状态的工作流定义
     * 
     * @return 激活状态的工作流定义列表
     */
    List<WorkflowDefinitionEntity> getActiveWorkflowDefinitions();

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
    IPage<WorkflowDefinitionEntity> getWorkflowDefinitionsWithConditions(
            Page<WorkflowDefinitionEntity> page,
            String name,
            WorkflowDefinitionEntity.Status status,
            String createdBy,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * 更新工作流定义
     * 
     * @param workflowDefinition 要更新的工作流定义
     * @return 更新后的工作流定义
     */
    WorkflowDefinitionEntity updateWorkflowDefinition(WorkflowDefinitionEntity workflowDefinition);

    /**
     * 激活工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否激活成功
     */
    boolean activateWorkflowDefinition(String workflowId);

    /**
     * 停用工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否停用成功
     */
    boolean deactivateWorkflowDefinition(String workflowId);

    /**
     * 废弃工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否废弃成功
     */
    boolean deprecateWorkflowDefinition(String workflowId);

    /**
     * 软删除工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否删除成功
     */
    boolean deleteWorkflowDefinition(String workflowId);

    /**
     * 恢复已删除的工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 是否恢复成功
     */
    boolean restoreWorkflowDefinition(String workflowId);

    /**
     * 批量更新工作流定义状态
     * 
     * @param workflowIds 工作流ID列表
     * @param status 目标状态
     * @return 更新成功的数量
     */
    int batchUpdateStatus(List<String> workflowIds, WorkflowDefinitionEntity.Status status);

    /**
     * 检查工作流名称和版本是否已存在
     * 
     * @param name 工作流名称
     * @param version 版本号
     * @return 是否存在
     */
    boolean existsByNameAndVersion(String name, String version);

    /**
     * 根据关键字搜索工作流定义
     * 
     * @param keyword 搜索关键字
     * @param page 分页参数
     * @return 搜索结果
     */
    IPage<WorkflowDefinitionEntity> searchWorkflowDefinitions(String keyword, Page<WorkflowDefinitionEntity> page);

    /**
     * 获取工作流定义的统计信息
     * 
     * @return 统计信息映射
     */
    Map<String, Object> getWorkflowDefinitionStatistics();

    /**
     * 获取指定创建者的工作流定义数量
     * 
     * @param createdBy 创建者
     * @return 工作流定义数量
     */
    long countByCreatedBy(String createdBy);

    /**
     * 获取指定状态的工作流定义数量
     * 
     * @param status 状态
     * @return 工作流定义数量
     */
    long countByStatus(WorkflowDefinitionEntity.Status status);

    /**
     * 获取最近创建的工作流定义
     * 
     * @param limit 限制数量
     * @return 最近创建的工作流定义列表
     */
    List<WorkflowDefinitionEntity> getRecentWorkflowDefinitions(int limit);

    /**
     * 获取即将过期的工作流定义
     * 
     * @param days 天数阈值
     * @return 即将过期的工作流定义列表
     */
    List<WorkflowDefinitionEntity> getExpiringWorkflowDefinitions(int days);

    /**
     * 创建工作流定义的新版本
     * 
     * @param originalWorkflowId 原工作流ID
     * @param newVersion 新版本号
     * @param definitionJson 新的定义JSON
     * @param description 版本描述
     * @return 新版本的工作流定义
     */
    WorkflowDefinitionEntity createNewVersion(String originalWorkflowId, String newVersion, 
                                            String definitionJson, String description);

    /**
     * 获取指定工作流的所有版本
     * 
     * @param name 工作流名称
     * @return 所有版本的工作流定义列表
     */
    List<WorkflowDefinitionEntity> getAllVersionsByName(String name);

    /**
     * 验证工作流定义的JSON格式
     * 
     * @param definitionJson 工作流定义JSON
     * @return 验证结果，包含是否有效和错误信息
     */
    Map<String, Object> validateWorkflowDefinition(String definitionJson);

    /**
     * 导出工作流定义
     * 
     * @param workflowId 工作流ID
     * @return 导出的JSON字符串
     */
    String exportWorkflowDefinition(String workflowId);

    /**
     * 导入工作流定义
     * 
     * @param definitionJson 工作流定义JSON
     * @param createdBy 创建者
     * @return 导入的工作流定义
     */
    WorkflowDefinitionEntity importWorkflowDefinition(String definitionJson, String createdBy);

    /**
     * 复制工作流定义
     * 
     * @param sourceWorkflowId 源工作流ID
     * @param newName 新工作流名称
     * @param newVersion 新版本号
     * @param createdBy 创建者
     * @return 复制的工作流定义
     */
    WorkflowDefinitionEntity copyWorkflowDefinition(String sourceWorkflowId, String newName, 
                                                   String newVersion, String createdBy);
}