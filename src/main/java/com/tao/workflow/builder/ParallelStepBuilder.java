package com.tao.workflow.builder;

import java.util.*;
import java.util.function.Consumer;

/**
 * 并行步骤构建器
 * 
 * 专门用于构建并行处理逻辑的Builder，支持复杂的并行分支和汇聚策略。
 * 通过链式调用和函数式接口，提供了直观且强大的并行构建方式。
 * 
 * 主要解决的问题：
 * 1. 并行分支的定义和管理
 * 2. 分支汇聚策略的配置
 * 3. 并行执行的超时和错误处理
 * 4. 分支间的数据传递和同步
 * 5. 动态分支数量的支持
 * 
 * 使用示例：
 * <pre>
 * ParallelStepBuilder parallelBuilder = new ParallelStepBuilder()
 *     .addBranch("hr_review", "人事审核")
 *     .addBranch("finance_check", "财务检查")
 *     .addBranch("legal_review", "法务审核")
 *     .joinType("AND") // 所有分支都必须完成
 *     .timeout(3600) // 1小时超时
 *     .onTimeout("timeout_handler")
 *     .onError("error_handler")
 *     .collectResults(true);
 * 
 * Map<String, Object> parallelConfig = parallelBuilder.build();
 * </pre>
 * 
 * @author Tao
 * @version 1.0
 */
public class ParallelStepBuilder {
    
    /**
     * 并行分支定义
     */
    public static class ParallelBranch {
        /** 分支ID */
        private final String branchId;
        
        /** 分支名称 */
        private final String branchName;
        
        /** 分支描述 */
        private final String description;
        
        /** 分支步骤ID列表 */
        private final List<String> stepIds;
        
        /** 分支配置 */
        private final Map<String, Object> configuration;
        
        /** 分支优先级 */
        private final int priority;
        
        /** 是否可选分支 */
        private final boolean optional;
        
        public ParallelBranch(String branchId, String branchName, String description, 
                            List<String> stepIds, Map<String, Object> configuration, 
                            int priority, boolean optional) {
            this.branchId = branchId;
            this.branchName = branchName;
            this.description = description;
            this.stepIds = new ArrayList<>(stepIds);
            this.configuration = new HashMap<>(configuration);
            this.priority = priority;
            this.optional = optional;
        }
        
        // Getters
        public String getBranchId() { return branchId; }
        public String getBranchName() { return branchName; }
        public String getDescription() { return description; }
        public List<String> getStepIds() { return new ArrayList<>(stepIds); }
        public Map<String, Object> getConfiguration() { return new HashMap<>(configuration); }
        public int getPriority() { return priority; }
        public boolean isOptional() { return optional; }
        
        @Override
        public String toString() {
            return String.format("ParallelBranch{id='%s', name='%s', stepCount=%d, optional=%s}", 
                               branchId, branchName, stepIds.size(), optional);
        }
    }
    
    /** 并行分支列表 */
    private final List<ParallelBranch> branches = new ArrayList<>();
    
    /** 汇聚类型：AND（所有分支完成）、OR（任一分支完成）、CUSTOM（自定义条件） */
    private String joinType = "AND";
    
    /** 自定义汇聚条件 */
    private String customJoinCondition;
    
    /** 并行执行超时时间（秒） */
    private Integer timeout;
    
    /** 超时处理步骤ID */
    private String timeoutStepId;
    
    /** 错误处理步骤ID */
    private String errorStepId;
    
    /** 是否收集所有分支的执行结果 */
    private boolean collectResults = true;
    
    /** 是否等待所有分支完成（即使某些分支失败） */
    private boolean waitForAll = false;
    
    /** 最大并发数（0表示无限制） */
    private int maxConcurrency = 0;
    
    /** 分支执行策略 */
    private String executionStrategy = "PARALLEL"; // PARALLEL, SEQUENTIAL, BATCH
    
    /** 批处理大小（当策略为BATCH时使用） */
    private int batchSize = 5;
    
    /** 分支间数据共享配置 */
    private final Map<String, Object> dataSharing = new HashMap<>();
    
    /** 分支优先级计数器 */
    private int priorityCounter = 1;
    
    /**
     * 默认构造函数
     */
    public ParallelStepBuilder() {
        // 初始化默认配置
    }
    
