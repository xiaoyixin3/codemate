package com.github.paicoding.forum.web.front.taskplan.view;

import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** AI 任务计划服务端页面。数据由同域 REST 接口按当前登录用户返回。 */
@Controller
@Permission(role = UserRole.LOGIN)
@RequestMapping("/task-plan")
public class AiTaskPlanViewController {
    @GetMapping({"", "/"})
    public String list() {
        return "views/task-plan/index";
    }

    @GetMapping("/{planId}")
    public String detail() {
        return "views/task-plan/detail";
    }
}
