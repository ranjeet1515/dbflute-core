/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.dbflute.logic.doc.policycheck;

import java.util.List;
import java.util.Map;

import org.apache.torque.engine.database.model.Column;
import org.apache.torque.engine.database.model.Table;

/**
 * @author jflute
 * @since 1.1.2 (2016/12/29 Thursday at higashi-ginza)
 */
public class DfSchemaPolicyColumnStatement {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final DfSchemaPolicyMiscSecretary _secretary = new DfSchemaPolicyMiscSecretary();

    // ===================================================================================
    //                                                                    Column Statement
    //                                                                    ================
    public void checkColumnStatement(Table table, Map<String, Object> columnMap, DfSchemaPolicyResult result) {
        final List<Column> columnList = table.getColumnList();
        for (Column column : columnList) {
            processColumnStatement(column, columnMap, result);
        }
    }

    protected void processColumnStatement(Column column, Map<String, Object> columnMap, DfSchemaPolicyResult result) {
        @SuppressWarnings("unchecked")
        final List<String> statementList = (List<String>) columnMap.get("statementList");
        if (statementList != null) {
            for (String statement : statementList) {
                evaluateColumnIfClause(column, statement, result, _secretary.extractIfClause(statement));
            }
        }
    }

    // ===================================================================================
    //                                                                            Evaluate
    //                                                                            ========
    // -----------------------------------------------------
    //                                             If Clause
    //                                             ---------
    // e.g.
    //  if columnName is suffix:_FLG then notNull
    //  if columnName is suffix:_FLG then dbType is integer
    protected void evaluateColumnIfClause(Column column, String statement, DfSchemaPolicyResult result, DfSchemaPolicyIfClause ifClause) {
        final String ifItem = ifClause.getIfItem();
        final String ifValue = ifClause.getIfValue();
        final boolean notIfValue = ifClause.isNotIfValue();
        if (ifItem.equalsIgnoreCase("columnName")) { // if columnName is ...
            if (isHitColumn(toColumnName(column), ifValue) == !notIfValue) {
                evaluateColumnThenClause(column, statement, result, ifClause);
            }
        } else if (ifItem.equalsIgnoreCase("alias")) { // if alias is ...
            if (isHitColumn(column.getAlias(), ifValue) == !notIfValue) {
                evaluateColumnThenClause(column, statement, result, ifClause);
            }
        } else if (ifItem.equalsIgnoreCase("dbType")) {// if dbType is ...
            if (column.hasDbType()) { // just in case
                if (isHitExp(column.getDbType(), ifValue) == !notIfValue) {
                    evaluateColumnThenClause(column, statement, result, ifClause);
                }
            }
        } else {
            throwSchemaPolicyCheckIllegalIfThenStatementException(statement, "Unknown if-item: " + ifItem);
        }
    }

    // -----------------------------------------------------
    //                                           Then Clause
    //                                           -----------
    protected void evaluateColumnThenClause(Column column, String statement, DfSchemaPolicyResult result, DfSchemaPolicyIfClause ifClause) {
        final String policy = toPolicy(ifClause);
        final String thenClause = ifClause.getThenClause();
        if (ifClause.getThenItem() != null) { // e.g. dbType is integer
            evaluateColumnThenItemValue(column, statement, result, ifClause);
        } else {
            final boolean notThenClause = ifClause.isNotThenClause();
            final String notOr = notThenClause ? "not " : "";
            if (thenClause.equalsIgnoreCase("bad") == !notThenClause) {
                result.addViolation(policy, "The column is no good: " + toColumnDisp(column));
            } else if (thenClause.equalsIgnoreCase("notNull")) {
                if (!column.isNotNull() == !notThenClause) {
                    result.addViolation(policy, "The column should " + notOr + "be not-null: " + toColumnDisp(column));
                }
            } else {
                throwSchemaPolicyCheckIllegalIfThenStatementException(statement, "Unknown then-clause: " + thenClause);
            }
        }
    }

    protected void evaluateColumnThenItemValue(Column column, String statement, DfSchemaPolicyResult result,
            DfSchemaPolicyIfClause ifClause) {
        final String policy = toPolicy(ifClause);
        final String thenItem = ifClause.getThenItem();
        final String thenValue = ifClause.getThenValue();
        final boolean notThenValue = ifClause.isNotThenValue();
        final String notOr = notThenValue ? "not " : "";
        if (thenItem.equalsIgnoreCase("columnName")) { // e.g. columnName is suffix:_ID
            final String columnName = toColumnName(column);
            if (!isHitExp(columnName, thenValue) == !notThenValue) {
                result.addViolation(policy,
                        "The column name should " + notOr + "be " + thenValue + " but " + columnName + ": " + toColumnDisp(column));
            }
        } else if (thenItem.equalsIgnoreCase("alias")) { // e.g. alias is suffix:ID
            if (column.hasAlias()) {
                final String alias = column.getAlias();
                if (!isHitExp(alias, thenValue) == !notThenValue) {
                    result.addViolation(policy,
                            "The column alias should " + notOr + "be " + thenValue + " but " + alias + ": " + toColumnDisp(column));
                }
            }
        } else if (thenItem.equalsIgnoreCase("dbType")) { // e.g. dbType is integer
            if (column.hasDbType()) {
                final String dbType = column.getDbType();
                if (!isHitExp(dbType, thenValue) == !notThenValue) {
                    result.addViolation(policy,
                            "The column db-type should " + notOr + "be " + thenValue + " but " + dbType + ": " + toColumnDisp(column));
                }
            }
        } else if (thenItem.equalsIgnoreCase("size")) { // e.g. size is 200
            if (column.hasColumnSize()) {
                final String size = column.getColumnSize(); // String expression #for_now
                if (!isHitExp(size, thenValue) == !notThenValue) {
                    result.addViolation(policy,
                            "The column size should " + notOr + "be " + thenValue + " but " + size + ": " + toColumnDisp(column));
                }
            }
        } else if (thenItem.equalsIgnoreCase("comment")) { // e.g. comment is contain:SEA
            if (column.hasAlias()) {
                final String comment = column.getComment();
                if (!isHitExp(comment, thenValue) == !notThenValue) {
                    result.addViolation(policy,
                            "The column comment should " + notOr + "be " + thenValue + " but " + comment + ": " + toColumnDisp(column));
                }
            }
        } else {
            throwSchemaPolicyCheckIllegalIfThenStatementException(statement, "Unknown then-item: " + thenItem);
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isHitColumn(String columnName, String hint) {
        return _secretary.isHitColumn(columnName, hint);
    }

    protected boolean isHitExp(String exp, String hint) {
        return _secretary.isHitExp(exp, hint);
    }

    protected String toColumnName(Column column) {
        return _secretary.toColumnName(column);
    }

    protected String toColumnDisp(Column column) {
        return _secretary.toColumnDisp(column);
    }

    protected String toPolicy(DfSchemaPolicyIfClause ifClause) {
        return "table.statement: " + ifClause.getStatement();
    }

    // ===================================================================================
    //                                                                           Exception
    //                                                                           =========
    protected void throwSchemaPolicyCheckUnknownThemeException(String theme, String targetType) {
        _secretary.throwSchemaPolicyCheckUnknownThemeException(theme, targetType);
    }

    protected void throwSchemaPolicyCheckUnknownPropertyException(String property) {
        _secretary.throwSchemaPolicyCheckUnknownPropertyException(property);
    }

    protected void throwSchemaPolicyCheckIllegalIfThenStatementException(String statement, String additional) {
        _secretary.throwSchemaPolicyCheckIllegalIfThenStatementException(statement, additional);
    }
}
