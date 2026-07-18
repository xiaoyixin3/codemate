package com.github.paicoding.forum.service.user.service;

import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;

public interface LoginService {
    String SESSION_KEY = "f-session";
    String USER_DEVICE_KEY = "f-device";

    void logout(String session);

    String loginByUserPwd(String username, String password);

    String registerByUserPwd(UserPwdLoginReq loginReq);
}
