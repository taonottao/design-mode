package com.tao.workflow.model;

/**
 * 工作流步骤类型枚举
 * 
 * 定义了工作流中不同类型的步骤，每种类型有不同的执行特性和用途。
 * 通过类型化的步骤设计，可以更好地组织和管理复杂的工作流程。
 * 
 * @author Tao
 * @version 1.0
 */
public enum StepType {
    
    /**
     * 任务步骤
     * 
     * 最常见的步骤类型，执行具体的业务逻辑。
     * 特点：
     * - 需要指定具体的执行器类
     * - 可以配置超时和重试
     * - 支持前置条件判断
     * - 执行结果影响流程走向
     */
    TASK("任务", "执行具体业务逻辑的步骤", true, true),
    
    /**
     * 条件判断步骤
     * 
     * 用于流程分支控制，根据条件决定下一步的执行路径。
     * 特点：
     * - 不执行具体业务逻辑
     * - 主要用于条件判断
     * - 根据判断结果选择不同的后续步骤
     * - 通常执行时间很短
     */
    CONDITION("条件判断", "根据条件控制流程分支的步骤", false, false),
    
    /**
     * 并行网关步骤
     * 
     * 用于启动并行执行的多个分支。
     * 特点：
     * - 将流程分解为多个并行分支
     * - 不执行具体业务逻辑
     * - 后续步骤可以并行执行
     * - 需要配合汇聚网关使用
     */
    PARALLEL_GATEWAY("并行网关", "启动并行分支的步骤", false, false),
    
    /**
     * 汇聚网关步骤
     * 
     * 用于等待并行分支完成并汇聚结果。
     * 特点：
     * - 等待所有并行分支完成
     * - 汇聚并行分支的执行结果
     * - 不执行具体业务逻辑
     * - 继续后续的串行执行
     */
    MERGE_GATEWAY("汇聚网关", "汇聚并行分支的步骤", false, false),
    
    /**
     * 用户任务步骤
     * 
     * 需要人工干预的步骤，等待用户操作。
     * 特点：
     * - 需要人工处理
     * - 可能长时间等待
     * - 支持任务分配和提醒
     * - 可以设置处理期限
     */
    USER_TASK("用户任务", "需要人工处理的步骤", true, false),
    
    /**
     * 服务调用步骤
     * 
     * 调用外部服务或API的步骤。
     * 特点：
     * - 调用外部系统
     * - 需要处理网络异常
     * - 支持重试机制
     * - 可能需要熔断保护
     */
    SERVICE_CALL("服务调用", "调用外部服务的步骤", true, true),
    
    /**
     * 脚本执行步骤
     * 
     * 执行脚本代码的步骤。
     * 特点：
     * - 执行动态脚本
     * - 支持多种脚本语言
     * - 灵活性高
     * - 需要注意安全性
     */
    SCRIPT("脚本执行", "执行脚本代码的步骤", true, true),
    
    /**
     * 邮件发送步骤
     * 
     * 专门用于发送邮件通知的步骤。
     * 特点：
     * - 发送邮件通知
     * - 支持模板和附件
     * - 可以批量发送
     * - 支持发送状态跟踪
     */
    EMAIL("邮件发送", "发送邮件通知的步骤", true, true),
    
    /**
     * 定时器步骤
     * 
     * 用于流程中的等待和延时。
     * 特点：
     * - 按时间延迟执行
     * - 可以设置具体时间点
     * - 支持周期性触发
     * - 不消耗系统资源
     */
    TIMER("定时器", "定时等待的步骤", false, false),
    
    /**
     * 开始步骤
     * 
     * 工作流的起始步骤。
     * 特点：
     * - 每个工作流的第一个步骤
     * - 初始化流程上下文
     * - 不执行具体业务逻辑
     * - 设置初始参数
     */
    START("开始", "工作流的起始步骤", false, false),
    
    /**
     * 结束步骤
     * 
     * 工作流的终止步骤。
     * 特点：
     * - 工作流的最后步骤
     * - 清理资源和上下文
     * - 记录执行结果
     * - 触发完成事件
     */
    END("结束", "工作流的终止步骤", false, false);
    