    /**
     * 添加简单的并行分支
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranch(String branchId, String branchName) {
        return addBranch(branchId, branchName, null, Collections.emptyList(), 
                        new HashMap<>(), priorityCounter++, false);
    }
    
    /**
     * 添加带描述的并行分支
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @param description 分支描述
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranch(String branchId, String branchName, String description) {
        return addBranch(branchId, branchName, description, Collections.emptyList(), 
                        new HashMap<>(), priorityCounter++, false);
    }
    
    /**
     * 添加包含步骤的并行分支
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @param stepIds 分支包含的步骤ID列表
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranch(String branchId, String branchName, List<String> stepIds) {
        return addBranch(branchId, branchName, null, stepIds, 
                        new HashMap<>(), priorityCounter++, false);
    }
    
    /**
     * 添加完整配置的并行分支
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @param description 分支描述
     * @param stepIds 分支包含的步骤ID列表
     * @param configuration 分支配置
     * @param priority 分支优先级
     * @param optional 是否为可选分支
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranch(String branchId, String branchName, String description,
                                        List<String> stepIds, Map<String, Object> configuration,
                                        int priority, boolean optional) {
        ParallelBranch branch = new ParallelBranch(
            branchId, branchName, description, stepIds, configuration, priority, optional
        );
        branches.add(branch);
        return this;
    }
    
    /**
     * 使用函数式接口配置并行分支
     * 
     * 提供更灵活的分支配置方式。
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @param branchConfigurer 分支配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranch(String branchId, String branchName, 
                                        Consumer<BranchConfigurer> branchConfigurer) {
        BranchConfigurer configurer = new BranchConfigurer();
        branchConfigurer.accept(configurer);
        
        return addBranch(branchId, branchName, configurer.description, 
                        configurer.stepIds, configurer.configuration, 
                        configurer.priority, configurer.optional);
    }
    
    /**
     * 分支配置器
     * 
     * 用于函数式配置分支的辅助类。
     */
    public static class BranchConfigurer {
        private String description;
        private final List<String> stepIds = new ArrayList<>();
        private final Map<String, Object> configuration = new HashMap<>();
        private int priority = 1;
        private boolean optional = false;
        
        public BranchConfigurer description(String description) {
            this.description = description;
            return this;
        }
        
        public BranchConfigurer addStep(String stepId) {
            this.stepIds.add(stepId);
            return this;
        }
        
        public BranchConfigurer steps(String... stepIds) {
            this.stepIds.addAll(Arrays.asList(stepIds));
            return this;
        }
        
        public BranchConfigurer config(String key, Object value) {
            this.configuration.put(key, value);
            return this;
        }
        
        public BranchConfigurer priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public BranchConfigurer optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public BranchConfigurer optional() {
            return optional(true);
        }
    }
    
