package com.tao.builder;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 21:57
 */
public class EmailDemo {

    public static void main(String[] args) {
        Email email = Email.builder().from("tom@gmail.com")
                .to("jerry@gmail.com")
                .content("hello world")
                .subject("hello")
                .priority(Email.Priority.HIGH)
                .build();
        EmailV2 build = EmailV2.newEmail()
                .from("tom@gmail.com")
                .to("jerry@gmail.com")
                .subject("hello")
                .content("hello world")
                .build();
    }

}
