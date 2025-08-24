package com.tao.builder;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @version 2.0
 * @Author T-WANG
 * @Date 2025/8/23 21:35
 * 优化后的邮件构建器 - 增强版
 * 
 * 相比于Email 1.0版本，主要改进：
 * 1. 引入阶段性接口设计，提供更好的类型安全和构建流程控制
 * 2. 支持多收件人、多抄送、多密送功能
 * 3. 增加邮件格式类型支持（HTML/纯文本）
 * 4. 增强的验证机制（邮箱格式验证、附件大小限制等）
 * 5. 支持邮件模板和变量替换
 * 6. 添加邮件发送时间调度功能
 * 7. 提供静态工厂方法和链式调用优化
 * 8. 增加邮件重要性级别和读取回执功能
 */
public class EmailV2 {
    
    // ==================== 邮件属性 ====================
    
    /**
     * 发件人邮箱
     */
    private final String from;
    
    /**
     * 发件人姓名
     */
    private final String fromName;
    
    /**
     * 收件人列表
     */
    private final List<String> toList;
    
    /**
     * 抄送列表
     */
    private final List<String> ccList;
    
    /**
     * 密送列表
     */
    private final List<String> bccList;
    
    /**
     * 邮件主题
     */
    private final String subject;
    
    /**
     * 邮件内容
     */
    private final String content;
    
    /**
     * 邮件格式类型
     */
    private final ContentType contentType;
    
    /**
     * 附件列表
     */
    private final List<File> attachments;
    
    /**
     * 邮件优先级
     */
    private final Priority priority;
    
    /**
     * 是否需要读取回执
     */
    private final boolean readReceipt;
    
    /**
     * 计划发送时间
     */
    private final LocalDateTime scheduledTime;
    
    /**
     * 邮件模板变量
     */
    private final Map<String, String> templateVariables;
    
    // ==================== 构造函数 ====================
    
