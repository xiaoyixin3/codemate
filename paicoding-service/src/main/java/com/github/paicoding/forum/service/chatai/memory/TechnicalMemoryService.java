package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.api.model.enums.ai.TechnicalMemoryTypeEnum;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemoryCreateReq;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemorySaveReq;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemoryVo;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.memory.repository.dao.AiUserTechnicalMemoryDao;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiUserTechnicalMemoryDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TechnicalMemoryService {
    private final AiUserTechnicalMemoryDao memoryDao;
    private final SensitiveMemoryGuard sensitiveGuard;
    private final LangChain4jProperties properties;

    public TechnicalMemoryService(AiUserTechnicalMemoryDao memoryDao, SensitiveMemoryGuard sensitiveGuard,
                                  LangChain4jProperties properties) {
        this.memoryDao = memoryDao;
        this.sensitiveGuard = sensitiveGuard;
        this.properties = properties;
    }

    public List<TechnicalMemoryVo> list(Long userId) {
        requireUser(userId);
        return memoryDao.listOwned(userId, properties.getMemoryPreferenceMaxItems()).stream()
                .map(this::toVo).collect(Collectors.toList());
    }

    public List<AiUserTechnicalMemoryDO> listActive(Long userId) {
        requireUser(userId);
        return memoryDao.listActive(userId, properties.getMemoryPreferenceMaxItems());
    }

    @Transactional(rollbackFor = Exception.class)
    public TechnicalMemoryVo create(Long userId, TechnicalMemoryCreateReq request) {
        requireUser(userId);
        sensitiveGuard.validate(request.getContent());
        validateConfidence(request.getConfidence());
        validateExpiry(request.getExpiresAt());
        AiUserTechnicalMemoryDO memory = new AiUserTechnicalMemoryDO();
        memory.setUserId(userId);
        memory.setMemoryType(normalizeType(request.getMemoryType()));
        memory.setContent(request.getContent().trim());
        memory.setSourceType("USER");
        memory.setConfidence(request.getConfidence() == null ? 1.0D : request.getConfidence());
        memory.setExpiresAt(request.getExpiresAt());
        memoryDao.save(memory);
        return toVo(memory);
    }

    @Transactional(rollbackFor = Exception.class)
    public TechnicalMemoryVo update(Long userId, Long memoryId, TechnicalMemorySaveReq request) {
        requireUser(userId);
        sensitiveGuard.validate(request.getContent());
        validateConfidence(request.getConfidence());
        validateExpiry(request.getExpiresAt());
        AiUserTechnicalMemoryDO memory = memoryDao.findOwned(userId, memoryId);
        if (memory == null) throw new IllegalArgumentException("Memory does not exist or is not owned by current user");
        memory.setMemoryType(normalizeType(request.getMemoryType()));
        memory.setContent(request.getContent().trim());
        memory.setConfidence(request.getConfidence() == null ? memory.getConfidence() : request.getConfidence());
        memory.setExpiresAt(request.getExpiresAt());
        memoryDao.updateById(memory);
        return toVo(memory);
    }

    public void delete(Long userId, Long memoryId) {
        requireUser(userId);
        if (memoryId == null || !memoryDao.removeOwned(userId, memoryId)) {
            throw new IllegalArgumentException("Memory does not exist or is not owned by current user");
        }
    }

    public void deleteConversationSources(Long userId, String chatId) {
        memoryDao.removeConversationSources(userId, chatId);
    }

    private String normalizeType(String value) {
        try {
            return TechnicalMemoryTypeEnum.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unsupported technical memory type");
        }
    }

    private void validateExpiry(Date expiresAt) {
        if (expiresAt != null && !expiresAt.after(new Date())) {
            throw new IllegalArgumentException("Memory expiry must be in the future");
        }
    }

    private void validateConfidence(Double confidence) {
        if (confidence != null && (confidence < 0D || confidence > 1D)) {
            throw new IllegalArgumentException("Memory confidence must be between 0 and 1");
        }
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Authenticated user is required");
    }

    private TechnicalMemoryVo toVo(AiUserTechnicalMemoryDO memory) {
        TechnicalMemoryVo vo = new TechnicalMemoryVo();
        vo.setId(memory.getId());
        vo.setMemoryType(memory.getMemoryType());
        vo.setContent(memory.getContent());
        vo.setSourceType(memory.getSourceType());
        vo.setSourceRef(memory.getSourceRef());
        vo.setConfidence(memory.getConfidence());
        vo.setCreateTime(memory.getCreateTime());
        vo.setUpdateTime(memory.getUpdateTime());
        vo.setExpiresAt(memory.getExpiresAt());
        return vo;
    }
}
