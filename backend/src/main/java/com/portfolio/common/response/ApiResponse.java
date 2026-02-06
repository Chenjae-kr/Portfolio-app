package com.portfolio.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private Meta meta;
    private ApiError error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(Meta.builder().timestamp(LocalDateTime.now()).build())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> additionalMeta) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(Meta.builder()
                        .timestamp(LocalDateTime.now())
                        .additional(additionalMeta)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .meta(Meta.builder().timestamp(LocalDateTime.now()).build())
                .error(ApiError.builder().code(code).message(message).build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Map<String, Object> details) {
        return ApiResponse.<T>builder()
                .meta(Meta.builder().timestamp(LocalDateTime.now()).build())
                .error(ApiError.builder().code(code).message(message).details(details).build())
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private LocalDateTime timestamp;
        private Map<String, Object> additional;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private String code;
        private String message;
        private Map<String, Object> details;
    }
}
