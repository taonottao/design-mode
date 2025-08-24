package com.tao.workflow.model;

/**
 * 工作流实例状态枚举
 * 
 * 定义了工作流实例在执行过程中的各种状态，用于跟踪实例的生命周期。
 * 每个状态都有明确的含义和转换规则。
 * 
 * 状态转换流程：
 * CREATED -> RUNNING -> (SUSPENDED) -> COMPLETED/FAILED/TERMINATED
 * 
 * @author Tao
 * @version 1.0
 */
public enum InstanceStatus {
    
    /**
     * 已创建状态
     * 
     * 实例刚创建，还未开始执行。
     * 在此状态下可以修改实例配置和初始参数。
     */
    CREATED("已创建", "实例已创建，等待开始执行", false),
    
    /**
     * 运行中状态
     * 
     * 实例正在执行中，当前有步骤在处理。
     * 这是实例的主要工作状态。
     */
    RUNNING("运行中", "实例正在执行中", false),
    
    /**
     * 等待状态
     * 
     * 实例在等待外部事件或用户操作。
     * 通常出现在用户任务或外部服务调用时。
     */
    WAITING("等待中", "实例等待外部事件或用户操作", false),
    
    /**
     * 暂停状态
     * 
     * 实例被手动暂停，可以恢复执行。
     * 暂停期间不会执行任何步骤。
     */
    SUSPENDED("已暂停", "实例被暂停，可以恢复执行", false),
    
    /**
     * 已完成状态
     * 
     * 实例成功执行完所有步骤。
     * 这是正常的终止状态。
     */
    COMPLETED("已完成", "实例成功执行完成", true),
    
    /**
     * 执行失败状态
     * 
     * 实例在执行过程中遇到无法恢复的错误。
     * 需要人工干预或重新启动。
     */
    FAILED("执行失败", "实例执行失败，需要人工处理", true),
    
    /**
     * 已终止状态
     * 
     * 实例被手动终止或因系统原因强制停止。
     * 不同于失败，这通常是主动的操作。
     */
    TERMINATED("已终止", "实例被手动终止或强制停止", true),
    
    /**
     * 已取消状态
     * 
     * 实例在执行前或执行过程中被取消。
     * 通常由业务需求变更导致。
     */
    CANCELLED("已取消", "实例被取消，不再执行", true);
    
    /** 状态显示名称 */
    private final String displayName;
    
    /** 状态描述 */
    private final String description;
    
    /** 是否为终态 */
    private final boolean finalState;
    
    /**
     * 构造函数
     * 
     * @param displayName 状态显示名称
     * @param description 状态描述
     * @param finalState 是否为终态
     */
    InstanceStatus(String displayName, String description, boolean finalState) {
        this.displayName = displayName;
        this.description = description;
        this.finalState = finalState;
    }
    
    /**
     * 获取状态显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取状态描述
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查是否为终态
     * 
     * 终态表示实例已经结束，不会再有状态变更。
     * 
     * @return 如果是终态返回true，否则返回false
     */
    public boolean isFinalState() {
        return finalState;
    }
    
    /**
     * 检查是否为成功状态
     * 
     * @return 如果是成功状态返回true，否则返回false
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * 检查是否为失败状态
     * 
     * @return 如果是失败状态返回true，否则返回false
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * 检查是否为活跃状态
     * 
     * 活跃状态表示实例正在执行或可能继续执行。
     * 
     * @return 如果是活跃状态返回true，否则返回false
     */
    public boolean isActive() {
        return this == RUNNING || this == WAITING;
    }
    
    /**
     * 检查是否可以暂停
     * 
     * 只有运行中和等待中的实例可以暂停。
     * 
     * @return 如果可以暂停返回true，否则返回false
     */
    public boolean canSuspend() {
        return this == RUNNING || this == WAITING;
    }
    
    /**
     * 检查是否可以恢复
     * 
     * 只有暂停状态的实例可以恢复。
     * 
     * @return 如果可以恢复返回true，否则返回false
     */
    public boolean canResume() {
        return this == SUSPENDED;
    }
    
    /**
     * 检查是否可以终止
     * 
     * 非终态的实例都可以被终止。
     * 
     * @return 如果可以终止返回true，否则返回false
     */
    public boolean canTerminate() {
        return !finalState;
    }
    
