package com.tao.workflow.model;

/**
 * 工作流状态枚举
 * 
 * 定义了工作流在整个生命周期中的各种状态，用于状态管理和流程控制。
 * 每个状态都有明确的含义和转换规则。
 * 
 * 状态转换流程：
 * DRAFT -> ACTIVE -> (SUSPENDED) -> COMPLETED/TERMINATED
 * 
 * @author Tao
 * @version 1.0
 */
public enum WorkflowStatus {
    
    /**
     * 草稿状态
     * 
     * 工作流刚创建，还在设计阶段，未正式启用。
     * 在此状态下可以自由修改工作流定义。
     */
    DRAFT("草稿", "工作流处于设计阶段，可以修改定义"),
    
    /**
     * 激活状态
     * 
     * 工作流已经发布并可以创建实例执行。
     * 在此状态下不建议修改工作流定义，以保证执行的一致性。
     */
    ACTIVE("激活", "工作流已发布，可以创建实例执行"),
    
    /**
     * 暂停状态
     * 
     * 工作流临时暂停，不能创建新的实例，但已有实例可以继续执行。
     * 通常用于维护或临时调整。
     */
    SUSPENDED("暂停", "工作流已暂停，不能创建新实例"),
    
    /**
     * 已完成状态
     * 
     * 工作流已经完成其生命周期，不再接受新的实例。
     * 所有相关的实例都已处理完毕。
     */
    COMPLETED("已完成", "工作流生命周期结束，所有实例已处理完毕"),
    
    /**
     * 已终止状态
     * 
     * 工作流被强制终止，可能由于错误或业务需要。
     * 所有正在执行的实例也会被终止。
     */
    TERMINATED("已终止", "工作流被强制终止，所有实例停止执行");
    
    /** 状态显示名称 */
    private final String displayName;
    
    /** 状态描述 */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param displayName 状态显示名称
     * @param description 状态描述
     */
    WorkflowStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
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
     * 检查是否可以创建新的工作流实例
     * 
     * 只有在ACTIVE状态下才能创建新的实例
     * 
     * @return 如果可以创建实例返回true，否则返回false
     */
    public boolean canCreateInstance() {
        return this == ACTIVE;
    }
    
    /**
     * 检查是否可以修改工作流定义
     * 
     * 只有在DRAFT状态下才能修改定义
     * 
     * @return 如果可以修改返回true，否则返回false
     */
    public boolean canModifyDefinition() {
        return this == DRAFT;
    }
    
    /**
     * 检查是否为终态
     * 
     * 终态包括COMPLETED和TERMINATED
     * 
     * @return 如果是终态返回true，否则返回false
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == TERMINATED;
    }
    
    /**
     * 检查是否可以转换到目标状态
     * 
     * 定义状态转换规则：
     * - DRAFT可以转换到ACTIVE
     * - ACTIVE可以转换到SUSPENDED、COMPLETED、TERMINATED
     * - SUSPENDED可以转换到ACTIVE、TERMINATED
     * - 终态不能转换到其他状态
     * 
     * @param targetStatus 目标状态
     * @return 如果可以转换返回true，否则返回false
     */
    public boolean canTransitionTo(WorkflowStatus targetStatus) {
        // 终态不能转换
        if (this.isFinalState()) {
            return false;
        }
        
        // 不能转换到自己
        if (this == targetStatus) {
            return false;
        }
        
        switch (this) {
            case DRAFT:
                // 草稿只能激活
                return targetStatus == ACTIVE;
                
            case ACTIVE:
                // 激活状态可以暂停、完成或终止
                return targetStatus == SUSPENDED || 
                       targetStatus == COMPLETED || 
                       targetStatus == TERMINATED;
                       
            case SUSPENDED:
                // 暂停状态可以重新激活或终止
                return targetStatus == ACTIVE || 
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
    public WorkflowStatus[] getTransitionableStates() {
        switch (this) {
            case DRAFT:
                return new WorkflowStatus[]{ACTIVE};
                
            case ACTIVE:
                return new WorkflowStatus[]{SUSPENDED, COMPLETED, TERMINATED};
                
            case SUSPENDED:
                return new WorkflowStatus[]{ACTIVE, TERMINATED};
                
            default:
                return new WorkflowStatus[0];
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", displayName, name());
    }
}