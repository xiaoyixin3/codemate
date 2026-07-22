package com.github.paicoding.forum.service.chatai.rag.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.chatai.rag.config.RagProperties;
import com.github.paicoding.forum.service.chatai.rag.repository.dao.AiKnowledgeChunkDao;
import com.github.paicoding.forum.service.chatai.rag.repository.entity.AiKnowledgeChunkDO;
import com.github.paicoding.forum.service.chatai.rag.service.VectorSimilarity;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MysqlArticleEmbeddingStore implements EmbeddingStore<TextSegment> {
    public static final String ARTICLE_ID = "articleId";
    public static final String ARTICLE_TITLE = "articleTitle";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String ARTICLE_HEADING = "articleHeading";

    private final AiKnowledgeChunkDao chunkDao;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public MysqlArticleEmbeddingStore(AiKnowledgeChunkDao chunkDao,
                                      RagProperties properties,
                                      ObjectMapper objectMapper) {
        this.chunkDao = chunkDao;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String add(Embedding embedding) {
        throw readOnly();
    }

    @Override
    public void add(String id, Embedding embedding) {
        throw readOnly();
    }

    @Override
    public String add(Embedding embedding, TextSegment embedded) {
        throw readOnly();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        throw readOnly();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        double[] query = request.queryEmbedding().vectorAsDoubleArray();
        int candidateLimit = Math.max(request.maxResults(), Math.min(properties.getMaxCandidateChunks(), 10000));
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (AiKnowledgeChunkDO chunk : chunkDao.listCandidates(properties.getEmbeddingModel(),
                properties.getIndexVersion(), candidateLimit)) {
            Embedding embedding = readEmbedding(chunk.getEmbedding());
            double score = VectorSimilarity.cosine(query, embedding.vectorAsDoubleArray());
            if (score >= request.minScore()) {
                Metadata metadata = new Metadata()
                        .put(ARTICLE_ID, chunk.getArticleId())
                        .put(ARTICLE_TITLE, chunk.getTitle())
                        .put(CHUNK_INDEX, chunk.getChunkIndex());
                TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
                matches.add(new EmbeddingMatch<>(score, String.valueOf(chunk.getId()), embedding, segment));
            }
        }
        List<EmbeddingMatch<TextSegment>> topMatches = matches.stream()
                .sorted(Comparator.comparingDouble((EmbeddingMatch<TextSegment> match) -> match.score()).reversed())
                .limit(request.maxResults())
                .collect(Collectors.toList());
        return new EmbeddingSearchResult<>(topMatches);
    }

    private Embedding readEmbedding(String value) {
        try {
            double[] doubles = objectMapper.readValue(value, double[].class);
            float[] floats = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                floats[i] = (float) doubles[i];
            }
            return Embedding.from(floats);
        } catch (Exception e) {
            throw new IllegalStateException("知识库向量数据损坏", e);
        }
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("文章向量必须通过 KnowledgeRagService 批量索引");
    }
}