    /**
     * 检查是否可以重启
     * 
     * 失败和终止的实例可以重启。
     * 
     * @return 如果可以重启返回true，否则返回false
     */
    public boolean canRestart() {
        return this == FAILED || this == TERMINATED;
    }
    
    /**
     * 检查是否可以转换到目标状态
     * 
     * 定义状态转换规则：
     * - CREATED可以转换到RUNNING、CANCELLED
     * - RUNNING可以转换到WAITING、SUSPENDED、COMPLETED、FAILED、TERMINATED
     * - WAITING可以转换到RUNNING、SUSPENDED、FAILED、TERMINATED
     * - SUSPENDED可以转换到RUNNING、TERMINATED
     * - 终态不能转换到其他状态
     * 
     * @param targetStatus 目标状态
     * @return 如果可以转换返回true，否则返回false
     */
    public boolean canTransitionTo(InstanceStatus targetStatus) {
        // 终态不能转换
        if (this.finalState) {
            return false;
        }
        
        // 不能转换到自己
        if (this == targetStatus) {
            return false;
        }
        
        switch (this) {
            case CREATED:
                // 创建状态可以开始运行或取消
                return targetStatus == RUNNING || targetStatus == CANCELLED;
                
            case RUNNING:
                // 运行状态可以转换到任何其他状态
                return targetStatus == WAITING || 
                       targetStatus == SUSPENDED || 
                       targetStatus == COMPLETED || 
                       targetStatus == FAILED || 
                       targetStatus == TERMINATED;
                       
            case WAITING:
                // 等待状态可以继续运行、暂停或结束
                return targetStatus == RUNNING || 
                       targetStatus == SUSPENDED || 
                       targetStatus == FAILED || 
                       targetStatus == TERMINATED;
                       
            case SUSPENDED:
                // 暂停状态可以恢复运行或终止
                return targetStatus == RUNNING || 
                       targetStatus == TERMINATED;
                       
            default:
                return false;
        }
    }
    
    /**
     * 获取所有可转换的目标状态
     * 
     * @return 可转换的状态数组
     */
    public InstanceStatus[] getTransitionableStates() {
        switch (this) {
            case CREATED:
                return new InstanceStatus[]{RUNNING, CANCELLED};
                
            case RUNNING:
                return new InstanceStatus[]{WAITING, SUSPENDED, COMPLETED, FAILED, TERMINATED};
                
            case WAITING:
                return new InstanceStatus[]{RUNNING, SUSPENDED, FAILED, TERMINATED};
                
            case SUSPENDED:
                return new InstanceStatus[]{RUNNING, TERMINATED};
                
            default:
                return new InstanceStatus[0];
        }
    }
    
    /**
     * 获取状态的优先级
     * 
     * 用于状态排序和显示，数值越大优先级越高。
     * 
     * @return 状态优先级
     */
    public int getPriority() {
        switch (this) {
            case FAILED:
                return 100; // 失败状态优先级最高
            case RUNNING:
                return 90;  // 运行状态次之
            case WAITING:
                return 80;  // 等待状态
            case SUSPENDED:
                return 70;  // 暂停状态
            case CREATED:
                return 60;  // 创建状态
            case TERMINATED:
                return 50;  // 终止状态
            case CANCELLED:
                return 40;  // 取消状态
            case COMPLETED:
                return 30;  // 完成状态优先级最低
            default:
                return 0;
        }
    }
    
    /**
     * 检查状态是否需要人工干预
     * 
     * @return 如果需要人工干预返回true，否则返回false
     */
    public boolean requiresIntervention() {
        return this == FAILED || this == SUSPENDED;
    }
    
    /**
     * 获取状态对应的颜色代码（用于UI显示）
     * 
     * @return 颜色代码
     */
    public String getColorCode() {
        switch (this) {
            case COMPLETED:
                return "#28a745"; // 绿色
            case RUNNING:
                return "#007bff"; // 蓝色
            case WAITING:
                return "#ffc107"; // 黄色
            case SUSPENDED:
                return "#6c757d"; // 灰色
            case FAILED:
                return "#dc3545"; // 红色
            case TERMINATED:
                return "#fd7e14"; // 橙色
            case CANCELLED:
                return "#6f42c1"; // 紫色
            case CREATED:
                return "#20c997"; // 青色
            default:
                return "#000000"; // 黑色
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", displayName, name());
    }
}