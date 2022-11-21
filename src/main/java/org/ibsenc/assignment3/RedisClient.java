package org.ibsenc.assignment3;

import org.ibsenc.Constants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisClient {
  private static JedisPool jedisPool;

  public RedisClient(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  private void initializePool() {
    jedisPool = new JedisPool(Constants.REDIS_SERVER_PUBLIC_IP, Constants.REDIS_PORT);
  }

  public void ping() {
    if (jedisPool == null) {
      initializePool();
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.ping();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Could not obtain jedis connection.");
    }
  }

  public void put(String key, String value) {
    try (Jedis jedis = jedisPool.getResource()) {
      System.out.println(String.format("Adding key: %s, value: %s", key, value));
      jedis.set(key, value);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Could not obtain jedis connection.");
    }
  }

  public String get(String key) {
    String value = "";
    try (Jedis jedis = jedisPool.getResource()) {
      value = jedis.get(key);
//      System.out.println(String.format("Getting value: key: %s, value: %s", key, value));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Could not obtain jedis connection.");
    }

    return value;
  }
}
