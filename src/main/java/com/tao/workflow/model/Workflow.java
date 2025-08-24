package com.tao.workflow.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流定义实体类
 * 
 * 这个类代表一个完整的工作流定义，包含了工作流的基本信息、步骤列表、配置参数等。
 * 使用Builder模式来构建，确保工作流定义的完整性和一致性。
 * 
 * 核心特性：
 * 1. 不可变设计 - 一旦创建，工作流定义不可修改
 * 2. 步骤管理 - 维护有序的步骤列表
 * 3. 配置支持 - 支持灵活的配置参数
 * 4. 版本控制 - 支持工作流版本管理
 * 
 * @author Tao
 * @version 1.0
 */
public class Workflow {
    
    /** 工作流唯一标识 */
    private final String id;
    
    /** 工作流名称 */
    private final String name;
    
    /** 工作流描述 */
    private final String description;
    
    /** 工作流版本号 */
    private final String version;
    
    /** 工作流步骤列表 - 有序的步骤集合 */
    private final List<WorkflowStep> steps;
    
    /** 工作流配置参数 - 支持动态配置 */
    private final Map<String, Object> config;
    
    /** 工作流状态 */
    private final WorkflowStatus status;
    
    /** 创建时间 */
    private final LocalDateTime createTime;
    
    /** 更新时间 */
    private final LocalDateTime updateTime;
    
    /**
     * 私有构造函数 - 只能通过Builder创建
     * 
     * 这种设计确保了：
     * 1. 对象的不可变性
     * 2. 构建过程的可控性
     * 3. 参数验证的统一性
     * 
     * @param builder 工作流构建器
     */
    private Workflow(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        // 创建不可变的步骤列表副本
        this.steps = new ArrayList<>(builder.steps);
        // 创建不可变的配置参数副本
        this.config = new ConcurrentHashMap<>(builder.config);
        this.status = builder.status;
        this.createTime = builder.createTime;
        this.updateTime = builder.updateTime;
    }
    
    /**
     * 获取工作流ID
     * @return 工作流唯一标识
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取工作流名称
     * @return 工作流名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取工作流描述
     * @return 工作流描述信息
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取工作流版本
     * @return 版本号
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取工作流步骤列表
     * @return 不可变的步骤列表
     */
    public List<WorkflowStep> getSteps() {
        // 返回不可变视图，防止外部修改
        return new ArrayList<>(steps);
    }
    
    /**
     * 获取指定索引的步骤
     * @param index 步骤索引
     * @return 工作流步骤
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    public WorkflowStep getStep(int index) {
        if (index < 0 || index >= steps.size()) {
            throw new IndexOutOfBoundsException("步骤索引超出范围: " + index);
        }
        return steps.get(index);
    }
    
    /**
     * 获取步骤总数
     * @return 步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
    
    /**
     * 获取工作流配置参数
     * @return 不可变的配置参数映射
     */
    public Map<String, Object> getConfig() {
        // 返回不可变视图，防止外部修改
        return new ConcurrentHashMap<>(config);
    }
    
    /**
     * 获取指定配置参数
     * @param key 配置键
     * @return 配置值，如果不存在返回null
     */
    public Object getConfigValue(String key) {
        return config.get(key);
    }
    
