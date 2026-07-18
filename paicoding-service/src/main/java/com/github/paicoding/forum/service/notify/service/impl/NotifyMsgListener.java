package com.github.paicoding.forum.service.notify.service.impl;

import com.github.paicoding.forum.api.model.enums.DocumentTypeEnum;
import com.github.paicoding.forum.api.model.enums.NotifyStatEnum;
import com.github.paicoding.forum.api.model.enums.NotifyTypeEnum;
import com.github.paicoding.forum.api.model.vo.notify.NotifyMsgEvent;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.core.util.SpringUtil;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.service.ArticleReadService;
import com.github.paicoding.forum.service.comment.repository.entity.CommentDO;
import com.github.paicoding.forum.service.comment.service.CommentReadService;
import com.github.paicoding.forum.service.notify.repository.dao.NotifyMsgDao;
import com.github.paicoding.forum.service.notify.repository.entity.NotifyMsgDO;
import com.github.paicoding.forum.service.notify.service.NotifyService;
import com.github.paicoding.forum.service.user.repository.entity.UserFootDO;
import com.github.paicoding.forum.service.user.repository.entity.UserRelationDO;
import com.github.paicoding.forum.service.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author YiHui
 * @date 2022/9/3
 */
@Slf4j
@Async
@Service
public class NotifyMsgListener<T> implements ApplicationListener<NotifyMsgEvent<T>> {
    private static final Long ADMIN_ID = 1L;
    private final ArticleReadService articleReadService;

    private final CommentReadService commentReadService;

    private final NotifyMsgDao notifyMsgDao;

    private final NotifyService notifyService;

    private final UserService userService;

    public NotifyMsgListener(ArticleReadService articleReadService,
                             CommentReadService commentReadService,
                             NotifyService notifyService,
                             NotifyMsgDao notifyMsgDao,
                             UserService userService) {
        this.articleReadService = articleReadService;
        this.commentReadService = commentReadService;
        this.notifyService = notifyService;
        this.notifyMsgDao = notifyMsgDao;
        this.userService = userService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onApplicationEvent(NotifyMsgEvent<T> msgEvent) {
        switch (msgEvent.getNotifyType()) {
            case COMMENT:
                saveCommentNotify((NotifyMsgEvent<CommentDO>) msgEvent);
                break;
            case REPLY:
                saveReplyNotify((NotifyMsgEvent<CommentDO>) msgEvent);
                break;
            case PRAISE:
            case COLLECT:
                saveArticleNotify((NotifyMsgEvent<UserFootDO>) msgEvent);
                break;
            case CANCEL_PRAISE:
            case CANCEL_COLLECT:
                removeArticleNotify((NotifyMsgEvent<UserFootDO>) msgEvent);
                break;
            case FOLLOW:
                saveFollowNotify((NotifyMsgEvent<UserRelationDO>) msgEvent);
                break;
            case CANCEL_FOLLOW:
                removeFollowNotify((NotifyMsgEvent<UserRelationDO>) msgEvent);
                break;
            case LOGIN:
                // todo 用户登录，判断是否需要插入新的通知消息，暂时先不做
                break;
            case REGISTER:
                // 首次注册，插入一个欢迎的消息
                saveRegisterSystemNotify((Long) msgEvent.getContent());
                break;
            default:
                // todo 系统消息
        }
    }

    /**
     * 评论 + 回复
     *
     * @param event
     */
    private void saveCommentNotify(NotifyMsgEvent<CommentDO> event) {
        NotifyMsgDO msg = new NotifyMsgDO();
        CommentDO comment = event.getContent();
        ArticleDO article = articleReadService.queryBasicArticle(comment.getArticleId());
        msg.setNotifyUserId(article.getUserId())
                .setOperateUserId(comment.getUserId())
                .setRelatedId(article.getId())
                .setType(event.getNotifyType().getType())
                .setState(NotifyStatEnum.UNREAD.getStat()).setMsg(comment.getContent());
        // 对于评论而言，支持多次评论；因此若之前有也不删除
        notifyMsgDao.save(msg);

        // 消息通知
        notifyService.notifyToUser(msg.getNotifyUserId(), NotifyTypeEnum.COMMENT,
                String.format("您的文章《%s》收到一个新的评论，快去看看吧", article.getTitle()));
    }

    /**
     * 评论回复消息
     *
     * @param event
     */
    private void saveReplyNotify(NotifyMsgEvent<CommentDO> event) {
        NotifyMsgDO msg = new NotifyMsgDO();
        CommentDO comment = event.getContent();
        CommentDO parent = commentReadService.queryComment(comment.getParentCommentId());
        msg.setNotifyUserId(parent.getUserId())
                .setOperateUserId(comment.getUserId())
                .setRelatedId(comment.getArticleId())
                .setCommentId(comment.getId())
                .setType(event.getNotifyType().getType())
                .setState(NotifyStatEnum.UNREAD.getStat()).setMsg(comment.getContent());
        // 回复同样支持多次回复,不做幂等校验
        notifyMsgDao.save(msg);

        // 消息通知
        notifyService.notifyToUser(msg.getNotifyUserId(), NotifyTypeEnum.REPLY,
                String.format("您的评价《%s》收到一个新的回复，快去看看吧", parent.getContent()));
    }

    /**
     * 点赞 + 收藏
     *
     * @param event
     */
    private void saveArticleNotify(NotifyMsgEvent<UserFootDO> event) {
        UserFootDO foot = event.getContent();
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(foot.getDocumentId())
                .setNotifyUserId(foot.getDocumentUserId())
                .setOperateUserId(foot.getUserId())
                .setType(event.getNotifyType().getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg("");
        if (Objects.equals(foot.getDocumentType(), DocumentTypeEnum.COMMENT.getCode())) {
            // 点赞评论时，详情内容中显示评论信息
            CommentDO comment = commentReadService.queryComment(foot.getDocumentId());
            ArticleDO article = articleReadService.queryBasicArticle(comment.getArticleId());
            msg.setMsg(String.format("赞了您在文章 <a href=\"/article/detail/%d\">%s</a> 下的评论 <span style=\"color:darkslategray;font-style: italic;font-size: 0.9em\">%s</span>", article.getId(), article.getTitle(), comment.getContent()));
        }

        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            // 若之前已经有对应的通知，则不重复记录；因为一个用户对一篇文章，可以重复的点赞、取消点赞，但是最终我们只通知一次
            notifyMsgDao.save(msg);
            // 消息通知
            notifyService.notifyToUser(msg.getNotifyUserId(), event.getNotifyType(),
                    String.format("太棒了，您的%s %s数+1!!!",
                            Objects.equals(foot.getDocumentType(), DocumentTypeEnum.ARTICLE.getCode()) ? "文章" : "评论",
                            event.getNotifyType().getMsg()));
        }
    }

    public void saveArticleNotify(UserFootDO foot, NotifyTypeEnum notifyTypeEnum) {
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(foot.getDocumentId())
                .setNotifyUserId(foot.getDocumentUserId())
                .setOperateUserId(foot.getUserId())
                .setType(notifyTypeEnum.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            // 若之前已经有对应的通知，则不重复记录；因为一个用户对一篇文章，可以重复的点赞、取消点赞，但是最终我们只通知一次
            notifyMsgDao.save(msg);
        }
    }

    /**
     * 取消点赞，取消收藏
     *
     * @param event
     */
    private void removeArticleNotify(NotifyMsgEvent<UserFootDO> event) {
        UserFootDO foot = event.getContent();
        NotifyMsgDO msg = new NotifyMsgDO()
                .setRelatedId(foot.getDocumentId())
                .setNotifyUserId(foot.getDocumentUserId())
                .setOperateUserId(foot.getUserId())
                .setType(event.getNotifyType().getType())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record != null) {
            notifyMsgDao.removeById(record.getId());
        }
    }

