package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.github.paicoding.forum.api.model.enums.PushStatusEnum;
import com.github.paicoding.forum.api.model.vo.article.dto.TagDTO;
import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.article.repository.dao.ArticleTagDao;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDetailDO;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArticleKnowledgeTools implements CodeMateTool {
    private static final int MAX_RESULTS = 5;
    private final ArticleDao articleDao;
    private final ArticleTagDao articleTagDao;
    private final SafeToolExecutor executor;
    private final LangChain4jProperties properties;

    public ArticleKnowledgeTools(ArticleDao articleDao, ArticleTagDao articleTagDao,
                                 SafeToolExecutor executor, LangChain4jProperties properties) {
        this.articleDao = articleDao;
        this.articleTagDao = articleTagDao;
        this.executor = executor;
        this.properties = properties;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ_ONLY;
    }

    @Tool("按关键词搜索 CodeMate 站内已发布文章，返回可继续读取的文章 ID、标题和摘要。")
    public String searchPublishedArticles(@ToolMemoryId String memoryId,
                                          @P("关键词，1 到 80 个字符") String keyword) {
        return executor.execute(memoryId, "searchPublishedArticles", riskLevel(), ToolAccess.PUBLIC, null,
                StringUtils.trimToEmpty(keyword), () -> {
            String normalized = validateKeyword(keyword);
            List<Map<String, Object>> articles = articleDao.listSimpleArticlesByBySearchKey(normalized).stream()
                    .limit(MAX_RESULTS).map(this::articleSummary).collect(Collectors.toList());
            return ToolResult.success("searchPublishedArticles", riskLevel(), articles,
                    articles.isEmpty() ? "未找到匹配的已发布文章" : "找到 " + articles.size() + " 篇已发布文章");
        });
    }

    @Tool("根据文章 ID 读取一篇 CodeMate 已发布文章的详情。只能读取公开且未删除的文章。")
    public String getPublishedArticle(@ToolMemoryId String memoryId,
                                      @P("正整数文章 ID") Long articleId) {
        return executor.execute(memoryId, "getPublishedArticle", riskLevel(), ToolAccess.PUBLIC, null,
                String.valueOf(articleId), () -> {
            validatePositiveId(articleId, "articleId");
            ArticleDO article = articleDao.getPublishedArticle(articleId);
            if (article == null || article.getStatus() == null || article.getStatus() != PushStatusEnum.ONLINE.getCode()) {
                throw new ToolExecutionException("ARTICLE_NOT_FOUND", "文章不存在或未公开发布");
            }
            ArticleDetailDO detail = articleDao.findLatestDetail(articleId);
            Map<String, Object> data = articleSummary(article);
            data.put("tags", articleTagDao.queryArticleTagDetails(articleId).stream()
                    .map(TagDTO::getTag).collect(Collectors.toList()));
            String content = detail == null ? "" : StringUtils.defaultString(detail.getContent());
            boolean truncated = content.length() > properties.getToolMaxArticleContentChars();
            data.put("content", StringUtils.left(content, properties.getToolMaxArticleContentChars()));
            data.put("contentTruncated", truncated);
            return ToolResult.success("getPublishedArticle", riskLevel(), data,
                    truncated ? "文章详情已读取，正文因长度限制被截断" : "文章详情已读取");
        });
    }

    @Tool("按分类 ID 或标签 ID 检索 CodeMate 已发布文章。categoryId 和 tagId 必须且只能提供一个。")
    public String findPublishedArticlesByCategoryOrTag(
            @ToolMemoryId String memoryId,
            @P("正整数分类 ID；按标签查询时留空") Long categoryId,
            @P("正整数标签 ID；按分类查询时留空") Long tagId) {
        return executor.execute(memoryId, "findPublishedArticlesByCategoryOrTag", riskLevel(), ToolAccess.PUBLIC, null,
                "categoryId=" + categoryId + ",tagId=" + tagId, () -> {
            if ((categoryId == null) == (tagId == null)) {
                throw new ToolExecutionException("INVALID_ARGUMENT", "categoryId 和 tagId 必须且只能提供一个");
            }
            validatePositiveId(categoryId == null ? tagId : categoryId, categoryId == null ? "tagId" : "categoryId");
            List<Long> articleIds = tagId == null ? null : articleTagDao.listArticleIdsByTagId(tagId, 50);
            List<Map<String, Object>> articles = articleDao.listPublishedArticles(categoryId, articleIds, MAX_RESULTS)
                    .stream().map(this::articleSummary).collect(Collectors.toList());
            return ToolResult.success("findPublishedArticlesByCategoryOrTag", riskLevel(), articles,
                    articles.isEmpty() ? "该分类或标签下没有已发布文章" : "找到 " + articles.size() + " 篇已发布文章");
        });
    }

    private String validateKeyword(String keyword) {
        String normalized = StringUtils.trimToEmpty(keyword);
        if (normalized.isEmpty()) {
            throw new ToolExecutionException("INVALID_ARGUMENT", "关键词不能为空");
        }
        if (normalized.length() > properties.getToolMaxKeywordLength()) {
            throw new ToolExecutionException("INVALID_ARGUMENT", "关键词长度不能超过 " + properties.getToolMaxKeywordLength() + " 个字符");
        }
        return normalized;
    }

    private void validatePositiveId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new ToolExecutionException("INVALID_ARGUMENT", name + " 必须是正整数");
        }
    }

    private Map<String, Object> articleSummary(ArticleDO article) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleId", article.getId());
        result.put("title", StringUtils.defaultIfBlank(article.getTitle(), article.getShortTitle()));
        result.put("summary", StringUtils.left(StringUtils.defaultString(article.getSummary()), 300));
        result.put("categoryId", article.getCategoryId());
        return result;
    }
}
