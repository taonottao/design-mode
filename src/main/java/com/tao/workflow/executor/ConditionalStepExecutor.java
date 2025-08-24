package com.tao.workflow.executor;

import com.tao.workflow.engine.*;
import com.tao.workflow.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 条件步骤执行器
 * 
 * 用于执行条件判断步骤，支持多种条件类型：
 * 1. 表达式条件：支持简单的比较表达式
 * 2. 脚本条件：执行脚本代码进行判断
 * 3. 规则条件：基于规则引擎的条件判断
 * 4. 自定义条件：通过配置的条件处理器执行
 * 
 * 支持的配置参数：
 * - conditions: 条件列表，每个条件包含expression、target等
 * - evaluationStrategy: 评估策略（FIRST_MATCH/ALL_MATCH/PRIORITY）
 * - defaultTarget: 默认目标步骤ID
 * - errorTarget: 错误处理目标步骤ID
 * 
 * @author Tao
 * @version 1.0
 */
public class ConditionalStepExecutor extends AbstractStepExecutor {
    
    /** 条件评估器注册表 */
    private final Map<String, ConditionEvaluator> evaluators = new HashMap<>();
    
    /** 默认条件评估器 */
    private final Map<String, ConditionEvaluator> defaultEvaluators = new HashMap<>();
    
    /**
     * 构造函数
     */
    public ConditionalStepExecutor() {
        super("ConditionalStepExecutor", "1.0.0", StepType.CONDITION);
        
        // 注册默认条件评估器
        registerDefaultEvaluators();
        
        logger.info("条件步骤执行器已初始化，支持评估器类型: {}", defaultEvaluators.keySet());
    }
    
    /**
     * 注册默认条件评估器
     */
    private void registerDefaultEvaluators() {
        // 表达式评估器
        defaultEvaluators.put("expression", new ExpressionEvaluator());
        
        // 脚本评估器
        defaultEvaluators.put("script", new ScriptEvaluator());
        
        // 比较评估器
        defaultEvaluators.put("comparison", new ComparisonEvaluator());
        
        // 正则表达式评估器
        defaultEvaluators.put("regex", new RegexEvaluator());
        
        // 范围评估器
        defaultEvaluators.put("range", new RangeEvaluator());
        
        // 包含评估器
        defaultEvaluators.put("contains", new ContainsEvaluator());
        
        // 空值评估器
        defaultEvaluators.put("null", new NullEvaluator());
        
        // 默认评估器
        defaultEvaluators.put("default", new DefaultEvaluator());
    }
    
    /**
     * 注册自定义条件评估器
     */
    public void registerConditionEvaluator(String type, ConditionEvaluator evaluator) {
        Objects.requireNonNull(type, "评估器类型不能为空");
        Objects.requireNonNull(evaluator, "条件评估器不能为空");
        
        evaluators.put(type, evaluator);
        logger.info("已注册自定义条件评估器: {} -> {}", type, evaluator.getClass().getSimpleName());
    }
    
