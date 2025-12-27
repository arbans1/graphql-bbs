package com.example.bbs.domain.user.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.bbs.domain.user.dto.AuthPayload;
import com.example.bbs.domain.user.dto.AuthTokens;
import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.entity.UserEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

	@Mapping(target = "profile.nickname", source = "nickname")
	@Mapping(target = "profile.imageUrl", source = "profileImageUrl")
	User toDto(UserEntity entity);

	List<User> toDtoList(List<UserEntity> entities);

	@Mapping(target = "user", source = "userEntity")
	AuthPayload toAuthPayload(UserEntity userEntity, String accessToken, long expiresIn);

	AuthTokens toAuthTokens(AuthPayload payload, String refreshToken);
}