    /**
     * 添加可选分支
     * 
     * 可选分支失败不会影响整个并行步骤的执行。
     * 
     * @param branchId 分支ID
     * @param branchName 分支名称
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addOptionalBranch(String branchId, String branchName) {
        return addBranch(branchId, branchName, null, Collections.emptyList(), 
                        new HashMap<>(), priorityCounter++, true);
    }
    
    /**
     * 批量添加分支
     * 
     * @param branchNames 分支名称数组，分支ID将自动生成
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder addBranches(String... branchNames) {
        for (String branchName : branchNames) {
            String branchId = "branch_" + (branches.size() + 1);
            addBranch(branchId, branchName);
        }
        return this;
    }
    
    /**
     * 设置汇聚类型
     * 
     * @param joinType 汇聚类型（AND、OR、CUSTOM）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder joinType(String joinType) {
        this.joinType = joinType.toUpperCase();
        return this;
    }
    
    /**
     * 设置为AND汇聚
     * 
     * 所有分支都必须成功完成。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder joinAnd() {
        return joinType("AND");
    }
    
    /**
     * 设置为OR汇聚
     * 
     * 任意一个分支成功完成即可。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder joinOr() {
        return joinType("OR");
    }
    
    /**
     * 设置自定义汇聚条件
     * 
     * @param condition 自定义汇聚条件表达式
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder customJoin(String condition) {
        this.joinType = "CUSTOM";
        this.customJoinCondition = condition;
        return this;
    }
    
    /**
     * 设置最少成功分支数
     * 
     * 至少指定数量的分支成功完成即可。
     * 
     * @param minSuccessCount 最少成功分支数
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder minSuccess(int minSuccessCount) {
        String condition = String.format("successCount >= %d", minSuccessCount);
        return customJoin(condition);
    }
    
    /**
     * 设置成功率阈值
     * 
     * 成功分支比例达到指定阈值即可。
     * 
     * @param successRate 成功率阈值（0.0-1.0）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder successRate(double successRate) {
        String condition = String.format("(successCount * 1.0 / totalCount) >= %.2f", successRate);
        return customJoin(condition);
    }
    
    /**
     * 设置超时时间
     * 
     * @param timeout 超时时间（秒）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * 设置超时时间（分钟）
     * 
     * @param minutes 超时时间（分钟）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder timeoutMinutes(int minutes) {
        return timeout(minutes * 60);
    }
    
    /**
     * 设置超时时间（小时）
     * 
     * @param hours 超时时间（小时）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder timeoutHours(int hours) {
        return timeout(hours * 3600);
    }
    
    /**
     * 设置超时处理步骤
     * 
     * @param timeoutStepId 超时处理步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder onTimeout(String timeoutStepId) {
        this.timeoutStepId = timeoutStepId;
        return this;
    }
    
    /**
     * 设置错误处理步骤
     * 
     * @param errorStepId 错误处理步骤ID
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder onError(String errorStepId) {
        this.errorStepId = errorStepId;
        return this;
    }
    
    /**
     * 设置是否收集执行结果
     * 
     * @param collectResults 是否收集结果
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder collectResults(boolean collectResults) {
        this.collectResults = collectResults;
        return this;
    }
    
    /**
     * 启用结果收集
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder collectResults() {
        return collectResults(true);
    }
    
    /**
     * 禁用结果收集
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder noResultCollection() {
        return collectResults(false);
    }
    
    /**
     * 设置是否等待所有分支完成
     * 
     * @param waitForAll 是否等待所有分支
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder waitForAll(boolean waitForAll) {
        this.waitForAll = waitForAll;
        return this;
    }
    
    /**
     * 等待所有分支完成
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder waitForAll() {
        return waitForAll(true);
    }
    
    /**
     * 设置最大并发数
     * 
     * @param maxConcurrency 最大并发数（0表示无限制）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder maxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        return this;
    }
    
    /**
     * 设置无并发限制
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder unlimitedConcurrency() {
        return maxConcurrency(0);
    }
    
    /**
     * 设置执行策略
     * 
     * @param strategy 执行策略（PARALLEL、SEQUENTIAL、BATCH）
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder executionStrategy(String strategy) {
        this.executionStrategy = strategy.toUpperCase();
        return this;
    }
    
    /**
     * 设置为并行执行
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder parallel() {
        return executionStrategy("PARALLEL");
    }
    
    /**
     * 设置为顺序执行
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder sequential() {
        return executionStrategy("SEQUENTIAL");
    }
    
    /**
     * 设置为批处理执行
     * 
     * @param batchSize 批处理大小
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder batch(int batchSize) {
        this.executionStrategy = "BATCH";
        this.batchSize = batchSize;
        return this;
    }
    
    /**
     * 启用分支间数据共享
     * 
     * @param key 共享数据键
     * @param value 共享数据值
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder shareData(String key, Object value) {
        this.dataSharing.put(key, value);
        return this;
    }
    
    /**
     * 批量设置共享数据
     * 
     * @param sharedData 共享数据映射
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder shareData(Map<String, Object> sharedData) {
        this.dataSharing.putAll(sharedData);
        return this;
    }
    
    /**
     * 移除指定分支
     * 
     * @param branchId 分支ID
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder removeBranch(String branchId) {
        branches.removeIf(branch -> branch.getBranchId().equals(branchId));
        return this;
    }
    
    /**
     * 清空所有分支
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public ParallelStepBuilder clearBranches() {
        branches.clear();
        priorityCounter = 1;
        return this;
    }
    
    /**
     * 验证并行配置的完整性
     * 
     * @throws IllegalStateException 如果配置不完整或不正确
     */
    private void validate() {
        if (branches.isEmpty()) {
            throw new IllegalStateException("并行步骤必须包含至少一个分支");
        }
        
        // 验证分支ID的唯一性
        Set<String> branchIds = new HashSet<>();
        for (ParallelBranch branch : branches) {
            if (!branchIds.add(branch.getBranchId())) {
                throw new IllegalStateException("分支ID重复: " + branch.getBranchId());
            }
        }
        
        // 验证汇聚类型
        if (!Arrays.asList("AND", "OR", "CUSTOM").contains(joinType)) {
            throw new IllegalStateException("无效的汇聚类型: " + joinType);
        }
        
        // 验证自定义汇聚条件
        if ("CUSTOM".equals(joinType) && (customJoinCondition == null || customJoinCondition.trim().isEmpty())) {
            throw new IllegalStateException("自定义汇聚类型必须指定汇聚条件");
        }
        
        // 验证执行策略
        if (!Arrays.asList("PARALLEL", "SEQUENTIAL", "BATCH").contains(executionStrategy)) {
            throw new IllegalStateException("无效的执行策略: " + executionStrategy);
        }
        
        // 验证批处理大小
        if ("BATCH".equals(executionStrategy) && batchSize <= 0) {
            throw new IllegalStateException("批处理大小必须大于0");
        }
        
        // 验证超时时间
        if (timeout != null && timeout <= 0) {
            throw new IllegalStateException("超时时间必须大于0");
        }
        
        // 验证最大并发数
        if (maxConcurrency < 0) {
            throw new IllegalStateException("最大并发数不能为负数");
        }
    }
    
