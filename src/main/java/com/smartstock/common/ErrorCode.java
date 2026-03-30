package com.smartstock.common;

public class ErrorCode {

    // HTTP 标准错误码
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    // 用户相关业务错误码
    public static final int USER_NOT_FOUND = 1001;
    public static final int USER_ALREADY_EXISTS = 1002;

    // 股票相关错误码
    public static final int STOCK_NOT_FOUND = 2001;

    // 交易相关错误码
    public static final int INSUFFICIENT_FUNDS = 3001;
    public static final int INSUFFICIENT_POSITION = 3002;
    public static final int INVALID_QUANTITY = 3003;
    public static final int PRICE_LIMIT = 3004;

    // 自选股相关错误码
    public static final int WATCHLIST_FULL = 4001;
    public static final int WATCHLIST_ALREADY_EXISTS = 4002;

    private ErrorCode() {
        // Utility class, no instantiation
    }
}
