package com.cs.api.common;

/**
 * 响应状态码枚举
 * 
 * @author YK
 * @since 1.0.0
 */
public enum ResultCode {

    // 通用状态码
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 业务错误 1xxx
    PARAM_INVALID(1001, "参数校验失败"),
    
    // 系统错误 2xxx
    SYSTEM_ERROR(2001, "系统内部错误"),
    DATABASE_ERROR(2002, "数据库操作失败"),
    NETWORK_ERROR(2003, "网络连接异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据状态码获取枚举
     */
    public static ResultCode getByCode(Integer code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return null;
    }
} 