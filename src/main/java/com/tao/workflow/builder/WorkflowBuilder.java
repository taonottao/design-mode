package com.tao.workflow.builder;

import com.tao.workflow.model.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 工作流构建器
 * 
 * 这是Builder模式的核心实现，用于构建复杂的工作流定义。
 * 通过链式调用和函数式接口，提供了灵活且易读的工作流构建方式。
 * 
 * 主要解决的问题：
 * 1. 复杂工作流定义的构建复杂性
 * 2. 动态步骤配置和条件分支
 * 3. 并行处理和网关配置
 * 4. 参数验证和默认值设置
 * 5. 代码可读性和维护性
 * 
 * 使用示例：
 * <pre>
 * Workflow workflow = WorkflowBuilder.create()
 *     .name("请假审批流程")
 *     .description("员工请假审批工作流")
 *     .version("1.0")
 *     .addStep(step -> step
 *         .name("提交申请")
 *         .type(StepType.USER_TASK)
 *         .order(1)
 *         .executor("com.example.SubmitLeaveRequestExecutor")
 *         .timeout(3600)
 *     )
 *     .addConditionalStep("审批决策", 2, condition -> condition
 *         .when(ctx -> ctx.get("amount") > 1000)
 *         .thenGoto("manager_approval")
 *         .otherwise("auto_approval")
 *     )
 *     .addParallelSteps("并行处理", 3, parallel -> parallel
 *         .addBranch("hr_review", "人事审核")
 *         .addBranch("finance_check", "财务检查")
 *         .joinType("AND")
 *     )
 *     .build();
 * </pre>
 * 
 * @author Tao
 * @version 1.0
 */
public class WorkflowBuilder {
    
    /** 工作流ID */
    private String id;
    
    /** 工作流名称 */
    private String name;
    
    /** 工作流描述 */
    private String description;
    
    /** 工作流版本 */
    private String version;
    
    /** 工作流步骤列表 */
    private final List<WorkflowStep> steps = new ArrayList<>();
    
    /** 工作流配置 */
    private final Map<String, Object> configuration = new HashMap<>();
    
    /** 工作流状态 */
    private WorkflowStatus status = WorkflowStatus.DRAFT;
    
    /** 步骤ID计数器，用于自动生成步骤ID */
    private int stepIdCounter = 1;
    
    /** 步骤名称到ID的映射，用于快速查找 */
    private final Map<String, String> stepNameToIdMap = new HashMap<>();
    
    /**
     * 私有构造函数，防止直接实例化
     * 必须通过静态工厂方法create()创建实例
     */
    private WorkflowBuilder() {
        // 设置默认值
        this.id = "workflow_" + System.currentTimeMillis();
        this.version = "1.0";
        this.status = WorkflowStatus.DRAFT;
    }
    
