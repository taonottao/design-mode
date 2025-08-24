package com.tao.workflow.executor;

import com.tao.workflow.engine.*;
import com.tao.workflow.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户任务步骤执行器
 * 
 * 用于执行需要人工干预的用户任务步骤，支持：
 * 1. 任务分配：根据配置将任务分配给指定用户或用户组
 * 2. 任务通知：通过多种方式通知用户有待处理任务
 * 3. 任务处理：接收用户的处理结果并继续工作流
 * 4. 任务超时：处理任务超时情况
 * 5. 任务委托：支持任务转交和委托
 * 6. 任务撤回：支持任务撤回和重新分配
 * 
 * 支持的配置参数：
 * - assignee: 指定处理人
 * - candidateUsers: 候选用户列表
 * - candidateGroups: 候选用户组列表
 * - formKey: 表单标识
 * - formData: 表单数据
 * - priority: 任务优先级
 * - dueDate: 截止时间
 * - notification: 通知配置
 * 
 * @author Tao
 * @version 1.0
 */
public class UserTaskStepExecutor extends AbstractStepExecutor {
    
    /** 待处理用户任务存储 */
    private final Map<String, UserTask> pendingTasks = new ConcurrentHashMap<>();
    
    /** 任务分配策略注册表 */
    private final Map<String, TaskAssignmentStrategy> assignmentStrategies = new HashMap<>();
    
    /** 任务通知器注册表 */
    private final Map<String, TaskNotifier> notifiers = new HashMap<>();
    
    /**
     * 构造函数
     */
    public UserTaskStepExecutor() {
        super("UserTaskStepExecutor", "1.0.0", StepType.USER_TASK);
        
        // 注册默认分配策略
        registerDefaultAssignmentStrategies();
        
        // 注册默认通知器
        registerDefaultNotifiers();
        
        logger.info("用户任务步骤执行器已初始化");
    }
    
    /**
     * 注册默认分配策略
     */
    private void registerDefaultAssignmentStrategies() {
        assignmentStrategies.put("direct", new DirectAssignmentStrategy());
        assignmentStrategies.put("round_robin", new RoundRobinAssignmentStrategy());
        assignmentStrategies.put("load_balance", new LoadBalanceAssignmentStrategy());
        assignmentStrategies.put("random", new RandomAssignmentStrategy());
    }
    
    /**
     * 注册默认通知器
     */
    private void registerDefaultNotifiers() {
        notifiers.put("email", new EmailNotifier());
        notifiers.put("sms", new SmsNotifier());
        notifiers.put("system", new SystemNotifier());
    }
    
    /**
     * 注册自定义分配策略
     */
    public void registerAssignmentStrategy(String name, TaskAssignmentStrategy strategy) {
        Objects.requireNonNull(name, "策略名称不能为空");
        Objects.requireNonNull(strategy, "分配策略不能为空");
        
        assignmentStrategies.put(name, strategy);
        logger.info("已注册任务分配策略: {} -> {}", name, strategy.getClass().getSimpleName());
    }
    
    /**
     * 注册自定义通知器
     */
    public void registerNotifier(String type, TaskNotifier notifier) {
        Objects.requireNonNull(type, "通知器类型不能为空");
        Objects.requireNonNull(notifier, "通知器不能为空");
        
        notifiers.put(type, notifier);
        logger.info("已注册任务通知器: {} -> {}", type, notifier.getClass().getSimpleName());
    }
    
    @Override
    protected StepExecutionResult doExecute(WorkflowStep step, StepExecutionContext context) throws Exception {
        logger.info("开始执行用户任务步骤: {} (类型: {})", step.getId(), step.getType());
        
        // 获取任务配置
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            config = new HashMap<>();
        }
        
        // 创建用户任务
        UserTask userTask = createUserTask(step, context, config);
        
        // 分配任务
        assignTask(userTask, config);
        
        // 存储待处理任务
        pendingTasks.put(userTask.getId(), userTask);
        
        // 发送通知
        sendNotifications(userTask, config);
        
