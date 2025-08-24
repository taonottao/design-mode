package com.tao.workflow.engine;

import com.tao.workflow.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 默认工作流引擎实现
 * 
 * 这是WorkflowEngine接口的默认实现，提供完整的工作流执行功能。
 * 支持同步和异步执行、步骤重试、错误处理、状态管理等核心功能。
 * 
 * 主要特性：
 * 1. 支持多种步骤类型的执行
 * 2. 提供完整的生命周期管理
 * 3. 支持并发执行和线程安全
 * 4. 提供丰富的监控和统计功能
 * 5. 支持扩展和自定义
 * 
 * @author Tao
 * @version 1.0
 */
public class DefaultWorkflowEngine implements WorkflowEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultWorkflowEngine.class);
    
    /** 步骤执行器注册表 */
    private final Map<String, StepExecutor> executorRegistry = new ConcurrentHashMap<>();
    
    /** 工作流实例存储 */
    private final Map<String, WorkflowInstance> instanceStorage = new ConcurrentHashMap<>();
    
    /** 工作流定义存储 */
    private final Map<String, Workflow> workflowStorage = new ConcurrentHashMap<>();
    
    /** 异步执行线程池 */
    private final ExecutorService asyncExecutor;
    
    /** 定时任务调度器 */
    private final ScheduledExecutorService scheduler;
    
    /** 执行历史记录 */
    private final Map<String, List<StepExecutionResult>> executionHistory = new ConcurrentHashMap<>();
    
    /** 用户任务存储 */
    private final Map<String, List<UserTask>> userTaskStorage = new ConcurrentHashMap<>();
    
    /** 引擎配置 */
    private final EngineConfiguration configuration;
    
    /** 引擎状态 */
    private volatile boolean running = false;
    
    /** 统计信息 */
    private final EngineStatistics statistics = new EngineStatistics();
    
    /**
     * 构造函数
     */
    public DefaultWorkflowEngine() {
        this(EngineConfiguration.defaultConfig());
    }
    
    /**
     * 带配置的构造函数
     */
    public DefaultWorkflowEngine(EngineConfiguration configuration) {
        this.configuration = configuration;
        
        // 初始化线程池
        this.asyncExecutor = Executors.newFixedThreadPool(
            configuration.getAsyncThreadPoolSize(),
            r -> {
                Thread t = new Thread(r, "workflow-async-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }

     @Override
    public WorkflowInstance rollbackToStep(String instanceId, String targetStepId, String userId, String reason) throws WorkflowException {
         Objects.requireNonNull(instanceId, "实例ID不能为空");
         Objects.requireNonNull(targetStepId, "目标步骤ID不能为空");
         
         WorkflowInstance instance = getWorkflowInstance(instanceId);
         if (instance == null) {
             throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
         }
         
         // 检查实例状态
         if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus())) {
             throw new WorkflowException("已完成或已取消的工作流实例不能回滚", WorkflowException.ErrorType.INVALID_STATE);
         }
         
         // 获取工作流定义
         Workflow workflow = workflowStorage.get(instance.getWorkflowId());
         if (workflow == null) {
             throw new WorkflowException("工作流定义不存在: " + instance.getWorkflowId(), WorkflowException.ErrorType.WORKFLOW_NOT_FOUND);
         }
         
         // 验证目标步骤存在
         WorkflowStep targetStep = workflow.getSteps().stream()
                 .filter(step -> targetStepId.equals(step.getId()))
                 .findFirst()
                 .orElseThrow(() -> new WorkflowException("目标步骤不存在: " + targetStepId, WorkflowException.ErrorType.STEP_NOT_FOUND));
         
         // 检查是否可以回滚到目标步骤（目标步骤必须在当前步骤之前）
         List<ExecutionHistory> histories = getExecutionHistory(instanceId);
         boolean targetStepExecuted = histories.stream()
                 .anyMatch(history -> targetStepId.equals(history.getStepId()) && "SUCCESS".equals(history.getStatus()));
         
         if (!targetStepExecuted) {
             throw new WorkflowException("不能回滚到未执行过的步骤: " + targetStepId, WorkflowException.ErrorType.INVALID_OPERATION);
         }
         
         // 检查目标步骤是否支持回滚
         if (!targetStep.isRollbackable()) {
             throw new WorkflowException("目标步骤不支持回滚: " + targetStepId, WorkflowException.ErrorType.INVALID_OPERATION);
         }
         
         // 执行回滚操作
         instance.setCurrentStepId(targetStepId);
         instance.setStatus("RUNNING");
         instance.setUpdateTime(LocalDateTime.now());
         
         // 记录回滚操作
        recordExecutionResult(instance, targetStep, "ROLLBACK", "回滚到步骤: " + targetStepId + ", 原因: " + reason, null);
         
         // 清理目标步骤之后的执行历史和用户任务
         cleanupAfterRollback(instanceId, targetStepId);
         
         return instance;
     }

     @Override
    public boolean canPerformOperation(String instanceId, WorkflowOperation operation, String userId) {
         Objects.requireNonNull(instanceId, "实例ID不能为空");
         Objects.requireNonNull(operation, "操作不能为空");
         
         WorkflowInstance instance = getWorkflowInstance(instanceId);
         if (instance == null) {
             return false;
         }
         
         String status = instance.getStatus();
         
         switch (operation) {
             case START:
                 return "CREATED".equals(status);
             case CONTINUE:
                 return "RUNNING".equals(status) || "WAITING".equals(status);
             case SUSPEND:
                 return "RUNNING".equals(status);
             case RESUME:
                 return "SUSPENDED".equals(status);
             case TERMINATE:
                 return "RUNNING".equals(status) || "SUSPENDED".equals(status) || "WAITING".equals(status);
             case CANCEL:
                 return "RUNNING".equals(status) || "SUSPENDED".equals(status) || "WAITING".equals(status) || "CREATED".equals(status);
             case RETRY_STEP:
                 return "FAILED".equals(status) || "ERROR".equals(status);
             case SKIP_STEP:
                 return "RUNNING".equals(status) || "FAILED".equals(status) || "ERROR".equals(status);
             case ROLLBACK:
                 return !"COMPLETED".equals(status) && !"CANCELLED".equals(status);
             case UPDATE_CONTEXT:
                 return !"COMPLETED".equals(status) && !"CANCELLED".equals(status);
             case VIEW_INSTANCE:
             case VIEW_HISTORY:
             case EXPORT:
                 return true; // 这些操作总是允许的
             case IMPORT:
                 return true; // 导入操作不依赖于特定实例状态
             default:
                 return false;
         }
     }

     @Override
    public List<WorkflowOperation> getAvailableOperations(String instanceId, String userId) {
         Objects.requireNonNull(instanceId, "实例ID不能为空");
         
         List<WorkflowOperation> availableOperations = new ArrayList<>();
         
         // 检查每个操作是否可用
        for (WorkflowOperation operation : WorkflowOperation.values()) {
            if (canPerformOperation(instanceId, operation, userId)) {
                availableOperations.add(operation);
            }
        }
         
         return availableOperations;
     }

     @Override
    public BatchOperationResult batchOperation(List<String> instanceIds, WorkflowOperation operation, String userId, Map<String, Object> parameters) throws WorkflowException {
         Objects.requireNonNull(instanceIds, "实例ID列表不能为空");
        Objects.requireNonNull(operation, "操作不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        Objects.requireNonNull(parameters, "操作参数不能为空");
         
         BatchOperationResult result = new BatchOperationResult();
         result.setOperation(operation);
         result.setUserId(userId);
         result.setStartTime(LocalDateTime.now());
         result.setTotalCount(instanceIds.size());
         
         for (String instanceId : instanceIds) {
             try {
                 // 检查操作是否可用
                 if (!canPerformOperation(instanceId, operation)) {
                     result.addSkippedDetail(new BatchOperationResult.SkippedDetail(
                         instanceId, "操作不可用: " + operation.getDisplayName()));
                     continue;
                 }
                 
                 // 执行操作
                 switch (operation) {
                     case SUSPEND:
                         suspendWorkflow(instanceId);
                         break;
                     case RESUME:
                         resumeWorkflow(instanceId);
                         break;
                     case TERMINATE:
                         terminateWorkflow(instanceId, "批量终止操作");
                         break;
                     case CANCEL:
                         cancelWorkflow(instanceId, "批量取消操作");
                         break;
                     default:
                         result.addSkippedDetail(new BatchOperationResult.SkippedDetail(
                             instanceId, "不支持的批量操作: " + operation.getDisplayName()));
                         continue;
                 }
                 
                 result.addSuccessfulInstanceId(instanceId);
                 
             } catch (Exception e) {
                 result.addFailureDetail(new BatchOperationResult.FailureDetail(
                     instanceId, e.getMessage(), e.getClass().getSimpleName()));
             }
         }
         
         result.setEndTime(LocalDateTime.now());
         result.calculateStatistics();
         
         return result;
      }

      @Override
      public WorkflowEngineStatistics getStatistics() {
          WorkflowEngineStatistics stats = new WorkflowEngineStatistics();
          
          // 计算实例统计
          long totalStarted = 0;
          long completed = 0;
          long failed = 0;
          long terminated = 0;
          long cancelled = 0;
          long suspended = 0;
          long running = 0;
          long waiting = 0;
          
          List<Long> executionTimes = new ArrayList<>();
          Map<String, Long> statusCounts = new HashMap<>();
          Map<String, WorkflowEngineStatistics.WorkflowTypeStatistics> typeStats = new HashMap<>();
          
          for (WorkflowInstance instance : instanceStorage.values()) {
              totalStarted++;
              String status = instance.getStatus();
              
              // 按状态统计
              statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
              
              switch (status) {
                  case "COMPLETED":
                      completed++;
                      // 计算执行时间
                      if (instance.getEndTime() != null && instance.getStartTime() != null) {
                          long duration = Duration.between(instance.getStartTime(), instance.getEndTime()).toMillis();
                          executionTimes.add(duration);
                      }
                      break;
                  case "FAILED":
                      failed++;
                      break;
                  case "TERMINATED":
                      terminated++;
                      break;
                  case "CANCELLED":
                      cancelled++;
                      break;
                  case "SUSPENDED":
                      suspended++;
                      break;
                  case "RUNNING":
                      running++;
                      break;
                  case "WAITING":
                      waiting++;
                      break;
              }
              
              // 按工作流类型统计
              String workflowId = instance.getWorkflowId();
              WorkflowEngineStatistics.WorkflowTypeStatistics typeStatistics = typeStats.computeIfAbsent(
                  workflowId, 
                  id -> {
                      Workflow workflow = workflowStorage.get(id);
                      return new WorkflowEngineStatistics.WorkflowTypeStatistics(id, 
                          workflow != null ? workflow.getName() : "Unknown");
                  }
              );
              
              typeStatistics.setInstanceCount(typeStatistics.getInstanceCount() + 1);
              if ("COMPLETED".equals(status)) {
                  typeStatistics.setCompletedCount(typeStatistics.getCompletedCount() + 1);
              } else if ("FAILED".equals(status)) {
                  typeStatistics.setFailedCount(typeStatistics.getFailedCount() + 1);
              }
          }
          
          // 设置基本统计
          stats.setTotalStartedInstances(totalStarted);
          stats.setCompletedInstances(completed);
          stats.setFailedInstances(failed);
          stats.setTerminatedInstances(terminated);
          stats.setCancelledInstances(cancelled);
          stats.setSuspendedInstances(suspended);
          stats.setRunningInstances(running);
          stats.setWaitingInstances(waiting);
          
          // 计算执行时间统计
          if (!executionTimes.isEmpty()) {
              double avgTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
              long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
              long minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
              
              stats.setAverageExecutionTime(avgTime);
              stats.setMaxExecutionTime(maxTime);
              stats.setMinExecutionTime(minTime);
          }
          
          // 计算成功率
          stats.calculateSuccessRate();
          
          // 计算每小时处理实例数
          if (configuration.getSystemStartTime() != null) {
              long uptimeHours = Duration.between(configuration.getSystemStartTime(), LocalDateTime.now()).toHours();
              if (uptimeHours > 0) {
                  stats.setInstancesPerHour((double) stats.getTotalProcessedInstances() / uptimeHours);
              }
          }
          
          // 设置系统信息
          stats.setActiveExecutorCount(executorStorage.size());
          stats.setRegisteredWorkflowCount(workflowStorage.size());
          stats.setSystemStartTime(configuration.getSystemStartTime());
          if (configuration.getSystemStartTime() != null) {
              stats.setSystemUptime(Duration.between(configuration.getSystemStartTime(), LocalDateTime.now()).toMillis());
          }
          
          // 设置内存使用情况
          Runtime runtime = Runtime.getRuntime();
          long totalMemory = runtime.totalMemory();
          long freeMemory = runtime.freeMemory();
          long usedMemory = totalMemory - freeMemory;
          long maxMemory = runtime.maxMemory();
          stats.setMemoryUsage(new WorkflowEngineStatistics.MemoryUsage(totalMemory, usedMemory, freeMemory, maxMemory));
          
          // 设置按状态分组的统计
          stats.setInstancesByStatus(statusCounts);
          
          // 设置按工作流类型分组的统计
          for (WorkflowEngineStatistics.WorkflowTypeStatistics typeStatistics : typeStats.values()) {
              if (typeStatistics.getInstanceCount() > 0) {
                  typeStatistics.setSuccessRate((double) typeStatistics.getCompletedCount() / typeStatistics.getInstanceCount());
              }
              stats.addWorkflowTypeStats(typeStatistics.getWorkflowId(), typeStatistics);
          }
          
          return stats;
      }

      @Override
      public long getActiveInstanceCount() {
          return instanceStorage.values().stream()
                  .filter(instance -> "RUNNING".equals(instance.getStatus()) || "WAITING".equals(instance.getStatus()))
                  .count();
      }

      @Override
      public long getInstanceCount(String status) {
          if (status == null || status.trim().isEmpty()) {
              return instanceStorage.size();
          }
          
          return instanceStorage.values().stream()
                  .filter(instance -> status.equals(instance.getStatus()))
                  .count();
      }

      @Override
      public int cleanupCompletedInstances(int maxAge) {
          LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxAge);
          
          List<String> instancesToRemove = new ArrayList<>();
          
          for (Map.Entry<String, WorkflowInstance> entry : instanceStorage.entrySet()) {
              WorkflowInstance instance = entry.getValue();
              
              // 只清理已完成、已取消或已终止的实例
              if (("COMPLETED".equals(instance.getStatus()) || 
                   "CANCELLED".equals(instance.getStatus()) || 
                   "TERMINATED".equals(instance.getStatus())) &&
                  (instance.getEndTime() != null && instance.getEndTime().isBefore(cutoffTime))) {
                  
                  instancesToRemove.add(entry.getKey());
              }
          }
          
          // 执行清理
          for (String instanceId : instancesToRemove) {
              instanceStorage.remove(instanceId);
              executionHistoryStorage.remove(instanceId);
              userTaskStorage.remove(instanceId);
          }
          
          return instancesToRemove.size();
      }
        );
        
        // 初始化调度器
        this.scheduler = Executors.newScheduledThreadPool(
            configuration.getSchedulerThreadPoolSize(),
            r -> {
                Thread t = new Thread(r, "workflow-scheduler-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
        
        // 启动引擎
        start();
        
        logger.info("工作流引擎已初始化，配置: {}", configuration);
    }
    
    /**
     * 启动引擎
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        
        // 启动定期清理任务
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredInstances,
            configuration.getCleanupIntervalMinutes(),
            configuration.getCleanupIntervalMinutes(),
            TimeUnit.MINUTES
        );
        
        logger.info("工作流引擎已启动");
    }
    
    /**
     * 停止引擎
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // 关闭线程池
        asyncExecutor.shutdown();
        scheduler.shutdown();
        
        try {
            // 等待任务完成
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncExecutor.shutdownNow();
            scheduler.shutdownNow();
        }
        
        logger.info("工作流引擎已停止");
    }
    
    /**
     * 注册步骤执行器
     */
    public void registerExecutor(String stepType, StepExecutor executor) {
        Objects.requireNonNull(stepType, "步骤类型不能为空");
        Objects.requireNonNull(executor, "执行器不能为空");
        
        executorRegistry.put(stepType, executor);
        logger.info("已注册步骤执行器: {} -> {}", stepType, executor.getName());
    }
    
    /**
     * 注册工作流定义
     */
    public void registerWorkflow(Workflow workflow) {
        Objects.requireNonNull(workflow, "工作流定义不能为空");
        
        workflowStorage.put(workflow.getId(), workflow);
        logger.info("已注册工作流定义: {} ({})", workflow.getId(), workflow.getName());
    }
    
    @Override
    public WorkflowInstance startWorkflow(String workflowId, String startUserId, Map<String, Object> initialContext) throws Exception {
        Objects.requireNonNull(workflowId, "工作流ID不能为空");
        Objects.requireNonNull(startUserId, "用户ID不能为空");
        
        // 获取工作流定义
        Workflow workflow = workflowStorage.get(workflowId);
        if (workflow == null) {
            throw new WorkflowException("工作流定义不存在: " + workflowId, 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        // 检查工作流状态
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new WorkflowException("工作流状态不允许启动: " + workflow.getStatus(), 
                                      WorkflowException.WorkflowErrorType.STATE_ERROR);
        }
        
        // 创建工作流实例
        String instanceId = generateInstanceId();
        WorkflowInstance instance = WorkflowInstance.builder()
            .id(instanceId)
            .workflowId(workflowId)
            .name(workflow.getName())
            .status(InstanceStatus.CREATED)
            .startUserId(startUserId)
            .context(initialContext != null ? initialContext : new HashMap<>())
            .build();
        
        // 保存实例
        instanceStorage.put(instanceId, instance);
        
        // 更新统计
        statistics.incrementStartedInstances();
        
        logger.info("工作流实例已创建: {} (工作流: {}, 用户: {})", instanceId, workflowId, startUserId);
        
        // 开始执行
        try {
            continueWorkflow(instanceId, startUserId);
        } catch (Exception e) {
            // 如果启动失败，更新实例状态
            updateInstanceStatus(instanceId, InstanceStatus.FAILED, e.getMessage());
            throw e;
        }
        
        return instance;
    }
    
    @Override
    public WorkflowInstance continueWorkflow(String instanceId, String userId, Map<String, Object> stepResult) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        // 检查实例状态
        if (!instance.getStatus().isActive()) {
            throw new WorkflowException("工作流实例状态不允许继续执行: " + instance.getStatus(), 
                                      WorkflowException.WorkflowErrorType.STATE_ERROR);
        }
        
        // 获取工作流定义
        Workflow workflow = workflowStorage.get(instance.getWorkflowId());
        if (workflow == null) {
            throw new WorkflowException("工作流定义不存在: " + instance.getWorkflowId(), 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        // 确定下一个要执行的步骤
        WorkflowStep nextStep = determineNextStep(instance, workflow);
        if (nextStep == null) {
            // 没有更多步骤，完成工作流
            completeWorkflow(instanceId);
            return;
        }
        
        // 执行步骤
        executeStep(instance, nextStep, userId);
        
        return instance;
    }
    
    @Override
    public StepExecutionResult executeStep(String instanceId, String stepId, String userId, Map<String, Object> stepContext) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(stepId, "步骤ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        // 获取工作流定义
        Workflow workflow = workflowStorage.get(instance.getWorkflowId());
        if (workflow == null) {
            throw new WorkflowException("工作流定义不存在: " + instance.getWorkflowId(), 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        // 查找步骤
        WorkflowStep step = workflow.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new WorkflowException("步骤不存在: " + stepId, 
                                                   WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR));
        
        // 执行步骤
        executeStep(instance, step, userId, stepContext);
        
        // 返回执行结果（这里需要从执行历史中获取最新结果）
        List<StepExecutionResult> history = executionHistory.get(instanceId);
        if (history != null && !history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        
        // 如果没有历史记录，返回默认成功结果
        return StepExecutionResult.success()
            .stepId(stepId)
            .executorName("DefaultWorkflowEngine")
            .message("步骤执行完成")
            .build();
    }
    
    /**
     * 执行单个步骤
     */
    private void executeStep(WorkflowInstance instance, WorkflowStep step, String userId) {
        executeStep(instance, step, userId, null);
    }
    
    /**
     * 执行单个步骤（带输入数据）
     */
    private void executeStep(WorkflowInstance instance, WorkflowStep step, String userId, Map<String, Object> inputData) {
        logger.info("开始执行步骤: {} (实例: {}, 用户: {})", step.getId(), instance.getId(), userId);
        
        // 更新实例状态
        updateInstanceStatus(instance.getId(), InstanceStatus.RUNNING, null);
        updateCurrentStep(instance.getId(), step.getId());
        
        // 检查前置条件
        if (!checkPrecondition(instance, step)) {
            logger.warn("步骤前置条件不满足，跳过执行: {} (实例: {})", step.getId(), instance.getId());
            
            // 记录跳过结果
            StepExecutionResult result = StepExecutionResult.builder(StepExecutionResult.Status.SKIPPED)
                .nextStepId(step.getId())
                .executorName("system")
                .message("前置条件不满足")
                .build();
            
            recordExecutionResult(instance.getId(), result);
            
            // 继续执行下一步
            continueWorkflow(instance.getId(), userId);
            return;
        }
        
        // 获取执行器
        StepExecutor executor = getExecutor(step);
        if (executor == null) {
            String errorMsg = "未找到步骤执行器: " + step.getType();
            logger.error(errorMsg);
            
            StepExecutionResult result = StepExecutionResult.builder()
                .status(StepExecutionResult.Status.FAILED)
                .stepId(step.getId())
                .executorName("unknown")
                .errorMessage(errorMsg)
                .build();
            
            recordExecutionResult(instance.getId(), result);
            handleStepFailure(instance, step, result, userId);
            return;
        }
        
        // 创建执行上下文
        StepExecutionContext context = createExecutionContext(instance, step, userId, inputData);
        
        // 异步执行或同步执行
        if (step.getType() == StepType.TIMER || context.isAsync()) {
            executeStepAsync(instance, step, executor, context, userId);
        } else {
            executeStepSync(instance, step, executor, context, userId);
        }
    }
    
    /**
     * 同步执行步骤
     */
    private void executeStepSync(WorkflowInstance instance, WorkflowStep step, 
                                StepExecutor executor, StepExecutionContext context, String userId) {
        try {
            // 执行步骤
            StepExecutionResult result = executor.execute(step, context);
            
            // 记录执行结果
            recordExecutionResult(instance.getId(), result);
            
            // 处理执行结果
            handleStepResult(instance, step, result, userId);
            
        } catch (Exception e) {
            logger.error("步骤执行异常: {} (实例: {})", step.getId(), instance.getId(), e);
            
            StepExecutionResult result = StepExecutionResult.builder()
                .status(StepExecutionResult.Status.FAILED)
                .stepId(step.getId())
                .executorName(executor.getExecutorName())
                .errorMessage(e.getMessage())
                .exception(e)
                .build();
            
            recordExecutionResult(instance.getId(), result);
            handleStepFailure(instance, step, result, userId);
        }
    }
    
    /**
     * 异步执行步骤
     */
    private void executeStepAsync(WorkflowInstance instance, WorkflowStep step, 
                                 StepExecutor executor, StepExecutionContext context, String userId) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return executor.execute(step, context);
            } catch (Exception e) {
                logger.error("异步步骤执行异常: {} (实例: {})", step.getId(), instance.getId(), e);
                return StepExecutionResult.builder()
                    .status(StepExecutionResult.Status.FAILED)
                    .stepId(step.getId())
                    .executorName(executor.getExecutorName())
                    .errorMessage(e.getMessage())
                    .exception(e)
                    .build();
            }
        }, asyncExecutor)
        .thenAccept(result -> {
            // 记录执行结果
            recordExecutionResult(instance.getId(), result);
            
            // 处理执行结果
            handleStepResult(instance, step, result, userId);
        })
        .exceptionally(throwable -> {
            logger.error("异步步骤执行失败: {} (实例: {})", step.getId(), instance.getId(), throwable);
            
            StepExecutionResult result = StepExecutionResult.builder()
                .status(StepExecutionResult.Status.FAILED)
                .stepId(step.getId())
                .executorName(executor.getExecutorName())
                .errorMessage(throwable.getMessage())
                .exception(throwable)
                .build();
            
            recordExecutionResult(instance.getId(), result);
            handleStepFailure(instance, step, result, userId);
            return null;
        });
    }
    
    /**
     * 处理步骤执行结果
     */
    private void handleStepResult(WorkflowInstance instance, WorkflowStep step, 
                                 StepExecutionResult result, String userId) {
        switch (result.getStatus()) {
            case SUCCESS:
                handleStepSuccess(instance, step, result, userId);
                break;
            case FAILED:
                handleStepFailure(instance, step, result, userId);
                break;
            case WAITING:
                handleStepWaiting(instance, step, result, userId);
                break;
            case RETRY:
                handleStepRetry(instance, step, result, userId);
                break;
            case TIMEOUT:
                handleStepTimeout(instance, step, result, userId);
                break;
            case SKIPPED:
                handleStepSkipped(instance, step, result, userId);
                break;
            default:
                logger.warn("未知的步骤执行状态: {} (步骤: {}, 实例: {})", 
                          result.getStatus(), step.getId(), instance.getId());
                break;
        }
    }
    
    /**
     * 处理步骤成功
     */
    private void handleStepSuccess(WorkflowInstance instance, WorkflowStep step, 
                                  StepExecutionResult result, String userId) {
        logger.info("步骤执行成功: {} (实例: {})", step.getId(), instance.getId());
        
        // 更新实例上下文
        if (result.getOutputData() != null && !result.getOutputData().isEmpty()) {
            updateInstanceContext(instance.getId(), result.getOutputData());
        }
        
        // 继续执行下一步
        continueWorkflow(instance.getId(), userId);
    }
    
    /**
     * 处理步骤失败
     */
    private void handleStepFailure(WorkflowInstance instance, WorkflowStep step, 
                                  StepExecutionResult result, String userId) {
        logger.error("步骤执行失败: {} (实例: {}), 错误: {}", 
                    step.getId(), instance.getId(), result.getErrorMessage());
        
        // 检查是否可以重试
        if (canRetryStep(instance, step, result)) {
            scheduleStepRetry(instance, step, userId);
            return;
        }
        
        // 检查是否有错误处理步骤
        if (step.getErrorStepId() != null) {
            // 执行错误处理步骤
            executeErrorStep(instance, step, userId);
            return;
        }
        
        // 如果步骤是可选的，继续执行
        if (step.isOptional()) {
            logger.info("可选步骤失败，继续执行: {} (实例: {})", step.getId(), instance.getId());
            continueWorkflow(instance.getId(), userId);
            return;
        }
        
        // 工作流失败
        updateInstanceStatus(instance.getId(), InstanceStatus.FAILED, result.getErrorMessage());
        statistics.incrementFailedInstances();
    }
    
    /**
     * 处理步骤等待
     */
    private void handleStepWaiting(WorkflowInstance instance, WorkflowStep step, 
                                  StepExecutionResult result, String userId) {
        logger.info("步骤进入等待状态: {} (实例: {})", step.getId(), instance.getId());
        
        // 更新实例状态为等待
        updateInstanceStatus(instance.getId(), InstanceStatus.WAITING, result.getMessage());
        
        // 如果是用户任务，创建用户任务记录
        if (step.getType() == StepType.USER_TASK) {
            createUserTask(instance, step, result);
        }
    }
    
    /**
     * 处理步骤重试
     */
    private void handleStepRetry(WorkflowInstance instance, WorkflowStep step, 
                                StepExecutionResult result, String userId) {
        logger.info("步骤请求重试: {} (实例: {})", step.getId(), instance.getId());
        scheduleStepRetry(instance, step, userId);
    }
    
    /**
     * 处理步骤超时
     */
    private void handleStepTimeout(WorkflowInstance instance, WorkflowStep step, 
                                  StepExecutionResult result, String userId) {
        logger.warn("步骤执行超时: {} (实例: {})", step.getId(), instance.getId());
        
        // 按失败处理
        handleStepFailure(instance, step, result, userId);
    }
    
    /**
     * 处理步骤跳过
     */
    private void handleStepSkipped(WorkflowInstance instance, WorkflowStep step, 
                                  StepExecutionResult result, String userId) {
        logger.info("步骤被跳过: {} (实例: {})", step.getId(), instance.getId());
        
        // 继续执行下一步
        continueWorkflow(instance.getId(), userId);
    }
    
    // 其他实现方法...
    
    @Override
    public WorkflowInstance suspendWorkflow(String instanceId, String userId, String reason) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
        }
        
        updateInstanceStatus(instanceId, InstanceStatus.SUSPENDED, reason);
        logger.info("工作流实例已暂停: {} (用户: {}, 原因: {})", instanceId, userId, reason);
        
        return instanceStorage.get(instanceId);
    }
    
    @Override
    public WorkflowInstance resumeWorkflow(String instanceId, String userId) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
        }
        
        if (instance.getStatus() != InstanceStatus.SUSPENDED) {
            throw new WorkflowException("工作流实例状态不允许恢复: " + instance.getStatus(), WorkflowException.ErrorType.INVALID_STATE);
        }
        
        updateInstanceStatus(instanceId, InstanceStatus.RUNNING, "工作流已恢复");
         
         logger.info("工作流已恢复: {} (用户: {})", instanceId, userId);
         
         // 继续执行工作流
         return continueWorkflow(instanceId, userId, new HashMap<>());
    }
    
    @Override
    public WorkflowInstance terminateWorkflow(String instanceId, String userId, String reason) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
        }
        
        updateInstanceStatus(instanceId, InstanceStatus.TERMINATED, reason);
        statistics.incrementTerminatedInstances();
        logger.info("工作流实例已终止: {} (用户: {}, 原因: {})", instanceId, userId, reason);
        
        return instanceStorage.get(instanceId);
    }
    
    @Override
    public WorkflowInstance cancelWorkflow(String instanceId, String userId, String reason) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
        }
        
        updateInstanceStatus(instanceId, InstanceStatus.CANCELLED, reason);
        statistics.incrementCancelledInstances();
        logger.info("工作流实例已取消: {} (用户: {}, 原因: {})", instanceId, userId, reason);
        
        return instanceStorage.get(instanceId);
    }
    
    @Override
    public WorkflowInstance getWorkflowInstance(String instanceId) {
        return instanceStorage.get(instanceId);
    }
    
    @Override
    public List<WorkflowInstance> getWorkflowInstances(String workflowId, InstanceStatus status, int limit) {
        return instanceStorage.values().stream()
            .filter(instance -> workflowId == null || workflowId.equals(instance.getWorkflowId()))
            .filter(instance -> status == null || status == instance.getStatus())
            .limit(limit > 0 ? limit : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StepExecutionHistory> getExecutionHistory(String instanceId) {
        List<StepExecutionResult> results = executionHistory.getOrDefault(instanceId, Collections.emptyList());
        return results.stream()
                .map(result -> new StepExecutionHistory(
                        result.getStepId(),
                        result.getStatus(),
                        result.getStartTime(),
                        result.getEndTime(),
                        result.getErrorMessage(),
                        result.getOutputData()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<InstanceStatus> getWorkflowState(String instanceId) {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance == null) {
            return Optional.empty();
        }
        
        InstanceStatus status = new InstanceStatus(
                instance.getId(),
                instance.getWorkflowId(),
                instance.getBusinessKey(),
                instance.getStatus(),
                instance.getCurrentStepId(),
                instance.getCurrentStepOrder(),
                instance.getContext(),
                instance.getStartTime(),
                instance.getUpdateTime(),
                instance.getErrorMessage()
        );
        
        return Optional.of(status);
    }
    
    @Override
    public WorkflowInstance updateWorkflowContext(String instanceId, Map<String, Object> contextUpdates, String userId) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        Objects.requireNonNull(contextUpdates, "上下文更新数据不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
        }
        
        // 检查实例状态是否允许更新上下文
        if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus()) || "TERMINATED".equals(instance.getStatus())) {
            throw new WorkflowException("已完成、已取消或已终止的工作流实例不能更新上下文", WorkflowException.ErrorType.INVALID_STATE);
        }
        
        // 更新上下文
        Map<String, Object> newContext = new HashMap<>(instance.getContext());
        newContext.putAll(contextUpdates);
        
        // 创建更新后的实例
        WorkflowInstance updatedInstance = WorkflowInstance.builder()
                .from(instance)
                .context(newContext)
                .updateTime(LocalDateTime.now())
                .build();
        
        // 保存更新后的实例
        instanceStorage.put(instanceId, updatedInstance);
        
        // 记录上下文更新操作
        recordContextUpdate(instanceId, contextUpdates, userId);
        
        return updatedInstance;
    }

    /**
     * 记录上下文更新操作
     * @param instanceId 实例ID
     * @param contextUpdates 上下文更新内容
     * @param userId 操作用户ID
     */
    private void recordContextUpdate(String instanceId, Map<String, Object> contextUpdates, String userId) {
        // 创建上下文更新的执行结果记录
        StepExecutionResult updateResult = new StepExecutionResult(
            "CONTEXT_UPDATE", // stepId
            StepExecutionStatus.SUCCESS,
            "Context updated: " + contextUpdates.keySet(),
            contextUpdates,
            null, // error
            LocalDateTime.now(),
            LocalDateTime.now(),
            userId
        );
        
        // 记录到执行历史中
        recordExecutionResult(instanceId, updateResult);
        
        logger.info("记录上下文更新操作 - 实例ID: {}, 更新字段: {}, 操作用户: {}", 
                   instanceId, contextUpdates.keySet(), userId);
    }

    private void updateInstanceContext(String instanceId, Map<String, Object> contextData) {
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance != null) {
            Map<String, Object> newContext = new HashMap<>(instance.getContext());
            newContext.putAll(contextData);
            
            WorkflowInstance updatedInstance = WorkflowInstance.builder()
                .from(instance)
                .context(newContext)
                .updateTime(LocalDateTime.now())
                .build();
            
            instanceStorage.put(instanceId, updatedInstance);
        }
    }
    
    @Override
    public List<WorkflowTask> getUserTasks(String userId) {
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        List<WorkflowTask> tasks = new ArrayList<>();
        
        // 遍历所有实例的用户任务
        for (List<UserTask> userTasks : userTaskStorage.values()) {
            for (UserTask userTask : userTasks) {
                if ("PENDING".equals(userTask.getStatus())) {
                    // 获取工作流实例信息
                    WorkflowInstance instance = instanceStorage.get(userTask.getInstanceId());
                    if (instance != null) {
                        WorkflowTask task = new WorkflowTask(
                            userTask.getId(),
                            userTask.getInstanceId(),
                            userTask.getStepId(),
                            userTask.getName(),
                            userTask.getDescription(),
                            instance.getWorkflowId(),
                            instance.getName(),
                            userTask.getData(),
                            userTask.getCreateTime(),
                            "PENDING"
                        );
                        tasks.add(task);
                    }
                }
            }
        }
        
        return tasks;
    }
    
    @Override
    public PageResult<WorkflowTask> getUserTasks(String userId, int page, int size) {
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        List<WorkflowTask> allTasks = getUserTasks(userId);
        
        // 计算分页
        int total = allTasks.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        List<WorkflowTask> pagedTasks = start < total ? allTasks.subList(start, end) : new ArrayList<>();
        
        return new PageResult<>(pagedTasks, total, page, size);
    }
    
    @Override
     public List<WorkflowInstance> getWorkflowInstancesByBusinessKey(String businessKey) {
         if (businessKey == null || businessKey.trim().isEmpty()) {
             return new ArrayList<>();
         }
         
         return instanceStorage.values().stream()
             .filter(instance -> businessKey.equals(instance.getBusinessKey()))
             .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
             .collect(Collectors.toList());
     }

     @Override
    public StepExecutionResult retryStep(String instanceId, String stepId, String userId) throws WorkflowException {
         Objects.requireNonNull(instanceId, "实例ID不能为空");
         Objects.requireNonNull(stepId, "步骤ID不能为空");
         
         WorkflowInstance instance = getWorkflowInstance(instanceId);
         if (instance == null) {
             throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
         }
         
         // 检查实例状态
         if (!"FAILED".equals(instance.getStatus()) && !"ERROR".equals(instance.getStatus())) {
             throw new WorkflowException("只能重试失败或错误状态的工作流实例", WorkflowException.ErrorType.INVALID_STATE);
         }
         
         // 检查当前步骤是否匹配
         if (!stepId.equals(instance.getCurrentStepId())) {
             throw new WorkflowException("指定的步骤不是当前步骤: " + stepId, WorkflowException.ErrorType.INVALID_STATE);
         }
         
         // 获取工作流定义
         Workflow workflow = workflowStorage.get(instance.getWorkflowId());
         if (workflow == null) {
             throw new WorkflowException("工作流定义不存在: " + instance.getWorkflowId(), WorkflowException.ErrorType.WORKFLOW_NOT_FOUND);
         }
         
         // 获取当前步骤
         WorkflowStep currentStep = workflow.getSteps().stream()
                 .filter(step -> stepId.equals(step.getId()))
                 .findFirst()
                 .orElseThrow(() -> new WorkflowException("步骤不存在: " + stepId, WorkflowException.ErrorType.STEP_NOT_FOUND));
         
         // 检查是否可以重试
         if (!canRetryStep(instance, currentStep)) {
             throw new WorkflowException("步骤已达到最大重试次数: " + stepId, WorkflowException.ErrorType.RETRY_LIMIT_EXCEEDED);
         }
         
         // 重置实例状态
         instance.setStatus("RUNNING");
         instance.setUpdateTime(LocalDateTime.now());
         
         // 记录重试操作
         recordExecutionResult(instance, currentStep, "RETRY", "手动重试步骤: " + stepId, null);
         
         // 执行步骤
        try {
            return executeStep(instanceId, stepId, userId, new HashMap<>());
        } catch (Exception e) {
            // 如果重试失败，更新实例状态
            instance.setStatus("FAILED");
            instance.setUpdateTime(LocalDateTime.now());
            recordExecutionResult(instance, currentStep, "FAILED", "重试失败: " + e.getMessage(), null);
            throw new WorkflowException("重试步骤失败: " + e.getMessage(), WorkflowException.ErrorType.EXECUTION_ERROR, e);
        }
     }

     @Override
    public WorkflowInstance skipStep(String instanceId, String stepId, String userId, String reason) throws WorkflowException {
         Objects.requireNonNull(instanceId, "实例ID不能为空");
         Objects.requireNonNull(stepId, "步骤ID不能为空");
         
         WorkflowInstance instance = getWorkflowInstance(instanceId);
         if (instance == null) {
             throw new WorkflowException("工作流实例不存在: " + instanceId, WorkflowException.ErrorType.INSTANCE_NOT_FOUND);
         }
         
         // 检查实例状态
         if (!"RUNNING".equals(instance.getStatus()) && !"FAILED".equals(instance.getStatus()) && !"ERROR".equals(instance.getStatus())) {
             throw new WorkflowException("只能跳过运行中、失败或错误状态的工作流实例的步骤", WorkflowException.ErrorType.INVALID_STATE);
         }
         
         // 检查当前步骤是否匹配
         if (!stepId.equals(instance.getCurrentStepId())) {
             throw new WorkflowException("指定的步骤不是当前步骤: " + stepId, WorkflowException.ErrorType.INVALID_STATE);
         }
         
         // 获取工作流定义
         Workflow workflow = workflowStorage.get(instance.getWorkflowId());
         if (workflow == null) {
             throw new WorkflowException("工作流定义不存在: " + instance.getWorkflowId(), WorkflowException.ErrorType.WORKFLOW_NOT_FOUND);
         }
         
         // 获取当前步骤
         WorkflowStep currentStep = workflow.getSteps().stream()
                 .filter(step -> stepId.equals(step.getId()))
                 .findFirst()
                 .orElseThrow(() -> new WorkflowException("步骤不存在: " + stepId, WorkflowException.ErrorType.STEP_NOT_FOUND));
         
         // 检查步骤是否可以跳过
         if (currentStep.isRequired()) {
             throw new WorkflowException("必需步骤不能跳过: " + stepId, WorkflowException.ErrorType.INVALID_OPERATION);
         }
         
         // 记录跳过操作
        recordExecutionResult(instance, currentStep, "SKIPPED", "手动跳过步骤: " + stepId + ", 原因: " + reason, null);
         
         // 确定下一步骤
         WorkflowStep nextStep = determineNextStep(workflow, currentStep, instance.getContext());
         
         if (nextStep != null) {
             // 更新到下一步骤
             updateCurrentStep(instance, nextStep.getId());
             instance.setStatus("RUNNING");
             
             // 继续执行工作流
            return continueWorkflow(instanceId, userId, new HashMap<>());
         } else {
             // 没有下一步骤，完成工作流
             completeWorkflow(instance);
             return instance;
         }
     }
    
    /**
      * 回滚后清理操作
      * 清理目标步骤之后的执行历史和用户任务
      * 
      * @param instanceId 实例ID
      * @param targetStepId 目标步骤ID
      */
     private void cleanupAfterRollback(String instanceId, String targetStepId) {
         // 清理执行历史中目标步骤之后的记录
         List<ExecutionHistory> histories = executionHistoryStorage.get(instanceId);
         if (histories != null) {
             // 找到目标步骤的最后一次成功执行时间
             LocalDateTime targetStepTime = histories.stream()
                     .filter(h -> targetStepId.equals(h.getStepId()) && "SUCCESS".equals(h.getStatus()))
                     .map(ExecutionHistory::getEndTime)
                     .max(LocalDateTime::compareTo)
                     .orElse(LocalDateTime.MIN);
             
             // 移除目标步骤之后的执行历史
             histories.removeIf(h -> h.getStartTime().isAfter(targetStepTime));
         }
         
         // 清理用户任务
         List<UserTask> userTasks = userTaskStorage.get(instanceId);
         if (userTasks != null) {
             userTasks.removeIf(task -> !targetStepId.equals(task.getStepId()));
         }
     }

     // 辅助方法
    
    /**
     * 生成实例ID
     */
    private String generateInstanceId() {
        return "WF_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 确定下一个要执行的步骤
     */
    private WorkflowStep determineNextStep(WorkflowInstance instance, Workflow workflow) {
        if (instance.getCurrentStepId() == null) {
            // 查找开始步骤
            return workflow.getSteps().stream()
                .filter(step -> step.getType() == StepType.START)
                .findFirst()
                .orElse(workflow.getSteps().isEmpty() ? null : workflow.getSteps().get(0));
        }
        
        // 查找当前步骤
        WorkflowStep currentStep = workflow.getSteps().stream()
            .filter(step -> step.getId().equals(instance.getCurrentStepId()))
            .findFirst()
            .orElse(null);
        
        if (currentStep == null || currentStep.getNextStepId() == null) {
            return null;
        }
        
        // 查找下一个步骤
        return workflow.getSteps().stream()
            .filter(step -> step.getId().equals(currentStep.getNextStepId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 检查前置条件
     */
    private boolean checkPrecondition(WorkflowInstance instance, WorkflowStep step) {
        if (step.getPrecondition() == null || step.getPrecondition().trim().isEmpty()) {
            return true;
        }
        
        // 这里应该实现条件表达式的解析和执行
        // 简化实现，总是返回true
        return true;
    }
    
    /**
     * 获取步骤执行器
     */
    private StepExecutor getExecutor(WorkflowStep step) {
        return executorRegistry.get(step.getType().name());
    }
    
    /**
     * 创建执行上下文
     */
    private StepExecutionContext createExecutionContext(WorkflowInstance instance, WorkflowStep step, 
                                                        String userId, Map<String, Object> inputData) {
        return StepExecutionContext.builder()
            .userId(userId)
            .inputParameters(inputData != null ? inputData : new HashMap<>())
            .instanceContext(instance.getContext())
            .timeoutMillis(step.getTimeoutSeconds() != null ? step.getTimeoutSeconds() * 1000L : null)
            .retryCount(0)
            .build();
    }
    
    /**
     * 记录执行结果
     */
    private void recordExecutionResult(String instanceId, StepExecutionResult result) {
        executionHistory.computeIfAbsent(instanceId, k -> new ArrayList<>()).add(result);
    }
    
    /**
     * 更新实例状态
     */
    private void updateInstanceStatus(String instanceId, InstanceStatus status, String message) {
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance != null) {
            WorkflowInstance updatedInstance = WorkflowInstance.builder()
                .from(instance)
                .status(status)
                .message(message)
                .updateTime(LocalDateTime.now())
                .build();
            
            instanceStorage.put(instanceId, updatedInstance);
        }
    }
    
    /**
     * 更新当前步骤
     */
    private void updateCurrentStep(String instanceId, String stepId) {
        WorkflowInstance instance = instanceStorage.get(instanceId);
        if (instance != null) {
            WorkflowInstance updatedInstance = WorkflowInstance.builder()
                .from(instance)
                .currentStepId(stepId)
                .updateTime(LocalDateTime.now())
                .build();
            
            instanceStorage.put(instanceId, updatedInstance);
        }
    }
    
    /**
     * 完成工作流
     */
    private void completeWorkflow(String instanceId) {
        updateInstanceStatus(instanceId, InstanceStatus.COMPLETED, "工作流执行完成");
        statistics.incrementCompletedInstances();
        logger.info("工作流实例执行完成: {}", instanceId);
    }
    
    /**
     * 检查步骤是否可以重试
     */
    private boolean canRetryStep(WorkflowInstance instance, WorkflowStep step, StepExecutionResult result) {
        if (step.getRetryCount() == null || step.getRetryCount() <= 0) {
            return false;
        }
        
        // 获取当前步骤的重试次数
        List<StepExecutionResult> history = executionHistory.getOrDefault(instance.getId(), Collections.emptyList());
        long retryCount = history.stream()
            .filter(r -> r.getStepId().equals(step.getId()))
            .filter(r -> r.getStatus() == StepExecutionResult.Status.FAILED || 
                        r.getStatus() == StepExecutionResult.Status.TIMEOUT)
            .count();
        
        return retryCount < step.getRetryCount();
    }
    
    /**
     * 调度步骤重试
     */
    private void scheduleStepRetry(WorkflowInstance instance, WorkflowStep step, String userId) {
        // 计算重试延迟
        long delaySeconds = calculateRetryDelay(instance, step);
        
        scheduler.schedule(() -> {
            try {
                executeStep(instance, step, userId);
            } catch (Exception e) {
                logger.error("重试步骤执行失败: {} (实例: {})", step.getId(), instance.getId(), e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
        
        logger.info("已调度步骤重试: {} (实例: {}, 延迟: {}秒)", step.getId(), instance.getId(), delaySeconds);
    }
    
    /**
     * 计算重试延迟
     */
    private long calculateRetryDelay(WorkflowInstance instance, WorkflowStep step) {
        // 获取当前重试次数
        List<StepExecutionResult> history = executionHistory.getOrDefault(instance.getId(), Collections.emptyList());
        long retryCount = history.stream()
            .filter(r -> r.getNextStepId().equals(step.getId()))
            .filter(r -> r.getStatus() == StepExecutionResult.Status.FAILED || 
                        r.getStatus() == StepExecutionResult.Status.TIMEOUT)
            .count();
        
        // 指数退避算法
        return Math.min(configuration.getMaxRetryDelaySeconds(), 
                       configuration.getBaseRetryDelaySeconds() * (1L << retryCount));
    }
    
    /**
     * 执行错误处理步骤
     */
    private void executeErrorStep(WorkflowInstance instance, WorkflowStep step, String userId) {
        // 查找错误处理步骤
        Workflow workflow = workflowStorage.get(instance.getWorkflowId());
        if (workflow != null) {
            WorkflowStep errorStep = workflow.getSteps().stream()
                .filter(s -> s.getId().equals(step.getErrorStepId()))
                .findFirst()
                .orElse(null);
            
            if (errorStep != null) {
                logger.info("执行错误处理步骤: {} (实例: {})", errorStep.getId(), instance.getId());
                executeStep(instance, errorStep, userId);
            }
        }
    }
    
    /**
     * 创建用户任务
     */
    private void createUserTask(WorkflowInstance instance, WorkflowStep step, StepExecutionResult result) {
        UserTask userTask = new UserTask(
            UUID.randomUUID().toString(),
            instance.getId(),
            step.getId(),
            step.getName(),
            step.getDescription(),
            result.getOutputData(),
            LocalDateTime.now(),
            null,
            "PENDING"
        );
        
        userTaskStorage.computeIfAbsent(instance.getId(), k -> new ArrayList<>()).add(userTask);
        logger.info("已创建用户任务: {} (实例: {}, 步骤: {})", userTask.getId(), instance.getId(), step.getId());
    }
    
    /**
     * 清理过期实例
     */
    private void cleanupExpiredInstances() {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(configuration.getInstanceRetentionDays());
            
            List<String> expiredInstances = instanceStorage.values().stream()
                .filter(instance -> instance.getCreateTime().isBefore(expireTime))
                .filter(instance -> instance.getStatus().isFinal())
                .map(WorkflowInstance::getId)
                .collect(Collectors.toList());
            
            for (String instanceId : expiredInstances) {
                instanceStorage.remove(instanceId);
                executionHistory.remove(instanceId);
                userTaskStorage.remove(instanceId);
            }
            
            if (!expiredInstances.isEmpty()) {
                logger.info("已清理过期实例: {} 个", expiredInstances.size());
            }
        } catch (Exception e) {
            logger.error("清理过期实例失败", e);
        }
    }
    
    // 内部类
    
    /**
     * 引擎配置
     */
    public static class EngineConfiguration {
        private int asyncThreadPoolSize = 10;
        private int schedulerThreadPoolSize = 5;
        private int cleanupIntervalMinutes = 60;
        private int instanceRetentionDays = 30;
        private long baseRetryDelaySeconds = 1;
        private long maxRetryDelaySeconds = 300;
        
        public static EngineConfiguration defaultConfig() {
            return new EngineConfiguration();
        }
        
        // Getters and setters
        public int getAsyncThreadPoolSize() { return asyncThreadPoolSize; }
        public void setAsyncThreadPoolSize(int asyncThreadPoolSize) { this.asyncThreadPoolSize = asyncThreadPoolSize; }
        
        public int getSchedulerThreadPoolSize() { return schedulerThreadPoolSize; }
        public void setSchedulerThreadPoolSize(int schedulerThreadPoolSize) { this.schedulerThreadPoolSize = schedulerThreadPoolSize; }
        
        public int getCleanupIntervalMinutes() { return cleanupIntervalMinutes; }
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) { this.cleanupIntervalMinutes = cleanupIntervalMinutes; }
        
        public int getInstanceRetentionDays() { return instanceRetentionDays; }
        public void setInstanceRetentionDays(int instanceRetentionDays) { this.instanceRetentionDays = instanceRetentionDays; }
        
        public long getBaseRetryDelaySeconds() { return baseRetryDelaySeconds; }
        public void setBaseRetryDelaySeconds(long baseRetryDelaySeconds) { this.baseRetryDelaySeconds = baseRetryDelaySeconds; }
        
        public long getMaxRetryDelaySeconds() { return maxRetryDelaySeconds; }
        public void setMaxRetryDelaySeconds(long maxRetryDelaySeconds) { this.maxRetryDelaySeconds = maxRetryDelaySeconds; }
        
        @Override
        public String toString() {
            return String.format("EngineConfiguration{asyncThreadPoolSize=%d, schedulerThreadPoolSize=%d, cleanupIntervalMinutes=%d, instanceRetentionDays=%d}", 
                               asyncThreadPoolSize, schedulerThreadPoolSize, cleanupIntervalMinutes, instanceRetentionDays);
        }
    }
    
    /**
     * 引擎统计信息
     */
    public static class EngineStatistics {
        private volatile long startedInstances = 0;
        private volatile long completedInstances = 0;
        private volatile long failedInstances = 0;
        private volatile long terminatedInstances = 0;
        private volatile long cancelledInstances = 0;
        
        public void incrementStartedInstances() { startedInstances++; }
        public void incrementCompletedInstances() { completedInstances++; }
        public void incrementFailedInstances() { failedInstances++; }
        public void incrementTerminatedInstances() { terminatedInstances++; }
        public void incrementCancelledInstances() { cancelledInstances++; }
        
        // Getters
        public long getStartedInstances() { return startedInstances; }
        public long getCompletedInstances() { return completedInstances; }
        public long getFailedInstances() { return failedInstances; }
        public long getTerminatedInstances() { return terminatedInstances; }
        public long getCancelledInstances() { return cancelledInstances; }
        public long getRunningInstances() { return startedInstances - completedInstances - failedInstances - terminatedInstances - cancelledInstances; }
        
        @Override
        public String toString() {
            return String.format("EngineStatistics{started=%d, completed=%d, failed=%d, terminated=%d, cancelled=%d, running=%d}", 
                               startedInstances, completedInstances, failedInstances, terminatedInstances, cancelledInstances, getRunningInstances());
        }
    }
    
    /**
     * 用户任务
     */
    public static class UserTask {
        private final String id;
        private final String instanceId;
        private final String stepId;
        private final String name;
        private final String description;
        private final Map<String, Object> data;
        private final LocalDateTime createTime;
        private final LocalDateTime completeTime;
        private final String status;
        
        public UserTask(String id, String instanceId, String stepId, String name, String description, 
                       Map<String, Object> data, LocalDateTime createTime, LocalDateTime completeTime, String status) {
            this.id = id;
            this.instanceId = instanceId;
            this.stepId = stepId;
            this.name = name;
            this.description = description;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.createTime = createTime;
            this.completeTime = completeTime;
            this.status = status;
        }
        
        // Getters
        public String getId() { return id; }
        public String getInstanceId() { return instanceId; }
        public String getStepId() { return stepId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
        public LocalDateTime getCreateTime() { return createTime; }
        public LocalDateTime getCompleteTime() { return completeTime; }
        public String getStatus() { return status; }
    }
    
    /**
     * 导出工作流实例数据
     * 
     * 导出指定工作流实例的完整数据，包括实例信息、执行历史、用户任务等，
     * 用于备份、迁移或数据分析。
     * 
     * @param instanceId 工作流实例ID
     * @return 实例数据的JSON表示
     * @throws WorkflowException 如果导出失败
     */
    @Override
    public String exportWorkflowInstance(String instanceId) throws WorkflowException {
        Objects.requireNonNull(instanceId, "实例ID不能为空");
        
        // 获取工作流实例
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        if (instance == null) {
            throw new WorkflowException("工作流实例不存在: " + instanceId, 
                                      WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
        }
        
        try {
            // 构建导出数据结构
            Map<String, Object> exportData = new HashMap<>();
            
            // 1. 基本实例信息
            Map<String, Object> instanceData = new HashMap<>();
            instanceData.put("id", instance.getId());
            instanceData.put("workflowId", instance.getWorkflowId());
            instanceData.put("name", instance.getName());
            instanceData.put("status", instance.getStatus().name());
            instanceData.put("currentStepId", instance.getCurrentStepId());
            instanceData.put("currentStepOrder", instance.getCurrentStepOrder());
            instanceData.put("startUserId", instance.getStartUserId());
            instanceData.put("currentUserId", instance.getCurrentUserId());
            instanceData.put("businessKey", instance.getBusinessKey());
            instanceData.put("priority", instance.getPriority());
            instanceData.put("createTime", instance.getCreateTime());
            instanceData.put("updateTime", instance.getUpdateTime());
            instanceData.put("startTime", instance.getStartTime());
            instanceData.put("endTime", instance.getEndTime());
            instanceData.put("message", instance.getMessage());
            instanceData.put("context", new HashMap<>(instance.getContext()));
            instanceData.put("config", new HashMap<>(instance.getConfig()));
            exportData.put("instance", instanceData);
            
            // 2. 执行历史
            List<StepExecutionResult> history = executionHistory.get(instanceId);
            if (history != null) {
                List<Map<String, Object>> historyData = new ArrayList<>();
                for (StepExecutionResult result : history) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("stepId", result.getStepId());
                    resultData.put("stepName", result.getStepName());
                    resultData.put("stepOrder", result.getStepOrder());
                    resultData.put("status", result.getStatus().name());
                    resultData.put("message", result.getMessage());
                    resultData.put("executorId", result.getExecutorId());
                    resultData.put("startTime", result.getStartTime());
                    resultData.put("endTime", result.getEndTime());
                    resultData.put("duration", result.getDuration());
                    resultData.put("retryCount", result.getRetryCount());
                    resultData.put("output", result.getOutput() != null ? new HashMap<>(result.getOutput()) : null);
                    resultData.put("error", result.getError());
                    historyData.add(resultData);
                }
                exportData.put("executionHistory", historyData);
            }
            
            // 3. 用户任务
            List<WorkflowTask> tasks = userTaskStorage.get(instanceId);
            if (tasks != null) {
                List<Map<String, Object>> tasksData = new ArrayList<>();
                for (WorkflowTask task : tasks) {
                    Map<String, Object> taskData = new HashMap<>();
                    taskData.put("id", task.getId());
                    taskData.put("instanceId", task.getInstanceId());
                    taskData.put("workflowId", task.getWorkflowId());
                    taskData.put("stepId", task.getStepId());
                    taskData.put("name", task.getName());
                    taskData.put("description", task.getDescription());
                    taskData.put("assignee", task.getAssignee());
                    taskData.put("candidateUsers", task.getCandidateUsers() != null ? new ArrayList<>(task.getCandidateUsers()) : null);
                    taskData.put("candidateGroups", task.getCandidateGroups() != null ? new ArrayList<>(task.getCandidateGroups()) : null);
                    taskData.put("priority", task.getPriority());
                    taskData.put("dueDate", task.getDueDate());
                    taskData.put("status", task.getStatus().name());
                    taskData.put("variables", task.getVariables() != null ? new HashMap<>(task.getVariables()) : null);
                    taskData.put("createTime", task.getCreateTime());
                    taskData.put("updateTime", task.getUpdateTime());
                    tasksData.add(taskData);
                }
                exportData.put("userTasks", tasksData);
            }
            
            // 4. 导出元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportTime", LocalDateTime.now());
            metadata.put("exportVersion", "1.0");
            metadata.put("engineVersion", "1.0");
            exportData.put("metadata", metadata);
            
            // 转换为JSON字符串
            return JSONUtil.toJsonStr(exportData);
            
        } catch (Exception e) {
            throw new WorkflowException("导出工作流实例失败: " + e.getMessage(), 
                                      WorkflowException.WorkflowErrorType.EXECUTION_ERROR, e);
        }
    }
    
    /**
     * 导入工作流实例数据
     * 
     * 从导出的数据中恢复工作流实例，包括实例状态、执行历史、用户任务等。
     * 注意：导入时会生成新的实例ID以避免冲突。
     * 
     * @param instanceData 实例数据的JSON表示
     * @param userId 操作用户ID
     * @return 导入的工作流实例
     * @throws WorkflowException 如果导入失败
     */
    @Override
    public WorkflowInstance importWorkflowInstance(String instanceData, String userId) throws WorkflowException {
        Objects.requireNonNull(instanceData, "实例数据不能为空");
        Objects.requireNonNull(userId, "用户ID不能为空");
        
        try {
            // 解析JSON数据
            Map<String, Object> exportData = JSONUtil.toBean(instanceData, Map.class);
            
            // 验证数据格式
            if (!exportData.containsKey("instance") || !exportData.containsKey("metadata")) {
                throw new WorkflowException("无效的导入数据格式", 
                                          WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
            }
            
            Map<String, Object> instanceInfo = (Map<String, Object>) exportData.get("instance");
            
            // 验证工作流定义是否存在
            String workflowId = (String) instanceInfo.get("workflowId");
            if (!workflowStorage.containsKey(workflowId)) {
                throw new WorkflowException("工作流定义不存在: " + workflowId, 
                                          WorkflowException.WorkflowErrorType.CONFIGURATION_ERROR);
            }
            
            // 生成新的实例ID以避免冲突
            String newInstanceId = generateInstanceId();
            
            // 重建工作流实例
            WorkflowInstance.Builder builder = WorkflowInstance.builder()
                .id(newInstanceId)
                .workflowId(workflowId)
                .name((String) instanceInfo.get("name"))
                .status(InstanceStatus.valueOf((String) instanceInfo.get("status")))
                .currentStepId((String) instanceInfo.get("currentStepId"))
                .currentStepOrder((Integer) instanceInfo.get("currentStepOrder"))
                .startUserId((String) instanceInfo.get("startUserId"))
                .currentUserId((String) instanceInfo.get("currentUserId"))
                .businessKey((String) instanceInfo.get("businessKey"))
                .priority((Integer) instanceInfo.get("priority"))
                .message((String) instanceInfo.get("message"));
            
            // 设置上下文和配置
            Map<String, Object> context = (Map<String, Object>) instanceInfo.get("context");
            if (context != null) {
                builder.context(new HashMap<>(context));
            }
            
            Map<String, Object> config = (Map<String, Object>) instanceInfo.get("config");
            if (config != null) {
                builder.config(new HashMap<>(config));
            }
            
            // 设置时间字段（导入时更新创建时间和更新时间）
            LocalDateTime now = LocalDateTime.now();
            builder.createTime(now)
                   .updateTime(now)
                   .startTime(now);
            
            // 如果原实例已结束，保持结束时间
            Object endTimeObj = instanceInfo.get("endTime");
            if (endTimeObj != null) {
                builder.endTime((LocalDateTime) endTimeObj);
            }
            
            WorkflowInstance instance = builder.build();
            
            // 保存实例
            instanceStorage.put(newInstanceId, instance);
            
            // 重建执行历史
            List<Map<String, Object>> historyData = (List<Map<String, Object>>) exportData.get("executionHistory");
            if (historyData != null) {
                List<StepExecutionResult> history = new ArrayList<>();
                for (Map<String, Object> resultData : historyData) {
                    StepExecutionResult result = StepExecutionResult.builder()
                        .stepId((String) resultData.get("stepId"))
                        .stepName((String) resultData.get("stepName"))
                        .stepOrder((Integer) resultData.get("stepOrder"))
                        .status(StepExecutionStatus.valueOf((String) resultData.get("status")))
                        .message((String) resultData.get("message"))
                        .executorId((String) resultData.get("executorId"))
                        .startTime((LocalDateTime) resultData.get("startTime"))
                        .endTime((LocalDateTime) resultData.get("endTime"))
                        .duration((Long) resultData.get("duration"))
                        .retryCount((Integer) resultData.get("retryCount"))
                        .error((String) resultData.get("error"))
                        .build();
                    
                    Map<String, Object> output = (Map<String, Object>) resultData.get("output");
                    if (output != null) {
                        result.setOutput(new HashMap<>(output));
                    }
                    
                    history.add(result);
                }
                executionHistory.put(newInstanceId, history);
            }
            
            // 重建用户任务
            List<Map<String, Object>> tasksData = (List<Map<String, Object>>) exportData.get("userTasks");
            if (tasksData != null) {
                List<WorkflowTask> tasks = new ArrayList<>();
                for (Map<String, Object> taskData : tasksData) {
                    WorkflowTask.Builder taskBuilder = WorkflowTask.builder()
                        .id((String) taskData.get("id"))
                        .instanceId(newInstanceId)  // 使用新的实例ID
                        .workflowId((String) taskData.get("workflowId"))
                        .stepId((String) taskData.get("stepId"))
                        .name((String) taskData.get("name"))
                        .description((String) taskData.get("description"))
                        .assignee((String) taskData.get("assignee"))
                        .priority((Integer) taskData.get("priority"))
                        .dueDate((LocalDateTime) taskData.get("dueDate"))
                        .status(WorkflowTask.TaskStatus.valueOf((String) taskData.get("status")))
                        .createTime((LocalDateTime) taskData.get("createTime"))
                        .updateTime((LocalDateTime) taskData.get("updateTime"));
                    
                    List<String> candidateUsers = (List<String>) taskData.get("candidateUsers");
                    if (candidateUsers != null) {
                        taskBuilder.candidateUsers(new ArrayList<>(candidateUsers));
                    }
                    
                    List<String> candidateGroups = (List<String>) taskData.get("candidateGroups");
                    if (candidateGroups != null) {
                        taskBuilder.candidateGroups(new ArrayList<>(candidateGroups));
                    }
                    
                    Map<String, Object> variables = (Map<String, Object>) taskData.get("variables");
                    if (variables != null) {
                        taskBuilder.variables(new HashMap<>(variables));
                    }
                    
                    tasks.add(taskBuilder.build());
                }
                userTaskStorage.put(newInstanceId, tasks);
            }
            
            // 记录导入操作
            log.info("成功导入工作流实例: {} -> {}, 操作用户: {}", 
                    instanceInfo.get("id"), newInstanceId, userId);
            
            return instance;
            
        } catch (Exception e) {
            throw new WorkflowException("导入工作流实例失败: " + e.getMessage(), 
                                      WorkflowException.WorkflowErrorType.EXECUTION_ERROR, e);
        }
    }
}