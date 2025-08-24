package com.tao.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.entity.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义Mapper接口
 * 提供工作流定义的数据库操作方法
 * 
 * @author tao
 * @since 2024-01-15
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinitionEntity> {

    /**
     * 根据名称和版本查询工作流定义
     * 
     * @param name 工作流名称
     * @param version 工作流版本
     * @return 工作流定义实体
     */
    WorkflowDefinitionEntity selectByNameAndVersion(@Param("name") String name, @Param("version") String version);

    /**
     * 根据名称查询所有版本的工作流定义
     * 
     * @param name 工作流名称
     * @return 工作流定义列表
     */
    List<WorkflowDefinitionEntity> selectAllVersionsByName(@Param("name") String name);

    /**
     * 根据名称查询最新版本的工作流定义
     * 
     * @param name 工作流名称
     * @return 最新版本的工作流定义
     */
    WorkflowDefinitionEntity selectLatestVersionByName(@Param("name") String name);

    /**
     * 查询所有激活状态的工作流定义
     * 
     * @return 激活状态的工作流定义列表
     */
    List<WorkflowDefinitionEntity> selectActiveDefinitions();

    /**
     * 分页查询工作流定义
     * 
     * @param page 分页参数
     * @param name 工作流名称（可选，模糊查询）
     * @param status 工作流状态（可选）
     * @param createdBy 创建人（可选）
     * @param startTime 创建开始时间（可选）
     * @param endTime 创建结束时间（可选）
     * @return 分页结果
     */
    IPage<WorkflowDefinitionEntity> selectDefinitionsWithConditions(
            Page<WorkflowDefinitionEntity> page,
            @Param("name") String name,
            @Param("status") String status,
            @Param("createdBy") String createdBy,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计各状态的工作流定义数量
     * 
     * @return 状态统计结果，key为状态，value为数量
     */
    List<Map<String, Object>> countDefinitionsByStatus();

    /**
     * 查询指定创建人的工作流定义数量
     * 
     * @param createdBy 创建人
     * @return 工作流定义数量
     */
    Integer countDefinitionsByCreator(@Param("createdBy") String createdBy);

    /**
     * 查询最近创建的工作流定义
     * 
     * @param limit 限制数量
     * @return 最近创建的工作流定义列表
     */
    List<WorkflowDefinitionEntity> selectRecentDefinitions(@Param("limit") Integer limit);

    /**
     * 批量更新工作流定义状态
     * 
     * @param ids 工作流定义ID列表
     * @param status 新状态
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    Integer batchUpdateStatus(@Param("ids") List<String> ids, 
                             @Param("status") String status, 
                             @Param("updatedBy") String updatedBy);

    /**
     * 检查工作流名称是否已存在
     * 
     * @param name 工作流名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 存在的记录数
     */
    Integer checkNameExists(@Param("name") String name, @Param("excludeId") String excludeId);

    /**
     * 检查工作流名称和版本组合是否已存在
     * 
     * @param name 工作流名称
     * @param version 工作流版本
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 存在的记录数
     */
    Integer checkNameVersionExists(@Param("name") String name, 
                                  @Param("version") String version, 
                                  @Param("excludeId") String excludeId);

    /**
     * 查询工作流定义的基本信息（不包含definition_json字段）
     * 用于列表查询，提高性能
     * 
     * @return 工作流定义基本信息列表
     */
    List<WorkflowDefinitionEntity> selectBasicInfo();

    /**
     * 根据关键词搜索工作流定义
     * 在名称和描述字段中进行模糊搜索
     * 
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索结果列表
     */
    List<WorkflowDefinitionEntity> searchDefinitions(@Param("keyword") String keyword, 
                                                     @Param("limit") Integer limit);

    /**
     * 查询指定时间范围内创建的工作流定义统计
     * 按日期分组统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果，包含日期和数量
     */
    List<Map<String, Object>> countDefinitionsByDateRange(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查询工作流定义的版本历史
     * 
     * @param name 工作流名称
     * @return 版本历史列表，按版本号倒序排列
     */
    List<WorkflowDefinitionEntity> selectVersionHistory(@Param("name") String name);

    /**
     * 软删除工作流定义（更新状态为DEPRECATED）
     * 
     * @param id 工作流定义ID
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    Integer softDelete(@Param("id") String id, @Param("updatedBy") String updatedBy);

    /**
     * 恢复已软删除的工作流定义（更新状态为INACTIVE）
     * 
     * @param id 工作流定义ID
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    Integer restore(@Param("id") String id, @Param("updatedBy") String updatedBy);

    /**
     * 查询需要清理的过期工作流定义
     * 查询创建时间超过指定天数且状态为DEPRECATED的定义
     * 
     * @param days 过期天数
     * @return 过期的工作流定义列表
     */
    List<WorkflowDefinitionEntity> selectExpiredDefinitions(@Param("days") Integer days);

    /**
     * 获取工作流定义的使用统计
     * 统计每个工作流定义被创建实例的次数
     * 
     * @return 使用统计结果，包含工作流定义信息和实例数量
     */
    List<Map<String, Object>> getDefinitionUsageStats();
}