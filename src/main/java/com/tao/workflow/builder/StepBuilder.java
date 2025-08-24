package com.tao.workflow.builder;

import com.tao.workflow.model.StepType;
import com.tao.workflow.model.WorkflowStep;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 工作流步骤构建器
 * 
 * 这是Builder模式在步骤级别的实现，专门用于构建单个工作流步骤。
 * 通过链式调用和函数式接口，提供了灵活且类型安全的步骤构建方式。
 * 
 * 主要解决的问题：
 * 1. 复杂步骤配置的构建复杂性
 * 2. 参数验证和默认值设置
 * 3. 类型安全和编译时检查
 * 4. 代码可读性和维护性
 * 5. 配置的灵活性和扩展性
 * 
 * 使用示例：
 * <pre>
 * WorkflowStep step = new StepBuilder()
 *     .name("用户审批")
 *     .description("等待用户审批申请")
 *     .type(StepType.USER_TASK)
 *     .order(1)
 *     .executor("com.example.UserApprovalExecutor")
 *     .timeout(3600)
 *     .retryCount(3)
 *     .config("assignee", "manager")
 *     .config("priority", "high")
 *     .precondition(ctx -> ctx.get("amount") != null)
 *     .onSuccess("next_step")
 *     .onError("error_handler")
 *     .build();
 * </pre>
 * 
 * @author Tao
 * @version 1.0
 */
public class StepBuilder {
    
    /** 步骤ID */
    private String id;
    
    /** 步骤名称 */
    private String name;
    
    /** 步骤描述 */
    private String description;
    
    /** 步骤类型 */
    private StepType type;
    
    /** 步骤顺序 */
    private Integer order;
    
    /** 执行器类名 */
    private String executorClass;
    
    /** 步骤配置 */
    private final Map<String, Object> configuration = new HashMap<>();
    
    /** 前置条件表达式 */
    private String precondition;
    
    /** 下一步骤ID */
    private String nextStepId;
    
    /** 错误处理步骤ID */
    private String errorStepId;
    
    /** 是否可选步骤 */
    private Boolean optional = false;
    
    /** 超时时间（秒） */
    private Integer timeout;
    
    /** 重试次数 */
    private Integer retryCount;
    
    /**
     * 默认构造函数
     * 
     * 初始化构建器并设置默认值。
     */
    public StepBuilder() {
        // 设置默认值
        this.optional = false;
        this.retryCount = 0;
    }
    
