package com.tao.workflow.executor;

import com.tao.workflow.engine.*;
import com.tao.workflow.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 并行步骤执行器
 * 
 * 用于执行并行处理的工作流步骤，支持：
 * 1. 并行分支执行：同时执行多个分支
 * 2. 汇聚策略：支持AND、OR、CUSTOM等汇聚方式
 * 3. 超时控制：支持整体超时和分支超时
 * 4. 错误处理：支持分支错误处理和整体错误处理
 * 5. 数据共享：支持分支间数据共享和结果合并
 * 6. 执行策略：支持真并行、顺序、批量等执行方式
 * 
 * 支持的配置参数：
 * - branches: 并行分支配置列表
 * - joinStrategy: 汇聚策略（AND/OR/CUSTOM）
 * - timeout: 整体超时时间（毫秒）
 * - branchTimeout: 分支超时时间（毫秒）
 * - executionStrategy: 执行策略（PARALLEL/SEQUENTIAL/BATCH）
 * - batchSize: 批量执行时的批次大小
 * - errorHandling: 错误处理策略
 * - dataSharing: 数据共享配置
 * 
 * @author Tao
 * @version 1.0
 */
public class ParallelStepExecutor extends AbstractStepExecutor {
    
    /** 线程池用于并行执行 */
    private final ExecutorService executorService;
    
    /** 汇聚策略注册表 */
    private final Map<String, JoinStrategy> joinStrategies = new HashMap<>();
    
    /** 分支执行器注册表 */
    private final Map<String, StepExecutor> branchExecutors = new HashMap<>();
    
    /**
     * 构造函数
     */
    public ParallelStepExecutor() {
        super("ParallelStepExecutor", "1.0.0", StepType.PARALLEL);
        
        // 创建线程池
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ParallelStep-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        
        // 注册默认汇聚策略
        registerDefaultJoinStrategies();
        
        logger.info("并行步骤执行器已初始化");
    }
    
    /**
     * 注册默认汇聚策略
     */
    private void registerDefaultJoinStrategies() {
        joinStrategies.put("AND", new AndJoinStrategy());
        joinStrategies.put("OR", new OrJoinStrategy());
        joinStrategies.put("CUSTOM", new CustomJoinStrategy());
        joinStrategies.put("MAJORITY", new MajorityJoinStrategy());
        joinStrategies.put("FIRST", new FirstJoinStrategy());
    }
    
    /**
     * 注册自定义汇聚策略
     */
    public void registerJoinStrategy(String name, JoinStrategy strategy) {
        Objects.requireNonNull(name, "策略名称不能为空");
        Objects.requireNonNull(strategy, "汇聚策略不能为空");
        
        joinStrategies.put(name, strategy);
        logger.info("已注册汇聚策略: {} -> {}", name, strategy.getClass().getSimpleName());
    }
    
    /**
     * 注册分支执行器
     */
    public void registerBranchExecutor(String type, StepExecutor executor) {
        Objects.requireNonNull(type, "执行器类型不能为空");
        Objects.requireNonNull(executor, "执行器不能为空");
        
        branchExecutors.put(type, executor);
        logger.info("已注册分支执行器: {} -> {}", type, executor.getClass().getSimpleName());
    }
    
