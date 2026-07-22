package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.api.model.enums.ArticleEventEnum;
import com.github.paicoding.forum.api.model.event.ArticleMsgEvent;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ArticleRagIndexListener {
    private final KnowledgeRagService ragService;

    public ArticleRagIndexListener(KnowledgeRagService ragService) {
        this.ragService = ragService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onArticleChanged(ArticleMsgEvent<?> event) {
        if (!(event.getContent() instanceof ArticleDO)) return;
        Long articleId = ((ArticleDO) event.getContent()).getId();
        try {
            if (event.getType() == ArticleEventEnum.ONLINE) {
                if (ragService.isAvailable()) ragService.indexArticle(articleId);
            } else if (event.getType() == ArticleEventEnum.OFFLINE || event.getType() == ArticleEventEnum.DELETE
                    || event.getType() == ArticleEventEnum.REVIEW) {
                ragService.removeArticleIndex(articleId);
            }
        } catch (RuntimeException e) {
            log.warn("Incremental RAG index update failed, articleId={}, event={}", articleId, event.getType(), e);
        }
    }
}
