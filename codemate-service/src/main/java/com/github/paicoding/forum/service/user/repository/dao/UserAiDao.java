package com.github.paicoding.forum.service.user.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.service.user.converter.UserAiConverter;
import com.github.paicoding.forum.service.user.repository.entity.UserAiDO;
import com.github.paicoding.forum.service.user.repository.mapper.UserAiMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserAiDao extends ServiceImpl<UserAiMapper, UserAiDO> {

    public UserAiDO getByUserId(Long userId) {
        LambdaQueryWrapper<UserAiDO> query = Wrappers.lambdaQuery();
        query.eq(UserAiDO::getUserId, userId)
                .eq(UserAiDO::getDeleted, YesOrNoEnum.NO.getCode());
        return baseMapper.selectOne(query);
    }

    public UserAiDO getOrInitAiInfo(Long userId) {
        UserAiDO ai = getByUserId(userId);
        if (ai != null) {
            return ai;
        }
        ai = UserAiConverter.initAi(userId);
        save(ai);
        return ai;
    }
}