    /**
     * 关注
     *
     * @param event
     */
    private void saveFollowNotify(NotifyMsgEvent<UserRelationDO> event) {
        UserRelationDO relation = event.getContent();
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(0L)
                .setNotifyUserId(relation.getUserId())
                .setOperateUserId(relation.getFollowUserId())
                .setType(event.getNotifyType().getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            // 若之前已经有对应的通知，则不重复记录；因为用户的关注是一对一的，可以重复的关注、取消，但是最终我们只通知一次
            notifyMsgDao.save(msg);

            notifyService.notifyToUser(msg.getNotifyUserId(), NotifyTypeEnum.FOLLOW, "恭喜您获得一枚新粉丝~");
        }
    }

    /**
     * 取消关注
     *
     * @param event
     */
    private void removeFollowNotify(NotifyMsgEvent<UserRelationDO> event) {
        UserRelationDO relation = event.getContent();
        NotifyMsgDO msg = new NotifyMsgDO()
                .setRelatedId(0L)
                .setNotifyUserId(relation.getUserId())
                .setOperateUserId(relation.getFollowUserId())
                .setType(event.getNotifyType().getType())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record != null) {
            notifyMsgDao.removeById(record.getId());
        }
    }

    private void saveRegisterSystemNotify(Long userId) {
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(0L)
                .setNotifyUserId(userId)
                .setOperateUserId(ADMIN_ID)
                .setType(NotifyTypeEnum.REGISTER.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg(SpringUtil.getConfig("view.site.welcomeInfo"));
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            // 若之前已经有对应的通知，则不重复记录；因为用户的关注是一对一的，可以重复的关注、取消，但是最终我们只通知一次
            notifyMsgDao.save(msg);

            notifyService.notifyToUser(msg.getNotifyUserId(), NotifyTypeEnum.SYSTEM, "您有一个新的系统通知消息，请注意查收");
        }
    }

}
