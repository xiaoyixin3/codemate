package com.github.paicoding.forum.service.user.service;

import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;

public interface RegisterService {
    Long registerSystemUser(String loginUser, String nickUser, String avatar);

    Long registerByUserNameAndPassword(UserPwdLoginReq loginReq);
}
