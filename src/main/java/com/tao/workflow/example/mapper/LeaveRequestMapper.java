package com.tao.workflow.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tao.workflow.example.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 请假申请Mapper接口
 * 提供请假申请的数据库操作方法
 * 
 * @author tao
 * @since 2024-01-15
 */
@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    /**
     * 根据申请人ID查询请假申请
     * 
     * @param applicantId 申请人ID
     * @return 请假申请列表
     */
    List<LeaveRequest> selectByApplicantId(@Param("applicantId") String applicantId);

    /**
     * 根据审批状态查询请假申请
     * 
     * @param status 审批状态
     * @return 请假申请列表
     */
    List<LeaveRequest> selectByStatus(@Param("status") String status);

    /**
     * 根据请假类型查询请假申请
     * 
     * @param leaveType 请假类型
     * @return 请假申请列表
     */
    List<LeaveRequest> selectByLeaveType(@Param("leaveType") String leaveType);

    /**
     * 查询指定日期范围内的请假申请
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 请假申请列表
     */
    List<LeaveRequest> selectByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /**
     * 查询指定用户的待审批请假申请
     * 
     * @param userId 用户ID
     * @return 待审批请假申请列表
     */
    List<LeaveRequest> selectPendingApprovalsByUser(@Param("userId") String userId);

    /**
     * 查询指定部门的请假申请
     * 
     * @param department 部门名称
     * @return 请假申请列表
     */
    List<LeaveRequest> selectByDepartment(@Param("department") String department);

    /**
     * 分页查询请假申请
     * 
     * @param page 分页参数
     * @param applicantId 申请人ID（可选）
     * @param applicantName 申请人姓名（可选，模糊查询）
     * @param department 部门（可选）
     * @param leaveType 请假类型（可选）
     * @param status 审批状态（可选）
     * @param startTime 申请开始时间（可选）
     * @param endTime 申请结束时间（可选）
     * @return 分页结果
     */
    IPage<LeaveRequest> selectLeaveRequestsWithConditions(
            Page<LeaveRequest> page,
            @Param("applicantId") String applicantId,
            @Param("applicantName") String applicantName,
            @Param("department") String department,
            @Param("leaveType") String leaveType,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计各状态的请假申请数量
     * 
     * @return 状态统计结果，key为状态，value为数量
     */
    List<Map<String, Object>> countLeaveRequestsByStatus();

    /**
     * 统计各请假类型的申请数量
     * 
     * @return 请假类型统计结果
     */
    List<Map<String, Object>> countLeaveRequestsByType();

    /**
     * 统计各部门的请假申请数量
     * 
     * @return 部门统计结果
     */
    List<Map<String, Object>> countLeaveRequestsByDepartment();

    /**
     * 查询指定申请人的请假申请数量
     * 
     * @param applicantId 申请人ID
     * @return 申请数量
     */
    Integer countLeaveRequestsByApplicant(@Param("applicantId") String applicantId);

    /**
     * 查询最近提交的请假申请
     * 
     * @param limit 限制数量
     * @return 最近提交的请假申请列表
     */
    List<LeaveRequest> selectRecentLeaveRequests(@Param("limit") Integer limit);

    /**
     * 查询长期请假申请（超过指定天数）
     * 
     * @param days 天数阈值
     * @return 长期请假申请列表
     */
    List<LeaveRequest> selectLongTermLeaveRequests(@Param("days") Integer days);

    /**
     * 查询紧急请假申请
     * 
     * @return 紧急请假申请列表
     */
    List<LeaveRequest> selectEmergencyLeaveRequests();

    /**
     * 批量更新请假申请状态
     * 
     * @param requestIds 请假申请ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    Integer batchUpdateStatus(@Param("requestIds") List<String> requestIds, 
                             @Param("status") String status);

    /**
     * 更新请假申请的审批信息
     * 
     * @param requestId 请假申请ID
     * @param approverType 审批人类型（DIRECT_MANAGER, DEPARTMENT_MANAGER, HR, GENERAL_MANAGER）
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approvalTime 审批时间
     * @param result 审批结果
     * @return 更新的记录数
     */
    Integer updateApprovalInfo(@Param("requestId") String requestId,
                              @Param("approverType") String approverType,
                              @Param("approverId") String approverId,
                              @Param("approverName") String approverName,
                              @Param("comment") String comment,
                              @Param("approvalTime") LocalDateTime approvalTime,
                              @Param("result") String result);

    /**
     * 更新请假申请的最终结果
     * 
     * @param requestId 请假申请ID
     * @param finalResult 最终结果
     * @param rejectionReason 拒绝原因（可选）
     * @return 更新的记录数
     */
    Integer updateFinalResult(@Param("requestId") String requestId,
                             @Param("finalResult") String finalResult,
                             @Param("rejectionReason") String rejectionReason);

    /**
     * 根据关键词搜索请假申请
     * 
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索结果列表
     */
    List<LeaveRequest> searchLeaveRequests(@Param("keyword") String keyword,
                                          @Param("limit") Integer limit);

    /**
     * 查询指定时间范围内的请假申请统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> getLeaveRequestStats(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查询请假申请的审批流程信息
     * 
     * @param requestId 请假申请ID
     * @return 审批流程信息
     */
    Map<String, Object> getApprovalProcessInfo(@Param("requestId") String requestId);

    /**
     * 查询用户的请假余额信息
     * 
     * @param applicantId 申请人ID
     * @param year 年份
     * @return 请假余额信息
     */
    Map<String, Object> getUserLeaveBalance(@Param("applicantId") String applicantId,
                                           @Param("year") Integer year);

    /**
     * 查询需要提醒的请假申请
     * 查询即将开始但未审批完成的请假申请
     * 
     * @param reminderDays 提醒提前天数
     * @return 需要提醒的请假申请列表
     */
    List<LeaveRequest> selectLeaveRequestsForReminder(@Param("reminderDays") Integer reminderDays);

    /**
     * 查询过期的请假申请
     * 查询已过期但仍在审批中的请假申请
     * 
     * @return 过期的请假申请列表
     */
    List<LeaveRequest> selectExpiredLeaveRequests();

    /**
     * 查询需要清理的历史请假申请
     * 
     * @param days 保留天数
     * @return 需要清理的请假申请列表
     */
    List<LeaveRequest> selectLeaveRequestsForCleanup(@Param("days") Integer days);

    /**
     * 查询请假申请的详细信息（包含所有审批信息）
     * 
     * @param requestId 请假申请ID
     * @return 请假申请详细信息
     */
    LeaveRequest selectLeaveRequestWithDetails(@Param("requestId") String requestId);

    /**
     * 查询请假申请的基本信息（不包含审批详情）
     * 
     * @param requestId 请假申请ID
     * @return 请假申请基本信息
     */
    LeaveRequest selectLeaveRequestBasicInfo(@Param("requestId") String requestId);

    /**
     * 检查请假日期是否有冲突
     * 
     * @param applicantId 申请人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param excludeRequestId 排除的请假申请ID（用于更新时排除自己）
     * @return 冲突的请假申请数量
     */
    Integer checkDateConflict(@Param("applicantId") String applicantId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("excludeRequestId") String excludeRequestId);

    /**
     * 查询指定日期范围内的请假申请趋势
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请假申请趋势统计
     */
    List<Map<String, Object>> getLeaveRequestTrend(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询审批人的工作负载统计
     * 
     * @return 审批人工作负载统计
     */
    List<Map<String, Object>> getApproverWorkloadStats();
}