    /**
     * 设置步骤ID
     * 
     * @param id 步骤唯一标识
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * 设置步骤名称
     * 
     * @param name 步骤名称
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置步骤描述
     * 
     * @param description 步骤描述信息
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * 设置步骤类型
     * 
     * 步骤类型决定了步骤的执行方式和所需的配置参数。
     * 
     * @param type 步骤类型
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder type(StepType type) {
        this.type = type;
        
        // 根据步骤类型设置默认配置
        setDefaultConfigurationForType(type);
        
        return this;
    }
    
    /**
     * 设置步骤顺序
     * 
     * @param order 步骤在工作流中的执行顺序
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder order(int order) {
        this.order = order;
        return this;
    }
    
    /**
     * 设置执行器类名
     * 
     * 执行器是实际处理步骤逻辑的类，必须实现相应的接口。
     * 
     * @param executorClass 执行器类的完全限定名
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder executor(String executorClass) {
        this.executorClass = executorClass;
        return this;
    }
    
    /**
     * 设置执行器类
     * 
     * 类型安全的执行器设置方法。
     * 
     * @param executorClass 执行器类
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder executor(Class<?> executorClass) {
        this.executorClass = executorClass.getName();
        return this;
    }
    
    /**
     * 添加配置项
     * 
     * 配置项用于传递步骤执行所需的参数。
     * 
     * @param key 配置键
     * @param value 配置值
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder config(String key, Object value) {
        this.configuration.put(key, value);
        return this;
    }
    
    /**
     * 批量添加配置项
     * 
     * @param configs 配置映射
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder configs(Map<String, Object> configs) {
        this.configuration.putAll(configs);
        return this;
    }
    
    /**
     * 使用函数式接口配置步骤
     * 
     * 这种方式允许更复杂的配置逻辑，提高了灵活性。
     * 
     * @param configurer 配置函数
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder configure(Consumer<Map<String, Object>> configurer) {
        configurer.accept(this.configuration);
        return this;
    }
    
    /**
     * 设置前置条件
     * 
     * 前置条件用于判断步骤是否应该执行。
     * 
     * @param condition 条件表达式字符串
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder precondition(String condition) {
        this.precondition = condition;
        return this;
    }
    
    /**
     * 使用函数式接口设置前置条件
     * 
     * 这种方式提供了类型安全的条件设置。
     * 
     * @param condition 条件判断函数
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder precondition(Predicate<Map<String, Object>> condition) {
        // 将函数式条件转换为表达式字符串
        // 在实际实现中，可以使用表达式引擎如SpEL或自定义解析器
        this.precondition = condition.toString();
        return this;
    }
    
    /**
     * 设置下一步骤ID
     * 
     * 指定步骤成功执行后的下一个步骤。
     * 
     * @param nextStepId 下一步骤的ID
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder nextStepId(String nextStepId) {
        this.nextStepId = nextStepId;
        return this;
    }
    
    /**
     * 设置成功后的下一步骤
     * 
     * 这是nextStepId的别名方法，提供更直观的API。
     * 
     * @param nextStepId 下一步骤的ID
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder onSuccess(String nextStepId) {
        return nextStepId(nextStepId);
    }
    
    /**
     * 设置错误处理步骤ID
     * 
     * 指定步骤执行失败时的错误处理步骤。
     * 
     * @param errorStepId 错误处理步骤的ID
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder errorStepId(String errorStepId) {
        this.errorStepId = errorStepId;
        return this;
    }
    
    /**
     * 设置错误处理步骤
     * 
     * 这是errorStepId的别名方法，提供更直观的API。
     * 
     * @param errorStepId 错误处理步骤的ID
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder onError(String errorStepId) {
        return errorStepId(errorStepId);
    }
    
    /**
     * 设置步骤为可选
     * 
     * 可选步骤在执行失败时不会导致整个工作流失败。
     * 
     * @param optional 是否为可选步骤
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder optional(boolean optional) {
        this.optional = optional;
        return this;
    }
    
    /**
     * 设置步骤为可选
     * 
     * 便捷方法，默认设置为可选。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder optional() {
        return optional(true);
    }
    
    /**
     * 设置步骤为必需
     * 
     * 必需步骤在执行失败时会导致整个工作流失败。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder required() {
        return optional(false);
    }
    
    /**
     * 设置超时时间
     * 
     * @param timeout 超时时间（秒）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * 设置超时时间（分钟）
     * 
     * 便捷方法，自动转换为秒。
     * 
     * @param minutes 超时时间（分钟）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder timeoutMinutes(int minutes) {
        return timeout(minutes * 60);
    }
    
    /**
     * 设置超时时间（小时）
     * 
     * 便捷方法，自动转换为秒。
     * 
     * @param hours 超时时间（小时）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder timeoutHours(int hours) {
        return timeout(hours * 3600);
    }
    
    /**
     * 设置重试次数
     * 
     * @param retryCount 重试次数
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }
    
    /**
     * 设置不重试
     * 
     * 便捷方法，设置重试次数为0。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder noRetry() {
        return retryCount(0);
    }
    
    /**
     * 设置任务分配人
     * 
     * 用于用户任务类型的步骤。
     * 
     * @param assignee 任务分配人
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder assignee(String assignee) {
        return config("assignee", assignee);
    }
    
    /**
     * 设置任务候选人组
     * 
     * 用于用户任务类型的步骤。
     * 
     * @param candidateGroups 候选人组列表
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder candidateGroups(String... candidateGroups) {
        return config("candidateGroups", candidateGroups);
    }
    
    /**
     * 设置任务优先级
     * 
     * @param priority 优先级（1-10，数字越大优先级越高）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder priority(int priority) {
        return config("priority", priority);
    }
    
    /**
     * 设置高优先级
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder highPriority() {
        return priority(8);
    }
    
    /**
     * 设置普通优先级
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder normalPriority() {
        return priority(5);
    }
    
    /**
     * 设置低优先级
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder lowPriority() {
        return priority(2);
    }
    
    /**
     * 设置服务URL
     * 
     * 用于服务调用类型的步骤。
     * 
     * @param url 服务URL
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder serviceUrl(String url) {
        return config("serviceUrl", url);
    }
    
    /**
     * 设置HTTP方法
     * 
     * 用于服务调用类型的步骤。
     * 
     * @param method HTTP方法（GET、POST、PUT、DELETE等）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder httpMethod(String method) {
        return config("httpMethod", method.toUpperCase());
    }
    
    /**
     * 设置请求体
     * 
     * 用于服务调用类型的步骤。
     * 
     * @param requestBody 请求体内容
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder requestBody(String requestBody) {
        return config("requestBody", requestBody);
    }
    
    /**
     * 设置请求头
     * 
     * 用于服务调用类型的步骤。
     * 
     * @param headers 请求头映射
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder headers(Map<String, String> headers) {
        return config("headers", headers);
    }
    
    /**
     * 添加单个请求头
     * 
     * @param name 请求头名称
     * @param value 请求头值
     * @return 当前构建器实例，支持链式调用
     */
    @SuppressWarnings("unchecked")
    public StepBuilder header(String name, String value) {
        Map<String, String> headers = (Map<String, String>) configuration.get("headers");
        if (headers == null) {
            headers = new HashMap<>();
            configuration.put("headers", headers);
        }
        headers.put(name, value);
        return this;
    }
    
