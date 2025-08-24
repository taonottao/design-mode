package com.tao.builder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 15:25
 */
public class Demo {

    public static void main1(String[] args) {
//        User.Builder builder = new User.Builder();
//        builder.name("Tom");
//        builder.age(11);
//        User user = builder.build();
        User user = User.builder()
                .name("Tom")
                .age(11)
                .build();

        List.of(1).stream().map(i -> i + 1).collect(Collectors.toSet());

        User.Builder builder1 = User.builder().name("Jerry");
        User.Builder builder2 = builder1.age(12);

        CompletableFuture.supplyAsync(() -> builder2.build()).thenAccept(user1 -> System.out.println(user1));

    }

    public static void main(String[] args) {
//        String sql = SQL.builder(SQL.SQLType.SELECT)
//                .select("name", "age")
//                .table("user")
//                .table("user2") // 这里user2会覆盖user，所以应该限制这个方法只能调用一次
//                .set("name", "Tom") // 这里应该不允许调用这个方法
//                .where("id = 1")
//                .buildSql();
//        System.out.println(sql);
//        String updateSql = SQL.builder(SQL.SQLType.UPDATE)
//                .table("user")
//                .set("name", "Tom")
//                .set("age", "11")
//                .where("id = 1")
//                .buildSql();
//        System.out.println(updateSql);

//        System.out.println(SQL.select("name", "age").from("user").where("id = 1").buildSql());
//        System.out.println(SQL.update().table("user").set("name", "Tom").set("age", "11").where("id = 1").buildSql());
        System.out.println(SQL.update().table("user").where("id = 1").set("name", "Tom").set("age", "11").buildSql());
        System.out.println(SQL.select("name", "age").from("user").where("id = 1").buildSql());
        System.out.println(SQL.delete().from("user").where("id = 1").buildSql());
        System.out.println(SQL.insert("user").columns(new String[]{"name", "age"}, new String[]{"Tom", "11"}).buildSql());
    }

}
