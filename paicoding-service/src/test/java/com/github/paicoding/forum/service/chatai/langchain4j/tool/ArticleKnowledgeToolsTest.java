package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.PushStatusEnum;
import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.article.repository.dao.ArticleTagDao;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDetailDO;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleKnowledgeToolsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ArticleDao articleDao;
    private ArticleTagDao articleTagDao;
    private SafeToolExecutor executor;
    private ArticleKnowledgeTools tools;

    @BeforeEach
    void setUp() {
        articleDao = mock(ArticleDao.class);
        articleTagDao = mock(ArticleTagDao.class);
        LangChain4jProperties properties = new LangChain4jProperties();
        executor = new SafeToolExecutor(objectMapper, properties,
                new ToolMetrics(new SimpleMeterRegistry()), Executors.newSingleThreadExecutor());
        tools = new ArticleKnowledgeTools(articleDao, articleTagDao, executor, properties);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void searchReturnsStructuredSuccessAndEmptyResults() throws Exception {
        ArticleDO article = article(10L, "Spring timeout guide");
        when(articleDao.listSimpleArticlesByBySearchKey("timeout")).thenReturn(Collections.singletonList(article));
        JsonNode success = objectMapper.readTree(tools.searchPublishedArticles("server-memory", " timeout "));
        assertTrue(success.get("success").asBoolean());
        assertEquals(10L, success.at("/data/0/articleId").asLong());

        when(articleDao.listSimpleArticlesByBySearchKey("missing")).thenReturn(Collections.emptyList());
        JsonNode empty = objectMapper.readTree(tools.searchPublishedArticles("server-memory", "missing"));
        assertTrue(empty.get("success").asBoolean());
        assertEquals(0, empty.get("data").size());
    }

    @Test
    void oversizedKeywordIsRejectedWithoutCallingDatabase() throws Exception {
        String oversized = String.join("", Collections.nCopies(81, "x"));
        JsonNode response = objectMapper.readTree(tools.searchPublishedArticles("server-memory", oversized));
        assertFalse(response.get("success").asBoolean());
        assertEquals("INVALID_ARGUMENT", response.get("errorCode").asText());
        verify(articleDao, never()).listSimpleArticlesByBySearchKey(anyString());
    }

    @Test
    void invalidArticleIdAndRepositoryExceptionBecomeSafeJson() throws Exception {
        JsonNode invalid = objectMapper.readTree(tools.getPublishedArticle("server-memory", 0L));
        assertEquals("INVALID_ARGUMENT", invalid.get("errorCode").asText());

        when(articleDao.listSimpleArticlesByBySearchKey("boom")).thenThrow(new IllegalStateException("secret-db-detail"));
        String json = tools.searchPublishedArticles("server-memory", "boom");
        JsonNode failed = objectMapper.readTree(json);
        assertEquals("TOOL_EXECUTION_ERROR", failed.get("errorCode").asText());
        assertFalse(json.contains("secret-db-detail"));
    }

    @Test
    void readsPublishedArticleAndTruncatesLongContent() throws Exception {
        ArticleDO article = article(11L, "Published article");
        article.setStatus(PushStatusEnum.ONLINE.getCode());
        ArticleDetailDO detail = new ArticleDetailDO();
        detail.setContent(String.join("", Collections.nCopies(6100, "a")));
        when(articleDao.getPublishedArticle(11L)).thenReturn(article);
        when(articleDao.findLatestDetail(11L)).thenReturn(detail);
        when(articleTagDao.queryArticleTagDetails(11L)).thenReturn(Collections.emptyList());

        JsonNode response = objectMapper.readTree(tools.getPublishedArticle("server-memory", 11L));
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.at("/data/contentTruncated").asBoolean());
        assertEquals(6000, response.at("/data/content").asText().length());
    }

    @Test
    void filtersPublishedArticlesByTagAndRejectsAmbiguousFilter() throws Exception {
        when(articleTagDao.listArticleIdsByTagId(3L, 50)).thenReturn(Arrays.asList(10L, 11L));
        when(articleDao.listPublishedArticles(null, Arrays.asList(10L, 11L), 5))
                .thenReturn(Collections.singletonList(article(10L, "Tagged article")));
        JsonNode success = objectMapper.readTree(
                tools.findPublishedArticlesByCategoryOrTag("server-memory", null, 3L));
        assertTrue(success.get("success").asBoolean());
        assertEquals(10L, success.at("/data/0/articleId").asLong());

        JsonNode invalid = objectMapper.readTree(
                tools.findPublishedArticlesByCategoryOrTag("server-memory", 2L, 3L));
        assertFalse(invalid.get("success").asBoolean());
        assertEquals("INVALID_ARGUMENT", invalid.get("errorCode").asText());
    }

    private ArticleDO article(Long id, String title) {
        ArticleDO article = new ArticleDO();
        article.setId(id);
        article.setTitle(title);
        article.setSummary("summary");
        article.setCategoryId(2L);
        return article;
    }
}
