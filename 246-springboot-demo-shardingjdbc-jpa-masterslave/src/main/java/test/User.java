package test;

public class User {
    // 静态变量（类级别）
    private static String staticField;
    // 成员变量（对象级别）
    private String name;

    // 1. 静态代码块：类加载时执行，仅1次
    static {
        staticField = "静态变量初始化";
        System.out.println("1. 静态代码块执行：" + staticField);
    }

    // 2. 无参构造器：new对象时执行，每次new都执行
    public User() {
        this.name = "默认名称";
        System.out.println("2. 构造器执行：" + name);
    }

    public static void main(String[] args) {
        System.out.println("=====第一次创建User对象=====");
        User user1 = new User(); // 触发类加载→静态代码块→构造器

        System.out.println("=====第二次创建User对象=====");
        User user2 = new User(); // 仅触发构造器（静态代码块已执行过）
    }
}