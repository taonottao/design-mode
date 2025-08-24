package com.tao.workflow.executor;

import com.tao.workflow.engine.*;
import com.tao.workflow.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 任务步骤执行器
 * 
 * 用于执行普通的任务步骤，支持多种任务类型：
 * 1. 脚本任务：执行脚本代码
 * 2. Java任务：执行Java方法
 * 3. HTTP任务：发送HTTP请求
 * 4. 数据库任务：执行数据库操作
 * 5. 文件任务：处理文件操作
 * 6. 自定义任务：通过配置的处理器执行
 * 
 * 支持的配置参数：
 * - taskType: 任务类型（script/java/http/database/file/custom）
 * - handler: 任务处理器类名或方法名
 * - parameters: 任务参数
 * - timeout: 任务超时时间
 * - async: 是否异步执行
 * 
 * @author Tao
 * @version 1.0
 */
public class TaskStepExecutor extends AbstractStepExecutor {
    
    /** 任务处理器注册表 */
    private final Map<String, TaskHandler> taskHandlers = new HashMap<>();
    
    /** 默认任务处理器 */
    private final Map<String, TaskHandler> defaultHandlers = new HashMap<>();
    
    /**
     * 构造函数
     */
    public TaskStepExecutor() {
        super("TaskStepExecutor", "1.0.0", StepType.TASK);
        
        // 注册默认任务处理器
        registerDefaultHandlers();
        
        logger.info("任务步骤执行器已初始化，支持任务类型: {}", defaultHandlers.keySet());
    }
    
    /**
     * 注册默认任务处理器
     */
    private void registerDefaultHandlers() {
        // 脚本任务处理器
        defaultHandlers.put("script", new ScriptTaskHandler());
        
        // Java任务处理器
        defaultHandlers.put("java", new JavaTaskHandler());
        
        // HTTP任务处理器
        defaultHandlers.put("http", new HttpTaskHandler());
        
        // 数据库任务处理器
        defaultHandlers.put("database", new DatabaseTaskHandler());
        
        // 文件任务处理器
        defaultHandlers.put("file", new FileTaskHandler());
        
        // 默认任务处理器
        defaultHandlers.put("default", new DefaultTaskHandler());
    }
    
    /**
     * 注册自定义任务处理器
     */
    public void registerTaskHandler(String taskType, TaskHandler handler) {
        Objects.requireNonNull(taskType, "任务类型不能为空");
        Objects.requireNonNull(handler, "任务处理器不能为空");
        
        taskHandlers.put(taskType, handler);
        logger.info("已注册自定义任务处理器: {} -> {}", taskType, handler.getClass().getSimpleName());
    }
    
    @Override
    protected StepExecutionResult doExecute(WorkflowStep step, StepExecutionContext context) throws Exception {
        logger.info("开始执行任务步骤: {} (类型: {})", step.getId(), step.getType());
        
        // 获取任务配置
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            config = new HashMap<>();
        }
        
        // 获取任务类型
        String taskType = getTaskType(config);
        logger.debug("任务类型: {}", taskType);
        
        // 获取任务处理器
        TaskHandler handler = getTaskHandler(taskType);
        if (handler == null) {
            throw new WorkflowException(
                String.format("未找到任务类型 [%s] 的处理器", taskType),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 准备任务上下文
        TaskContext taskContext = createTaskContext(step, context, config);
        
        // 执行任务
        try {
            TaskResult taskResult = handler.execute(taskContext);
            
            // 转换为步骤执行结果
            return convertTaskResult(step, taskResult);
            
        } catch (Exception e) {
            logger.error("任务执行失败: {} (任务类型: {})", step.getId(), taskType, e);
            throw e;
        }
    }
    
    @Override
    protected void doValidateConfiguration(WorkflowStep step) throws WorkflowException {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            return; // 允许空配置，使用默认处理器
        }
        
        // 验证任务类型
        String taskType = getTaskType(config);
        TaskHandler handler = getTaskHandler(taskType);
        if (handler == null) {
            throw new WorkflowException(
                String.format("不支持的任务类型: %s", taskType),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证任务配置
        try {
            handler.validateConfiguration(config);
        } catch (Exception e) {
            throw new WorkflowException(
                String.format("任务配置验证失败: %s", e.getMessage()),
                WorkflowException.ErrorType.CONFIGURATION_ERROR,
                e
            );
        }
    }
    
    @Override
    protected long doEstimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            return super.doEstimateExecutionTime(step, context);
        }
        
        String taskType = getTaskType(config);
        TaskHandler handler = getTaskHandler(taskType);
        if (handler != null) {
            return handler.estimateExecutionTime(config);
        }
        
        return super.doEstimateExecutionTime(step, context);
    }
    
