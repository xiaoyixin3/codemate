package com.github.paicoding.forum.api.model.openapi.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class OpenApiUserDTO implements Serializable {
    private static final long serialVersionUID = 4663622879892017339L;

    @ApiModelProperty(value = "用户 id", required = true)
    private Long userId;
    @ApiModelProperty(value = "用户昵称", required = true)
    private String userName;
    @ApiModelProperty(value = "登录用户名", required = true)
    private String loginName;
    private String role;
    private String photo;
    private String email;
    private String profile;
    private String position;
    private String company;
}
