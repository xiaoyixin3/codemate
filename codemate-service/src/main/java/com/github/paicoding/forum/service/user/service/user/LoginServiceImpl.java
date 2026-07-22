package com.github.paicoding.forum.service.user.service.user;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.service.user.repository.dao.UserDao;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.service.LoginService;
import com.github.paicoding.forum.service.user.service.RegisterService;
import com.github.paicoding.forum.service.user.service.UserAiService;
import com.github.paicoding.forum.service.user.service.UserService;
import com.github.paicoding.forum.service.user.service.help.UserPasswordService;
import com.github.paicoding.forum.service.user.service.help.UserSessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Autowired private UserDao userDao;
    @Autowired private UserSessionHelper userSessionHelper;
    @Autowired private RegisterService registerService;
    @Autowired private UserPasswordService userPasswordService;
    @Autowired private UserService userService;
    @Autowired private UserAiService userAiService;

    @Override
    public void logout(String session) {
        userSessionHelper.removeSession(session);
    }

    @Override
    public String loginByUserPwd(String username, String password) {
        UserDO user = userDao.getUserByUserName(username);
        if (user == null) throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userName=" + username);
        if (!userPasswordService.matches(password, user.getPassword())) throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        upgradeLegacyPassword(user, password);
        userAiService.initOrUpdateAiInfo(new UserPwdLoginReq().setUserId(user.getId()).setUsername(username));
        ReqInfoContext.getReqInfo().setUserId(user.getId());
        return userSessionHelper.genSession(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerByUserPwd(UserPwdLoginReq loginReq) {
        if (StringUtils.isBlank(loginReq.getUsername()) || StringUtils.isBlank(loginReq.getPassword())) throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        Long userId = ReqInfoContext.getReqInfo().getUserId();
        loginReq.setUserId(userId);
        if (userId != null) {
            userService.bindUserInfo(loginReq);
            return ReqInfoContext.getReqInfo().getSession();
        }
        UserDO user = userDao.getUserByUserName(loginReq.getUsername());
        if (user != null) {
            if (!userPasswordService.matches(loginReq.getPassword(), user.getPassword())) throw ExceptionUtil.of(StatusEnum.USER_LOGIN_NAME_REPEAT, loginReq.getUsername());
            upgradeLegacyPassword(user, loginReq.getPassword());
            userId = user.getId();
            userAiService.initOrUpdateAiInfo(new UserPwdLoginReq().setUserId(userId));
        } else {
            userId = registerService.registerByUserNameAndPassword(loginReq);
        }
        ReqInfoContext.getReqInfo().setUserId(userId);
        return userSessionHelper.genSession(userId);
    }

    private void upgradeLegacyPassword(UserDO user, String rawPassword) {
        if (!userPasswordService.isLegacyHash(user.getPassword())) return;
        try {
            userDao.upgradePasswordIfMatch(user.getId(), user.getPassword(), userPasswordService.encode(rawPassword));
        } catch (RuntimeException ex) {
            log.warn("Legacy password migration failed, userId={}", user.getId(), ex);
        }
    }
}
