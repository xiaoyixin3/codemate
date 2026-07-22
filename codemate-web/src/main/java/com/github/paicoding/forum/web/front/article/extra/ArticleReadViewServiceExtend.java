package com.github.paicoding.forum.web.front.article.extra;

import com.github.paicoding.forum.api.model.enums.ArticleReadTypeEnum;
import com.github.paicoding.forum.api.model.vo.article.dto.ArticleDTO;
import org.springframework.stereotype.Service;

@Service
public class ArticleReadViewServiceExtend {
    public String formatArticleReadType(ArticleDTO article) {
        ArticleReadTypeEnum readType = ArticleReadTypeEnum.typeOf(article.getReadType());
        if (readType == ArticleReadTypeEnum.PAY_READ) {
            article.setReadType(ArticleReadTypeEnum.NORMAL.getType());
        }
        article.setCanRead(true);
        return article.getContent();
    }
}
