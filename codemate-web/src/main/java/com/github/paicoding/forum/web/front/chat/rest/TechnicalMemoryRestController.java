package com.github.paicoding.forum.web.front.chat.rest;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemoryCreateReq;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemorySaveReq;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemoryVo;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.chatai.memory.TechnicalMemoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Permission(role = UserRole.LOGIN)
@RequestMapping("/chat/api/memories")
public class TechnicalMemoryRestController {
    private final TechnicalMemoryService memoryService;

    public TechnicalMemoryRestController(TechnicalMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ResVo<List<TechnicalMemoryVo>> list() {
        return ResVo.ok(memoryService.list(currentUserId()));
    }

    @PostMapping
    public ResVo<TechnicalMemoryVo> create(@RequestBody TechnicalMemoryCreateReq request) {
        return ResVo.ok(memoryService.create(currentUserId(), request));
    }

    @PutMapping("/{memoryId}")
    public ResVo<TechnicalMemoryVo> update(@PathVariable Long memoryId,
                                           @RequestBody TechnicalMemorySaveReq request) {
        return ResVo.ok(memoryService.update(currentUserId(), memoryId, request));
    }

    @DeleteMapping("/{memoryId}")
    public ResVo<Boolean> delete(@PathVariable Long memoryId) {
        memoryService.delete(currentUserId(), memoryId);
        return ResVo.ok(true);
    }

    private Long currentUserId() {
        return ReqInfoContext.getReqInfo().getUserId();
    }
}
