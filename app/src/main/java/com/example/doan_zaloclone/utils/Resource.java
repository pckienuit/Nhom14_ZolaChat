package com.example.doan_zaloclone.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic wrapper class for handling data states in MVVM architecture
 * Provides unified approach to representing Loading, Success, and Error states
 * 
 * @param <T> The type of data being wrapped
 */
public class Resource<T> {
    
    @NonNull
    private final Status status;
    
    @Nullable
    private final T data;
    
    @Nullable
    private final String message;
    
    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }
    
    /**
     * Create a LOADING state resource
     */
    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }
    
    /**
     * Create a LOADING state resource with existing data (for refresh scenarios)
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }
    
    /**
     * Create a SUCCESS state resource with data
     */
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }
    
    /**
     * Create an ERROR state resource with error message
     */
    public static <T> Resource<T> error(@NonNull String message) {
        return new Resource<>(Status.ERROR, null, message);
    }
    
    /**
     * Create an ERROR state resource with error message and existing data
     */
    public static <T> Resource<T> error(@NonNull String message, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, message);
    }
    
    @NonNull
    public Status getStatus() {
        return status;
    }
    
    @Nullable
    public T getData() {
        return data;
    }
    
    @Nullable
    public String getMessage() {
        return message;
    }
    
    public boolean isLoading() {
        return status == Status.LOADING;
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    /**
     * Enum representing the state of the resource
     */
    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }
}
