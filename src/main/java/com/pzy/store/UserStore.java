package com.pzy.store;

import com.pzy.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户数据仓库：用一个线程安全的内存 Map 假装是数据库。
 * <p>
 * 单例模式（整个应用只有一个实例），对外只暴露下面这几个方法。
 * 换 MySQL 时只需要把这个类的方法体改成 JDBC 实现，
 * 三个 Servlet 和四个页面一行都不用动 —— 这就是分层的好处。
 *
 * @author 蒲正艺
 */
public final class UserStore {

    /**
     * 时间格式化常量必须写在 INSTANCE 前面！
     * 类的静态初始化是按书写顺序执行的：如果 INSTANCE 在前，
     * 执行 new UserStore() 时下面的常量还是 null，构造方法里一用就 NPE，
     * 整个类加载失败（ExceptionInInitializerError），所有碰这个类的 Servlet 全部 500。
     */
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";

    /** 唯一实例 */
    private static final UserStore INSTANCE = new UserStore();

    /** 用户名 -> 用户对象。ConcurrentHashMap 保证多线程下增删查安全 */
    private final Map<String, User> users = new ConcurrentHashMap<>();

    /** 私有构造：禁止外部 new，同时预置一个测试账号方便演示 */
    private UserStore() {
        users.put(DEFAULT_USERNAME,
                new User(DEFAULT_USERNAME, DEFAULT_PASSWORD, "系统管理员", "admin@example.com", "13800000000"));
    }

    /** 取全局唯一实例 */
    public static UserStore getInstance() {
        return INSTANCE;
    }

    /**
     * 判断用户名是否已被注册。
     *
     * @param username 用户名
     * @return 已存在返回 true
     */
    public boolean exists(String username) {
        return users.containsKey(username);
    }

    /**
     * 新增用户。
     * 用 putIfAbsent 而不是「先 containsKey 再 put」：
     * 后者两步之间有间隙，两个人同时注册同一个用户名会都成功。
     * putIfAbsent 是原子操作，并发下只有一个能成功。
     *
     * @param user 待新增的用户
     * @return 新增成功返回 true；用户名已被抢先注册返回 false
     */
    public boolean add(User user) {
        return users.putIfAbsent(user.getUsername(), user) == null;
    }

    /**
     * 按用户名查用户。
     *
     * @param username 用户名
     * @return 找到返回用户对象，找不到返回 null
     */
    public User findByUsername(String username) {
        return username == null ? null : users.get(username);
    }

    /**
     * 校验账号密码（登录用）。
     *
     * @param username 用户名
     * @param password 密码
     * @return 校验通过返回用户对象，失败返回 null
     */
    public User verify(String username, String password) {
        User user = findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * 重置密码（忘记密码用）。
     *
     * @param username    用户名
     * @param newPassword 新密码
     * @return 重置成功返回 true，用户不存在返回 false
     */
    public boolean resetPassword(String username, String newPassword) {
        User user = findByUsername(username);
        if (user == null) {
            return false;
        }
        user.setPassword(newPassword);
        return true;
    }

    /**
     * 当前注册用户总数（首页展示用）。
     *
     * @return 用户数量
     */
    public int count() {
        return users.size();
    }
}