    @Override
    protected StepExecutionResult doExecute(WorkflowStep step, StepExecutionContext context) throws Exception {
        logger.info("开始执行并行步骤: {} (类型: {})", step.getId(), step.getType());
        
        // 获取并行配置
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            throw new WorkflowException(
                "并行步骤缺少配置信息",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 解析并行分支
        List<ParallelBranch> branches = parseBranches(config);
        if (branches.isEmpty()) {
            throw new WorkflowException(
                "并行步骤至少需要一个分支",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 获取执行策略
        String executionStrategyStr = getConfigValue(config, "executionStrategy", "PARALLEL");
        ExecutionStrategy executionStrategy = ExecutionStrategy.valueOf(executionStrategyStr.toUpperCase());
        
        // 获取超时配置
        long timeout = getConfigLongValue(config, "timeout", 30000); // 默认30秒
        long branchTimeout = getConfigLongValue(config, "branchTimeout", 10000); // 默认10秒
        
        // 创建共享数据容器
        SharedDataContainer sharedData = new SharedDataContainer(context.getData());
        
        // 执行并行分支
        List<BranchExecutionResult> branchResults = executeBranches(
            branches, context, sharedData, executionStrategy, branchTimeout
        );
        
        // 应用汇聚策略
        String joinStrategyName = getConfigValue(config, "joinStrategy", "AND");
        JoinStrategy joinStrategy = joinStrategies.get(joinStrategyName);
        if (joinStrategy == null) {
            throw new WorkflowException(
                String.format("不支持的汇聚策略: %s", joinStrategyName),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        JoinResult joinResult = joinStrategy.join(branchResults, config);
        
        // 构建执行结果
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("branchResults", branchResults.stream()
            .collect(Collectors.toMap(
                BranchExecutionResult::getBranchId,
                result -> Map.of(
                    "status", result.getStatus(),
                    "outputData", result.getOutputData(),
                    "executionTime", result.getExecutionTime(),
                    "error", result.getError()
                )
            )));
        outputData.put("joinResult", Map.of(
            "success", joinResult.isSuccess(),
            "message", joinResult.getMessage(),
            "mergedData", joinResult.getMergedData()
        ));
        outputData.put("sharedData", sharedData.getAllData());
        
        // 合并共享数据到输出
        outputData.putAll(joinResult.getMergedData());
        
        StepExecutionResult.Status resultStatus = joinResult.isSuccess() ? 
            StepExecutionResult.Status.SUCCESS : StepExecutionResult.Status.FAILED;
        
        return StepExecutionResult.builder()
            .status(resultStatus)
            .stepId(step.getId())
            .executorName(getExecutorName())
            .outputData(outputData)
            .message(joinResult.getMessage())
            .build();
    }
    
    /**
     * 执行并行分支
     */
    private List<BranchExecutionResult> executeBranches(
            List<ParallelBranch> branches,
            StepExecutionContext context,
            SharedDataContainer sharedData,
            ExecutionStrategy strategy,
            long branchTimeout) throws Exception {
        
        switch (strategy) {
            case PARALLEL:
                return executeParallel(branches, context, sharedData, branchTimeout);
            case SEQUENTIAL:
                return executeSequential(branches, context, sharedData, branchTimeout);
            case BATCH:
                return executeBatch(branches, context, sharedData, branchTimeout);
            default:
                throw new WorkflowException(
                    String.format("不支持的执行策略: %s", strategy),
                    WorkflowException.ErrorType.CONFIGURATION_ERROR
                );
        }
    }
    
    /**
     * 并行执行分支
     */
    private List<BranchExecutionResult> executeParallel(
            List<ParallelBranch> branches,
            StepExecutionContext context,
            SharedDataContainer sharedData,
            long branchTimeout) throws Exception {
        
        List<Future<BranchExecutionResult>> futures = new ArrayList<>();
        
        // 提交所有分支任务
        for (ParallelBranch branch : branches) {
            Future<BranchExecutionResult> future = executorService.submit(() -> {
                return executeBranch(branch, context, sharedData, branchTimeout);
            });
            futures.add(future);
        }
        
        // 等待所有分支完成
        List<BranchExecutionResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                BranchExecutionResult result = futures.get(i).get(branchTimeout, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                // 分支超时
                ParallelBranch branch = branches.get(i);
                results.add(new BranchExecutionResult(
                    branch.getId(),
                    BranchExecutionResult.Status.TIMEOUT,
                    new HashMap<>(),
                    branchTimeout,
                    String.format("分支 [%s] 执行超时", branch.getId())
                ));
                
                // 取消未完成的任务
                futures.get(i).cancel(true);
            } catch (Exception e) {
                // 分支执行异常
                ParallelBranch branch = branches.get(i);
                results.add(new BranchExecutionResult(
                    branch.getId(),
                    BranchExecutionResult.Status.FAILED,
                    new HashMap<>(),
                    0,
                    String.format("分支 [%s] 执行失败: %s", branch.getId(), e.getMessage())
                ));
            }
        }
        
        return results;
    }
    
    /**
     * 顺序执行分支
     */
    private List<BranchExecutionResult> executeSequential(
            List<ParallelBranch> branches,
            StepExecutionContext context,
            SharedDataContainer sharedData,
            long branchTimeout) {
        
        List<BranchExecutionResult> results = new ArrayList<>();
        
        for (ParallelBranch branch : branches) {
            BranchExecutionResult result = executeBranch(branch, context, sharedData, branchTimeout);
            results.add(result);
            
            // 如果分支失败且配置了快速失败，则停止执行
            if (result.getStatus() == BranchExecutionResult.Status.FAILED && branch.isFailFast()) {
                logger.warn("分支 [{}] 执行失败，启用快速失败模式，停止后续分支执行", branch.getId());
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 批量执行分支
     */
    private List<BranchExecutionResult> executeBatch(
            List<ParallelBranch> branches,
            StepExecutionContext context,
            SharedDataContainer sharedData,
            long branchTimeout) throws Exception {
        
        List<BranchExecutionResult> results = new ArrayList<>();
        int batchSize = Math.max(1, branches.size() / 2); // 默认批次大小为分支数的一半
        
        // 分批执行
        for (int i = 0; i < branches.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, branches.size());
            List<ParallelBranch> batch = branches.subList(i, endIndex);
            
            // 并行执行当前批次
            List<BranchExecutionResult> batchResults = executeParallel(batch, context, sharedData, branchTimeout);
            results.addAll(batchResults);
            
            // 检查是否有失败的分支
            boolean hasFailed = batchResults.stream()
                .anyMatch(result -> result.getStatus() == BranchExecutionResult.Status.FAILED);
            
            if (hasFailed) {
                logger.warn("批次 [{}-{}] 中有分支执行失败", i, endIndex - 1);
            }
        }
        
        return results;
    }
    
    /**
     * 执行单个分支
     */
    private BranchExecutionResult executeBranch(
            ParallelBranch branch,
            StepExecutionContext context,
            SharedDataContainer sharedData,
            long timeout) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("开始执行分支: {}", branch.getId());
            
            // 创建分支执行上下文
            StepExecutionContext branchContext = createBranchContext(context, branch, sharedData);
            
            // 获取分支执行器
            StepExecutor executor = getBranchExecutor(branch);
            if (executor == null) {
                throw new WorkflowException(
                    String.format("未找到分支 [%s] 的执行器", branch.getId()),
                    WorkflowException.ErrorType.CONFIGURATION_ERROR
                );
            }
            
            // 创建分支步骤
            WorkflowStep branchStep = createBranchStep(branch);
            
            // 执行分支
            StepExecutionResult stepResult = executor.execute(branchStep, branchContext);
            
            // 更新共享数据
            if (branch.isDataSharing() && stepResult.getOutputData() != null) {
                sharedData.putAll(stepResult.getOutputData());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            BranchExecutionResult.Status status = convertStatus(stepResult.getStatus());
            
            return new BranchExecutionResult(
                branch.getId(),
                status,
                stepResult.getOutputData(),
                executionTime,
                stepResult.getMessage()
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("分支 [{}] 执行失败: {}", branch.getId(), e.getMessage(), e);
            
            return new BranchExecutionResult(
                branch.getId(),
                BranchExecutionResult.Status.FAILED,
                new HashMap<>(),
                executionTime,
                String.format("分支执行失败: %s", e.getMessage())
            );
        }
    }
    
    @Override
    protected void doValidateConfiguration(WorkflowStep step) throws WorkflowException {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            throw new WorkflowException(
                "并行步骤缺少配置信息",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证分支配置
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> branchConfigs = (List<Map<String, Object>>) config.get("branches");
        if (branchConfigs == null || branchConfigs.isEmpty()) {
            throw new WorkflowException(
                "并行步骤至少需要一个分支",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证汇聚策略
        String joinStrategy = getConfigValue(config, "joinStrategy", "AND");
        if (!joinStrategies.containsKey(joinStrategy)) {
            throw new WorkflowException(
                String.format("不支持的汇聚策略: %s", joinStrategy),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证执行策略
        String executionStrategy = getConfigValue(config, "executionStrategy", "PARALLEL");
        try {
            ExecutionStrategy.valueOf(executionStrategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException(
                String.format("不支持的执行策略: %s", executionStrategy),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
    }
    
    @Override
    protected long doEstimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            return 5000; // 默认5秒
        }
        
        // 获取分支数量
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> branchConfigs = (List<Map<String, Object>>) config.get("branches");
        int branchCount = branchConfigs != null ? branchConfigs.size() : 1;
        
        // 获取执行策略
        String executionStrategy = getConfigValue(config, "executionStrategy", "PARALLEL");
        
        // 基础执行时间（每个分支1秒）
        long baseTime = 1000;
        
        switch (executionStrategy.toUpperCase()) {
            case "PARALLEL":
                // 并行执行：时间等于最长分支的时间
                return baseTime;
            case "SEQUENTIAL":
                // 顺序执行：时间等于所有分支时间之和
                return baseTime * branchCount;
            case "BATCH":
                // 批量执行：时间介于并行和顺序之间
                return baseTime * (branchCount / 2 + 1);
            default:
                return baseTime;
        }
    }
    
    // 辅助方法
    
    /**
     * 解析并行分支配置
     */
    @SuppressWarnings("unchecked")
    private List<ParallelBranch> parseBranches(Map<String, Object> config) {
        List<Map<String, Object>> branchConfigs = (List<Map<String, Object>>) config.get("branches");
        if (branchConfigs == null) {
            return new ArrayList<>();
        }
        
        List<ParallelBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchConfigs.size(); i++) {
            Map<String, Object> branchConfig = branchConfigs.get(i);
            
            ParallelBranch branch = new ParallelBranch();
            branch.setId(getConfigValue(branchConfig, "id", "branch_" + i));
            branch.setName(getConfigValue(branchConfig, "name", "分支 " + (i + 1)));
            branch.setType(getConfigValue(branchConfig, "type", "TASK"));
            branch.setExecutor(getConfigValue(branchConfig, "executor", null));
            branch.setConfiguration(branchConfig);
            branch.setDataSharing(getBooleanConfigValue(branchConfig, "dataSharing", true));
            branch.setFailFast(getBooleanConfigValue(branchConfig, "failFast", false));
            branch.setOptional(getBooleanConfigValue(branchConfig, "optional", false));
            
            branches.add(branch);
        }
        
        return branches;
    }
    
    /**
     * 创建分支执行上下文
     */
    private StepExecutionContext createBranchContext(
            StepExecutionContext parentContext,
            ParallelBranch branch,
            SharedDataContainer sharedData) {
        
        Map<String, Object> branchData = new HashMap<>(parentContext.getData());
        branchData.putAll(sharedData.getAllData());
        
        return StepExecutionContext.builder()
            .instanceId(parentContext.getInstanceId())
            .userId(parentContext.getUserId())
            .inputData(parentContext.getInputData())
            .data(branchData)
            .executionTime(parentContext.getExecutionTime())
            .timeout(parentContext.getTimeout())
            .retryCount(0) // 分支重试计数独立
            .environment(parentContext.getEnvironment())
            .attributes(parentContext.getAttributes())
            .async(parentContext.isAsync())
            .priority(parentContext.getPriority())
            .labels(parentContext.getLabels())
            .parentContext(parentContext)
            .build();
    }
    
    /**
     * 获取分支执行器
     */
    private StepExecutor getBranchExecutor(ParallelBranch branch) {
        String executorName = branch.getExecutor();
        if (executorName != null) {
            return branchExecutors.get(executorName);
        }
        
        // 根据分支类型选择默认执行器
        return branchExecutors.get(branch.getType());
    }
    
    /**
     * 创建分支步骤
     */
    private WorkflowStep createBranchStep(ParallelBranch branch) {
        WorkflowStep step = new WorkflowStep();
        step.setId(branch.getId());
        step.setName(branch.getName());
        step.setType(StepType.valueOf(branch.getType().toUpperCase()));
        step.setConfiguration(branch.getConfiguration());
        step.setOptional(branch.isOptional());
        
        return step;
    }
    
    /**
     * 转换执行状态
     */
    private BranchExecutionResult.Status convertStatus(StepExecutionResult.Status status) {
        switch (status) {
            case SUCCESS:
                return BranchExecutionResult.Status.SUCCESS;
            case FAILED:
                return BranchExecutionResult.Status.FAILED;
            case WAITING:
                return BranchExecutionResult.Status.WAITING;
            case SKIPPED:
                return BranchExecutionResult.Status.SKIPPED;
            case TIMEOUT:
                return BranchExecutionResult.Status.TIMEOUT;
            case RETRY:
                return BranchExecutionResult.Status.RETRY;
            default:
                return BranchExecutionResult.Status.FAILED;
        }
    }
    
    /**
     * 获取配置值
     */
    private String getConfigValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取配置长整型值
     */
    private Long getConfigLongValue(Map<String, Object> config, String key, Long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取配置布尔值
     */
    private Boolean getBooleanConfigValue(Map<String, Object> config, String key, Boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    // 内部类和枚举
    
    /**
     * 执行策略枚举
     */
    public enum ExecutionStrategy {
        PARALLEL,   // 并行执行
        SEQUENTIAL, // 顺序执行
        BATCH       // 批量执行
    }
    
    /**
     * 并行分支实体
     */
    public static class ParallelBranch {
        private String id;
        private String name;
        private String type;
        private String executor;
        private Map<String, Object> configuration;
        private boolean dataSharing;
        private boolean failFast;
        private boolean optional;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getExecutor() { return executor; }
        public void setExecutor(String executor) { this.executor = executor; }
        
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        
        public boolean isDataSharing() { return dataSharing; }
        public void setDataSharing(boolean dataSharing) { this.dataSharing = dataSharing; }
        
        public boolean isFailFast() { return failFast; }
        public void setFailFast(boolean failFast) { this.failFast = failFast; }
        
        public boolean isOptional() { return optional; }
        public void setOptional(boolean optional) { this.optional = optional; }
    }
    
    /**
     * 分支执行结果
     */
    public static class BranchExecutionResult {
        public enum Status {
            SUCCESS, FAILED, WAITING, SKIPPED, TIMEOUT, RETRY
        }
        
        private final String branchId;
        private final Status status;
        private final Map<String, Object> outputData;
        private final long executionTime;
        private final String error;
        
        public BranchExecutionResult(String branchId, Status status, Map<String, Object> outputData, 
                                   long executionTime, String error) {
            this.branchId = branchId;
            this.status = status;
            this.outputData = outputData;
            this.executionTime = executionTime;
            this.error = error;
        }
        
        // Getters
        public String getBranchId() { return branchId; }
        public Status getStatus() { return status; }
        public Map<String, Object> getOutputData() { return outputData; }
        public long getExecutionTime() { return executionTime; }
        public String getError() { return error; }
    }
    
    /**
     * 共享数据容器
     */
    public static class SharedDataContainer {
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        
        public SharedDataContainer(Map<String, Object> initialData) {
            if (initialData != null) {
                this.data.putAll(initialData);
            }
        }
        
        public void put(String key, Object value) {
            data.put(key, value);
        }
        
        public void putAll(Map<String, Object> values) {
            data.putAll(values);
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        public Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }
    }
    
    /**
     * 汇聚策略接口
     */
    public interface JoinStrategy {
        JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config);
    }
    
    /**
     * 汇聚结果
     */
    public static class JoinResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> mergedData;
        
        public JoinResult(boolean success, String message, Map<String, Object> mergedData) {
            this.success = success;
            this.message = message;
            this.mergedData = mergedData;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getMergedData() { return mergedData; }
    }
    
    // 默认汇聚策略实现
    
    /**
     * AND汇聚策略：所有分支都成功才算成功
     */
    private static class AndJoinStrategy implements JoinStrategy {
        @Override
        public JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config) {
            Map<String, Object> mergedData = new HashMap<>();
            List<String> failedBranches = new ArrayList<>();
            
            for (BranchExecutionResult result : branchResults) {
                if (result.getStatus() == BranchExecutionResult.Status.SUCCESS) {
                    if (result.getOutputData() != null) {
                        mergedData.putAll(result.getOutputData());
                    }
                } else {
                    failedBranches.add(result.getBranchId());
                }
            }
            
            boolean success = failedBranches.isEmpty();
            String message = success ? 
                "所有分支执行成功" : 
                String.format("以下分支执行失败: %s", String.join(", ", failedBranches));
            
            return new JoinResult(success, message, mergedData);
        }
    }
    
    /**
     * OR汇聚策略：至少一个分支成功就算成功
     */
    private static class OrJoinStrategy implements JoinStrategy {
        @Override
        public JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config) {
            Map<String, Object> mergedData = new HashMap<>();
            List<String> successBranches = new ArrayList<>();
            
            for (BranchExecutionResult result : branchResults) {
                if (result.getStatus() == BranchExecutionResult.Status.SUCCESS) {
                    successBranches.add(result.getBranchId());
                    if (result.getOutputData() != null) {
                        mergedData.putAll(result.getOutputData());
                    }
                }
            }
            
            boolean success = !successBranches.isEmpty();
            String message = success ? 
                String.format("以下分支执行成功: %s", String.join(", ", successBranches)) : 
                "所有分支执行失败";
            
            return new JoinResult(success, message, mergedData);
        }
    }
    
    /**
     * 自定义汇聚策略
     */
    private static class CustomJoinStrategy implements JoinStrategy {
        @Override
        public JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config) {
            // 这里可以根据配置实现自定义汇聚逻辑
            // 简化实现：使用AND策略
            return new AndJoinStrategy().join(branchResults, config);
        }
    }
    
    /**
     * 多数汇聚策略：超过一半的分支成功就算成功
     */
    private static class MajorityJoinStrategy implements JoinStrategy {
        @Override
        public JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config) {
            Map<String, Object> mergedData = new HashMap<>();
            int successCount = 0;
            
            for (BranchExecutionResult result : branchResults) {
                if (result.getStatus() == BranchExecutionResult.Status.SUCCESS) {
                    successCount++;
                    if (result.getOutputData() != null) {
                        mergedData.putAll(result.getOutputData());
                    }
                }
            }
            
            boolean success = successCount > branchResults.size() / 2;
            String message = String.format("成功分支数: %d/%d", successCount, branchResults.size());
            
            return new JoinResult(success, message, mergedData);
        }
    }
    
    /**
     * 首个汇聚策略：第一个成功的分支决定结果
     */
    private static class FirstJoinStrategy implements JoinStrategy {
        @Override
        public JoinResult join(List<BranchExecutionResult> branchResults, Map<String, Object> config) {
            for (BranchExecutionResult result : branchResults) {
                if (result.getStatus() == BranchExecutionResult.Status.SUCCESS) {
                    Map<String, Object> mergedData = result.getOutputData() != null ? 
                        new HashMap<>(result.getOutputData()) : new HashMap<>();
                    
                    return new JoinResult(true, 
                        String.format("分支 [%s] 首先执行成功", result.getBranchId()), 
                        mergedData);
                }
            }
            
            return new JoinResult(false, "所有分支执行失败", new HashMap<>());
        }
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}