    /** 类型显示名称 */
    private final String displayName;
    
    /** 类型描述 */
    private final String description;
    
    /** 是否需要执行器 */
    private final boolean requiresExecutor;
    
    /** 是否支持重试 */
    private final boolean supportsRetry;
    
    /**
     * 构造函数
     * 
     * @param displayName 类型显示名称
     * @param description 类型描述
     * @param requiresExecutor 是否需要执行器
     * @param supportsRetry 是否支持重试
     */
    StepType(String displayName, String description, boolean requiresExecutor, boolean supportsRetry) {
        this.displayName = displayName;
        this.description = description;
        this.requiresExecutor = requiresExecutor;
        this.supportsRetry = supportsRetry;
    }
    
    /**
     * 获取类型显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取类型描述
     * @return 类型描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查是否需要执行器
     * 
     * 某些步骤类型（如条件判断、网关）不需要具体的执行器，
     * 而任务类型步骤则必须指定执行器。
     * 
     * @return 如果需要执行器返回true，否则返回false
     */
    public boolean requiresExecutor() {
        return requiresExecutor;
    }
    
    /**
     * 检查是否支持重试
     * 
     * 某些步骤类型（如网关、条件判断）不支持重试，
     * 而业务任务类型步骤通常支持重试机制。
     * 
     * @return 如果支持重试返回true，否则返回false
     */
    public boolean supportsRetry() {
        return supportsRetry;
    }
    
    /**
     * 检查是否为网关类型
     * 
     * 网关类型步骤用于流程控制，不执行具体业务逻辑。
     * 
     * @return 如果是网关类型返回true，否则返回false
     */
    public boolean isGateway() {
        return this == PARALLEL_GATEWAY || 
               this == MERGE_GATEWAY || 
               this == CONDITION;
    }
    
    /**
     * 检查是否为业务任务类型
     * 
     * 业务任务类型步骤执行具体的业务逻辑。
     * 
     * @return 如果是业务任务类型返回true，否则返回false
     */
    public boolean isBusinessTask() {
        return this == TASK || 
               this == USER_TASK || 
               this == SERVICE_CALL || 
               this == SCRIPT || 
               this == EMAIL;
    }
    
    /**
     * 检查是否为控制类型
     * 
     * 控制类型步骤用于流程控制，包括开始、结束、定时器等。
     * 
     * @return 如果是控制类型返回true，否则返回false
     */
    public boolean isControlType() {
        return this == START || 
               this == END || 
               this == TIMER;
    }
    
    /**
     * 检查是否需要人工干预
     * 
     * 某些步骤类型需要人工处理，不能自动完成。
     * 
     * @return 如果需要人工干预返回true，否则返回false
     */
    public boolean requiresHumanIntervention() {
        return this == USER_TASK;
    }
    
    /**
     * 获取推荐的超时时间（秒）
     * 
     * 根据步骤类型返回推荐的超时时间设置。
     * 
     * @return 推荐的超时时间（秒）
     */
    public long getRecommendedTimeout() {
        switch (this) {
            case TASK:
            case SCRIPT:
                return 300; // 5分钟
                
            case SERVICE_CALL:
                return 60; // 1分钟
                
            case EMAIL:
                return 30; // 30秒
                
            case USER_TASK:
                return 86400; // 24小时
                
            case CONDITION:
            case PARALLEL_GATEWAY:
            case MERGE_GATEWAY:
            case START:
            case END:
                return 10; // 10秒
                
            case TIMER:
                return Long.MAX_VALUE; // 无限制
                
            default:
                return 300; // 默认5分钟
        }
    }
    
    /**
     * 获取推荐的重试次数
     * 
     * 根据步骤类型返回推荐的重试次数设置。
     * 
     * @return 推荐的重试次数
     */
    public int getRecommendedRetryCount() {
        switch (this) {
            case SERVICE_CALL:
                return 3; // 服务调用重试3次
                
            case EMAIL:
                return 2; // 邮件发送重试2次
                
            case TASK:
            case SCRIPT:
                return 1; // 任务和脚本重试1次
                
            default:
                return 0; // 其他类型不重试
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", displayName, name());
    }
}