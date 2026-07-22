package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.api.model.enums.ArticleEventEnum;
import com.github.paicoding.forum.api.model.event.ArticleMsgEvent;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleRagIndexListenerTest {
    @Test
    void onlineArticleTriggersIncrementalIndex() {
        KnowledgeRagService service = mock(KnowledgeRagService.class);
        when(service.isAvailable()).thenReturn(true);
        ArticleDO article = new ArticleDO(); article.setId(9L);

        new ArticleRagIndexListener(service).onArticleChanged(
                new ArticleMsgEvent<>(this, ArticleEventEnum.ONLINE, article));

        verify(service).indexArticle(9L);
    }

    @Test
    void offlineArticleRemovesIndexEvenWhenEmbeddingIsUnavailable() {
        KnowledgeRagService service = mock(KnowledgeRagService.class);
        ArticleDO article = new ArticleDO(); article.setId(9L);

        new ArticleRagIndexListener(service).onArticleChanged(
                new ArticleMsgEvent<>(this, ArticleEventEnum.OFFLINE, article));

        verify(service).removeArticleIndex(9L);
    }
}
