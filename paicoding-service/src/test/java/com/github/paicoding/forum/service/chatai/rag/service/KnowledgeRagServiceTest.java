package com.github.paicoding.forum.service.chatai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.PushStatusEnum;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.article.repository.dao.ArticleTagDao;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDetailDO;
import com.github.paicoding.forum.service.article.service.CategoryService;
import com.github.paicoding.forum.service.chatai.rag.config.RagProperties;
import com.github.paicoding.forum.service.chatai.rag.model.RagSearchResult;
import com.github.paicoding.forum.service.chatai.rag.observability.RagMetrics;
import com.github.paicoding.forum.service.chatai.rag.repository.dao.AiKnowledgeChunkDao;
import com.github.paicoding.forum.service.chatai.rag.repository.entity.AiKnowledgeChunkDO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeRagServiceTest {
    private RagProperties properties;
    private EmbeddingModel embeddingModel;
    private AiKnowledgeChunkDao chunkDao;
    private ArticleDao articleDao;
    private ArticleTagDao articleTagDao;
    private CategoryService categoryService;
    private KnowledgeRagService service;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-only");
        properties.setTopK(5);
        properties.setVectorCandidateChunks(20);
        properties.setKeywordCandidateChunks(20);
        embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embedAll(any())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            List<Embedding> embeddings = new ArrayList<>();
            for (TextSegment segment : segments) embeddings.add(vectorFor(segment.text()));
            return Response.from(embeddings);
        });
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> Response.from(vectorFor(invocation.getArgument(0))));
        chunkDao = mock(AiKnowledgeChunkDao.class);
        articleDao = mock(ArticleDao.class);
        articleTagDao = mock(ArticleTagDao.class);
        categoryService = mock(CategoryService.class);
        service = new KnowledgeRagService(properties, embeddingModel, new RagChunker(), chunkDao,
                articleDao, articleTagDao, categoryService, new ObjectMapper(), mock(RagMetrics.class));
    }

    @Test
    void hybridSearchCombinesVectorKeywordAndRankingReasons() throws Exception {
        AiKnowledgeChunkDO spring = chunk(1L, "Spring 事务", "事务传播", "Spring transaction", new double[]{1, 0});
        AiKnowledgeChunkDO redis = chunk(2L, "Redis 缓存", "缓存穿透", "Redis 缓存穿透解决方案", new double[]{0, 1});
        when(chunkDao.listCandidates(anyString(), anyString(), anyInt())).thenReturn(java.util.Arrays.asList(spring, redis));
        when(chunkDao.listKeywordCandidates(anyString(), anyString(), any(), anyInt())).thenReturn(Collections.singletonList(redis));

        List<RagSearchResult> results = service.search("Redis 缓存", 5);

        assertEquals(2L, results.get(0).getArticleId());
        assertTrue(results.get(0).getVectorScore() > 0.9D);
        assertTrue(results.get(0).getKeywordScore() > 0D);
        assertTrue(results.get(0).getRankingReasons().contains("keyword-match"));
    }

    @Test
    void unchangedChunkHashSkipsSecondEmbeddingRequest() {
        ArticleDO article = new ArticleDO();
        article.setId(9L); article.setTitle("Redis 指南"); article.setCategoryId(2L);
        article.setStatus(PushStatusEnum.ONLINE.getCode()); article.setDeleted(YesOrNoEnum.NO.getCode());
        article.setUpdateTime(new Date());
        ArticleDetailDO detail = new ArticleDetailDO();
        detail.setContent("# 缓存穿透\n使用布隆过滤器和空值缓存。");
        when(articleDao.getById(9L)).thenReturn(article);
        when(articleDao.findLatestDetail(9L)).thenReturn(detail);
        when(categoryService.queryCategoryName(2L)).thenReturn("后端");
        when(articleTagDao.queryArticleTagDetails(9L)).thenReturn(Collections.emptyList());
        when(chunkDao.listArticleChunks(any(), anyString(), anyString())).thenReturn(Collections.emptyList());
        List<AiKnowledgeChunkDO> persisted = new ArrayList<>();
        doAnswer(invocation -> { persisted.clear(); persisted.addAll(invocation.getArgument(1)); return null; })
                .when(chunkDao).replaceArticleChunks(any(), any());

        int first = service.indexArticle(9L);
        when(chunkDao.listArticleChunks(any(), anyString(), anyString())).thenReturn(persisted);
        int second = service.indexArticle(9L);

        assertTrue(first > 0);
        assertEquals(0, second);
        verify(embeddingModel, times(1)).embedAll(any());
        assertEquals(properties.getIndexVersion(), persisted.get(0).getIndexVersion());
        assertEquals(2, persisted.get(0).getEmbeddingDimension());
    }

    @Test
    void emptyHybridRetrievalBuildsExplicitNoEvidencePrompt() {
        when(chunkDao.listCandidates(anyString(), anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(chunkDao.listKeywordCandidates(anyString(), anyString(), any(), anyInt())).thenReturn(Collections.emptyList());

        String prompt = service.buildPrompt("没有资料的问题");

        assertTrue(prompt.contains("NO_EVIDENCE"));
        assertTrue(prompt.contains("必须拒绝给出确定性答案"));
    }

    private AiKnowledgeChunkDO chunk(Long articleId, String title, String heading, String content, double[] vector) throws Exception {
        AiKnowledgeChunkDO chunk = new AiKnowledgeChunkDO();
        chunk.setId(articleId); chunk.setArticleId(articleId); chunk.setChunkIndex(0); chunk.setTitle(title);
        chunk.setHeading(heading); chunk.setContent(content); chunk.setContentType("TEXT"); chunk.setTags(heading);
        chunk.setCategory("后端"); chunk.setEmbedding(new ObjectMapper().writeValueAsString(vector));
        chunk.setArticleUpdatedAt(new Date());
        return chunk;
    }

    private Embedding vectorFor(String text) {
        return text != null && text.toLowerCase().contains("redis")
                ? Embedding.from(new float[]{0F, 1F}) : Embedding.from(new float[]{1F, 0F});
    }
}
