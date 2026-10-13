package com.pzy.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户实体类：一个 User 对象 = 表里的一行记录。
 * <p>
 * 本作业用内存 Map 充当数据库，但字段设计完全按真实表结构来写，
 * 以后换成 MySQL 时这个类可以直接当 MyBatis / JPA 的实体用，不用改字段。
 *
 * @author 蒲正艺
 */
public class User {

    /** 时间格式化器：注册时间统一按 yyyy-MM-dd HH:mm:ss 展示 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 用户名（登录账号，全表唯一） */
    private String username;

    /** 密码（本作业演示用明文存储；真实项目必须加盐哈希后存储） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 注册邮箱（找回密码时的第二重身份凭证） */
    private String email;

    /** 手机号（11 位） */
    private String phone;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 无参构造：给框架/序列化用 */
    public User() {
    }

    /** 常用构造：注册时一次性把信息塞进来 */
    public User(String username, String password, String realName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.email = email;
        this.phone = phone;
        this.registerTime = LocalDateTime.now();
    }

    /** 注册时间的字符串形式，页面上直接显示 */
    public String getRegisterTimeText() {
        return registerTime == null ? "" : TIME_FMT.format(registerTime);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    /** 只打印用户名，绝不把密码写进日志 */
    @Override
    public String toString() {
        return "User{username='" + username + "', realName='" + realName + "'}";
    }
}