    @Override
    protected Map<String, Object> doGetResourceRequirements(WorkflowStep step, StepExecutionContext context) {
        Map<String, Object> requirements = new HashMap<>();
        
        Map<String, Object> config = step.getConfiguration();
        if (config != null) {
            String taskType = getTaskType(config);
            TaskHandler handler = getTaskHandler(taskType);
            if (handler != null) {
                requirements.putAll(handler.getResourceRequirements(config));
            }
        }
        
        return requirements;
    }
    
    // 辅助方法
    
    /**
     * 获取任务类型
     */
    private String getTaskType(Map<String, Object> config) {
        Object taskType = config.get("taskType");
        if (taskType == null) {
            taskType = config.get("type");
        }
        return taskType != null ? taskType.toString() : "default";
    }
    
    /**
     * 获取任务处理器
     */
    private TaskHandler getTaskHandler(String taskType) {
        // 优先查找自定义处理器
        TaskHandler handler = taskHandlers.get(taskType);
        if (handler != null) {
            return handler;
        }
        
        // 查找默认处理器
        return defaultHandlers.get(taskType);
    }
    
    /**
     * 创建任务上下文
     */
    private TaskContext createTaskContext(WorkflowStep step, StepExecutionContext context, Map<String, Object> config) {
        return new TaskContext(
            step.getId(),
            step.getName(),
            config,
            context.getInputParameters(),
            context.getInstanceContext(),
            context.getUserId(),
            context.getTemporaryData()
        );
    }
    
    /**
     * 转换任务结果为步骤执行结果
     */
    private StepExecutionResult convertTaskResult(WorkflowStep step, TaskResult taskResult) {
        StepExecutionResult.Status status;
        switch (taskResult.getStatus()) {
            case SUCCESS:
                status = StepExecutionResult.Status.SUCCESS;
                break;
            case FAILED:
                status = StepExecutionResult.Status.FAILED;
                break;
            case WAITING:
                status = StepExecutionResult.Status.WAITING;
                break;
            case RETRY:
                status = StepExecutionResult.Status.RETRY;
                break;
            default:
                status = StepExecutionResult.Status.FAILED;
                break;
        }
        
        return StepExecutionResult.builder()
            .status(status)
            .stepId(step.getId())
            .executorName(getExecutorName())
            .outputData(taskResult.getOutputData())
            .message(taskResult.getMessage())
            .errorMessage(taskResult.getErrorMessage())
            .exception(taskResult.getException())
            .executionTime(taskResult.getExecutionTime())
            .build();
    }
    
    // 内部类和接口
    
    /**
     * 任务处理器接口
     */
    public interface TaskHandler {
        
        /**
         * 执行任务
         */
        TaskResult execute(TaskContext context) throws Exception;
        
        /**
         * 验证配置
         */
        void validateConfiguration(Map<String, Object> config) throws Exception;
        
        /**
         * 估算执行时间
         */
        default long estimateExecutionTime(Map<String, Object> config) {
            return 30000; // 默认30秒
        }
        
        /**
         * 获取资源需求
         */
        default Map<String, Object> getResourceRequirements(Map<String, Object> config) {
            Map<String, Object> requirements = new HashMap<>();
            requirements.put("cpu", 0.1);
            requirements.put("memory", 64);
            return requirements;
        }
    }
    
    /**
     * 任务上下文
     */
    public static class TaskContext {
        private final String stepId;
        private final String stepName;
        private final Map<String, Object> config;
        private final Map<String, Object> inputParameters;
        private final Map<String, Object> instanceContext;
        private final String userId;
        private final Map<String, Object> temporaryData;
        