    @Override
    protected StepExecutionResult doExecute(WorkflowStep step, StepExecutionContext context) throws Exception {
        logger.info("开始执行条件步骤: {} (类型: {})", step.getId(), step.getType());
        
        // 获取条件配置
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            throw new WorkflowException(
                "条件步骤必须配置条件信息",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 获取条件列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
        if (conditions == null || conditions.isEmpty()) {
            throw new WorkflowException(
                "条件步骤必须配置至少一个条件",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 获取评估策略
        String strategy = getConfigValue(config, "evaluationStrategy", "FIRST_MATCH");
        EvaluationStrategy evaluationStrategy = EvaluationStrategy.valueOf(strategy.toUpperCase());
        
        // 获取默认目标和错误目标
        String defaultTarget = getConfigValue(config, "defaultTarget", null);
        String errorTarget = getConfigValue(config, "errorTarget", null);
        
        try {
            // 执行条件评估
            ConditionEvaluationResult result = evaluateConditions(
                conditions, context, evaluationStrategy
            );
            
            // 确定下一步骤
            String nextStepId = determineNextStep(result, defaultTarget, errorTarget);
            
            // 构建执行结果
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("conditionResult", result.isMatched());
            outputData.put("matchedConditions", result.getMatchedConditions());
            outputData.put("evaluationDetails", result.getEvaluationDetails());
            outputData.put("nextStepId", nextStepId);
            
            return StepExecutionResult.builder()
                .status(StepExecutionResult.Status.SUCCESS)
                .stepId(step.getId())
                .executorName(getExecutorName())
                .outputData(outputData)
                .nextStepId(nextStepId)
                .message(String.format("条件评估完成，匹配结果: %s", result.isMatched()))
                .build();
                
        } catch (Exception e) {
            logger.error("条件评估失败: {}", step.getId(), e);
            
            // 如果有错误目标，跳转到错误处理步骤
            if (errorTarget != null) {
                Map<String, Object> outputData = new HashMap<>();
                outputData.put("error", e.getMessage());
                outputData.put("nextStepId", errorTarget);
                
                return StepExecutionResult.builder()
                    .status(StepExecutionResult.Status.SUCCESS)
                    .stepId(step.getId())
                    .executorName(getExecutorName())
                    .outputData(outputData)
                    .nextStepId(errorTarget)
                    .message("条件评估出错，跳转到错误处理步骤")
                    .build();
            }
            
            throw e;
        }
    }
    
    @Override
    protected void doValidateConfiguration(WorkflowStep step) throws WorkflowException {
        Map<String, Object> config = step.getConfiguration();
        if (config == null) {
            throw new WorkflowException(
                "条件步骤必须配置条件信息",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证条件列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
        if (conditions == null || conditions.isEmpty()) {
            throw new WorkflowException(
                "条件步骤必须配置至少一个条件",
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 验证每个条件
        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> condition = conditions.get(i);
            try {
                validateCondition(condition);
            } catch (Exception e) {
                throw new WorkflowException(
                    String.format("条件 [%d] 配置无效: %s", i, e.getMessage()),
                    WorkflowException.ErrorType.CONFIGURATION_ERROR,
                    e
                );
            }
        }
        
        // 验证评估策略
        String strategy = getConfigValue(config, "evaluationStrategy", "FIRST_MATCH");
        try {
            EvaluationStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WorkflowException(
                String.format("不支持的评估策略: %s", strategy),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
    }
    
    // 辅助方法
    
    /**
     * 评估条件列表
     */
    private ConditionEvaluationResult evaluateConditions(
            List<Map<String, Object>> conditions,
            StepExecutionContext context,
            EvaluationStrategy strategy) throws Exception {
        
        List<ConditionResult> results = new ArrayList<>();
        List<Map<String, Object>> matchedConditions = new ArrayList<>();
        Map<String, Object> evaluationDetails = new HashMap<>();
        
        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> condition = conditions.get(i);
            
            try {
                // 评估单个条件
                ConditionResult result = evaluateCondition(condition, context);
                results.add(result);
                
                // 记录评估详情
                evaluationDetails.put("condition_" + i, result.getDetails());
                
                // 如果条件匹配
                if (result.isMatched()) {
                    matchedConditions.add(condition);
                    
                    // 根据策略决定是否继续评估
                    if (strategy == EvaluationStrategy.FIRST_MATCH) {
                        break;
                    }
                }
                
            } catch (Exception e) {
                logger.warn("条件 [{}] 评估失败: {}", i, e.getMessage());
                evaluationDetails.put("condition_" + i + "_error", e.getMessage());
                
                // 根据策略决定是否抛出异常
                if (strategy == EvaluationStrategy.ALL_MATCH) {
                    throw e;
                }
            }
        }
        
        // 根据策略确定最终结果
        boolean finalResult = determineFinalResult(results, strategy);
        
        return new ConditionEvaluationResult(
            finalResult,
            matchedConditions,
            evaluationDetails,
            results
        );
    }
    
    /**
     * 评估单个条件
     */
    private ConditionResult evaluateCondition(Map<String, Object> condition, StepExecutionContext context) throws Exception {
        String type = getConfigValue(condition, "type", "expression");
        
        // 获取条件评估器
        ConditionEvaluator evaluator = getConditionEvaluator(type);
        if (evaluator == null) {
            throw new WorkflowException(
                String.format("未找到条件类型 [%s] 的评估器", type),
                WorkflowException.ErrorType.CONFIGURATION_ERROR
            );
        }
        
        // 创建条件上下文
        ConditionContext conditionContext = new ConditionContext(
            condition,
            context.getInputParameters(),
            context.getInstanceContext(),
            context.getTemporaryData()
        );
        
        // 执行条件评估
        return evaluator.evaluate(conditionContext);
    }
    
    /**
     * 验证条件配置
     */
    private void validateCondition(Map<String, Object> condition) throws Exception {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("条件配置不能为空");
        }
        
        String type = getConfigValue(condition, "type", "expression");
        ConditionEvaluator evaluator = getConditionEvaluator(type);
        if (evaluator == null) {
            throw new IllegalArgumentException(String.format("不支持的条件类型: %s", type));
        }
        
        evaluator.validateConfiguration(condition);
    }
    
    /**
     * 获取条件评估器
     */
    private ConditionEvaluator getConditionEvaluator(String type) {
        // 优先查找自定义评估器
        ConditionEvaluator evaluator = evaluators.get(type);
        if (evaluator != null) {
            return evaluator;
        }
        
        // 查找默认评估器
        return defaultEvaluators.get(type);
    }
    
    /**
     * 根据策略确定最终结果
     */
    private boolean determineFinalResult(List<ConditionResult> results, EvaluationStrategy strategy) {
        if (results.isEmpty()) {
            return false;
        }
        
        switch (strategy) {
            case FIRST_MATCH:
                return results.stream().anyMatch(ConditionResult::isMatched);
            case ALL_MATCH:
                return results.stream().allMatch(ConditionResult::isMatched);
            case PRIORITY:
                // 按优先级排序，返回第一个匹配的结果
                return results.stream()
                    .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                    .findFirst()
                    .map(ConditionResult::isMatched)
                    .orElse(false);
            default:
                return false;
        }
    }
    
    /**
     * 确定下一步骤
     */
    private String determineNextStep(ConditionEvaluationResult result, String defaultTarget, String errorTarget) {
        if (result.isMatched() && !result.getMatchedConditions().isEmpty()) {
            // 获取第一个匹配条件的目标步骤
            Map<String, Object> firstMatch = result.getMatchedConditions().get(0);
            String target = getConfigValue(firstMatch, "target", null);
            if (target != null) {
                return target;
            }
        }
        
        // 返回默认目标
        return defaultTarget;
    }
    
    /**
     * 获取配置值
     */
    private String getConfigValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    // 内部类和接口
    
    /**
     * 评估策略枚举
     */
    public enum EvaluationStrategy {
        /** 第一个匹配 */
        FIRST_MATCH,
        /** 全部匹配 */
        ALL_MATCH,
        /** 按优先级 */
        PRIORITY
    }
    
    /**
     * 条件评估器接口
     */
    public interface ConditionEvaluator {
        
        /**
         * 评估条件
         */
        ConditionResult evaluate(ConditionContext context) throws Exception;
        
        /**
         * 验证配置
         */
        void validateConfiguration(Map<String, Object> config) throws Exception;
    }
    
    /**
     * 条件上下文
     */
    public static class ConditionContext {
        private final Map<String, Object> condition;
        private final Map<String, Object> inputParameters;
        private final Map<String, Object> instanceContext;
        private final Map<String, Object> temporaryData;
        
        public ConditionContext(Map<String, Object> condition,
                               Map<String, Object> inputParameters,
                               Map<String, Object> instanceContext,
                               Map<String, Object> temporaryData) {
            this.condition = new HashMap<>(condition);
            this.inputParameters = new HashMap<>(inputParameters);
            this.instanceContext = new HashMap<>(instanceContext);
            this.temporaryData = new HashMap<>(temporaryData);
        }
        
        // Getters
        public Map<String, Object> getCondition() { return new HashMap<>(condition); }
        public Map<String, Object> getInputParameters() { return new HashMap<>(inputParameters); }
        public Map<String, Object> getInstanceContext() { return new HashMap<>(instanceContext); }
        public Map<String, Object> getTemporaryData() { return new HashMap<>(temporaryData); }
        
        // 便利方法
        public String getConditionValue(String key) {
            Object value = condition.get(key);
            return value != null ? value.toString() : null;
        }
        
        public Object getParameterValue(String key) {
            return inputParameters.get(key);
        }
        
        public Object getContextValue(String key) {
            return instanceContext.get(key);
        }
        
        public Object getTemporaryValue(String key) {
            return temporaryData.get(key);
        }
        
        /**
         * 获取变量值（按优先级：临时数据 > 输入参数 > 实例上下文）
         */
        public Object getVariableValue(String key) {
            Object value = temporaryData.get(key);
            if (value != null) {
                return value;
            }
            
            value = inputParameters.get(key);
            if (value != null) {
                return value;
            }
            
            return instanceContext.get(key);
        }
    }
    
    /**
     * 条件结果
     */
    public static class ConditionResult {
        private final boolean matched;
        private final String target;
        private final int priority;
        private final Map<String, Object> details;
        private final String message;
        
        public ConditionResult(boolean matched, String target, int priority, 
                              Map<String, Object> details, String message) {
            this.matched = matched;
            this.target = target;
            this.priority = priority;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
            this.message = message;
        }
        
        public static ConditionResult matched(String target) {
            return new ConditionResult(true, target, 0, null, "条件匹配");
        }
        
        public static ConditionResult matched(String target, int priority) {
            return new ConditionResult(true, target, priority, null, "条件匹配");
        }
        
        public static ConditionResult matched(String target, Map<String, Object> details) {
            return new ConditionResult(true, target, 0, details, "条件匹配");
        }
        
        public static ConditionResult notMatched() {
            return new ConditionResult(false, null, 0, null, "条件不匹配");
        }
        
        public static ConditionResult notMatched(String message) {
            return new ConditionResult(false, null, 0, null, message);
        }
        
        // Getters
        public boolean isMatched() { return matched; }
        public String getTarget() { return target; }
        public int getPriority() { return priority; }
        public Map<String, Object> getDetails() { return new HashMap<>(details); }
        public String getMessage() { return message; }
    }
    
    /**
     * 条件评估结果
     */
    public static class ConditionEvaluationResult {
        private final boolean matched;
        private final List<Map<String, Object>> matchedConditions;
        private final Map<String, Object> evaluationDetails;
        private final List<ConditionResult> results;
        
        public ConditionEvaluationResult(boolean matched,
                                        List<Map<String, Object>> matchedConditions,
                                        Map<String, Object> evaluationDetails,
                                        List<ConditionResult> results) {
            this.matched = matched;
            this.matchedConditions = new ArrayList<>(matchedConditions);
            this.evaluationDetails = new HashMap<>(evaluationDetails);
            this.results = new ArrayList<>(results);
        }
        
        // Getters
        public boolean isMatched() { return matched; }
        public List<Map<String, Object>> getMatchedConditions() { return new ArrayList<>(matchedConditions); }
        public Map<String, Object> getEvaluationDetails() { return new HashMap<>(evaluationDetails); }
        public List<ConditionResult> getResults() { return new ArrayList<>(results); }
    }
    
    // 默认条件评估器实现
    
    /**
     * 表达式评估器
     */
    private static class ExpressionEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String expression = context.getConditionValue("expression");
            String target = context.getConditionValue("target");
            
            if (expression == null) {
                return ConditionResult.notMatched("表达式不能为空");
            }
            
            // 简化实现：解析简单的比较表达式
            boolean result = evaluateSimpleExpression(expression, context);
            
            Map<String, Object> details = new HashMap<>();
            details.put("expression", expression);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("expression")) {
                throw new IllegalArgumentException("表达式条件必须配置 'expression' 参数");
            }
        }
        
        /**
         * 评估简单表达式（如：age > 18, status == 'active'）
         */
        private boolean evaluateSimpleExpression(String expression, ConditionContext context) {
            try {
                // 简化实现：支持基本的比较操作
                if (expression.contains("==")) {
                    String[] parts = expression.split("==");
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim().replace("'", "").replace("\"", "");
                        Object leftValue = context.getVariableValue(left);
                        return Objects.equals(String.valueOf(leftValue), right);
                    }
                } else if (expression.contains(">")) {
                    String[] parts = expression.split(">");
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim();
                        Object leftValue = context.getVariableValue(left);
                        if (leftValue instanceof Number && isNumeric(right)) {
                            return ((Number) leftValue).doubleValue() > Double.parseDouble(right);
                        }
                    }
                } else if (expression.contains("<")) {
                    String[] parts = expression.split("<");
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim();
                        Object leftValue = context.getVariableValue(left);
                        if (leftValue instanceof Number && isNumeric(right)) {
                            return ((Number) leftValue).doubleValue() < Double.parseDouble(right);
                        }
                    }
                }
                
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        
        private boolean isNumeric(String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    /**
     * 脚本评估器
     */
    private static class ScriptEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String script = context.getConditionValue("script");
            String target = context.getConditionValue("target");
            
            if (script == null) {
                return ConditionResult.notMatched("脚本不能为空");
            }
            
            // 这里应该集成脚本引擎执行脚本
            // 简化实现：模拟脚本执行
            boolean result = script.contains("true");
            
            Map<String, Object> details = new HashMap<>();
            details.put("script", script);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("script")) {
                throw new IllegalArgumentException("脚本条件必须配置 'script' 参数");
            }
        }
    }
    
    /**
     * 比较评估器
     */
    private static class ComparisonEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String variable = context.getConditionValue("variable");
            String operator = context.getConditionValue("operator");
            String value = context.getConditionValue("value");
            String target = context.getConditionValue("target");
            
            if (variable == null || operator == null || value == null) {
                return ConditionResult.notMatched("比较条件参数不完整");
            }
            
            Object variableValue = context.getVariableValue(variable);
            boolean result = performComparison(variableValue, operator, value);
            
            Map<String, Object> details = new HashMap<>();
            details.put("variable", variable);
            details.put("variableValue", variableValue);
            details.put("operator", operator);
            details.put("value", value);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("variable") || !config.containsKey("operator") || !config.containsKey("value")) {
                throw new IllegalArgumentException("比较条件必须配置 'variable'、'operator' 和 'value' 参数");
            }
        }
        
        private boolean performComparison(Object variableValue, String operator, String value) {
            if (variableValue == null) {
                return "null".equals(value) && ("==".equals(operator) || "eq".equals(operator));
            }
            
            String variableStr = String.valueOf(variableValue);
            
            switch (operator.toLowerCase()) {
                case "==":
                case "eq":
                    return variableStr.equals(value);
                case "!=":
                case "ne":
                    return !variableStr.equals(value);
                case ">":
                case "gt":
                    return compareNumeric(variableValue, value) > 0;
                case ">=":
                case "ge":
                    return compareNumeric(variableValue, value) >= 0;
                case "<":
                case "lt":
                    return compareNumeric(variableValue, value) < 0;
                case "<=":
                case "le":
                    return compareNumeric(variableValue, value) <= 0;
                default:
                    return false;
            }
        }
        
        private int compareNumeric(Object variableValue, String value) {
            try {
                double var = Double.parseDouble(String.valueOf(variableValue));
                double val = Double.parseDouble(value);
                return Double.compare(var, val);
            } catch (NumberFormatException e) {
                return String.valueOf(variableValue).compareTo(value);
            }
        }
    }
    
    /**
     * 正则表达式评估器
     */
    private static class RegexEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String variable = context.getConditionValue("variable");
            String pattern = context.getConditionValue("pattern");
            String target = context.getConditionValue("target");
            
            if (variable == null || pattern == null) {
                return ConditionResult.notMatched("正则表达式条件参数不完整");
            }
            
            Object variableValue = context.getVariableValue(variable);
            if (variableValue == null) {
                return ConditionResult.notMatched("变量值为空");
            }
            
            String variableStr = String.valueOf(variableValue);
            boolean result = Pattern.matches(pattern, variableStr);
            
            Map<String, Object> details = new HashMap<>();
            details.put("variable", variable);
            details.put("variableValue", variableStr);
            details.put("pattern", pattern);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("variable") || !config.containsKey("pattern")) {
                throw new IllegalArgumentException("正则表达式条件必须配置 'variable' 和 'pattern' 参数");
            }
            
            // 验证正则表达式
            String pattern = String.valueOf(config.get("pattern"));
            try {
                Pattern.compile(pattern);
            } catch (Exception e) {
                throw new IllegalArgumentException("无效的正则表达式: " + pattern);
            }
        }
    }
    
    /**
     * 范围评估器
     */
    private static class RangeEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String variable = context.getConditionValue("variable");
            String minValue = context.getConditionValue("min");
            String maxValue = context.getConditionValue("max");
            String target = context.getConditionValue("target");
            
            if (variable == null || (minValue == null && maxValue == null)) {
                return ConditionResult.notMatched("范围条件参数不完整");
            }
            
            Object variableValue = context.getVariableValue(variable);
            if (variableValue == null) {
                return ConditionResult.notMatched("变量值为空");
            }
            
            boolean result = isInRange(variableValue, minValue, maxValue);
            
            Map<String, Object> details = new HashMap<>();
            details.put("variable", variable);
            details.put("variableValue", variableValue);
            details.put("min", minValue);
            details.put("max", maxValue);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("variable")) {
                throw new IllegalArgumentException("范围条件必须配置 'variable' 参数");
            }
            if (!config.containsKey("min") && !config.containsKey("max")) {
                throw new IllegalArgumentException("范围条件必须配置 'min' 或 'max' 参数");
            }
        }
        
        private boolean isInRange(Object variableValue, String minValue, String maxValue) {
            try {
                double var = Double.parseDouble(String.valueOf(variableValue));
                
                if (minValue != null) {
                    double min = Double.parseDouble(minValue);
                    if (var < min) {
                        return false;
                    }
                }
                
                if (maxValue != null) {
                    double max = Double.parseDouble(maxValue);
                    if (var > max) {
                        return false;
                    }
                }
                
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    /**
     * 包含评估器
     */
    private static class ContainsEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String variable = context.getConditionValue("variable");
            String value = context.getConditionValue("value");
            String target = context.getConditionValue("target");
            
            if (variable == null || value == null) {
                return ConditionResult.notMatched("包含条件参数不完整");
            }
            
            Object variableValue = context.getVariableValue(variable);
            if (variableValue == null) {
                return ConditionResult.notMatched("变量值为空");
            }
            
            String variableStr = String.valueOf(variableValue);
            boolean result = variableStr.contains(value);
            
            Map<String, Object> details = new HashMap<>();
            details.put("variable", variable);
            details.put("variableValue", variableStr);
            details.put("value", value);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("variable") || !config.containsKey("value")) {
                throw new IllegalArgumentException("包含条件必须配置 'variable' 和 'value' 参数");
            }
        }
    }
    
    /**
     * 空值评估器
     */
    private static class NullEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String variable = context.getConditionValue("variable");
            String checkType = context.getConditionValue("checkType"); // null, notNull, empty, notEmpty
            String target = context.getConditionValue("target");
            
            if (variable == null || checkType == null) {
                return ConditionResult.notMatched("空值条件参数不完整");
            }
            
            Object variableValue = context.getVariableValue(variable);
            boolean result = performNullCheck(variableValue, checkType);
            
            Map<String, Object> details = new HashMap<>();
            details.put("variable", variable);
            details.put("variableValue", variableValue);
            details.put("checkType", checkType);
            details.put("result", result);
            
            return result ? ConditionResult.matched(target, details) : ConditionResult.notMatched();
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            if (!config.containsKey("variable") || !config.containsKey("checkType")) {
                throw new IllegalArgumentException("空值条件必须配置 'variable' 和 'checkType' 参数");
            }
        }
        
        private boolean performNullCheck(Object variableValue, String checkType) {
            switch (checkType.toLowerCase()) {
                case "null":
                    return variableValue == null;
                case "notnull":
                    return variableValue != null;
                case "empty":
                    return variableValue == null || String.valueOf(variableValue).trim().isEmpty();
                case "notempty":
                    return variableValue != null && !String.valueOf(variableValue).trim().isEmpty();
                default:
                    return false;
            }
        }
    }
    
    /**
     * 默认评估器
     */
    private static class DefaultEvaluator implements ConditionEvaluator {
        @Override
        public ConditionResult evaluate(ConditionContext context) throws Exception {
            String target = context.getConditionValue("target");
            
            // 默认评估器总是返回匹配
            Map<String, Object> details = new HashMap<>();
            details.put("message", "默认条件评估");
            
            return ConditionResult.matched(target, details);
        }
        
        @Override
        public void validateConfiguration(Map<String, Object> config) throws Exception {
            // 默认评估器不需要特殊配置验证
        }
    }
}