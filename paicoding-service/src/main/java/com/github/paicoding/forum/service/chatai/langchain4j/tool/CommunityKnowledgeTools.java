package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommunityKnowledgeTools {
    private final ArticleDao articleDao;

    public CommunityKnowledgeTools(ArticleDao articleDao) {
        this.articleDao = articleDao;
    }

    @Tool("按关键词搜索 CodeMate 站内已发布技术文章。仅用于检索公开文章标题和 ID，不执行任何写操作。")
    public String searchPublishedArticles(@P("技术关键词，最多 80 个字符") String keyword) {
        String safeKeyword = StringUtils.left(StringUtils.trimToEmpty(keyword), 80);
        if (StringUtils.isBlank(safeKeyword)) {
            return "关键词为空，未执行检索";
        }
        List<ArticleDO> articles = articleDao.listSimpleArticlesByBySearchKey(safeKeyword);
        if (articles.isEmpty()) {
            return "未找到匹配的站内文章";
        }
        return articles.stream().limit(5)
                .map(article -> "文章#" + article.getId() + "《" + article.getTitle() + "》")
                .collect(Collectors.joining("\n"));
    }
}