    /**
     * 创建工作流构建器实例
     * 
     * 这是Builder模式的入口点，提供了清晰的构建起始点。
     * 
     * @return 新的工作流构建器实例
     */
    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }
    
    /**
     * 创建带有指定名称的工作流构建器
     * 
     * 这是一个便捷方法，允许在创建时直接指定工作流名称。
     * 
     * @param name 工作流名称
     * @return 工作流构建器实例
     */
    public static WorkflowBuilder create(String name) {
        return new WorkflowBuilder().name(name);
    }
    
    /**
     * 设置工作流ID
     * 
     * @param id 工作流唯一标识
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * 设置工作流名称
     * 
     * @param name 工作流名称
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置工作流描述
     * 
     * @param description 工作流描述信息
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * 设置工作流版本
     * 
     * @param version 版本号
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder version(String version) {
        this.version = version;
        return this;
    }
    
    /**
     * 设置工作流状态
     * 
     * @param status 工作流状态
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder status(WorkflowStatus status) {
        this.status = status;
        return this;
    }
    
    /**
     * 添加配置项
     * 
     * 支持动态配置工作流的各种参数，如超时时间、重试次数等。
     * 
     * @param key 配置键
     * @param value 配置值
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder config(String key, Object value) {
        this.configuration.put(key, value);
        return this;
    }
    
    /**
     * 批量添加配置项
     * 
     * @param configs 配置映射
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder configs(Map<String, Object> configs) {
        this.configuration.putAll(configs);
        return this;
    }
    
    /**
     * 使用函数式接口配置工作流
     * 
     * 这种方式允许更复杂的配置逻辑，提高了灵活性。
     * 
     * @param configurer 配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder configure(Consumer<Map<String, Object>> configurer) {
        configurer.accept(this.configuration);
        return this;
    }
    
    /**
     * 添加工作流步骤
     * 
     * 使用函数式接口配置步骤，提供了灵活的步骤定义方式。
     * 这是Builder模式的核心特性，通过回调函数简化复杂对象的构建。
     * 
     * @param stepConfigurer 步骤配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addStep(Consumer<StepBuilder> stepConfigurer) {
        StepBuilder stepBuilder = new StepBuilder();
        
        // 自动设置步骤ID和顺序
        stepBuilder.id(generateStepId())
                   .order(steps.size() + 1);
        
        // 执行用户配置
        stepConfigurer.accept(stepBuilder);
        
        // 构建步骤并添加到列表
        WorkflowStep step = stepBuilder.build();
        steps.add(step);
        
        // 维护步骤名称到ID的映射
        if (step.getName() != null) {
            stepNameToIdMap.put(step.getName(), step.getId());
        }
        
        return this;
    }
    
    /**
     * 添加指定名称和类型的步骤
     * 
     * 这是一个便捷方法，用于快速添加简单步骤。
     * 
     * @param name 步骤名称
     * @param type 步骤类型
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addStep(String name, StepType type) {
        return addStep(step -> step.name(name).type(type));
    }
    
    /**
     * 添加指定名称、类型和执行器的步骤
     * 
     * @param name 步骤名称
     * @param type 步骤类型
     * @param executorClass 执行器类名
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addStep(String name, StepType type, String executorClass) {
        return addStep(step -> step
            .name(name)
            .type(type)
            .executor(executorClass));
    }
    
    /**
     * 添加条件分支步骤
     * 
     * 条件步骤是工作流中的重要组成部分，用于实现业务逻辑的分支处理。
     * 通过Builder模式，我们可以用声明式的方式定义复杂的条件逻辑。
     * 
     * @param name 条件步骤名称
     * @param order 步骤顺序
     * @param conditionConfigurer 条件配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addConditionalStep(String name, int order, 
                                             Consumer<ConditionalStepBuilder> conditionConfigurer) {
        return addStep(step -> {
            step.name(name)
                .type(StepType.CONDITION)
                .order(order);
            
            // 创建条件构建器并配置
            ConditionalStepBuilder conditionBuilder = new ConditionalStepBuilder();
            conditionConfigurer.accept(conditionBuilder);
            
            // 将条件配置添加到步骤配置中
            step.config("conditions", conditionBuilder.build());
        });
    }
    
    /**
     * 添加并行处理步骤
     * 
     * 并行步骤允许同时执行多个分支，提高工作流的执行效率。
     * 
     * @param name 并行步骤名称
     * @param order 步骤顺序
     * @param parallelConfigurer 并行配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addParallelSteps(String name, int order,
                                           Consumer<ParallelStepBuilder> parallelConfigurer) {
        return addStep(step -> {
            step.name(name)
                .type(StepType.PARALLEL_GATEWAY)
                .order(order);
            
            // 创建并行构建器并配置
            ParallelStepBuilder parallelBuilder = new ParallelStepBuilder();
            parallelConfigurer.accept(parallelBuilder);
            
            // 将并行配置添加到步骤配置中
            step.config("parallel", parallelBuilder.build());
        });
    }
    
    /**
     * 添加用户任务步骤
     * 
     * 用户任务是需要人工干预的步骤，通常用于审批、确认等场景。
     * 
     * @param name 任务名称
     * @param assignee 任务分配人
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addUserTask(String name, String assignee) {
        return addStep(step -> step
            .name(name)
            .type(StepType.USER_TASK)
            .config("assignee", assignee)
            .timeout(24 * 3600) // 默认24小时超时
        );
    }
    
    /**
     * 添加服务调用步骤
     * 
     * 服务调用步骤用于调用外部服务或API。
     * 
     * @param name 步骤名称
     * @param serviceUrl 服务URL
     * @param method HTTP方法
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addServiceCall(String name, String serviceUrl, String method) {
        return addStep(step -> step
            .name(name)
            .type(StepType.SERVICE_CALL)
            .config("serviceUrl", serviceUrl)
            .config("method", method)
            .retryCount(3) // 默认重试3次
            .timeout(30) // 默认30秒超时
        );
    }
    
    /**
     * 添加脚本执行步骤
     * 
     * 脚本步骤允许执行自定义的脚本逻辑。
     * 
     * @param name 步骤名称
     * @param scriptType 脚本类型（如：javascript、groovy等）
     * @param script 脚本内容
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addScript(String name, String scriptType, String script) {
        return addStep(step -> step
            .name(name)
            .type(StepType.SCRIPT)
            .config("scriptType", scriptType)
            .config("script", script)
        );
    }
    
    /**
     * 添加邮件发送步骤
     * 
     * @param name 步骤名称
     * @param to 收件人
     * @param subject 邮件主题
     * @param template 邮件模板
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addEmail(String name, String to, String subject, String template) {
        return addStep(step -> step
            .name(name)
            .type(StepType.EMAIL)
            .config("to", to)
            .config("subject", subject)
            .config("template", template)
        );
    }
    
    /**
     * 添加定时器步骤
     * 
     * 定时器步骤用于在工作流中添加延迟或等待。
     * 
     * @param name 步骤名称
     * @param duration 等待时长（秒）
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder addTimer(String name, int duration) {
        return addStep(step -> step
            .name(name)
            .type(StepType.TIMER)
            .config("duration", duration)
        );
    }
    
    /**
     * 设置步骤之间的连接关系
     * 
     * 这个方法用于在构建完成后设置步骤间的流转关系。
     * 
     * @param fromStepName 源步骤名称
     * @param toStepName 目标步骤名称
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder connect(String fromStepName, String toStepName) {
        String fromStepId = stepNameToIdMap.get(fromStepName);
        String toStepId = stepNameToIdMap.get(toStepName);
        
        if (fromStepId == null || toStepId == null) {
            throw new IllegalArgumentException("步骤名称不存在: " + fromStepName + " -> " + toStepName);
        }
        
        // 找到源步骤并设置下一步
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            if (step.getId().equals(fromStepId)) {
                // 重新构建步骤以更新nextStepId
                WorkflowStep updatedStep = step.toBuilder()
                    .nextStepId(toStepId)
                    .build();
                steps.set(i, updatedStep);
                break;
            }
        }
        
        return this;
    }
    
    /**
     * 设置错误处理连接
     * 
     * @param fromStepName 源步骤名称
     * @param errorStepName 错误处理步骤名称
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder onError(String fromStepName, String errorStepName) {
        String fromStepId = stepNameToIdMap.get(fromStepName);
        String errorStepId = stepNameToIdMap.get(errorStepName);
        
        if (fromStepId == null || errorStepId == null) {
            throw new IllegalArgumentException("步骤名称不存在: " + fromStepName + " -> " + errorStepName);
        }
        
        // 找到源步骤并设置错误处理步骤
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            if (step.getId().equals(fromStepId)) {
                WorkflowStep updatedStep = step.toBuilder()
                    .errorStepId(errorStepId)
                    .build();
                steps.set(i, updatedStep);
                break;
            }
        }
        
        return this;
    }
    
    /**
     * 验证工作流定义的完整性
     * 
     * 在构建工作流之前进行验证，确保定义的正确性。
     * 
     * @throws IllegalStateException 如果工作流定义不完整或不正确
     */
    private void validate() {
        // 验证基本信息
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("工作流名称不能为空");
        }
        
        if (steps.isEmpty()) {
            throw new IllegalStateException("工作流必须包含至少一个步骤");
        }
        
        // 验证步骤顺序的唯一性
        Set<Integer> orders = new HashSet<>();
        for (WorkflowStep step : steps) {
            if (!orders.add(step.getOrder())) {
                throw new IllegalStateException("步骤顺序重复: " + step.getOrder());
            }
        }
        
        // 验证步骤ID的唯一性
        Set<String> stepIds = new HashSet<>();
        for (WorkflowStep step : steps) {
            if (!stepIds.add(step.getId())) {
                throw new IllegalStateException("步骤ID重复: " + step.getId());
            }
        }
        
        // 验证步骤引用的完整性
        for (WorkflowStep step : steps) {
            if (step.getNextStepId() != null && !stepIds.contains(step.getNextStepId())) {
                throw new IllegalStateException("步骤引用了不存在的下一步: " + step.getNextStepId());
            }
            if (step.getErrorStepId() != null && !stepIds.contains(step.getErrorStepId())) {
                throw new IllegalStateException("步骤引用了不存在的错误处理步骤: " + step.getErrorStepId());
            }
        }
        
        // 验证是否有开始和结束步骤
        boolean hasStart = steps.stream().anyMatch(step -> step.getType() == StepType.START);
        boolean hasEnd = steps.stream().anyMatch(step -> step.getType() == StepType.END);
        
        if (!hasStart) {
            // 自动添加开始步骤
            addStep(step -> step
                .name("开始")
                .type(StepType.START)
                .order(0)
            );
        }
        
        if (!hasEnd) {
            // 自动添加结束步骤
            addStep(step -> step
                .name("结束")
                .type(StepType.END)
                .order(steps.size() + 1)
            );
        }
    }
    
    /**
     * 生成步骤ID
     * 
     * @return 唯一的步骤ID
     */
    private String generateStepId() {
        return "step_" + (stepIdCounter++);
    }
    
    /**
     * 构建工作流实例
     * 
     * 这是Builder模式的终点，返回构建完成的不可变工作流对象。
     * 在构建前会进行完整性验证，确保工作流定义的正确性。
     * 
     * @return 构建完成的工作流实例
     * @throws IllegalStateException 如果工作流定义不完整或不正确
     */
    public Workflow build() {
        // 验证工作流定义
        validate();
        
        // 按顺序排序步骤
        steps.sort(Comparator.comparing(WorkflowStep::getOrder));
        
        // 使用Workflow的Builder构建最终对象
        return Workflow.builder()
            .id(id)
            .name(name)
            .description(description)
            .version(version)
            .steps(new ArrayList<>(steps)) // 创建副本确保不可变性
            .configuration(new HashMap<>(configuration)) // 创建副本确保不可变性
            .status(status)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 获取当前已添加的步骤数量
     * 
     * @return 步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
    
    /**
     * 获取指定名称的步骤
     * 
     * @param stepName 步骤名称
     * @return 步骤对象，如果不存在返回null
     */
    public WorkflowStep getStep(String stepName) {
        String stepId = stepNameToIdMap.get(stepName);
        if (stepId == null) {
            return null;
        }
        
        return steps.stream()
            .filter(step -> step.getId().equals(stepId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 检查是否包含指定名称的步骤
     * 
     * @param stepName 步骤名称
     * @return 如果包含返回true，否则返回false
     */
    public boolean hasStep(String stepName) {
        return stepNameToIdMap.containsKey(stepName);
    }
    
    /**
     * 获取所有步骤名称
     * 
     * @return 步骤名称集合
     */
    public Set<String> getStepNames() {
        return new HashSet<>(stepNameToIdMap.keySet());
    }
    
    /**
     * 清空所有步骤
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public WorkflowBuilder clearSteps() {
        steps.clear();
        stepNameToIdMap.clear();
        stepIdCounter = 1;
        return this;
    }
    
    /**
     * 克隆当前构建器
     * 
     * 创建一个当前构建器的副本，用于基于现有定义创建新的工作流。
     * 
     * @return 新的构建器实例
     */
    public WorkflowBuilder clone() {
        WorkflowBuilder cloned = new WorkflowBuilder();
        cloned.id = this.id + "_copy";
        cloned.name = this.name + "_副本";
        cloned.description = this.description;
        cloned.version = this.version;
        cloned.status = this.status;
        cloned.configuration.putAll(this.configuration);
        
        // 复制步骤
        for (WorkflowStep step : this.steps) {
            cloned.steps.add(step);
            if (step.getName() != null) {
                cloned.stepNameToIdMap.put(step.getName(), step.getId());
            }
        }
        
        cloned.stepIdCounter = this.stepIdCounter;
        
        return cloned;
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowBuilder{name='%s', version='%s', stepCount=%d}", 
                           name, version, steps.size());
    }
}