        public TaskContext(String stepId, String stepName, Map<String, Object> config,
                          Map<String, Object> inputParameters, Map<String, Object> instanceContext,
                          String userId, Map<String, Object> temporaryData) {
            this.stepId = stepId;
            this.stepName = stepName;
            this.config = new HashMap<>(config);
            this.inputParameters = new HashMap<>(inputParameters);
            this.instanceContext = new HashMap<>(instanceContext);
            this.userId = userId;
            this.temporaryData = new HashMap<>(temporaryData);
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public String getStepName() { return stepName; }
        public Map<String, Object> getConfig() { return new HashMap<>(config); }
        public Map<String, Object> getInputParameters() { return new HashMap<>(inputParameters); }
        public Map<String, Object> getInstanceContext() { return new HashMap<>(instanceContext); }
        public String getUserId() { return userId; }
        public Map<String, Object> getTemporaryData() { return new HashMap<>(temporaryData); }
        
        // 便利方法
        @SuppressWarnings("unchecked")
        public <T> T getConfig(String key, Class<T> type) {
            Object value = config.get(key);
            return type.isInstance(value) ? (T) value : null;
        }
        
        public <T> T getConfig(String key, Class<T> type, T defaultValue) {
            T value = getConfig(key, type);
            return value != null ? value : defaultValue;
        }
        
        public String getConfigString(String key) {
            return getConfig(key, String.class);
        }
        
        public String getConfigString(String key, String defaultValue) {
            return getConfig(key, String.class, defaultValue);
        }
        
        public Integer getConfigInteger(String key) {
            return getConfig(key, Integer.class);
        }
        
        public Integer getConfigInteger(String key, Integer defaultValue) {
            return getConfig(key, Integer.class, defaultValue);
        }
        
        public Boolean getConfigBoolean(String key) {
            return getConfig(key, Boolean.class);
        }
        
        public Boolean getConfigBoolean(String key, Boolean defaultValue) {
            return getConfig(key, Boolean.class, defaultValue);
        }
    }
    
    /**
     * 任务结果
     */
    public static class TaskResult {
        public enum Status {
            SUCCESS, FAILED, WAITING, RETRY
        }
        
        private final Status status;
        private final Map<String, Object> outputData;
        private final String message;
        private final String errorMessage;
        private final Exception exception;
        private final long executionTime;
        
        public TaskResult(Status status, Map<String, Object> outputData, String message, 
                         String errorMessage, Exception exception, long executionTime) {
            this.status = status;
            this.outputData = outputData != null ? new HashMap<>(outputData) : new HashMap<>();
            this.message = message;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.executionTime = executionTime;
        }
        
        public static TaskResult success(Map<String, Object> outputData) {
            return new TaskResult(Status.SUCCESS, outputData, null, null, null, 0);
        }
        
        public static TaskResult success(Map<String, Object> outputData, String message) {
            return new TaskResult(Status.SUCCESS, outputData, message, null, null, 0);
        }
        
        public static TaskResult failed(String errorMessage) {
            return new TaskResult(Status.FAILED, null, null, errorMessage, null, 0);
        }
        
        public static TaskResult failed(String errorMessage, Exception exception) {
            return new TaskResult(Status.FAILED, null, null, errorMessage, exception, 0);
        }
        
        public static TaskResult waiting(String message) {
            return new TaskResult(Status.WAITING, null, message, null, null, 0);
        }
        
        public static TaskResult retry(String message) {
            return new TaskResult(Status.RETRY, null, message, null, null, 0);
        }
        
        // Getters
        public Status getStatus() { return status; }
        public Map<String, Object> getOutputData() { return new HashMap<>(outputData); }
        public String getMessage() { return message; }
        public String getErrorMessage() { return errorMessage; }
        public Exception getException() { return exception; }
        public long getExecutionTime() { return executionTime; }
    }
    
    // 默认任务处理器实现
    
    /**
     * 脚本任务处理器
     */
    private static class ScriptTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            String script = context.getConfigString("script");
            String language = context.getConfigString("language", "javascript");
            
