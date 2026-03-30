package com.smartstock.convert;

import com.smartstock.entity.User;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;
import org.springframework.stereotype.Component;

@Component
public class UserStructMapper {

    public UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public LoginVO toLoginVO(User user, String token, Long expiresIn) {
        if (user == null) {
            return null;
        }
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setToken(token);
        vo.setExpiresIn(expiresIn);
        return vo;
    }
}
