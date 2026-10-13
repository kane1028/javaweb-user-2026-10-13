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
import java.util.regex.Pattern;

/**
 * 注册处理 Servlet：接收 register.html 提交过来的表单（POST /register）。
 * <p>
 * 校验按固定顺序执行，取「第一个」不通过的项作为错误原因 ——
 * 让用户一次只看到一个明确的问题，而不是被十条红字糊脸。
 * 顺序：必填 → 用户名格式 → 密码长度 → 两次密码一致 → 邮箱格式 → 手机号格式 → 用户名查重。
 *
 * @author 蒲正艺
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 用户名规则：3~16 位字母、数字或下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    /** 邮箱规则：xxx@yyy.zzz 的常见形式 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /** 手机号规则：1 开头，第二位 3~9，共 11 位 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** 密码长度下限 */
    private static final int PASSWORD_MIN = 6;

    /** 密码长度上限 */
    private static final int PASSWORD_MAX = 20;

    /**
     * GET 请求：本地址只负责收表单。
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Pages.writeHtml(response, Pages.formHint(request, "注册", "register.html"));
    }

    /**
     * POST 请求：真正的注册逻辑。
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();
        String backPage = ctx + "/register.html";

        // 1. 取参数
        String username = Pages.trim(request.getParameter("username"));
        String password = Pages.trim(request.getParameter("password"));
        String confirmPassword = Pages.trim(request.getParameter("confirmPassword"));
        String realName = Pages.trim(request.getParameter("realName"));
        String email = Pages.trim(request.getParameter("email"));
        String phone = Pages.trim(request.getParameter("phone"));

        // 2. 必填校验
        if (username.isEmpty() || password.isEmpty() || realName.isEmpty()
                || email.isEmpty() || phone.isEmpty()) {
            Pages.redirectError(response, backPage, "用户名、密码、真实姓名、邮箱、手机号都不能为空");
            return;
        }

        // 3. 用户名格式
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            Pages.redirectError(response, backPage, "用户名只能是 3~16 位的字母、数字或下划线");
            return;
        }

        // 4. 密码长度
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            Pages.redirectError(response, backPage, "密码长度必须是 " + PASSWORD_MIN + "~" + PASSWORD_MAX + " 位");
            return;
        }

        // 5. 两次密码一致（这一条前端必须手写 JS 才能校验，服务端同样不能省）
        if (!password.equals(confirmPassword)) {
            Pages.redirectError(response, backPage, "两次输入的密码不一致，请重新输入");
            return;
        }

        // 6. 邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Pages.redirectError(response, backPage, "邮箱格式不正确，示例：student@example.com");
            return;
        }

        // 7. 手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            Pages.redirectError(response, backPage, "手机号格式不正确，必须是 1 开头的 11 位数字");
            return;
        }

        // 8. 用户名查重（真正的并发安全由 UserStore.add 的 putIfAbsent 保证）
        if (UserStore.getInstance().exists(username)) {
            Pages.redirectError(response, backPage, "用户名「" + username + "」已被注册，请换一个");
            return;
        }

        // 9. 入库
        User user = new User(username, password, realName, email, phone);
        if (!UserStore.getInstance().add(user)) {
            // 并发下被别的请求抢先注册了同一个用户名
            Pages.redirectError(response, backPage, "用户名「" + username + "」已被注册，请换一个");
            return;
        }

        // 10. 渲染成功页
        String links = "<a class=\"btn btn-primary\" href=\"" + ctx + "/login.html\">去登录</a>"
                + "<a class=\"btn btn-outline-secondary\" href=\"" + ctx + "/register.html\">再注册一个</a>";
        String msg = "<table class=\"table table-sm text-start mt-2\">"
                + "<tr><th class=\"text-muted\" style=\"width:40%\">用户名</th><td>" + Pages.esc(user.getUsername()) + "</td></tr>"
                + "<tr><th class=\"text-muted\">真实姓名</th><td>" + Pages.esc(user.getRealName()) + "</td></tr>"
                + "<tr><th class=\"text-muted\">邮箱</th><td>" + Pages.esc(user.getEmail()) + "</td></tr>"
                + "<tr><th class=\"text-muted\">手机号</th><td>" + Pages.esc(user.getPhone()) + "</td></tr>"
                + "<tr><th class=\"text-muted\">注册时间</th><td>" + Pages.esc(user.getRegisterTimeText()) + "</td></tr>"
                + "</table>"
                + "<p class=\"text-muted small mb-0\">已保存到内存仓库（当前共 "
                + UserStore.getInstance().count() + " 个用户）；重启 Tomcat 后会清空。</p>";
        Pages.writeHtml(response, Pages.shell(request, "注册成功",
                Pages.resultCard("success", "&#10024;", "注册成功", msg, links)));
    }
}