    /**
     * 构建并行配置
     * 
     * 返回包含所有并行配置的Map，用于在WorkflowStep中存储。
     * 
     * @return 并行配置Map
     * @throws IllegalStateException 如果配置不完整或不正确
     */
    public Map<String, Object> build() {
        // 验证配置
        validate();
        
        Map<String, Object> config = new HashMap<>();
        
        // 添加分支配置
        List<Map<String, Object>> branchConfigs = new ArrayList<>();
        for (ParallelBranch branch : branches) {
            Map<String, Object> branchConfig = new HashMap<>();
            branchConfig.put("branchId", branch.getBranchId());
            branchConfig.put("branchName", branch.getBranchName());
            if (branch.getDescription() != null) {
                branchConfig.put("description", branch.getDescription());
            }
            branchConfig.put("stepIds", branch.getStepIds());
            branchConfig.put("configuration", branch.getConfiguration());
            branchConfig.put("priority", branch.getPriority());
            branchConfig.put("optional", branch.isOptional());
            branchConfigs.add(branchConfig);
        }
        
        // 按优先级排序
        branchConfigs.sort((a, b) -> 
            Integer.compare((Integer) b.get("priority"), (Integer) a.get("priority")));
        
        config.put("branches", branchConfigs);
        config.put("joinType", joinType);
        
        if (customJoinCondition != null) {
            config.put("customJoinCondition", customJoinCondition);
        }
        
        if (timeout != null) {
            config.put("timeout", timeout);
        }
        
        if (timeoutStepId != null) {
            config.put("timeoutStepId", timeoutStepId);
        }
        
        if (errorStepId != null) {
            config.put("errorStepId", errorStepId);
        }
        
        config.put("collectResults", collectResults);
        config.put("waitForAll", waitForAll);
        config.put("maxConcurrency", maxConcurrency);
        config.put("executionStrategy", executionStrategy);
        
        if ("BATCH".equals(executionStrategy)) {
            config.put("batchSize", batchSize);
        }
        
        if (!dataSharing.isEmpty()) {
            config.put("dataSharing", new HashMap<>(dataSharing));
        }
        
        // 添加元数据
        config.put("branchCount", branches.size());
        config.put("optionalBranchCount", branches.stream().mapToInt(b -> b.isOptional() ? 1 : 0).sum());
        config.put("hasTimeout", timeout != null);
        config.put("hasErrorHandler", errorStepId != null);
        
        return config;
    }
    
    /**
     * 获取分支数量
     * 
     * @return 分支数量
     */
    public int getBranchCount() {
        return branches.size();
    }
    
    /**
     * 获取可选分支数量
     * 
     * @return 可选分支数量
     */
    public int getOptionalBranchCount() {
        return (int) branches.stream().filter(ParallelBranch::isOptional).count();
    }
    
    /**
     * 检查是否有超时配置
     * 
     * @return 如果有超时配置返回true，否则返回false
     */
    public boolean hasTimeout() {
        return timeout != null;
    }
    
    /**
     * 检查是否有错误处理
     * 
     * @return 如果有错误处理返回true，否则返回false
     */
    public boolean hasErrorHandler() {
        return errorStepId != null;
    }
    
    /**
     * 获取所有分支的副本
     * 
     * @return 分支列表的副本
     */
    public List<ParallelBranch> getBranches() {
        return new ArrayList<>(branches);
    }
    
    /**
     * 根据ID获取分支
     * 
     * @param branchId 分支ID
     * @return 分支对象，如果不存在返回null
     */
    public ParallelBranch getBranch(String branchId) {
        return branches.stream()
            .filter(branch -> branch.getBranchId().equals(branchId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 检查是否包含指定分支
     * 
     * @param branchId 分支ID
     * @return 如果包含返回true，否则返回false
     */
    public boolean hasBranch(String branchId) {
        return getBranch(branchId) != null;
    }
    
    @Override
    public String toString() {
        return String.format("ParallelStepBuilder{branchCount=%d, joinType='%s', strategy='%s'}", 
                           branches.size(), joinType, executionStrategy);
    }
}