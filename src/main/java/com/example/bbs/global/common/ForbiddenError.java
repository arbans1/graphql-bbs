package com.example.bbs.global.common;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ForbiddenError(String message, String code) implements MutationResult {
}
