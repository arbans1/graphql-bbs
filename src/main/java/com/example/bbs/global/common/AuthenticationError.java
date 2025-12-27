package com.example.bbs.global.common;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AuthenticationError(String message, String code) implements MutationResult {
}
