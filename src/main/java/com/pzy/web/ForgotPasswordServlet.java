package com.pzy.web;

import com.pzy.model.User;
import com.pzy.store.UserStore;
import com.pzy.util.Pages;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 忘记密码处理 Servlet：接收 forgot.html 提交过来的表单（POST /forgot）。
 * <p>
 * 思路：用「用户名 + 注册邮箱」做双重身份校验，通过后生成一个随机新密码写回，
 * 并显示在结果页上让用户抄走。
 * <p>
 * 说明：真实项目这里应该发邮件重置链接或短信验证码，本作业没有邮件服务器，
 * 所以直接把新密码显示在页面上（已在页面上标注这一点）。
 *
 * @author 蒲正艺
 */
@WebServlet("/forgot")
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 新密码字符集：故意去掉 0/O、1/l/I 这些看起来一样的字符，避免用户抄错 */
    private static final String PASSWORD_CHARS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 新密码长度 */
    private static final int NEW_PASSWORD_LENGTH = 8;

    /** 密码学安全的随机数发生器（比 Math.random 更适合生成凭证） */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * GET 请求：本地址只负责收表单。
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Pages.writeHtml(response, Pages.formHint(request, "找回密码", "forgot.html"));
    }

    /**
     * POST 请求：真正的找回密码逻辑。
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();
        String backPage = ctx + "/forgot.html";

        // 1. 取参数
        String username = Pages.trim(request.getParameter("username"));
        String email = Pages.trim(request.getParameter("email"));

        // 2. 必填校验
        if (username.isEmpty() || email.isEmpty()) {
            Pages.redirectError(response, backPage, "请填写用户名和注册邮箱");
            return;
        }

        // 3. 双重身份校验：用户名存在 且 邮箱与注册时一致
        User user = UserStore.getInstance().findByUsername(username);
        if (user == null || !user.getEmail().equalsIgnoreCase(email)) {
            // 提示统一写成一句话，不区分「用户不存在」和「邮箱不对」，
            // 否则别人可以拿这个接口试探出系统里有哪些账号存在。
            Pages.redirectError(response, backPage, "用户名与注册邮箱不匹配，请检查后重新填写");
            return;
        }

        // 4. 生成随机新密码并写回
        String newPassword = randomPassword();
        UserStore.getInstance().resetPassword(username, newPassword);

        // 5. 渲染结果页（把新密码显示给用户）
        String links = "<a class=\"btn btn-primary\" href=\"" + ctx + "/login.html\">用新密码去登录</a>"
                + "<a class=\"btn btn-outline-secondary\" href=\"" + ctx + "/index.html\">返回首页</a>";
        String msg = "<p class=\"mb-2\">账号 <strong>" + Pages.esc(user.getUsername())
                + "</strong> 的密码已重置，新密码如下（请立即抄下来）：</p>"
                + "<div class=\"alert alert-warning mb-2\"><span style=\"font-size:22px;letter-spacing:2px;font-family:monospace\">"
                + Pages.esc(newPassword) + "</span></div>"
                + "<p class=\"text-muted small mb-0\">真实项目应通过邮件重置链接或短信验证码完成这一步，"
                + "本作业没有邮件服务器，因此直接在此显示。用户数据在内存中，重启 Tomcat 后密码会恢复为初始值。</p>";
        Pages.writeHtml(response, Pages.shell(request, "密码重置成功",
                Pages.resultCard("warning", "&#128273;", "密码重置成功", msg, links)));
    }

    /**
     * 生成随机新密码。
     *
     * @return 长度为 NEW_PASSWORD_LENGTH 的随机密码
     */
    private String randomPassword() {
        StringBuilder sb = new StringBuilder(NEW_PASSWORD_LENGTH);
        for (int i = 0; i < NEW_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
