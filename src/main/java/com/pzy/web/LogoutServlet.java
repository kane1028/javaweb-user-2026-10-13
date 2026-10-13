package com.pzy.web;

import com.pzy.util.Pages;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 退出登录 Servlet：销毁 session，演示登录态的清除（GET /logout）。
 * <p>
 * 注意 getSession(false)：本来就没有会话时不要凭空新建一个。
 *
 * @author 蒲正艺
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String who = null;
        if (session != null) {
            who = (String) session.getAttribute("loginUser");
            session.invalidate(); // 销毁整个会话，登录态随之失效
        }

        String ctx = request.getContextPath();
        String links = "<a class=\"btn btn-primary\" href=\"" + ctx + "/login.html\">重新登录</a>"
                + "<a class=\"btn btn-outline-secondary\" href=\"" + ctx + "/index.html\">返回首页</a>";
        String msg = who == null
                ? "<p class=\"text-muted mb-0\">当前本来就没有登录态，无需退出。</p>"
                : "<p class=\"mb-0\">账号 <strong>" + Pages.esc(who) + "</strong> 已退出，session 已销毁。</p>";

        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(Pages.shell(request, "已退出",
                    Pages.resultCard("info", "&#128075;", "您已安全退出", msg, links)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
