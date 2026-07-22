package com.github.paicoding.forum.web.front.login.pwd

import com.github.paicoding.forum.api.model.context.ReqInfoContext
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO
import com.github.paicoding.forum.service.user.service.LoginService
import com.github.paicoding.forum.service.user.service.help.UserSessionHelper
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

class JwtAuthRestControllerTest extends Specification {
    def loginService = Mock(LoginService)
    def controller = new JwtAuthRestController()

    def setup() {
        def properties = new UserSessionHelper.JwtProperties(expire: 432000000L)
        ReflectionTestUtils.setField(controller, "loginService", loginService)
        ReflectionTestUtils.setField(controller, "jwtProperties", properties)
    }

    def cleanup() {
        ReqInfoContext.clear()
    }

    def "returns a bearer token after password login"() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        loginService.loginByUserPwd("alice", "secret") >> "signed-jwt"

        when:
        def response = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content('{"username":"alice","password":"secret"}'))
                .andReturn().response

        then:
        response.status == 200
        response.contentAsString.contains('"accessToken":"signed-jwt"')
        response.contentAsString.contains('"tokenType":"Bearer"')
        response.contentAsString.contains('"expiresIn":432000')
    }

    def "revokes the current bearer token on logout"() {
        given:
        def reqInfo = new ReqInfoContext.ReqInfo(session: "signed-jwt", user: new BaseUserInfoDTO().setUserId(12L))
        ReqInfoContext.addReqInfo(reqInfo)
        def mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

        when:
        def response = mockMvc.perform(post("/api/auth/logout")).andReturn().response

        then:
        response.status == 200
        response.contentAsString.contains('"result":true')
        1 * loginService.logout("signed-jwt")
    }
}