    /**
     * 私有构造函数，只能通过Builder创建
     */
    private EmailV2(Builder builder) {
        this.from = builder.from;
        this.fromName = builder.fromName;
        this.toList = Collections.unmodifiableList(new ArrayList<>(builder.toList));
        this.ccList = Collections.unmodifiableList(new ArrayList<>(builder.ccList));
        this.bccList = Collections.unmodifiableList(new ArrayList<>(builder.bccList));
        this.subject = builder.subject;
        this.content = processTemplate(builder.content, builder.templateVariables);
        this.contentType = builder.contentType;
        this.attachments = Collections.unmodifiableList(new ArrayList<>(builder.attachments));
        this.priority = builder.priority;
        this.readReceipt = builder.readReceipt;
        this.scheduledTime = builder.scheduledTime;
        this.templateVariables = Collections.unmodifiableMap(new HashMap<>(builder.templateVariables));
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建邮件构建器
     * @return 邮件构建器的发件人阶段
     */
    public static FromStage newEmail() {
        return new Builder();
    }
    
    /**
     * 创建简单邮件构建器（快速构建）
     * @param from 发件人
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     * @return 构建完成的邮件对象
     */
    public static EmailV2 simpleEmail(String from, String to, String subject, String content) {
        return newEmail()
                .from(from)
                .to(to)
                .subject(subject)
                .content(content)
                .build();
    }
    
    // ==================== 阶段性接口定义 ====================
    
    /**
     * 发件人设置阶段
     */
    public interface FromStage {
        /**
         * 设置发件人邮箱
         * @param email 发件人邮箱地址
         * @return 收件人设置阶段
         */
        ToStage from(String email);
        
        /**
         * 设置发件人邮箱和姓名
         * @param email 发件人邮箱地址
         * @param name 发件人姓名
         * @return 收件人设置阶段
         */
        ToStage from(String email, String name);
    }
    
    /**
     * 收件人设置阶段
     */
    public interface ToStage {
        /**
         * 添加收件人
         * @param email 收件人邮箱
         * @return 主题设置阶段
         */
        SubjectStage to(String email);
        
        /**
         * 添加多个收件人
         * @param emails 收件人邮箱列表
         * @return 主题设置阶段
         */
        SubjectStage to(String... emails);
        
        /**
         * 添加收件人列表
         * @param emails 收件人邮箱列表
         * @return 主题设置阶段
         */
        SubjectStage to(List<String> emails);
    }
    
    /**
     * 主题设置阶段
     */
    public interface SubjectStage {
        /**
         * 设置邮件主题
         * @param subject 邮件主题
         * @return 内容设置阶段
         */
        ContentStage subject(String subject);
    }
    
    /**
     * 内容设置阶段
     */
    public interface ContentStage {
        /**
         * 设置纯文本内容
         * @param content 邮件内容
         * @return 可选设置阶段
         */
        OptionalStage content(String content);
        
        /**
         * 设置HTML内容
         * @param htmlContent HTML格式的邮件内容
         * @return 可选设置阶段
         */
        OptionalStage htmlContent(String htmlContent);
        
        /**
         * 使用模板内容
         * @param template 模板内容
         * @param variables 模板变量
         * @return 可选设置阶段
         */
        OptionalStage templateContent(String template, Map<String, String> variables);
    }
    
    /**
     * 可选设置阶段
     */
    public interface OptionalStage extends BuildStage {
        /**
         * 添加抄送
         * @param email 抄送邮箱
         * @return 可选设置阶段
         */
        OptionalStage cc(String email);
        
        /**
         * 添加多个抄送
         * @param emails 抄送邮箱列表
         * @return 可选设置阶段
         */
        OptionalStage cc(String... emails);
        
        /**
         * 添加密送
         * @param email 密送邮箱
         * @return 可选设置阶段
         */
        OptionalStage bcc(String email);
        
        /**
         * 添加多个密送
         * @param emails 密送邮箱列表
         * @return 可选设置阶段
         */
        OptionalStage bcc(String... emails);
        
        /**
         * 添加附件
         * @param file 附件文件
         * @return 可选设置阶段
         */
        OptionalStage attachment(File file);
        
        /**
         * 添加多个附件
         * @param files 附件文件列表
         * @return 可选设置阶段
         */
        OptionalStage attachments(File... files);
        
        /**
         * 设置邮件优先级
         * @param priority 优先级
         * @return 可选设置阶段
         */
        OptionalStage priority(Priority priority);
        
        /**
         * 设置是否需要读取回执
         * @param readReceipt 是否需要读取回执
         * @return 可选设置阶段
         */
        OptionalStage readReceipt(boolean readReceipt);
        
        /**
         * 设置计划发送时间
         * @param scheduledTime 计划发送时间
         * @return 可选设置阶段
         */
        OptionalStage scheduledTime(LocalDateTime scheduledTime);
    }
    
    /**
     * 构建阶段
     */
    public interface BuildStage {
        /**
         * 构建邮件对象
         * @return 邮件对象
         */
        EmailV2 build();
    }
    
    // ==================== Builder实现类 ====================
    
    /**
     * 邮件构建器实现类
     */
    public static class Builder implements FromStage, ToStage, SubjectStage, ContentStage, OptionalStage {
        
        // 邮件属性字段
        private String from;
        private String fromName;
        private final List<String> toList = new ArrayList<>();
        private final List<String> ccList = new ArrayList<>();
        private final List<String> bccList = new ArrayList<>();
        private String subject;
        private String content;
        private ContentType contentType = ContentType.TEXT;
        private final List<File> attachments = new ArrayList<>();
        private Priority priority = Priority.NORMAL;
        private boolean readReceipt = false;
        private LocalDateTime scheduledTime;
        private final Map<String, String> templateVariables = new HashMap<>();
        
        // 邮箱格式验证正则表达式
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        );
        
        // 附件大小限制（25MB）
        private static final long MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024;
        
        /**
         * 私有构造函数
         */
        private Builder() {}
        
        @Override
        public ToStage from(String email) {
            validateEmail(email, "发件人邮箱");
            this.from = email;
            return this;
        }
        
        @Override
        public ToStage from(String email, String name) {
            validateEmail(email, "发件人邮箱");
            validateNotEmpty(name, "发件人姓名");
            this.from = email;
            this.fromName = name;
            return this;
        }
        
        @Override
        public SubjectStage to(String email) {
            validateEmail(email, "收件人邮箱");
            this.toList.add(email);
            return this;
        }
        
        @Override
        public SubjectStage to(String... emails) {
            for (String email : emails) {
                validateEmail(email, "收件人邮箱");
                this.toList.add(email);
            }
            return this;
        }
        
        @Override
        public SubjectStage to(List<String> emails) {
            for (String email : emails) {
                validateEmail(email, "收件人邮箱");
                this.toList.add(email);
            }
            return this;
        }
        
        @Override
        public ContentStage subject(String subject) {
            validateNotEmpty(subject, "邮件主题");
            this.subject = subject;
            return this;
        }
        
        @Override
        public OptionalStage content(String content) {
            validateNotEmpty(content, "邮件内容");
            this.content = content;
            this.contentType = ContentType.TEXT;
            return this;
        }
        
        @Override
        public OptionalStage htmlContent(String htmlContent) {
            validateNotEmpty(htmlContent, "HTML邮件内容");
            this.content = htmlContent;
            this.contentType = ContentType.HTML;
            return this;
        }
        
        @Override
        public OptionalStage templateContent(String template, Map<String, String> variables) {
            validateNotEmpty(template, "邮件模板");
            validateNotNull(variables, "模板变量");
            this.content = template;
            this.templateVariables.putAll(variables);
            this.contentType = ContentType.TEXT;
            return this;
        }
        
        @Override
        public OptionalStage cc(String email) {
            validateEmail(email, "抄送邮箱");
            this.ccList.add(email);
            return this;
        }
        
        @Override
        public OptionalStage cc(String... emails) {
            for (String email : emails) {
                validateEmail(email, "抄送邮箱");
                this.ccList.add(email);
            }
            return this;
        }
        
        @Override
        public OptionalStage bcc(String email) {
            validateEmail(email, "密送邮箱");
            this.bccList.add(email);
            return this;
        }
        
        @Override
        public OptionalStage bcc(String... emails) {
            for (String email : emails) {
                validateEmail(email, "密送邮箱");
                this.bccList.add(email);
            }
            return this;
        }
        
        @Override
        public OptionalStage attachment(File file) {
            validateAttachment(file);
            this.attachments.add(file);
            return this;
        }
        
        @Override
        public OptionalStage attachments(File... files) {
            for (File file : files) {
                validateAttachment(file);
                this.attachments.add(file);
            }
            return this;
        }
        
        @Override
        public OptionalStage priority(Priority priority) {
            validateNotNull(priority, "邮件优先级");
            this.priority = priority;
            return this;
        }
        
        @Override
        public OptionalStage readReceipt(boolean readReceipt) {
            this.readReceipt = readReceipt;
            return this;
        }
        
        @Override
        public OptionalStage scheduledTime(LocalDateTime scheduledTime) {
            if (scheduledTime != null && scheduledTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("计划发送时间不能早于当前时间");
            }
            this.scheduledTime = scheduledTime;
            return this;
        }
        
        @Override
        public EmailV2 build() {
            // 最终验证
            validateFinalState();
            return new EmailV2(this);
        }
        
        // ==================== 验证方法 ====================
        
        /**
         * 验证邮箱格式
         */
        private void validateEmail(String email, String fieldName) {
            validateNotEmpty(email, fieldName);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException(fieldName + "格式不正确: " + email);
            }
        }
        
