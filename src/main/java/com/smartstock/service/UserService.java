package com.smartstock.service;

import com.smartstock.dto.UserLoginDTO;
import com.smartstock.dto.UserRegisterDTO;
import com.smartstock.dto.UserUpdateDTO;
import com.smartstock.dto.PasswordChangeDTO;
import com.smartstock.dto.PhoneLoginDTO;
import com.smartstock.dto.PhoneRegisterDTO;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;

public interface UserService {

    UserVO register(UserRegisterDTO dto);

    UserVO registerByPhone(PhoneRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    LoginVO loginByPhone(PhoneLoginDTO dto);

    void logout(String token);

    UserVO getUserById(Long userId);

    UserVO updateUser(Long userId, UserUpdateDTO dto);

    void changePassword(Long userId, PasswordChangeDTO dto);
}
