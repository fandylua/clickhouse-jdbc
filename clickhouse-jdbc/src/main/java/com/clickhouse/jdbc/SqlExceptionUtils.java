package com.clickhouse.jdbc;

import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import com.clickhouse.client.ClickHouseException;

/**
 * Helper class for building {@link SQLException}.
 */
public final class SqlExceptionUtils {
    public static final String SQL_STATE_CLIENT_ERROR = "HY000";
    public static final String SQL_STATE_CONNECTION_EXCEPTION = "08000";
    public static final String SQL_STATE_SQL_ERROR = "07000";
    public static final String SQL_STATE_NO_DATA = "02000";
    public static final String SQL_STATE_INVALID_SCHEMA = "3F000";
    public static final String SQL_STATE_INVALID_TX_STATE = "25000";
    public static final String SQL_STATE_DATA_EXCEPTION = "22000";
    public static final String SQL_STATE_FEATURE_NOT_SUPPORTED = "0A000";

    private SqlExceptionUtils() {
    }

    // https://en.wikipedia.org/wiki/SQLSTATE
    private static String toSqlState(ClickHouseException e) {
        String sqlState;
        if (e.getErrorCode() == ClickHouseException.ERROR_NETWORK
                || e.getErrorCode() == ClickHouseException.ERROR_POCO) {
            sqlState = SQL_STATE_CONNECTION_EXCEPTION;
        } else if (e.getErrorCode() == 0) {
            sqlState = e.getCause() instanceof ConnectException ? SQL_STATE_CONNECTION_EXCEPTION
                    : SQL_STATE_CLIENT_ERROR;
        } else {
            sqlState = e.getCause() instanceof ConnectException ? SQL_STATE_CONNECTION_EXCEPTION : SQL_STATE_SQL_ERROR;
        }

        return sqlState;
    }

    public static SQLException clientError(String message) {
        return new SQLException(message, SQL_STATE_CLIENT_ERROR, null);
    }

    public static SQLException clientError(Throwable e) {
        return e != null ? new SQLException(e.getMessage(), SQL_STATE_CLIENT_ERROR, e) : unknownError();
    }

    public static SQLException clientError(String message, Throwable e) {
        return new SQLException(message, SQL_STATE_CLIENT_ERROR, e);
    }

    public static SQLException handle(ClickHouseException e) {
        return e != null ? new SQLException(e.getMessage(), toSqlState(e), e.getErrorCode(), e.getCause())
                : unknownError();
    }

    public static SQLException handle(Throwable e) {
        if (e == null) {
            return unknownError();
        } else if (e instanceof ClickHouseException) {
            return handle((ClickHouseException) e);
        } else if (e instanceof SQLException) {
            return (SQLException) e;
        }

        Throwable cause = e.getCause();
        if (cause instanceof ClickHouseException) {
            return handle((ClickHouseException) cause);
        } else if (e instanceof SQLException) {
            return (SQLException) cause;
        } else if (cause == null) {
            cause = e;
        }

        return new SQLException(cause);
    }

    public static SQLException forCancellation(Exception e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            cause = e;
        }

        // operation canceled
        return new SQLException(e.getMessage(), "HY008", ClickHouseException.ERROR_ABORTED, cause);
    }

    public static SQLFeatureNotSupportedException unsupportedError(String message) {
        return new SQLFeatureNotSupportedException(message, SQL_STATE_FEATURE_NOT_SUPPORTED);
    }

    public static SQLException unknownError() {
        return new SQLException("Unknown error", SQL_STATE_CLIENT_ERROR);
    }
}
