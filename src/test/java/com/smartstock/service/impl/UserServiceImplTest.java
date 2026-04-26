package com.smartstock.service.impl;

import com.smartstock.config.JwtProperties;
import com.smartstock.convert.UserStructMapper;
import com.smartstock.dto.PasswordChangeDTO;
import com.smartstock.dto.PhoneLoginDTO;
import com.smartstock.dto.PhoneRegisterDTO;
import com.smartstock.dto.UserLoginDTO;
import com.smartstock.dto.UserRegisterDTO;
import com.smartstock.entity.Account;
import com.smartstock.entity.User;
import com.smartstock.mapper.AccountMapper;
import com.smartstock.mapper.UserMapper;
import com.smartstock.util.JwtUtil;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserStructMapper userStructMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setExpirationSeconds(7200);
        userService = new UserServiceImpl(userMapper, accountMapper, passwordEncoder, jwtUtil, jwtProperties, userStructMapper);
    }

    @Test
    void registerShouldNormalizeEmailBeforePersisting() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setEmail("  Demo@Example.COM ");
        dto.setPassword("password123");
        dto.setNickname("  Demo User  ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(11L);
            return 1;
        }).when(userMapper).insert(userCaptor.capture());
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountMapper.insert(accountCaptor.capture())).thenReturn(1);
        when(userStructMapper.toUserVO(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            UserVO vo = new UserVO();
            vo.setUserId(user.getId());
            vo.setEmail(user.getEmail());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreatedAt(user.getCreatedAt());
            return vo;
        });

        UserVO result = userService.register(dto);

        assertNotNull(result);
        assertEquals("demo@example.com", userCaptor.getValue().getEmail());
        assertEquals("demo@example.com", userCaptor.getValue().getUsername());
        assertEquals("Demo User", userCaptor.getValue().getNickname());
        assertEquals(0, new BigDecimal("1000000").compareTo(accountCaptor.getValue().getTotalAssets()));
    }

    @Test
    void loginShouldNormalizeEmailBeforeLookup() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("  Demo@Example.COM ");
        dto.setPassword("password123");

        User user = User.builder()
                .id(7L)
                .email("demo@example.com")
                .username("demo@example.com")
                .password("encoded-password")
                .nickname("Demo")
                .status(1)
                .build();

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(7L, "demo@example.com")).thenReturn("jwt-token");
        when(userStructMapper.toLoginVO(user, "jwt-token", 7200L)).thenAnswer(invocation -> {
            LoginVO vo = new LoginVO();
            vo.setUserId(user.getId());
            vo.setEmail(user.getEmail());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setToken("jwt-token");
            vo.setExpiresIn(7200L);
            return vo;
        });

        LoginVO result = userService.login(dto);

        assertNotNull(result);
        assertEquals("demo@example.com", result.getEmail());
        assertEquals("jwt-token", result.getToken());
        assertEquals(7200, result.getExpiresIn());
    }

    @Test
    void registerByPhoneShouldPersistPhoneUserAndInitialAccount() {
        PhoneRegisterDTO dto = new PhoneRegisterDTO();
        dto.setPhone(" 13800138000 ");
        dto.setPassword("password123");
        dto.setNickname("  手机用户  ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(12L);
            return 1;
        }).when(userMapper).insert(userCaptor.capture());
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountMapper.insert(accountCaptor.capture())).thenReturn(1);
        when(userStructMapper.toUserVO(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            UserVO vo = new UserVO();
            vo.setUserId(user.getId());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setNickname(user.getNickname());
            return vo;
        });

        UserVO result = userService.registerByPhone(dto);

        assertEquals("13800138000", userCaptor.getValue().getPhone());
        assertEquals("13800138000", userCaptor.getValue().getUsername());
        assertEquals("13800138000@phone.smartstock.local", userCaptor.getValue().getEmail());
        assertEquals("手机用户", result.getNickname());
        assertEquals(0, new BigDecimal("1000000").compareTo(accountCaptor.getValue().getAvailableCash()));
    }

    @Test
    void changePasswordShouldVerifyOldPasswordBeforeUpdating() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("old-password");
        dto.setNewPassword("new-password");
        User user = User.builder()
                .id(7L)
                .email("demo@example.com")
                .password("encoded-old")
                .build();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        userService.changePassword(7L, dto);

        assertEquals("encoded-new", user.getPassword());
    }

    @Test
    void loginByPhoneShouldReturnToken() {
        PhoneLoginDTO dto = new PhoneLoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("password123");
        User user = User.builder()
                .id(8L)
                .email("13800138000@phone.smartstock.local")
                .phone("13800138000")
                .password("encoded-password")
                .nickname("手机用户")
                .status(1)
                .build();

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(8L, "13800138000@phone.smartstock.local")).thenReturn("phone-token");
        when(userStructMapper.toLoginVO(user, "phone-token", 7200L)).thenAnswer(invocation -> {
            LoginVO vo = new LoginVO();
            vo.setUserId(user.getId());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setNickname(user.getNickname());
            vo.setToken("phone-token");
            vo.setExpiresIn(7200L);
            return vo;
        });

        LoginVO result = userService.loginByPhone(dto);

        assertEquals("13800138000", result.getPhone());
        assertEquals("phone-token", result.getToken());
    }
}
