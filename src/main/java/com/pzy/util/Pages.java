package com.pzy.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

/**
 * 页面渲染工具类：三个 Servlet 共用同一套 HTML 外壳和结果卡片，避免重复代码。
 * <p>
 * 工具类不允许被实例化（私有构造），所有方法都是静态的。
 *
 * @author 蒲正艺
 */
public final class Pages {

    /** 私有构造：工具类不需要实例 */
    private Pages() {
    }

    /**
     * HTML 转义：所有要回显到页面上的用户输入都必须过一遍这个方法。
     * 不转义的话，用户在表单里填 &lt;script&gt;alert(1)&lt;/script&gt; 就会被浏览器当代码执行（XSS 攻击）。
     *
     * @param text 原始文本
     * @return 转义后的安全文本
     */
    public static String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 拼一整页 HTML（含导航条和本地 Bootstrap）。
     * 所有链接都用 request.getContextPath() 拼，
     * 这样项目改名 / 换 Context Path 之后页面链接依然正确。
     *
     * @param request 当前请求（用来取 Context Path）
     * @param title   浏览器标签页标题
     * @param inner   页面主体 HTML
     * @return 完整 HTML 字符串
     */
    public static String shell(HttpServletRequest request, String title, String inner) {
        String ctx = request.getContextPath();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        sb.append("  <title>").append(esc(title)).append(" - 用户管理系统</title>\n");
        sb.append("  <link rel=\"stylesheet\" href=\"").append(ctx).append("/static/css/bootstrap.min.css\">\n");
        sb.append("</head>\n<body class=\"bg-body-tertiary\">\n");
        // 顶部导航条
        sb.append("<nav class=\"navbar navbar-expand-lg navbar-dark bg-primary\">\n");
        sb.append("  <div class=\"container\">\n");
        sb.append("    <a class=\"navbar-brand\" href=\"").append(ctx).append("/index.html\">用户管理系统</a>\n");
        sb.append("    <div class=\"navbar-nav ms-auto\">\n");
        sb.append("      <a class=\"nav-link\" href=\"").append(ctx).append("/login.html\">登录</a>\n");
        sb.append("      <a class=\"nav-link\" href=\"").append(ctx).append("/register.html\">注册</a>\n");
        sb.append("      <a class=\"nav-link\" href=\"").append(ctx).append("/forgot.html\">忘记密码</a>\n");
        sb.append("      <a class=\"nav-link\" href=\"").append(ctx).append("/logout\">退出</a>\n");
        sb.append("    </div>\n  </div>\n</nav>\n");
        sb.append("<div class=\"container py-5\">\n").append(inner).append("\n</div>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    /**
     * 结果卡片：成功 / 失败 / 警告 / 提示 都能用，居中显示。
     *
     * @param tone         Bootstrap 语义色：success / danger / warning / info
     * @param icon         图标（emoji 即可）
     * @param heading      大字标题（内部会转义）
     * @param messageHtml  正文，允许传 HTML 片段（调用方自己保证已转义）
     * @param linksHtml    底部按钮区 HTML
     * @return 卡片 HTML
     */
    public static String resultCard(String tone, String icon, String heading, String messageHtml, String linksHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"row justify-content-center\"><div class=\"col-lg-6\">\n");
        sb.append("  <div class=\"card border-").append(tone).append(" shadow-sm\">\n");
        sb.append("    <div class=\"card-body text-center p-4\">\n");
        sb.append("      <div style=\"font-size:48px;line-height:1\">").append(icon).append("</div>\n");
        sb.append("      <h4 class=\"text-").append(tone).append(" mt-3\">").append(esc(heading)).append("</h4>\n");
        sb.append("      <div class=\"mt-3\">").append(messageHtml).append("</div>\n");
        if (linksHtml != null && !linksHtml.isEmpty()) {
            sb.append("      <div class=\"mt-4 d-flex gap-2 justify-content-center flex-wrap\">")
              .append(linksHtml).append("</div>\n");
        }
        sb.append("    </div>\n  </div>\n</div></div>\n");
        return sb.toString();
    }

    /**
     * 直接访问 Servlet（GET）时的提示页：告诉同学「这个地址是收表单的，别直接开」。
     *
     * @param request 当前请求
     * @param formName 表单名字，如「登录」
     * @param pageName 对应页面文件名，如 login.html
     * @return 完整 HTML
     */
    public static String formHint(HttpServletRequest request, String formName, String pageName) {
        String ctx = request.getContextPath();
        String links = "<a class=\"btn btn-primary\" href=\"" + ctx + "/" + pageName + "\">去填写"
                + esc(formName) + "表单</a>"
                + "<a class=\"btn btn-outline-secondary\" href=\"" + ctx + "/index.html\">返回首页</a>";
        String inner = resultCard("info", "&#128221;", "这是表单处理地址",
                "<p class=\"text-muted mb-0\">本地址（<code>" + esc(request.getServletPath())
                        + "</code>）用于接收 <strong>" + esc(formName)
                        + "</strong>表单的 POST 提交，不能直接在浏览器地址栏打开。<br>"
                        + "请回到页面填写表单后提交。</p>",
                links);
        return shell(request, formName + "处理", inner);
    }

    /**
     * 输出 HTML 页面：统一声明 UTF-8，否则页面上的中文会乱码。
     *
     * @param response 响应对象
     * @param html     完整 HTML 字符串
     */
    public static void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(html);
        }
    }

    /**
     * 带着中文错误提示跳回表单页面，页面上会读 ?error= 参数显示红色提示条。
     * 中文必须 URL 编码，否则不同浏览器/容器下会丢字或乱码。
     *
     * @param response 响应对象
     * @param pagePath 目标页面，如 /javaweb-user/register.html
     * @param message  要显示的错误原因
     */
    public static void redirectError(HttpServletResponse response, String pagePath, String message) throws IOException {
        response.sendRedirect(pagePath + "?error=" + URLEncoder.encode(message, "UTF-8"));
    }

    /**
     * 去掉字符串首尾空格，null 安全（用户复制粘贴经常带空格）。
     *
     * @param value 原始值
     * @return 去空格后的字符串，null 视为空串
     */
    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
