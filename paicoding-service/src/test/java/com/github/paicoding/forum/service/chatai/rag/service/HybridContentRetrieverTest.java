package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.service.chatai.rag.model.RagSearchResult;
import com.github.paicoding.forum.service.chatai.rag.store.MysqlArticleEmbeddingStore;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridContentRetrieverTest {
    @Test
    void returnedContentMetadataCanOnlyReferenceRetrievedChunk() {
        KnowledgeRagService service = mock(KnowledgeRagService.class);
        RagSearchResult hit = new RagSearchResult();
        hit.setArticleId(12L); hit.setChunkIndex(3); hit.setTitle("Redis 指南");
        hit.setHeading("缓存穿透"); hit.setContent("使用布隆过滤器"); hit.setScore(0.91D);
        when(service.search("缓存穿透")).thenReturn(Collections.singletonList(hit));

        List<Content> contents = new HybridContentRetriever(service).retrieve(Query.from("缓存穿透"));

        assertEquals(1, contents.size());
        assertEquals(12L, contents.get(0).textSegment().metadata().getLong(MysqlArticleEmbeddingStore.ARTICLE_ID));
        assertEquals(3, contents.get(0).textSegment().metadata().getInteger(MysqlArticleEmbeddingStore.CHUNK_INDEX));
        assertEquals("缓存穿透", contents.get(0).textSegment().metadata().getString(MysqlArticleEmbeddingStore.ARTICLE_HEADING));
    }
}