        /**
         * 验证字符串不为空
         */
        private void validateNotEmpty(String value, String fieldName) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + "不能为空");
            }
        }
        
        /**
         * 验证对象不为null
         */
        private void validateNotNull(Object value, String fieldName) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + "不能为null");
            }
        }
        
        /**
         * 验证附件
         */
        private void validateAttachment(File file) {
            validateNotNull(file, "附件文件");
            if (!file.exists()) {
                throw new IllegalArgumentException("附件文件不存在: " + file.getPath());
            }
            if (file.length() > MAX_ATTACHMENT_SIZE) {
                throw new IllegalArgumentException("附件文件过大，最大支持25MB: " + file.getPath());
            }
        }
        
        /**
         * 最终状态验证
         */
        private void validateFinalState() {
            if (from == null) {
                throw new IllegalStateException("发件人不能为空");
            }
            if (toList.isEmpty()) {
                throw new IllegalStateException("收件人不能为空");
            }
            if (subject == null) {
                throw new IllegalStateException("邮件主题不能为空");
            }
            if (content == null) {
                throw new IllegalStateException("邮件内容不能为空");
            }
            
            // 验证总收件人数量限制
            int totalRecipients = toList.size() + ccList.size() + bccList.size();
            if (totalRecipients > 100) {
                throw new IllegalStateException("收件人总数不能超过100个");
            }
            
            // 验证附件总大小
            long totalAttachmentSize = attachments.stream()
                .mapToLong(File::length)
                .sum();
            if (totalAttachmentSize > MAX_ATTACHMENT_SIZE) {
                throw new IllegalStateException("附件总大小不能超过25MB");
            }
        }
    }
    
    // ==================== 枚举定义 ====================
    
    /**
     * 邮件优先级
     */
    public enum Priority {
        /**
         * 高优先级
         */
        HIGH("高"),
        
        /**
         * 普通优先级
         */
        NORMAL("普通"),
        
        /**
         * 低优先级
         */
        LOW("低");
        
        private final String description;
        
        Priority(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 邮件内容类型
     */
    public enum ContentType {
        /**
         * 纯文本
         */
        TEXT("text/plain"),
        
        /**
         * HTML格式
         */
        HTML("text/html");
        
        private final String mimeType;
        
        ContentType(String mimeType) {
            this.mimeType = mimeType;
        }
        
        public String getMimeType() {
            return mimeType;
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 处理模板变量替换
     */
    private String processTemplate(String template, Map<String, String> variables) {
        if (template == null || variables.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }
    
    // ==================== Getter方法 ====================
    
    public String getFrom() {
        return from;
    }
    
    public String getFromName() {
        return fromName;
    }
    
    public List<String> getToList() {
        return toList;
    }
    
    public List<String> getCcList() {
        return ccList;
    }
    
    public List<String> getBccList() {
        return bccList;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public String getContent() {
        return content;
    }
    
    public ContentType getContentType() {
        return contentType;
    }
    
    public List<File> getAttachments() {
        return attachments;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public boolean isReadReceipt() {
        return readReceipt;
    }
    
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
    
    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    // ==================== toString方法 ====================
    
    @Override
    public String toString() {
        return "EmailV2{" +
                "from='" + from + '\'' +
                ", fromName='" + fromName + '\'' +
                ", toList=" + toList +
                ", ccList=" + ccList +
                ", bccList=" + bccList +
                ", subject='" + subject + '\'' +
                ", contentType=" + contentType +
                ", attachments=" + attachments.size() + " files" +
                ", priority=" + priority +
                ", readReceipt=" + readReceipt +
                ", scheduledTime=" + scheduledTime +
                '}';
    }
}