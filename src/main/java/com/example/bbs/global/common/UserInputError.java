package com.example.bbs.global.common;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.example.bbs.global.error.FieldError;

@NullMarked
public record UserInputError(
	String message,
	String code,
	List<FieldError> fieldErrors) implements MutationResult {
}
