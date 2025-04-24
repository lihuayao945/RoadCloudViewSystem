package com.RoadCloudVisualizationSystem.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private String status;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>("success", null, data);
    }

    public static <T> Result<T> success(String msg) {
        return new Result<>("success", msg, null);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>("success", msg, data);
    }

    public static <T> Result<T> success() {
        return new Result<>("success", null, null);
    }

    public static Result<Void> fail(String msg) {
        return new Result<>("fail", msg, null);
    }
}