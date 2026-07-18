package com.github.paicoding.forum.service.user.converter;

import com.github.paicoding.forum.service.user.repository.entity.UserAiDO;

/**
 * AI 用户记录转换。
 */
public class UserAiConverter {

    public static UserAiDO initAi(Long userId) {
        UserAiDO userAiDO = new UserAiDO();
        userAiDO.setUserId(userId);
        userAiDO.setDeleted(0);
        return userAiDO;
    }
}
