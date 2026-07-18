package com.github.paicoding.forum.service.user.service.ai;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.service.chatai.bot.AiBots;
import com.github.paicoding.forum.service.user.converter.UserAiConverter;
import com.github.paicoding.forum.service.user.repository.dao.UserAiDao;
import com.github.paicoding.forum.service.user.repository.dao.UserAiHistoryDao;
import com.github.paicoding.forum.service.user.repository.entity.UserAiDO;
import com.github.paicoding.forum.service.user.repository.entity.UserAiHistoryDO;
import com.github.paicoding.forum.service.user.service.UserAiService;
import com.github.paicoding.forum.service.user.service.conf.AiConfig;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserAiServiceImpl implements UserAiService {
    @Resource
    private UserAiHistoryDao userAiHistoryDao;

    @Resource
    private UserAiDao userAiDao;

    @Resource
    private AiConfig aiConfig;

    @Resource
    private AiBots aiBots;

    @Override
    public void pushChatItem(AISourceEnum source, Long user, ChatItemVo item) {
        UserAiHistoryDO userAiHistoryDO = new UserAiHistoryDO();
        userAiHistoryDO.setAiType(source.getCode());
        userAiHistoryDO.setUserId(user);
        userAiHistoryDO.setQuestion(item.getQuestion());
        userAiHistoryDO.setAnswer(item.getAnswer());
        userAiHistoryDO.setChatId(ReqInfoContext.getReqInfo().getChatId());
        userAiHistoryDao.save(userAiHistoryDO);
    }

    /**
     * 获取用户的最大使用次数
     *
     * @param userId
     * @return
     */
    public int getMaxChatCnt(Long userId) {
        // 对于系统AI机器人，不进行次数限制
        if (aiBots.aiBots(userId)) {
            return Integer.MAX_VALUE;
        }

        userAiDao.getOrInitAiInfo(userId);
        return aiConfig.getMaxNum().getBasic();
    }

    @Override
    public void initOrUpdateAiInfo(UserPwdLoginReq loginReq) {
        Long userId = loginReq.getUserId();
        UserAiDO userAiDO = userAiDao.getByUserId(userId);
        if (userAiDO == null) {
            userAiDao.save(UserAiConverter.initAi(userId));
        }
    }
}
