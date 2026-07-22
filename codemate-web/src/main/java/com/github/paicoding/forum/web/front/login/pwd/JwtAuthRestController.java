package com.github.paicoding.forum.web.front.login.pwd;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.JwtLoginRes;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.user.service.LoginService;
import com.github.paicoding.forum.service.user.service.help.UserSessionHelper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Stateless JWT endpoints for API clients. Browser pages continue to use the
 * existing cookie login endpoints.
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class JwtAuthRestController {
    @Resource
    private LoginService loginService;

    @Resource
    private UserSessionHelper.JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResVo<JwtLoginRes> login(@RequestBody UserPwdLoginReq loginReq) {
        String token = loginService.loginByUserPwd(loginReq.getUsername(), loginReq.getPassword());
        return ResVo.ok(new JwtLoginRes()
                .setAccessToken(token)
                .setTokenType("Bearer")
                .setExpiresIn(jwtProperties.getExpire() / 1000));
    }

    @Permission(role = UserRole.LOGIN)
    @PostMapping("/logout")
    public ResVo<Boolean> logout() {
        loginService.logout(ReqInfoContext.getReqInfo().getSession());
        return ResVo.ok(true);
    }

    @Permission(role = UserRole.LOGIN)
    @GetMapping("/me")
    public ResVo<BaseUserInfoDTO> currentUser() {
        return ResVo.ok(ReqInfoContext.getReqInfo().getUser());
    }
}
