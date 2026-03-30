package com.smartstock.service;

import com.smartstock.dto.UserLoginDTO;
import com.smartstock.dto.UserRegisterDTO;
import com.smartstock.dto.UserUpdateDTO;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;

public interface UserService {

    UserVO register(UserRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    void logout(String token);

    UserVO getUserById(Long userId);

    UserVO updateUser(Long userId, UserUpdateDTO dto);
}
