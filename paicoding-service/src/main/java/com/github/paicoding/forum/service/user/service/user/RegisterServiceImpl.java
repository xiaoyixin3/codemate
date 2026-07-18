package com.github.paicoding.forum.service.user.service.user;

import com.github.paicoding.forum.api.model.enums.NotifyTypeEnum;
import com.github.paicoding.forum.api.model.enums.user.LoginTypeEnum;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.notify.NotifyMsgEvent;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.core.util.SpringUtil;
import com.github.paicoding.forum.core.util.TransactionUtil;
import com.github.paicoding.forum.service.user.converter.UserAiConverter;
import com.github.paicoding.forum.service.user.repository.dao.UserAiDao;
import com.github.paicoding.forum.service.user.repository.dao.UserDao;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.repository.entity.UserInfoDO;
import com.github.paicoding.forum.service.user.service.RegisterService;
import com.github.paicoding.forum.service.user.service.help.UserPasswordService;
import com.github.paicoding.forum.service.user.service.help.UserRandomGenHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterServiceImpl implements RegisterService {
    @Autowired private UserPasswordService userPasswordService;
    @Autowired private UserDao userDao;
    @Autowired private UserAiDao userAiDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerSystemUser(String loginUser, String nickUser, String avatar) {
        UserDO existing = userDao.getUserByUserName(loginUser);
        if (existing != null) return existing.getId();
        UserDO user = new UserDO();
        user.setUserName(loginUser);
        user.setLoginType(LoginTypeEnum.USER_PWD.getType());
        userDao.saveUser(user);
        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(user.getId());
        userInfo.setUserName(nickUser);
        userInfo.setPhoto(avatar);
        userDao.save(userInfo);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerByUserNameAndPassword(UserPwdLoginReq loginReq) {
        if (!userPasswordService.isValid(loginReq.getPassword())) throw ExceptionUtil.of(StatusEnum.USER_PWD_INVALID);
        if (userDao.getUserByUserName(loginReq.getUsername()) != null) throw ExceptionUtil.of(StatusEnum.USER_LOGIN_NAME_REPEAT, loginReq.getUsername());
        UserDO user = new UserDO();
        user.setUserName(loginReq.getUsername());
        user.setPassword(userPasswordService.encode(loginReq.getPassword()));
        user.setLoginType(LoginTypeEnum.USER_PWD.getType());
        userDao.saveUser(user);
        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(user.getId());
        userInfo.setUserName(StringUtils.isNotBlank(loginReq.getDisplayName()) ? loginReq.getDisplayName() : loginReq.getUsername());
        userInfo.setPhoto(StringUtils.isNotBlank(loginReq.getAvatar()) ? loginReq.getAvatar() : UserRandomGenHelper.genAvatar());
        userDao.save(userInfo);
        userAiDao.save(UserAiConverter.initAi(user.getId()));
        processAfterUserRegister(user.getId());
        return user.getId();
    }

    private void processAfterUserRegister(Long userId) {
        TransactionUtil.registryAfterCommitOrImmediatelyRun(() -> SpringUtil.publishEvent(new NotifyMsgEvent<>(this, NotifyTypeEnum.REGISTER, userId)));
    }
}
