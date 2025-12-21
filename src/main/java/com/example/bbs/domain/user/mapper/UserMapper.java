package com.example.bbs.domain.user.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.bbs.domain.user.dto.UserDto;
import com.example.bbs.domain.user.entity.UserEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

	@Mapping(target = "profile.nickname", source = "nickname")
	@Mapping(target = "profile.imageUrl", source = "profileImageUrl")
	UserDto toDto(UserEntity entity);

	List<UserDto> toDtoList(List<UserEntity> entities);
}
