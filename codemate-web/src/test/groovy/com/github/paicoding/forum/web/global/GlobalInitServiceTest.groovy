package com.github.paicoding.forum.web.global

import com.github.paicoding.forum.api.model.context.ReqInfoContext
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO
import com.github.paicoding.forum.service.notify.service.NotifyService
import com.github.paicoding.forum.service.user.service.UserService
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification

class GlobalInitServiceTest extends Specification {
    def userService = Mock(UserService)
    def notifyService = Mock(NotifyService)
    def globalInitService = new GlobalInitService()

    def setup() {
        ReflectionTestUtils.setField(globalInitService, "userService", userService)
        ReflectionTestUtils.setField(globalInitService, "notifyService", notifyService)
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    def "authenticates a standard bearer token"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer jwt-token")
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        def user = new BaseUserInfoDTO().setUserId(12L)
        userService.getAndUpdateUserIpInfoBySessionId("jwt-token", null) >> user
        notifyService.queryUserNotifyMsgCount(12L) >> 0

        when:
        def reqInfo = new ReqInfoContext.ReqInfo()
        globalInitService.initLoginUser(reqInfo)

        then:
        reqInfo.userId == 12L
        reqInfo.session == "jwt-token"
    }

    def "does not fall back to a cookie for an invalid bearer token"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalid-token")
        request.setCookies(new javax.servlet.http.Cookie("f-session", "cookie-token"))
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        userService.getAndUpdateUserIpInfoBySessionId("invalid-token", null) >> null

        when:
        def reqInfo = new ReqInfoContext.ReqInfo()
        globalInitService.initLoginUser(reqInfo)

        then:
        reqInfo.userId == null
        0 * userService.getAndUpdateUserIpInfoBySessionId("cookie-token", null)
    }
}
