package com.tao.builder;

import java.io.File;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 21:23
 */
public class Email {

    /**
     * 发件人
     */
    private String from;

    /**
     * 收件人
     */
    private String to;

    /**
     * 抄送
     */
    private String cc;
    /**
     * 邮件主题
     */
    private String subject;

    private String content;
    private File attachment;
    private Priority priority;


    public interface FromStage {
        ToStage from(String from);
    }
    public interface ToStage {
        ContentStage to(String to);
    }
    public interface ContentStage {
        Builder content(String content);
    }


    private Email(Builder  builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.cc = builder.cc;
        this.subject = builder.subject;
        this.content = builder.content;
        this.attachment = builder.attachment;
        this.priority = builder.priority;
    }

    public static FromStage builder() {
        return new Builder();
    }


    public static class Builder implements FromStage, ToStage, ContentStage{
        private String from;
        private String to;
        private String cc;
        private String subject;

        private String content;
        private File attachment;
        private Priority priority;

        @Override
        public Builder from(String from) {
            this.from = from;
            return this;
        }
        @Override
        public Builder to(String to) {
            this.to = to;
            return this;
        }
        public Builder cc(String cc) {
            this.cc = cc;
            return this;
        }
        public Builder subject(String subject) {
            if (subject == null) {
                this.subject = "无主题";
            } else {
                this.subject = subject;
            }
            return this;
        }
        @Override
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        public Builder attachment(File attachment) {
            this.attachment = attachment;
            return this;
        }
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        public Email build() {
            if (this.from == null) {
                throw new IllegalArgumentException("发件人不能为空");
            }
            if (this.to == null) {
                throw new IllegalArgumentException("收件人不能为空");
            }
            if (this.content == null) {
                throw new IllegalArgumentException("邮件内容不能为空");
            }
            return new Email(this);
        }
        private Builder() {

        }
    }


    enum Priority {
        HIGH,
        NORMAL,
        LOW
    }


}