    /**
     * 获取指定配置参数（带默认值）
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = config.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 获取工作流状态
     * @return 当前状态
     */
    public WorkflowStatus getStatus() {
        return status;
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    /**
     * 获取更新时间
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    /**
     * 创建工作流构建器
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 基于现有工作流创建构建器（用于修改）
     * @param workflow 现有工作流
     * @return 预填充的构建器实例
     */
    public static Builder builder(Workflow workflow) {
        return new Builder(workflow);
    }
    
    @Override
    public String toString() {
        return String.format("Workflow{id='%s', name='%s', version='%s', stepCount=%d, status=%s}",
                id, name, version, steps.size(), status);
    }
    
    /**
     * 工作流构建器
     * 
     * 使用Builder模式的核心优势：
     * 1. 参数验证 - 在build()方法中统一验证
     * 2. 可读性 - 链式调用提高代码可读性
     * 3. 灵活性 - 支持可选参数和默认值
     * 4. 不可变性 - 确保构建的对象不可变
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String version = "1.0"; // 默认版本
        private List<WorkflowStep> steps = new ArrayList<>();
        private Map<String, Object> config = new ConcurrentHashMap<>();
        private WorkflowStatus status = WorkflowStatus.DRAFT; // 默认状态
        private LocalDateTime createTime = LocalDateTime.now();
        private LocalDateTime updateTime = LocalDateTime.now();
        
        /**
         * 默认构造函数
         */
        private Builder() {
        }
        
        /**
         * 基于现有工作流的构造函数
         * @param workflow 现有工作流
         */
        private Builder(Workflow workflow) {
            this.id = workflow.id;
            this.name = workflow.name;
            this.description = workflow.description;
            this.version = workflow.version;
            this.steps = new ArrayList<>(workflow.steps);
            this.config = new ConcurrentHashMap<>(workflow.config);
            this.status = workflow.status;
            this.createTime = workflow.createTime;
            this.updateTime = LocalDateTime.now(); // 更新时间设为当前时间
        }
        
        /**
         * 设置工作流ID
         * @param id 工作流唯一标识
         * @return 构建器实例
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * 设置工作流名称
         * @param name 工作流名称
         * @return 构建器实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * 设置工作流描述
         * @param description 工作流描述
         * @return 构建器实例
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * 设置工作流版本
         * @param version 版本号
         * @return 构建器实例
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        /**
         * 添加工作流步骤
         * @param step 工作流步骤
         * @return 构建器实例
         */
        public Builder addStep(WorkflowStep step) {
            if (step != null) {
                this.steps.add(step);
            }
            return this;
        }
        
        /**
         * 批量添加工作流步骤
         * @param steps 步骤列表
         * @return 构建器实例
         */
        public Builder addSteps(List<WorkflowStep> steps) {
            if (steps != null) {
                this.steps.addAll(steps);
            }
            return this;
        }
        
        /**
         * 设置配置参数
         * @param key 配置键
         * @param value 配置值
         * @return 构建器实例
         */
        public Builder config(String key, Object value) {
            this.config.put(key, value);
            return this;
        }
        
        /**
         * 批量设置配置参数
         * @param config 配置参数映射
         * @return 构建器实例
         */
        public Builder config(Map<String, Object> config) {
            if (config != null) {
                this.config.putAll(config);
            }
            return this;
        }
        
        /**
         * 设置工作流状态
         * @param status 工作流状态
         * @return 构建器实例
         */
        public Builder status(WorkflowStatus status) {
            this.status = status;
            return this;
        }
        
        /**
         * 构建工作流实例
         * 
         * 在构建过程中进行必要的验证：
         * 1. 必填字段检查
         * 2. 业务规则验证
         * 3. 数据一致性检查
         * 
         * @return 工作流实例
         * @throws IllegalStateException 如果构建参数不合法
         */
        public Workflow build() {
            // 验证必填字段
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalStateException("工作流ID不能为空");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("工作流名称不能为空");
            }
            
            // 验证业务规则
            if (steps.isEmpty()) {
                throw new IllegalStateException("工作流至少需要包含一个步骤");
            }
            
            // 验证步骤的连续性和完整性
            validateSteps();
            
            return new Workflow(this);
        }
        
        /**
         * 验证步骤的有效性
         * 
         * 检查项目：
         * 1. 步骤顺序的连续性
         * 2. 步骤ID的唯一性
         * 3. 步骤配置的完整性
         */
        private void validateSteps() {
            // 检查步骤顺序
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                if (step.getOrder() != i + 1) {
                    throw new IllegalStateException(
                        String.format("步骤顺序不连续，期望: %d, 实际: %d", i + 1, step.getOrder()));
                }
            }
            
            // 检查步骤ID唯一性
            long uniqueStepIds = steps.stream()
                .map(WorkflowStep::getId)
                .distinct()
                .count();
            
            if (uniqueStepIds != steps.size()) {
                throw new IllegalStateException("工作流步骤ID必须唯一");
            }
        }
    }
}