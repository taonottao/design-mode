package com.tao.builder;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 21:40
 * 邮件构建器对比演示 - Email 1.0 vs EmailV2
 */
public class EmailComparisonDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 邮件构建器对比演示 ===");
        System.out.println();
        
        // ==================== Email 1.0 演示 ====================
        System.out.println("📧 Email 1.0 (基础版本) 演示:");
        System.out.println("----------------------------------------");
        
        try {
            // 基本邮件构建
            Email email1 = Email.builder()
                    .from("sender@example.com")
                    .to("receiver@example.com")
                    .content("这是使用Email 1.0构建的邮件")
                    .subject("Email 1.0 测试邮件")
                    .priority(Email.Priority.HIGH)
                    .build();
            
            System.out.println("✅ Email 1.0 基本邮件构建成功");
            
            // 带附件的邮件
            Email email2 = Email.builder()
                    .from("sender@example.com")
                    .to("receiver@example.com")
                    .content("带附件的邮件")
                    .subject(null) // 测试默认主题处理
                    .cc("cc@example.com")
                    .attachment(new File("README.md")) // 假设文件存在
                    .build();
            
            System.out.println("✅ Email 1.0 带附件邮件构建成功");
            
        } catch (Exception e) {
            System.err.println("❌ Email 1.0 构建失败: " + e.getMessage());
        }
        
        System.out.println();
        
        // ==================== EmailV2 演示 ====================
        System.out.println("🚀 EmailV2 (增强版本) 演示:");
        System.out.println("----------------------------------------");
        
        try {
            // 1. 基本邮件构建（阶段性接口）
            EmailV2 emailV2_1 = EmailV2.newEmail()
                    .from("sender@example.com", "发件人姓名")
                    .to("receiver@example.com")
                    .subject("EmailV2 测试邮件")
                    .content("这是使用EmailV2构建的邮件")
                    .priority(EmailV2.Priority.HIGH)
                    .readReceipt(true)
                    .build();
            
            System.out.println("✅ EmailV2 基本邮件构建成功");
            System.out.println("   发件人: " + emailV2_1.getFrom() + " (" + emailV2_1.getFromName() + ")");
            System.out.println("   收件人: " + emailV2_1.getToList());
            System.out.println("   优先级: " + emailV2_1.getPriority().getDescription());
            System.out.println("   读取回执: " + emailV2_1.isReadReceipt());
            
            // 2. 多收件人邮件
            EmailV2 emailV2_2 = EmailV2.newEmail()
                    .from("sender@example.com")
                    .to("user1@example.com", "user2@example.com", "user3@example.com")
                    .subject("群发邮件")
                    .content("这是一封群发邮件")
                    .cc("manager@example.com")
                    .bcc("admin@example.com")
                    .build();
            
            System.out.println("✅ EmailV2 多收件人邮件构建成功");
            System.out.println("   收件人数量: " + emailV2_2.getToList().size());
            System.out.println("   抄送数量: " + emailV2_2.getCcList().size());
            System.out.println("   密送数量: " + emailV2_2.getBccList().size());
            
            // 3. HTML邮件
            EmailV2 emailV2_3 = EmailV2.newEmail()
                    .from("sender@example.com")
                    .to("receiver@example.com")
                    .subject("HTML格式邮件")
                    .htmlContent("<h1>欢迎</h1><p>这是一封<strong>HTML格式</strong>的邮件</p>")
                    .priority(EmailV2.Priority.NORMAL)
                    .build();
            
            System.out.println("✅ EmailV2 HTML邮件构建成功");
            System.out.println("   内容类型: " + emailV2_3.getContentType().getMimeType());
            
            // 4. 模板邮件
            Map<String, String> variables = new HashMap<>();
            variables.put("userName", "张三");
            variables.put("productName", "EmailV2构建器");
            variables.put("date", "2025年8月23日");
            
            EmailV2 emailV2_4 = EmailV2.newEmail()
                    .from("system@example.com")
                    .to("user@example.com")
                    .subject("欢迎使用${productName}")
                    .templateContent(
                        "亲爱的${userName}，\n\n" +
                        "欢迎使用${productName}！\n" +
                        "注册时间：${date}\n\n" +
                        "祝您使用愉快！",
                        variables
                    )
                    .build();
            
            System.out.println("✅ EmailV2 模板邮件构建成功");
            System.out.println("   处理后的内容预览: ");
            System.out.println("   " + emailV2_4.getContent().replace("\n", "\n   "));
            
            // 5. 计划发送邮件
            EmailV2 emailV2_5 = EmailV2.newEmail()
                    .from("scheduler@example.com")
                    .to("user@example.com")
                    .subject("计划发送的邮件")
                    .content("这封邮件将在指定时间发送")
                    .scheduledTime(LocalDateTime.now().plusHours(1))
                    .build();
            
            System.out.println("✅ EmailV2 计划发送邮件构建成功");
            System.out.println("   计划发送时间: " + emailV2_5.getScheduledTime());
            
            // 6. 快速构建邮件
            EmailV2 simpleEmail = EmailV2.simpleEmail(
                "quick@example.com",
                "user@example.com",
                "快速邮件",
                "这是使用快速构建方法创建的邮件"
            );
            
            System.out.println("✅ EmailV2 快速邮件构建成功");
            
        } catch (Exception e) {
            System.err.println("❌ EmailV2 构建失败: " + e.getMessage());
        }
        
        System.out.println();
        
        // ==================== 错误处理对比 ====================
        System.out.println("🔍 错误处理对比演示:");
        System.out.println("----------------------------------------");
        
        // Email 1.0 错误处理
        System.out.println("Email 1.0 错误处理:");
        try {
            Email invalidEmail1 = Email.builder()
                    .from("sender@example.com")
                    .to("receiver@example.com")
                    .content("缺少发件人")
                    .build();
        } catch (Exception e) {
            System.out.println("  ❌ " + e.getMessage());
        }
        
        // EmailV2 错误处理
        System.out.println("EmailV2 错误处理:");
        try {
            // 邮箱格式验证
            EmailV2 invalidEmail2 = EmailV2.newEmail()
                    .from("invalid-email")
                    .to("receiver@example.com")
                    .subject("测试")
                    .content("测试内容")
                    .build();
        } catch (Exception e) {
            System.out.println("  ❌ " + e.getMessage());
        }
        
        try {
            // 计划时间验证
            EmailV2 invalidEmail3 = EmailV2.newEmail()
                    .from("sender@example.com")
                    .to("receiver@example.com")
                    .subject("测试")
                    .content("测试内容")
                    .scheduledTime(LocalDateTime.now().minusHours(1)) // 过去的时间
                    .build();
        } catch (Exception e) {
            System.out.println("  ❌ " + e.getMessage());
        }
        
        System.out.println();
        
        // ==================== 功能对比总结 ====================
        System.out.println("📊 功能对比总结:");
        System.out.println("========================================");
        
        System.out.println("Email 1.0 特性:");
        System.out.println("  ✓ 基础Builder模式");
        System.out.println("  ✓ 单收件人、单抄送");
        System.out.println("  ✓ 基本验证（非空检查）");
        System.out.println("  ✓ 优先级设置");
        System.out.println("  ✓ 单个附件支持");
        System.out.println("  ✓ 默认主题处理");
        
        System.out.println();
        
        System.out.println("EmailV2 增强特性:");
        System.out.println("  🚀 阶段性接口设计（类型安全）");
        System.out.println("  🚀 多收件人、多抄送、密送支持");
        System.out.println("  🚀 邮箱格式验证");
        System.out.println("  🚀 HTML内容支持");
        System.out.println("  🚀 模板变量替换");
        System.out.println("  🚀 计划发送时间");
        System.out.println("  🚀 读取回执设置");
        System.out.println("  🚀 多附件支持");
        System.out.println("  🚀 附件大小验证");
        System.out.println("  🚀 收件人数量限制");
        System.out.println("  🚀 静态工厂方法");
        System.out.println("  🚀 快速构建方法");
        System.out.println("  🚀 不可变对象设计");
        System.out.println("  🚀 完善的toString方法");
        
        System.out.println();
        
        System.out.println("💡 推荐使用场景:");
        System.out.println("----------------------------------------");
        System.out.println("Email 1.0: 适用于简单的邮件发送场景，学习Builder模式基础");
        System.out.println("EmailV2: 适用于企业级邮件系统，需要复杂功能和严格验证的场景");
        
        System.out.println();
        System.out.println("🎯 Builder模式核心价值体现:");
        System.out.println("1. 流畅的API设计 - 链式调用提供良好的可读性");
        System.out.println("2. 类型安全 - 阶段性接口确保构建流程的正确性");
        System.out.println("3. 参数验证 - 在构建过程中进行逐步验证");
        System.out.println("4. 复杂度隐藏 - 将复杂的对象创建过程封装起来");
        System.out.println("5. 扩展性 - 易于添加新的可选参数和功能");
    }
}