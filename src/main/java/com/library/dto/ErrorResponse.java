package com.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized API error response")
public record ErrorResponse(
        @Schema(description = "Timestamp when the error occurred")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error description")
        String message,

        @Schema(description = "Request URI path that triggered the error")
        String path,

        @Schema(description = "Field-level validation errors, if applicable")
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status, error, message, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status, error, message, path, validationErrors);
    }
}

