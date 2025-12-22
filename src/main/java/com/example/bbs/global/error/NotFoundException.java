package com.example.bbs.global.error;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외. (HTTP 404 대응)
 */
@NullMarked
public class NotFoundException extends BusinessException {

	@Nullable
	private final String resourceType;

	public NotFoundException() {
		super(ErrorCode.NOT_FOUND);
		this.resourceType = null;
	}

	public NotFoundException(String resourceType) {
		super(ErrorCode.NOT_FOUND, resourceType + "을(를) 찾을 수 없습니다.");
		this.resourceType = resourceType;
	}

	public NotFoundException(String resourceType, String id) {
		super(ErrorCode.NOT_FOUND, resourceType + " (ID: " + id + ")을(를) 찾을 수 없습니다.");
		this.resourceType = resourceType;
	}

	@Nullable
	public String getResourceType() {
		return resourceType;
	}
}
