package com.tao.workflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据库配置类
 * 配置PostgreSQL数据源和HikariCP连接池
 * 
 * @author tao
 * @since 2024-01-15
 */
@Configuration
public class DatabaseConfig {

    // 数据库连接配置参数
    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/workflow_engine}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username:postgres}")
    private String username;
    
    @Value("${spring.datasource.password:123456}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;
    
    // HikariCP连接池配置参数
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * 配置HikariCP数据源
     * HikariCP是目前性能最好的数据库连接池
     * 
     * @return DataSource 数据源实例
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // 连接池大小配置
        config.setMaximumPoolSize(maximumPoolSize); // 连接池最大连接数
        config.setMinimumIdle(minimumIdle); // 连接池最小空闲连接数
        
        // 连接超时配置
        config.setConnectionTimeout(connectionTimeout); // 等待连接池分配连接的最大时长（毫秒）
        config.setIdleTimeout(idleTimeout); // 连接空闲超时时间（毫秒）
        config.setMaxLifetime(maxLifetime); // 连接最大存活时间（毫秒）
        
        // 连接泄漏检测
        config.setLeakDetectionThreshold(leakDetectionThreshold); // 连接泄漏检测阈值（毫秒）
        
        // 连接池名称
        config.setPoolName("WorkflowEngineHikariCP");
        
        // PostgreSQL特定配置
        config.addDataSourceProperty("cachePrepStmts", "true"); // 开启预编译语句缓存
        config.addDataSourceProperty("prepStmtCacheSize", "250"); // 预编译语句缓存大小
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // 预编译语句最大长度
        config.addDataSourceProperty("useServerPrepStmts", "true"); // 使用服务器端预编译语句
        config.addDataSourceProperty("useLocalSessionState", "true"); // 使用本地会话状态
        config.addDataSourceProperty("rewriteBatchedStatements", "true"); // 重写批量语句
        config.addDataSourceProperty("cacheResultSetMetadata", "true"); // 缓存结果集元数据
        config.addDataSourceProperty("cacheServerConfiguration", "true"); // 缓存服务器配置
        config.addDataSourceProperty("elideSetAutoCommits", "true"); // 省略自动提交设置
        config.addDataSourceProperty("maintainTimeStats", "false"); // 不维护时间统计
        
        // 字符集配置
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");
        
        // 连接测试配置
        config.setConnectionTestQuery("SELECT 1"); // 连接测试查询语句
        
        return new HikariDataSource(config);
    }
    
    /**
     * 配置JdbcTemplate
     * 用于执行原生SQL查询
     * 
     * @param dataSource 数据源
     * @return JdbcTemplate 实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // 设置查询超时时间（秒）
        jdbcTemplate.setQueryTimeout(30);
        // 设置最大行数限制
        jdbcTemplate.setMaxRows(10000);
        // 设置获取大小
        jdbcTemplate.setFetchSize(1000);
        return jdbcTemplate;
    }
}