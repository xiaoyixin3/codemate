package com.github.paicoding.forum.service.user.service.user;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.article.dto.YearArticleDTO;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.user.UserInfoSaveReq;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.SimpleUserInfoDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.UserStatisticInfoDTO;
import com.github.paicoding.forum.core.util.IpUtil;
import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.statistics.service.CountService;
import com.github.paicoding.forum.service.user.converter.UserConverter;
import com.github.paicoding.forum.service.user.repository.dao.UserDao;
import com.github.paicoding.forum.service.user.repository.dao.UserRelationDao;
import com.github.paicoding.forum.service.user.repository.entity.IpInfo;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.repository.entity.UserInfoDO;
import com.github.paicoding.forum.service.user.repository.entity.UserRelationDO;
import com.github.paicoding.forum.service.user.service.UserAiService;
import com.github.paicoding.forum.service.user.service.UserService;
import com.github.paicoding.forum.service.user.service.help.UserPasswordService;
import com.github.paicoding.forum.service.user.service.help.UserSessionHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired private UserDao userDao;
    @Autowired private UserRelationDao userRelationDao;
    @Autowired private CountService countService;
    @Autowired private ArticleDao articleDao;
    @Autowired private UserSessionHelper userSessionHelper;
    @Autowired private UserPasswordService userPasswordService;
    @Autowired private UserAiService userAiService;

    public List<SimpleUserInfoDTO> searchUser(String userName) {
        List<UserInfoDO> users = userDao.getByUserNameLike(userName);
        return CollectionUtils.isEmpty(users) ? Collections.emptyList() : users.stream().map(UserConverter::toSimpleInfo).collect(Collectors.toList());
    }
    public void saveUserInfo(UserInfoSaveReq req) { userDao.updateUserInfo(UserConverter.toDO(req)); }
    public BaseUserInfoDTO getAndUpdateUserIpInfoBySessionId(String session, String clientIp) {
        if (StringUtils.isBlank(session)) return null;
        Long userId = userSessionHelper.getUserIdBySession(session);
        if (userId == null) return null;
        UserInfoDO user = userDao.getByUserId(userId);
        if (user == null) { userSessionHelper.removeSession(session); return null; }
        IpInfo ip = user.getIp();
        if (clientIp != null && !Objects.equals(ip.getLatestIp(), clientIp)) {
            ip.setLatestIp(clientIp); ip.setLatestRegion(IpUtil.getLocationByIp(clientIp).toRegionStr());
            if (ip.getFirstIp() == null) { ip.setFirstIp(clientIp); ip.setFirstRegion(ip.getLatestRegion()); }
            userDao.updateById(user);
        }
        return UserConverter.toDTO(user);
    }
    public SimpleUserInfoDTO querySimpleUserInfo(Long userId) {
        UserInfoDO user = userDao.getByUserId(userId);
        if (user == null) throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userId=" + userId);
        return UserConverter.toSimpleInfo(user);
    }
    public BaseUserInfoDTO queryBasicUserInfo(Long userId) {
        UserInfoDO user = userDao.getByUserId(userId);
        if (user == null) throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userId=" + userId);
        return UserConverter.toDTO(user);
    }
    public List<SimpleUserInfoDTO> batchQuerySimpleUserInfo(Collection<Long> ids) {
        List<UserInfoDO> users = userDao.getByUserIds(ids);
        return CollectionUtils.isEmpty(users) ? Collections.emptyList() : users.stream().map(UserConverter::toSimpleInfo).collect(Collectors.toList());
    }
    public List<BaseUserInfoDTO> batchQueryBasicUserInfo(Collection<Long> ids) {
        List<UserInfoDO> users = userDao.getByUserIds(ids);
        if (CollectionUtils.isEmpty(users)) throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userId=" + ids);
        return users.stream().map(UserConverter::toDTO).collect(Collectors.toList());
    }
    public UserStatisticInfoDTO queryUserInfoWithStatistic(Long userId) {
        UserStatisticInfoDTO result = UserConverter.toUserHomeDTO(countService.queryUserStatisticInfo(userId), queryBasicUserInfo(userId));
        int complete = (StringUtils.isNotBlank(result.getCompany()) ? 1 : 0) + (StringUtils.isNotBlank(result.getPosition()) ? 1 : 0) + (StringUtils.isNotBlank(result.getProfile()) ? 1 : 0);
        result.setInfoPercent(complete * 100 / 3);
        Long viewer = ReqInfoContext.getReqInfo().getUserId();
        UserRelationDO relation = viewer == null ? null : userRelationDao.getUserRelationByUserId(userId, viewer);
        result.setFollowed(relation != null);
        result.setJoinDayCount(Math.max(1, (int) ((System.currentTimeMillis() - result.getCreateTime().getTime()) / 86400000)));
        result.setYearArticleList(articleDao.listYearArticleByUserId(userId));
        return result;
    }
    public Long getUserCount() { return userDao.getUserCount(); }
    @Transactional(rollbackFor = Exception.class)
    public void bindUserInfo(UserPwdLoginReq req) {
        if (!userPasswordService.isValid(req.getPassword())) throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        UserDO user = userDao.getUserByUserName(req.getUsername());
        if (user == null) {
            user = new UserDO();
            user.setId(req.getUserId());
        }
        else if (!Objects.equals(req.getUserId(), user.getId())) throw ExceptionUtil.of(StatusEnum.USER_LOGIN_NAME_REPEAT, req.getUsername());
        user.setUserName(req.getUsername()); user.setPassword(userPasswordService.encode(req.getPassword())); userDao.saveUser(user);
        userAiService.initOrUpdateAiInfo(new UserPwdLoginReq().setUserId(req.getUserId()));
    }
    public BaseUserInfoDTO queryUserByLoginName(String uname) { UserDO user = userDao.getUserByUserName(uname); return user == null ? null : queryBasicUserInfo(user.getId()); }
    public UserDO getUserDO(Long userId) { return userDao.getUserByUserId(userId); }
    public UserInfoDO getUserInfo(Long userId) { return userDao.getByUserId(userId); }
}
