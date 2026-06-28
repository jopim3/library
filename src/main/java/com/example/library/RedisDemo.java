// 2026-06-27 学习：Redis 五种数据类型（String、List、Set、Hash、Sorted Set）
package com.example.library;

import redis.clients.jedis.Jedis;

public class RedisDemo {
    public static void main(String[] args) {
        // 连接 Redis（默认端口 6379）
        Jedis jedis = new Jedis("localhost", 6379);
        System.out.println("连接成功");

        // 1. String（字符串）
        jedis.set("name", "张三");
        System.out.println("String 读取: " + jedis.get("name"));

        // 2. List（列表）
        jedis.lpush("books", "Java核心技术", "Spring Boot实战");
        System.out.println("List 读取: " + jedis.lrange("books", 0, -1));

        // 3. Set（集合，自动去重）
        jedis.sadd("tags", "后端", "Java", "后端");
        System.out.println("Set 读取: " + jedis.smembers("tags"));

        // 4. Hash（哈希，类似 Map）
        jedis.hset("user:1", "name", "李四");
        jedis.hset("user:1", "age", "25");
        System.out.println("Hash 读取 name: " + jedis.hget("user:1", "name"));
        System.out.println("Hash 读取全部: " + jedis.hgetAll("user:1"));

        // 5. Sorted Set（有序集合，按分数排序）
        jedis.zadd("scores", 95, "张三");
        jedis.zadd("scores", 88, "李四");
        System.out.println("Sorted Set 读取: " + jedis.zrange("scores", 0, -1));

        jedis.close();
    }
}
