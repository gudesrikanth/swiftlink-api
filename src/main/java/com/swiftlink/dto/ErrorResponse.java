package com.swiftlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(description = "HTTP status code") int status,
        @Schema(description = "Error code") String error,
        @Schema(description = "Human-readable message") String message,
        @Schema(description = "Request path") String path,
        @Schema(description = "Timestamp of the error") Instant timestamp,
        @Schema(description = "Field-level validation errors") List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ErrorResponse withFieldErrors(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(status, error, message, path, Instant.now(), fieldErrors);
    }
}
