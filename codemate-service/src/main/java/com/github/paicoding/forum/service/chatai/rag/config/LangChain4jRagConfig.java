package com.github.paicoding.forum.service.chatai.rag.config;

import com.github.paicoding.forum.service.chatai.rag.service.HybridContentRetriever;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jRagConfig {
    @Bean("codeMateContentRetriever")
    public ContentRetriever codeMateContentRetriever(HybridContentRetriever retriever) {
        return retriever;
    }
}