    /**
     * 设置脚本内容
     * 
     * 用于脚本执行类型的步骤。
     * 
     * @param script 脚本内容
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder script(String script) {
        return config("script", script);
    }
    
    /**
     * 设置脚本类型
     * 
     * 用于脚本执行类型的步骤。
     * 
     * @param scriptType 脚本类型（如：javascript、groovy、python等）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder scriptType(String scriptType) {
        return config("scriptType", scriptType);
    }
    
    /**
     * 设置邮件收件人
     * 
     * 用于邮件发送类型的步骤。
     * 
     * @param to 收件人邮箱地址
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder emailTo(String to) {
        return config("emailTo", to);
    }
    
    /**
     * 设置邮件主题
     * 
     * 用于邮件发送类型的步骤。
     * 
     * @param subject 邮件主题
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder emailSubject(String subject) {
        return config("emailSubject", subject);
    }
    
    /**
     * 设置邮件模板
     * 
     * 用于邮件发送类型的步骤。
     * 
     * @param template 邮件模板名称或内容
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder emailTemplate(String template) {
        return config("emailTemplate", template);
    }
    
    /**
     * 设置等待时长
     * 
     * 用于定时器类型的步骤。
     * 
     * @param duration 等待时长（秒）
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder waitDuration(int duration) {
        return config("waitDuration", duration);
    }
    
    /**
     * 根据步骤类型设置默认配置
     * 
     * 这个方法根据不同的步骤类型设置相应的默认配置，
     * 减少用户的配置工作量，提高易用性。
     * 
     * @param type 步骤类型
     */
    private void setDefaultConfigurationForType(StepType type) {
        switch (type) {
            case USER_TASK:
                // 用户任务默认配置
                if (timeout == null) {
                    timeout = 24 * 3600; // 默认24小时超时
                }
                config("taskType", "user");
                break;
                
            case SERVICE_CALL:
                // 服务调用默认配置
                if (timeout == null) {
                    timeout = 30; // 默认30秒超时
                }
                if (retryCount == null) {
                    retryCount = 3; // 默认重试3次
                }
                config("httpMethod", "POST");
                break;
                
            case SCRIPT:
                // 脚本执行默认配置
                if (timeout == null) {
                    timeout = 300; // 默认5分钟超时
                }
                config("scriptType", "javascript");
                break;
                
            case EMAIL:
                // 邮件发送默认配置
                if (timeout == null) {
                    timeout = 60; // 默认1分钟超时
                }
                if (retryCount == null) {
                    retryCount = 2; // 默认重试2次
                }
                break;
                
            case TIMER:
                // 定时器默认配置
                config("waitDuration", 60); // 默认等待1分钟
                break;
                
            case CONDITION:
                // 条件判断默认配置
                if (timeout == null) {
                    timeout = 10; // 默认10秒超时
                }
                break;
                
            case PARALLEL_GATEWAY:
                // 并行网关默认配置
                config("joinType", "AND"); // 默认AND连接
                break;
                
            default:
                // 其他类型的默认配置
                if (timeout == null) {
                    timeout = 60; // 默认1分钟超时
                }
                break;
        }
    }
    
    /**
     * 验证步骤配置的完整性
     * 
     * 在构建步骤之前进行验证，确保配置的正确性。
     * 
     * @throws IllegalStateException 如果步骤配置不完整或不正确
     */
    private void validate() {
        // 验证必需字段
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("步骤名称不能为空");
        }
        