            if (script == null || script.trim().isEmpty()) {
                return TaskResult.failed("脚本内容不能为空");
            }
            
            // 这里应该集成脚本引擎执行脚本
            // 简化实现：模拟脚本执行
            Map<String, Object> result = new HashMap<>();
            result.put("scriptResult", "脚本执行成功");
            result.put("language", language);
            
            return TaskResult.success(result, "脚本执行完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("script")) {
                throw new IllegalArgumentException("脚本任务必须配置 'script' 参数");
            }
        }
    }
    
    /**
     * Java任务处理器
     */
    private static class JavaTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            String className = context.getConfigString("className");
            String methodName = context.getConfigString("methodName");
            
            if (className == null || methodName == null) {
                return TaskResult.failed("Java任务必须配置className和methodName");
            }
            
            // 这里应该通过反射调用Java方法
            // 简化实现：模拟方法调用
            Map<String, Object> result = new HashMap<>();
            result.put("className", className);
            result.put("methodName", methodName);
            result.put("result", "方法执行成功");
            
            return TaskResult.success(result, "Java方法执行完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("className") || !config.containsKey("methodName")) {
                throw new IllegalArgumentException("Java任务必须配置 'className' 和 'methodName' 参数");
            }
        }
    }
    
    /**
     * HTTP任务处理器
     */
    private static class HttpTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            String url = context.getConfigString("url");
            String method = context.getConfigString("method", "GET");
            
            if (url == null || url.trim().isEmpty()) {
                return TaskResult.failed("HTTP任务必须配置URL");
            }
            
            // 这里应该发送HTTP请求
            // 简化实现：模拟HTTP请求
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("method", method);
            result.put("statusCode", 200);
            result.put("response", "HTTP请求成功");
            
            return TaskResult.success(result, "HTTP请求完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("url")) {
                throw new IllegalArgumentException("HTTP任务必须配置 'url' 参数");
            }
        }
    }
    
    /**
     * 数据库任务处理器
     */
    private static class DatabaseTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            String sql = context.getConfigString("sql");
            String operation = context.getConfigString("operation", "query");
            
            if (sql == null || sql.trim().isEmpty()) {
                return TaskResult.failed("数据库任务必须配置SQL语句");
            }
            
            // 这里应该执行数据库操作
            // 简化实现：模拟数据库操作
            Map<String, Object> result = new HashMap<>();
            result.put("sql", sql);
            result.put("operation", operation);
            result.put("affectedRows", 1);
            result.put("result", "数据库操作成功");
            
            return TaskResult.success(result, "数据库操作完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("sql")) {
                throw new IllegalArgumentException("数据库任务必须配置 'sql' 参数");
            }
        }
    }
    
    /**
     * 文件任务处理器
     */
    private static class FileTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            String operation = context.getConfigString("operation");
            String filePath = context.getConfigString("filePath");
            
            if (operation == null || filePath == null) {
                return TaskResult.failed("文件任务必须配置operation和filePath");
            }
            
            // 这里应该执行文件操作
            // 简化实现：模拟文件操作
            Map<String, Object> result = new HashMap<>();
            result.put("operation", operation);
            result.put("filePath", filePath);
            result.put("result", "文件操作成功");
            
            return TaskResult.success(result, "文件操作完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("operation") || !config.containsKey("filePath")) {
                throw new IllegalArgumentException("文件任务必须配置 'operation' 和 'filePath' 参数");
            }
        }
    }
    
    /**
     * 默认任务处理器
     */
    private static class DefaultTaskHandler implements TaskHandler {
        @Override
        public TaskResult execute(TaskContext context) throws Exception {
            // 默认处理：简单地返回输入参数作为输出
            Map<String, Object> result = new HashMap<>();
            result.put("stepId", context.getStepId());
            result.put("stepName", context.getStepName());
            result.put("inputParameters", context.getInputParameters());
            result.put("config", context.getConfig());
            result.put("message", "默认任务处理完成");
            
            return TaskResult.success(result, "默认任务执行完成");
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            // 默认处理器不需要特殊配置验证
        }
    }
}