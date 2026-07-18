package com.github.paicoding.forum.web.admin.rest;

import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.chatai.rag.model.RagSearchResult;
import com.github.paicoding.forum.service.chatai.rag.service.KnowledgeRagService;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Permission(role = UserRole.ADMIN)
@RequestMapping("/api/admin/ai/rag")
@Api(value = "CodeMate RAG 管理", tags = "AI Agent")
public class KnowledgeRagAdminController {
    @Resource
    private KnowledgeRagService knowledgeRagService;
    @Resource
    private LangChain4jProperties langChain4jProperties;
    @Resource
    private CodeMateChatMemoryProvider chatMemoryProvider;

    @GetMapping("/status")
    @ApiOperation("Agent runtime status")
    public ResVo<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", langChain4jProperties.isEnabled());
        status.put("available", langChain4jProperties.isAvailable());
        status.put("model", langChain4jProperties.getChatModel());
        status.put("fallbackEnabled", langChain4jProperties.isFallbackEnabled());
        status.put("activeMemories", chatMemoryProvider.activeMemoryCount());
        return ResVo.ok(status);
    }

    @PostMapping("/index")
    @ApiOperation("为指定已发布文章重建向量索引")
    public ResVo<Integer> index(@RequestParam Long articleId) {
        return ResVo.ok(knowledgeRagService.indexArticle(articleId));
    }

    @PostMapping("/index-all")
    @ApiOperation("为全部已发布文章重建向量索引")
    public ResVo<Integer> indexAll() {
        return ResVo.ok(knowledgeRagService.indexAllOnlineArticles());
    }

    @GetMapping("/search")
    @ApiOperation("调试向量 Top-K 检索结果")
    public ResVo<List<RagSearchResult>> search(@RequestParam String question) {
        return ResVo.ok(knowledgeRagService.search(question));
    }
}
