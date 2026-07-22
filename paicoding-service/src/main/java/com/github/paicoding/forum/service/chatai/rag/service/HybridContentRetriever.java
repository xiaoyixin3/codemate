package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.service.chatai.rag.model.RagSearchResult;
import com.github.paicoding.forum.service.chatai.rag.store.MysqlArticleEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HybridContentRetriever implements ContentRetriever {
    private final KnowledgeRagService ragService;

    public HybridContentRetriever(KnowledgeRagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public List<Content> retrieve(Query query) {
        return ragService.search(query.text()).stream().map(this::toContent).collect(Collectors.toList());
    }

    private Content toContent(RagSearchResult result) {
        Metadata metadata = new Metadata()
                .put(MysqlArticleEmbeddingStore.ARTICLE_ID, result.getArticleId())
                .put(MysqlArticleEmbeddingStore.ARTICLE_TITLE, result.getTitle())
                .put(MysqlArticleEmbeddingStore.CHUNK_INDEX, result.getChunkIndex())
                .put(MysqlArticleEmbeddingStore.ARTICLE_HEADING, result.getHeading());
        return Content.from(TextSegment.from(result.getContent(), metadata),
                Collections.singletonMap(ContentMetadata.SCORE, result.getScore()));
    }
}
