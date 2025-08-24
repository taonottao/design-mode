package com.tao.builder;

import java.sql.SQLType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 17:57
 */
public class SQL {

    private SQL() {
    }


    public static FromStage select(String... columns) {
        return new SelectBuilder(columns);
    }

    public static class SelectBuilder implements FromStage, SelectAndDeleteWhereStage,BuildStage{
        private String[] columns; // 列
        private String table; //  表
        private String where; // 条件
        public SelectBuilder (String[] columns) {
            this.columns = columns;
        }
        @Override
        public SelectAndDeleteWhereStage from(String table) {
            this.table = table;
            return this;
        }
        @Override
        public BuildStage where(String where) {
            this.where = where;
            return this;
        }
        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(String.join(", ", columns));
            sql.append(" FROM ").append(table);
            if (where != null) {
                sql.append(" WHERE ").append(where);
            }
            return sql.toString();
        }
    }

    interface TableStage {
        UpdateWhereStage table(String table);
    }
    interface InsertTableStage {
        ColumnsStage table(String table);
    }
    interface FromStage {
        SelectAndDeleteWhereStage from(String table);
    }

    interface UpdateWhereStage {
        SetterStage where(String where);
    }

    interface SelectAndDeleteWhereStage {
        BuildStage where(String where);
    }

    interface BuildStage {
        String buildSql();
    }

    interface ColumnsStage {
        BuildStage columns(String[]  columns, String[] values);
    }

    interface SetterStage extends BuildStage{
        SetterStage set(String col, String value);
    }


    public static class UpdateBuilder implements TableStage, UpdateWhereStage, SetterStage{
        private String table; //  表
        private String where; // 条件
        private Map<String, String> setMap = new LinkedHashMap<>(); // 设置

        @Override
        public UpdateBuilder table(String table) {
            this.table = table;
            return this;
        }
        @Override
        public UpdateBuilder set(String col, String value) {
            setMap.put(col, value);
            return this;
        }
        @Override
        public UpdateBuilder where(String where) {
            this.where = where;
            return this;
        }
        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(table).append(" SET ");

            String setString = setMap.entrySet().stream().map(entry -> {
                return entry.getKey() + " = " + entry.getValue();
            }).collect(Collectors.joining(", "));

            sql.append(setString);

            if (where != null) {
                sql.append(" WHERE ").append(where);
            }
            return sql.toString();
        }
    }

    public static TableStage update(){
        return new UpdateBuilder();
    }

    public static FromStage delete(){
        return new DeleteBuilder();
    }

    public static ColumnsStage insert(String table){
        return new InsertBuilder(table);
    }

    public static class InsertBuilder implements InsertTableStage, BuildStage, ColumnsStage{

        private String table;

        private String[] columns;

        private String[] values;
        
        public InsertBuilder(String table) {
            this.table = table;
        }


        @Override
        public BuildStage columns(String[] columns, String[] values) {
            this.columns = columns;
            this.values = values;
            return this;
        }

        @Override
        public ColumnsStage table(String table) {
            this.table = table;
            return this;
        }

        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(table);
            sql.append("(").append(String.join(", ", columns)).append(")");
            sql.append(" VALUES(").append(String.join(", ", values)).append(")");
            return sql.toString();
        }
    }

    public static class DeleteBuilder implements FromStage, BuildStage, SelectAndDeleteWhereStage{

        private String table;

        private String where;

        @Override
        public SelectAndDeleteWhereStage from(String table) {
            this.table = table;
            return this;
        }

        @Override
        public BuildStage where(String where) {
            this.where = where;
            return this;
        }

        @Override
        public String buildSql() {
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(table);
            if (where != null) {
                sql.append(" WHERE ").append(where);
            }
            return sql.toString();
        }
    }

}
