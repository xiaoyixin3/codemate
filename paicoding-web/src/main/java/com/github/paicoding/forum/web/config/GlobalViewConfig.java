package com.github.paicoding.forum.web.config;

import com.github.paicoding.forum.api.model.util.cdn.CdnUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "view.site")
@Component
public class GlobalViewConfig {
    private String cdnImgStyle;
    private String websiteRecord;
    private Integer pageSize;
    private String websiteName;
    private String websiteLogoUrl;
    private String websiteFaviconIconUrl;
    private String contactMeTitle;
    private String host;
    private String welcomeInfo;
    private String oss;
    private String needLoginArticleReadCount;

    public String getOss() {
        return oss == null ? "" : oss;
    }

    public GlobalViewConfig setWebsiteLogoUrl(String websiteLogoUrl) {
        this.websiteLogoUrl = CdnUtil.autoTransCdn(websiteLogoUrl);
        return this;
    }

    public GlobalViewConfig setWebsiteFaviconIconUrl(String websiteFaviconIconUrl) {
        this.websiteFaviconIconUrl = CdnUtil.autoTransCdn(websiteFaviconIconUrl);
        return this;
    }
}
