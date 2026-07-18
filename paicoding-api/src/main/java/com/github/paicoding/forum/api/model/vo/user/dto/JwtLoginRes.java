package com.github.paicoding.forum.api.model.vo.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * JWT login response for API clients.
 */
@Data
@Accessors(chain = true)
@ApiModel("JWT login response")
public class JwtLoginRes implements Serializable {
    private static final long serialVersionUID = 6232966935513931951L;

    @ApiModelProperty(value = "JWT access token", required = true)
    private String accessToken;

    @ApiModelProperty(value = "Authorization scheme", example = "Bearer")
    private String tokenType;

    @ApiModelProperty(value = "Access token validity in seconds", example = "432000")
    private Long expiresIn;
}
