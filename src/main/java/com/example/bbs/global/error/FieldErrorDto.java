package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record FieldErrorDto(
	String field,
	String message,
	@Nullable String code) {
}
