package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.dto.PasswordChangeDTO;
import com.smartstock.dto.PhoneLoginDTO;
import com.smartstock.dto.PhoneRegisterDTO;
import com.smartstock.dto.UserLoginDTO;
import com.smartstock.dto.UserRegisterDTO;
import com.smartstock.dto.UserUpdateDTO;
import com.smartstock.convert.UserStructMapper;
import com.smartstock.entity.Account;
import com.smartstock.entity.User;
import com.smartstock.config.JwtProperties;
import com.smartstock.mapper.AccountMapper;
import com.smartstock.mapper.UserMapper;
import com.smartstock.service.UserService;
import com.smartstock.util.JwtUtil;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserStructMapper userStructMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        String normalizedNickname = normalizeNickname(dto.getNickname());

        // 检查邮箱是否已注册
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }

        // 密码加密
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 创建用户
        User user = User.builder()
                .username(normalizedEmail)
                .email(normalizedEmail)
                .password(encodedPassword)
                .nickname(normalizedNickname)
                .status(1)
                .deleted(0)
                .build();
        userMapper.insert(user);

        createInitialAccount(user.getId());

        log.info("User registered: userId={}, email={}", user.getId(), user.getEmail());
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO registerByPhone(PhoneRegisterDTO dto) {
        String normalizedPhone = normalizePhone(dto.getPhone());
        String normalizedNickname = normalizeNickname(dto.getNickname());

        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, normalizedPhone));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已注册");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        User user = User.builder()
                .username(normalizedPhone)
                .email(normalizedPhone + "@phone.smartstock.local")
                .phone(normalizedPhone)
                .password(encodedPassword)
                .nickname(normalizedNickname)
                .status(1)
                .deleted(0)
                .build();
        userMapper.insert(user);
        createInitialAccount(user.getId());

        log.info("User registered by phone: userId={}, phone={}", user.getId(), user.getPhone());
        return toVO(user);
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已被禁用");
        }
        log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());
        return toLoginVO(user);
    }

    @Override
    public LoginVO loginByPhone(PhoneLoginDTO dto) {
        String normalizedPhone = normalizePhone(dto.getPhone());
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, normalizedPhone));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已被禁用");
        }
        log.info("User logged in by phone: userId={}, phone={}", user.getId(), user.getPhone());
        return toLoginVO(user);
    }

    @Override
    public void logout(String token) {
        jwtUtil.invalidateToken(token);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        if (dto.getNickname() != null) {
            if (!StringUtils.hasText(dto.getNickname())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
            }
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(StringUtils.hasText(dto.getAvatar()) ? dto.getAvatar().trim() : null);
        }

        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    private void createInitialAccount(Long userId) {
        Account account = Account.builder()
                .userId(userId)
                .totalAssets(new BigDecimal("1000000"))
                .availableCash(new BigDecimal("1000000"))
                .frozenCash(BigDecimal.ZERO)
                .positionValue(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
        accountMapper.insert(account);
    }

    private UserVO toVO(User user) {
        return userStructMapper.toUserVO(user);
    }

    private LoginVO toLoginVO(User user) {
        return userStructMapper.toLoginVO(
                user,
                jwtUtil.generateToken(user.getId(), user.getEmail()),
                jwtProperties.getExpirationSeconds()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private String normalizeNickname(String nickname) {
        return nickname == null ? null : nickname.trim();
    }
}
