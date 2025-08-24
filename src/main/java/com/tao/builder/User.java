package com.tao.builder;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 14:48
 * 假设有这样的需求，年龄小于10的不允许叫 Tom，年龄大于10的不允许叫Jerry
 */
public class User {

    private final String name;

    private final int age;

    private User(Builder builder){
        this.name = builder.name;
        this.age = builder.age;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder{
        private String name;

        private int age;

        private Builder(){}

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        public User build() {
            User user = new User(this);
            if (age < 10 && name.equals("Tom")) {
                return null;
            }
            if (age > 10 && name.equals("Jerry")) {
                return null;
            }
            return user;
        }
    }

    public String getName() {
        return name;
    }
    public int getAge() {
        return age;
    }

}
