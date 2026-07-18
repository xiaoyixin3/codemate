package com.github.paicoding.forum.service.chatai.rag.config;

import com.github.paicoding.forum.service.chatai.rag.store.MysqlArticleEmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jRagConfig {
    @Bean("codeMateContentRetriever")
    public ContentRetriever codeMateContentRetriever(MysqlArticleEmbeddingStore embeddingStore,
                                                     @Qualifier("codeMateEmbeddingModel") EmbeddingModel embeddingModel,
                                                     RagProperties properties) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(Math.max(1, Math.min(properties.getTopK(), 10)))
                .minScore(properties.getMinScore())
                .displayName("CodeMate MySQL Article Knowledge Base")
                .build();
    }
}
