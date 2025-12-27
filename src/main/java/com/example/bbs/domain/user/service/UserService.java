package com.example.bbs.domain.user.service;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.bbs.domain.user.dto.User;
import com.example.bbs.domain.user.mapper.UserMapper;
import com.example.bbs.domain.user.repository.UserRepository;
import com.example.bbs.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@NullMarked
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public User findById(String id) {
		return userMapper.toDto(userRepository.findById(id).orElseThrow(
			() -> new BusinessException.NotFoundException("사용자", id)));
	}
}