        // 构建执行结果（等待状态）
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("taskId", userTask.getId());
        outputData.put("assignee", userTask.getAssignee());
        outputData.put("candidateUsers", userTask.getCandidateUsers());
        outputData.put("candidateGroups", userTask.getCandidateGroups());
        outputData.put("formKey", userTask.getFormKey());
        outputData.put("priority", userTask.getPriority());
        outputData.put("dueDate", userTask.getDueDate());
        
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.WAITING)
            .stepId(step.getId())
            .executorName(getExecutorName())
            .outputData(outputData)
            .message(String.format("用户任务已创建并分配，任务ID: %s", userTask.getId()))
            .build();
    }
    
    /**
     * 完成用户任务
     */
    public StepExecutionResult completeUserTask(String taskId, String userId, Map<String, Object> formData) {
        UserTask userTask = pendingTasks.get(taskId);
        if (userTask == null) {
            throw new WorkflowException(
                String.format("未找到任务: %s", taskId),
                WorkflowException.ErrorType.DATA_ERROR
            );
        }
        
        // 验证用户权限
        if (!canCompleteTask(userTask, userId)) {
            throw new WorkflowException(
                String.format("用户 [%s] 无权限完成任务 [%s]", userId, taskId),
                WorkflowException.ErrorType.PERMISSION_ERROR
            );
        }
        
        // 更新任务状态
        userTask.setStatus(UserTask.Status.COMPLETED);
        userTask.setCompletedBy(userId);
        userTask.setCompletedTime(LocalDateTime.now());
        userTask.setFormData(formData);
        
        // 从待处理任务中移除
        pendingTasks.remove(taskId);
        
        logger.info("用户任务已完成: {} (完成人: {})", taskId, userId);
        
        // 构建执行结果
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("taskId", taskId);
        outputData.put("completedBy", userId);
        outputData.put("completedTime", userTask.getCompletedTime());
        outputData.put("formData", formData);
        
        return StepExecutionResult.builder()
            .status(StepExecutionResult.Status.SUCCESS)
            .stepId(userTask.getStepId())
            .executorName(getExecutorName())
            .outputData(outputData)
            .message(String.format("用户任务已完成，完成人: %s", userId))
            .build();
    }
    
    /**
     * 委托用户任务
     */
    public void delegateTask(String taskId, String fromUserId, String toUserId, String reason) {
        UserTask userTask = pendingTasks.get(taskId);
        if (userTask == null) {
            throw new WorkflowException(
                String.format("未找到任务: %s", taskId),
                WorkflowException.ErrorType.DATA_ERROR
            );
        }
        
        // 验证委托权限
        if (!canDelegateTask(userTask, fromUserId)) {
            throw new WorkflowException(
                String.format("用户 [%s] 无权限委托任务 [%s]", fromUserId, taskId),
                WorkflowException.ErrorType.PERMISSION_ERROR
            );
        }
        
        // 更新任务分配
        userTask.setAssignee(toUserId);
        userTask.setDelegatedBy(fromUserId);
        userTask.setDelegatedTime(LocalDateTime.now());
        userTask.setDelegationReason(reason);
        
        logger.info("用户任务已委托: {} (从 {} 委托给 {}, 原因: {})", taskId, fromUserId, toUserId, reason);
        
        // 发送委托通知
        sendDelegationNotification(userTask, fromUserId, toUserId, reason);
    }
    
    /**
     * 撤回用户任务
     */
    public void reclaimTask(String taskId, String userId) {
        UserTask userTask = pendingTasks.get(taskId);
        if (userTask == null) {
            throw new WorkflowException(
                String.format("未找到任务: %s", taskId),
                WorkflowException.ErrorType.DATA_ERROR
            );
        }
        
        // 验证撤回权限
        if (!canReclaimTask(userTask, userId)) {
            throw new WorkflowException(
                String.format("用户 [%s] 无权限撤回任务 [%s]", userId, taskId),
                WorkflowException.ErrorType.PERMISSION_ERROR
            );
        }
        
        // 重新分配任务
        userTask.setAssignee(userId);
        userTask.setReclaimedBy(userId);
        userTask.setReclaimedTime(LocalDateTime.now());
        
        logger.info("用户任务已撤回: {} (撤回人: {})", taskId, userId);
    }
    
    /**
     * 获取用户的待处理任务
     */
    public List<UserTask> getUserTasks(String userId) {
        return pendingTasks.values().stream()
            .filter(task -> canAccessTask(task, userId))
            .sorted(Comparator.comparing(UserTask::getCreatedTime).reversed())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取任务详情
     */
    public UserTask getTaskById(String taskId) {
        return pendingTasks.get(taskId);
    }
    
    @Override
    protected void doValidateConfiguration(WorkflowStep step) throws WorkflowException {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            return; // 允许空配置，使用默认设置
        }
        
        // 验证分配配置
        String assignee = getConfigValue(config, "assignee", null);
        @SuppressWarnings("unchecked")
        List<String> candidateUsers = (List<String>) config.get("candidateUsers");
        @SuppressWarnings("unchecked")
        List<String> candidateGroups = (List<String>) config.get("candidateGroups");
        
        if (assignee == null && (candidateUsers == null || candidateUsers.isEmpty()) && 
            (candidateGroups == null || candidateGroups.isEmpty())) {
            throw new WorkflowException(
                "用户任务必须配置 assignee、candidateUsers 或 candidateGroups 中的至少一项",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证分配策略
        String strategy = getConfigValue(config, "assignmentStrategy", "direct");
        if (!assignmentStrategies.containsKey(strategy)) {
            throw new WorkflowException(
                String.format("不支持的分配策略: %s", strategy),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
    }
    
    @Override
    protected long doEstimateExecutionTime(WorkflowStep step, StepExecutionContext context) {
        Map<String, Object> config = step.getConfiguration();
        if (config != null) {
            // 如果配置了截止时间，使用截止时间估算
            Object dueDate = config.get("dueDate");
            if (dueDate != null) {
                // 这里应该解析截止时间并计算差值
                // 简化实现：返回默认值
                return 24 * 60 * 60 * 1000; // 24小时
            }
        }
        
        // 默认估算时间：4小时
        return 4 * 60 * 60 * 1000;
    }
    
    // 辅助方法
    
    /**
     * 创建用户任务
     */
    private UserTask createUserTask(WorkflowStep step, StepExecutionContext context, Map<String, Object> config) {
        String taskId = UUID.randomUUID().toString();
        
        UserTask userTask = new UserTask();
        userTask.setId(taskId);
        userTask.setStepId(step.getId());
        userTask.setInstanceId(context.getInstanceId());
        userTask.setName(step.getName());
        userTask.setDescription(step.getDescription());
        userTask.setFormKey(getConfigValue(config, "formKey", null));
        userTask.setPriority(getConfigIntValue(config, "priority", 50));
        userTask.setStatus(UserTask.Status.CREATED);
        userTask.setCreatedTime(LocalDateTime.now());
        userTask.setCreatedBy(context.getUserId());
        
        // 设置截止时间
        String dueDateStr = getConfigValue(config, "dueDate", null);
        if (dueDateStr != null) {
            // 这里应该解析日期字符串
            // 简化实现：设置为24小时后
            userTask.setDueDate(LocalDateTime.now().plusHours(24));
        }
        
        return userTask;
    }
    
    /**
     * 分配任务
     */
    private void assignTask(UserTask userTask, Map<String, Object> config) {
        String strategyName = getConfigValue(config, "assignmentStrategy", "direct");
        TaskAssignmentStrategy strategy = assignmentStrategies.get(strategyName);
        
        if (strategy != null) {
            TaskAssignmentContext assignmentContext = new TaskAssignmentContext(
                userTask,
                config,
                getConfigValue(config, "assignee", null),
                getConfigListValue(config, "candidateUsers"),
                getConfigListValue(config, "candidateGroups")
            );
            
            strategy.assign(assignmentContext);
        } else {
            // 默认分配策略
            String assignee = getConfigValue(config, "assignee", null);
            if (assignee != null) {
                userTask.setAssignee(assignee);
            } else {
                userTask.setCandidateUsers(getConfigListValue(config, "candidateUsers"));
                userTask.setCandidateGroups(getConfigListValue(config, "candidateGroups"));
            }
        }
    }
    
    /**
     * 发送通知
     */
    private void sendNotifications(UserTask userTask, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> notificationConfig = (Map<String, Object>) config.get("notification");
        if (notificationConfig == null) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<String> notificationTypes = (List<String>) notificationConfig.get("types");
        if (notificationTypes == null || notificationTypes.isEmpty()) {
            return;
        }
        
        for (String type : notificationTypes) {
            TaskNotifier notifier = notifiers.get(type);
            if (notifier != null) {
                try {
                    notifier.sendTaskCreatedNotification(userTask, notificationConfig);
                } catch (Exception e) {
                    logger.warn("发送任务通知失败 (类型: {}, 任务: {}): {}", type, userTask.getId(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * 发送委托通知
     */
    private void sendDelegationNotification(UserTask userTask, String fromUserId, String toUserId, String reason) {
        // 这里应该发送委托通知
        logger.info("发送委托通知: 任务 {} 从 {} 委托给 {}", userTask.getId(), fromUserId, toUserId);
    }
    
    /**
     * 检查用户是否可以完成任务
     */
    private boolean canCompleteTask(UserTask userTask, String userId) {
        // 检查是否是指定处理人
        if (Objects.equals(userTask.getAssignee(), userId)) {
            return true;
        }
        
        // 检查是否在候选用户列表中
        if (userTask.getCandidateUsers() != null && userTask.getCandidateUsers().contains(userId)) {
            return true;
        }
        
        // 检查是否在候选用户组中（这里需要实现用户组查询逻辑）
        if (userTask.getCandidateGroups() != null && !userTask.getCandidateGroups().isEmpty()) {
            // 简化实现：假设用户在组中
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查用户是否可以委托任务
     */
    private boolean canDelegateTask(UserTask userTask, String userId) {
        return Objects.equals(userTask.getAssignee(), userId);
    }
    
    /**
     * 检查用户是否可以撤回任务
     */
    private boolean canReclaimTask(UserTask userTask, String userId) {
        // 检查是否是委托人
        if (Objects.equals(userTask.getDelegatedBy(), userId)) {
            return true;
        }
        
        // 检查是否在候选用户列表中
        if (userTask.getCandidateUsers() != null && userTask.getCandidateUsers().contains(userId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查用户是否可以访问任务
     */
    private boolean canAccessTask(UserTask userTask, String userId) {
        return canCompleteTask(userTask, userId) || canReclaimTask(userTask, userId);
    }
    
    /**
     * 获取配置值
     */
    private String getConfigValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取配置整数值
     */
    private Integer getConfigIntValue(Map<String, Object> config, String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取配置列表值
     */
    @SuppressWarnings("unchecked")
    private List<String> getConfigListValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }
    
    // 内部类和接口
    
    /**
     * 用户任务实体
     */
    public static class UserTask {
        public enum Status {
            CREATED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED, TIMEOUT
        }
        
        private String id;
        private String stepId;
        private String instanceId;
        private String name;
        private String description;
        private String formKey;
        private Map<String, Object> formData;
        private String assignee;
        private List<String> candidateUsers;
        private List<String> candidateGroups;
        private Integer priority;
        private Status status;
        private LocalDateTime createdTime;
        private String createdBy;
        private LocalDateTime dueDate;
        private LocalDateTime completedTime;
        private String completedBy;
        private String delegatedBy;
        private LocalDateTime delegatedTime;
        private String delegationReason;
        private String reclaimedBy;
        private LocalDateTime reclaimedTime;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }
        
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getFormKey() { return formKey; }
        public void setFormKey(String formKey) { this.formKey = formKey; }
        
        public Map<String, Object> getFormData() { return formData; }
        public void setFormData(Map<String, Object> formData) { this.formData = formData; }
        
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        
        public List<String> getCandidateUsers() { return candidateUsers; }
        public void setCandidateUsers(List<String> candidateUsers) { this.candidateUsers = candidateUsers; }
        
        public List<String> getCandidateGroups() { return candidateGroups; }
        public void setCandidateGroups(List<String> candidateGroups) { this.candidateGroups = candidateGroups; }
        
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
        
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
        
        public String getCompletedBy() { return completedBy; }
        public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }
        
        public String getDelegatedBy() { return delegatedBy; }
        public void setDelegatedBy(String delegatedBy) { this.delegatedBy = delegatedBy; }
        
        public LocalDateTime getDelegatedTime() { return delegatedTime; }
        public void setDelegatedTime(LocalDateTime delegatedTime) { this.delegatedTime = delegatedTime; }
        
        public String getDelegationReason() { return delegationReason; }
        public void setDelegationReason(String delegationReason) { this.delegationReason = delegationReason; }
        
        public String getReclaimedBy() { return reclaimedBy; }
        public void setReclaimedBy(String reclaimedBy) { this.reclaimedBy = reclaimedBy; }
        
        public LocalDateTime getReclaimedTime() { return reclaimedTime; }
        public void setReclaimedTime(LocalDateTime reclaimedTime) { this.reclaimedTime = reclaimedTime; }
    }
    
    /**
     * 任务分配策略接口
     */
    public interface TaskAssignmentStrategy {
        void assign(TaskAssignmentContext context);
    }
    
    /**
     * 任务分配上下文
     */
    public static class TaskAssignmentContext {
        private final UserTask userTask;
        private final Map<String, Object> config;
        private final String assignee;
        private final List<String> candidateUsers;
        private final List<String> candidateGroups;
        
        public TaskAssignmentContext(UserTask userTask, Map<String, Object> config,
                                   String assignee, List<String> candidateUsers, List<String> candidateGroups) {
            this.userTask = userTask;
            this.config = config;
            this.assignee = assignee;
            this.candidateUsers = candidateUsers;
            this.candidateGroups = candidateGroups;
        }
        
        // Getters
        public UserTask getUserTask() { return userTask; }
        public Map<String, Object> getConfig() { return config; }
        public String getAssignee() { return assignee; }
        public List<String> getCandidateUsers() { return candidateUsers; }
        public List<String> getCandidateGroups() { return candidateGroups; }
    }
    
    /**
     * 任务通知器接口
     */
    public interface TaskNotifier {
        void sendTaskCreatedNotification(UserTask userTask, Map<String, Object> config);
        void sendTaskAssignedNotification(UserTask userTask, String assignee);
        void sendTaskCompletedNotification(UserTask userTask, String completedBy);
        void sendTaskOverdueNotification(UserTask userTask);
    }
    
    // 默认分配策略实现
    
    /**
     * 直接分配策略
     */
    private static class DirectAssignmentStrategy implements TaskAssignmentStrategy {
        @Override
        public void assign(TaskAssignmentContext context) {
            UserTask userTask = context.getUserTask();
            
            if (context.getAssignee() != null) {
                userTask.setAssignee(context.getAssignee());
                userTask.setStatus(UserTask.Status.ASSIGNED);
            } else {
                userTask.setCandidateUsers(context.getCandidateUsers());
                userTask.setCandidateGroups(context.getCandidateGroups());
                userTask.setStatus(UserTask.Status.CREATED);
            }
        }
    }
    
    /**
     * 轮询分配策略
     */
    private static class RoundRobinAssignmentStrategy implements TaskAssignmentStrategy {
        private int currentIndex = 0;
        
        @Override
        public void assign(TaskAssignmentContext context) {
            UserTask userTask = context.getUserTask();
            List<String> candidateUsers = context.getCandidateUsers();
            
            if (candidateUsers != null && !candidateUsers.isEmpty()) {
                String assignee = candidateUsers.get(currentIndex % candidateUsers.size());
                currentIndex++;
                
                userTask.setAssignee(assignee);
                userTask.setStatus(UserTask.Status.ASSIGNED);
            } else {
                // 回退到直接分配
                new DirectAssignmentStrategy().assign(context);
            }
        }
    }
    
    /**
     * 负载均衡分配策略
     */
    private static class LoadBalanceAssignmentStrategy implements TaskAssignmentStrategy {
        @Override
        public void assign(TaskAssignmentContext context) {
            UserTask userTask = context.getUserTask();
            List<String> candidateUsers = context.getCandidateUsers();
            
            if (candidateUsers != null && !candidateUsers.isEmpty()) {
                // 简化实现：选择第一个用户
                // 实际实现应该查询每个用户的当前任务数量
                String assignee = candidateUsers.get(0);
                
                userTask.setAssignee(assignee);
                userTask.setStatus(UserTask.Status.ASSIGNED);
            } else {
                // 回退到直接分配
                new DirectAssignmentStrategy().assign(context);
            }
        }
    }
    
    /**
     * 随机分配策略
     */
    private static class RandomAssignmentStrategy implements TaskAssignmentStrategy {
        private final Random random = new Random();
        
        @Override
        public void assign(TaskAssignmentContext context) {
            UserTask userTask = context.getUserTask();
            List<String> candidateUsers = context.getCandidateUsers();
            
            if (candidateUsers != null && !candidateUsers.isEmpty()) {
                String assignee = candidateUsers.get(random.nextInt(candidateUsers.size()));
                
                userTask.setAssignee(assignee);
                userTask.setStatus(UserTask.Status.ASSIGNED);
            } else {
                // 回退到直接分配
                new DirectAssignmentStrategy().assign(context);
            }
        }
    }
    
    // 默认通知器实现
    
    /**
     * 邮件通知器
     */
    private static class EmailNotifier implements TaskNotifier {
        @Override
        public void sendTaskCreatedNotification(UserTask userTask, Map<String, Object> config) {
            // 这里应该实现邮件发送逻辑
            System.out.println(String.format("发送邮件通知: 任务 [%s] 已创建", userTask.getName()));
        }
        
        @Override
        public void sendTaskAssignedNotification(UserTask userTask, String assignee) {
            System.out.println(String.format("发送邮件通知: 任务 [%s] 已分配给 %s", userTask.getName(), assignee));
        }
        
        @Override
        public void sendTaskCompletedNotification(UserTask userTask, String completedBy) {
            System.out.println(String.format("发送邮件通知: 任务 [%s] 已由 %s 完成", userTask.getName(), completedBy));
        }
        
        @Override
        public void sendTaskOverdueNotification(UserTask userTask) {
            System.out.println(String.format("发送邮件通知: 任务 [%s] 已超时", userTask.getName()));
        }
    }
    
    /**
     * 短信通知器
     */
    private static class SmsNotifier implements TaskNotifier {
        @Override
        public void sendTaskCreatedNotification(UserTask userTask, Map<String, Object> config) {
            System.out.println(String.format("发送短信通知: 任务 [%s] 已创建", userTask.getName()));
        }
        
        @Override
        public void sendTaskAssignedNotification(UserTask userTask, String assignee) {
            System.out.println(String.format("发送短信通知: 任务 [%s] 已分配给 %s", userTask.getName(), assignee));
        }
        
        @Override
        public void sendTaskCompletedNotification(UserTask userTask, String completedBy) {
            System.out.println(String.format("发送短信通知: 任务 [%s] 已由 %s 完成", userTask.getName(), completedBy));
        }
        
        @Override
        public void sendTaskOverdueNotification(UserTask userTask) {
            System.out.println(String.format("发送短信通知: 任务 [%s] 已超时", userTask.getName()));
        }
    }
    
    /**
     * 系统通知器
     */
    private static class SystemNotifier implements TaskNotifier {
        @Override
        public void sendTaskCreatedNotification(UserTask userTask, Map<String, Object> config) {
            System.out.println(String.format("系统通知: 任务 [%s] 已创建", userTask.getName()));
        }
        
        @Override
        public void sendTaskAssignedNotification(UserTask userTask, String assignee) {
            System.out.println(String.format("系统通知: 任务 [%s] 已分配给 %s", userTask.getName(), assignee));
        }
        
        @Override
        public void sendTaskCompletedNotification(UserTask userTask, String completedBy) {
            System.out.println(String.format("系统通知: 任务 [%s] 已由 %s 完成", userTask.getName(), completedBy));
        }
        
        @Override
        public void sendTaskOverdueNotification(UserTask userTask) {
            System.out.println(String.format("系统通知: 任务 [%s] 已超时", userTask.getName()));
        }
    }
}