        if (type == null) {
            throw new IllegalStateException("步骤类型不能为空");
        }
        
        if (order == null) {
            throw new IllegalStateException("步骤顺序不能为空");
        }
        
        // 验证类型特定的配置
        validateTypeSpecificConfiguration();
        
        // 验证超时时间
        if (timeout != null && timeout <= 0) {
            throw new IllegalStateException("超时时间必须大于0");
        }
        
        // 验证重试次数
        if (retryCount != null && retryCount < 0) {
            throw new IllegalStateException("重试次数不能为负数");
        }
    }
    
    /**
     * 验证特定类型的配置
     * 
     * 根据步骤类型验证相应的必需配置。
     */
    private void validateTypeSpecificConfiguration() {
        switch (type) {
            case TASK:
            case USER_TASK:
            case SERVICE_CALL:
            case SCRIPT:
                // 这些类型需要执行器
                if (executorClass == null || executorClass.trim().isEmpty()) {
                    throw new IllegalStateException(type.getDisplayName() + "类型的步骤必须指定执行器");
                }
                break;
                
            case SERVICE_CALL:
                // 服务调用需要URL
                if (!configuration.containsKey("serviceUrl")) {
                    throw new IllegalStateException("服务调用步骤必须指定服务URL");
                }
                break;
                
            case SCRIPT:
                // 脚本执行需要脚本内容
                if (!configuration.containsKey("script")) {
                    throw new IllegalStateException("脚本执行步骤必须指定脚本内容");
                }
                break;
                
            case EMAIL:
                // 邮件发送需要收件人和主题
                if (!configuration.containsKey("emailTo")) {
                    throw new IllegalStateException("邮件发送步骤必须指定收件人");
                }
                if (!configuration.containsKey("emailSubject")) {
                    throw new IllegalStateException("邮件发送步骤必须指定邮件主题");
                }
                break;
                
            case TIMER:
                // 定时器需要等待时长
                if (!configuration.containsKey("waitDuration")) {
                    throw new IllegalStateException("定时器步骤必须指定等待时长");
                }
                break;
                
            default:
                // 其他类型暂不验证
                break;
        }
    }
    
    /**
     * 构建工作流步骤实例
     * 
     * 这是Builder模式的终点，返回构建完成的不可变步骤对象。
     * 在构建前会进行完整性验证，确保步骤配置的正确性。
     * 
     * @return 构建完成的工作流步骤实例
     * @throws IllegalStateException 如果步骤配置不完整或不正确
     */
    public WorkflowStep build() {
        // 验证步骤配置
        validate();
        
        // 使用WorkflowStep的Builder构建最终对象
        return WorkflowStep.builder()
            .id(id)
            .name(name)
            .description(description)
            .type(type)
            .order(order)
            .executorClass(executorClass)
            .configuration(new HashMap<>(configuration)) // 创建副本确保不可变性
            .precondition(precondition)
            .nextStepId(nextStepId)
            .errorStepId(errorStepId)
            .optional(optional)
            .timeout(timeout)
            .retryCount(retryCount)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 克隆当前构建器
     * 
     * 创建一个当前构建器的副本，用于基于现有配置创建新的步骤。
     * 
     * @return 新的构建器实例
     */
    public StepBuilder clone() {
        StepBuilder cloned = new StepBuilder();
        cloned.id = this.id;
        cloned.name = this.name;
        cloned.description = this.description;
        cloned.type = this.type;
        cloned.order = this.order;
        cloned.executorClass = this.executorClass;
        cloned.configuration.putAll(this.configuration);
        cloned.precondition = this.precondition;
        cloned.nextStepId = this.nextStepId;
        cloned.errorStepId = this.errorStepId;
        cloned.optional = this.optional;
        cloned.timeout = this.timeout;
        cloned.retryCount = this.retryCount;
        
        return cloned;
    }
    
    /**
     * 重置构建器
     * 
     * 清空所有配置，恢复到初始状态。
     * 
     * @return 当前构建器实例，支持链式调用
     */
    public StepBuilder reset() {
        this.id = null;
        this.name = null;
        this.description = null;
        this.type = null;
        this.order = null;
        this.executorClass = null;
        this.configuration.clear();
        this.precondition = null;
        this.nextStepId = null;
        this.errorStepId = null;
        this.optional = false;
        this.timeout = null;
        this.retryCount = 0;
        
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("StepBuilder{name='%s', type=%s, order=%d}", 
                           name, type, order);
    }
}