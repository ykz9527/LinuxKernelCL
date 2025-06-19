package com.cs.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常处理器
 * 统一处理应用中的异常，返回标准化的错误响应
 * 
 * @author YK
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("参数校验失败: {} {}", request.getMethod(), request.getRequestURI(), ex);
        
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        StringBuilder errorMsg = new StringBuilder("参数校验失败: ");
        
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError error = fieldErrors.get(i);
            errorMsg.append(error.getField()).append(" ").append(error.getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                errorMsg.append(", ");
            }
        }
        
        return Result.error(ResultCode.PARAM_INVALID.getCode(), errorMsg.toString());
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler({BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException ex, HttpServletRequest request) {
        logger.warn("数据绑定异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        StringBuilder errorMsg = new StringBuilder("参数绑定失败: ");
        
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError error = fieldErrors.get(i);
            errorMsg.append(error.getField()).append(" ").append(error.getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                errorMsg.append(", ");
            }
        }
        
        return Result.error(ResultCode.PARAM_INVALID.getCode(), errorMsg.toString());
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Object> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        logger.warn("请求方法不支持: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), "请求方法 " + ex.getMethod() + " 不支持");
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Object> handleNotFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        logger.warn("资源不存在: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在: " + request.getRequestURI());
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.error("运行时异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleException(Exception ex, HttpServletRequest request) {
        logger.error("系统异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "系统异常，请联系管理员");
    }
} 