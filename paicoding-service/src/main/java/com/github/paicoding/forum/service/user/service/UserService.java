package com.github.paicoding.forum.service.user.service;

import com.github.paicoding.forum.api.model.vo.user.UserInfoSaveReq;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.SimpleUserInfoDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.UserStatisticInfoDTO;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.repository.entity.UserInfoDO;
import java.util.Collection;
import java.util.List;

public interface UserService {
    List<SimpleUserInfoDTO> searchUser(String userName);
    void saveUserInfo(UserInfoSaveReq req);
    BaseUserInfoDTO getAndUpdateUserIpInfoBySessionId(String session, String clientIp);
    SimpleUserInfoDTO querySimpleUserInfo(Long userId);
    BaseUserInfoDTO queryBasicUserInfo(Long userId);
    List<SimpleUserInfoDTO> batchQuerySimpleUserInfo(Collection<Long> userIds);
    List<BaseUserInfoDTO> batchQueryBasicUserInfo(Collection<Long> userIds);
    UserStatisticInfoDTO queryUserInfoWithStatistic(Long userId);
    Long getUserCount();
    void bindUserInfo(UserPwdLoginReq loginReq);
    BaseUserInfoDTO queryUserByLoginName(String uname);
    UserDO getUserDO(Long userId);
    UserInfoDO getUserInfo(Long userId);
}
