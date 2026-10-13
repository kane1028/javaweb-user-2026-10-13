package com.pzy.web;

import com.pzy.model.User;
import com.pzy.store.UserStore;
import com.pzy.util.Pages;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 登录处理 Servlet：对应 login.html 提交过来的表单。
 * <p>
 * 成功：把用户名写进 session（登录态），并渲染成功卡片；
 * 失败：用 sendRedirect 带着错误原因跳回 login.html，由页面显示红条提示。
 *
 * @author 蒲正艺
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 「记住我」勾选后 session 的存活时间：7 天（单位秒） */
    private static final int REMEMBER_ME_SECONDS = 7 * 24 * 60 * 60;

    /**
     * GET 请求：说明这个地址只收表单，避免同学直接在地址栏打开看到 404/500。
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Pages.writeHtml(response, Pages.formHint(request, "登录", "login.html"));
    }

    /**
     * POST 请求：真正的登录逻辑。
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. 统一 UTF-8，防止中文参数乱码（web.xml 里已有全局声明，这里双保险）
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();

        // 2. 取参数并去掉首尾空格（用户复制粘贴常常带空格）
        String username = Pages.trim(request.getParameter("username"));
        String password = Pages.trim(request.getParameter("password"));

        // 3. 服务端必填校验。前端 required 只是体验，这里才是真正的安全边界
        if (username.isEmpty() || password.isEmpty()) {
            Pages.redirectError(response, ctx + "/login.html", "用户名和密码都不能为空");
            return;
        }

        // 4. 校验账号密码
        User user = UserStore.getInstance().verify(username, password);
        if (user == null) {
            // 区分「账号不存在」和「密码错」，用户体验更好
            // （真实对外的系统应统一提示，避免被拿来试探哪些账号存在）
            String msg = UserStore.getInstance().exists(username)
                    ? "密码不正确，请重新输入"
                    : "该用户名不存在，请先注册";
            Pages.redirectError(response, ctx + "/login.html", msg);
            return;
        }

        // 5. 登录成功：写入 session 登录态
        HttpSession session = request.getSession();
        session.setAttribute("loginUser", user.getUsername());
        // 勾了「记住我」就把会话有效期拉长到 7 天
        if ("on".equals(request.getParameter("remember"))) {
            session.setMaxInactiveInterval(REMEMBER_ME_SECONDS);
        }

        // 6. 渲染成功页
        String links = "<a class=\"btn btn-primary\" href=\"" + ctx + "/index.html\">返回首页</a>"
                + "<a class=\"btn btn-outline-danger\" href=\"" + ctx + "/logout\">退出登录</a>";
        String msg = "<p class=\"mb-1\">欢迎回来，<strong>" + Pages.esc(user.getRealName())
                + "</strong>（" + Pages.esc(user.getUsername()) + "）！</p>"
                + "<p class=\"text-muted small mb-0\">登录态已写入 session，键名 <code>loginUser</code>；"
                + "注册时间：" + Pages.esc(user.getRegisterTimeText()) + "</p>";
        Pages.writeHtml(response, Pages.shell(request, "登录成功",
                Pages.resultCard("success", "&#127881;", "登录成功", msg, links)));
    }
}
