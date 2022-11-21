package org.ibsenc.assignment3;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import org.ibsenc.Constants;
import redis.clients.jedis.JedisPool;

public class ConsumerRunnableWithRedis implements Runnable {

  private static final Integer SKIER_ID_INDEX = 3;
  private LinkedBlockingQueue channelQueue;
  private JedisPool jedisPool;
  private RedisClient redisClient;

  public ConsumerRunnableWithRedis( LinkedBlockingQueue<Channel> channelQueue, JedisPool jedisPool, RedisClient redisClient) {
    this.channelQueue = channelQueue;
    this.jedisPool = jedisPool;
    this.redisClient = redisClient;
  }

  @Override
  public void run() {
    if (channelQueue.isEmpty()) {
      System.out.println("Couldn't find an available channel");
      return;
    }

    Channel channel = (Channel) channelQueue.poll();
    try {
      channel.queueDeclare(Constants.RPC_QUEUE_NAME, false, false, false, null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      channel.queuePurge(Constants.RPC_QUEUE_NAME);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      channel.basicQos(1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println(" [x] Awaiting RPC requests");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      AMQP.BasicProperties replyProps = new AMQP.BasicProperties
          .Builder()
          .correlationId(delivery.getProperties().getCorrelationId())
          .build();

      String response = "";
      try {
        String liftRideJson = new String(delivery.getBody(), "UTF-8");
//        System.out.println("Processing liftRide with corrId: " + delivery.getProperties().getCorrelationId());

        // Assignment 3: Put skierID and associated liftRide in the DB
          String skierID = getSkierId(liftRideJson).toString();
          redisClient.put(skierID, liftRideJson);
//          redisClient.get(skierID);


//        response += "Processed liftRide with corrId: " + delivery.getProperties().getCorrelationId();
      } catch (RuntimeException e) {
        e.printStackTrace();
      } finally {
        channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
    };

    try {
      channel.basicConsume(Constants.RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Integer getSkierId(String liftRideJson) {
    String[] jsonFields = liftRideJson.split(",");
    String[] skierIdLabelAndValue = jsonFields[SKIER_ID_INDEX].split(":");

    Integer skierId = Integer.valueOf(skierIdLabelAndValue[1]);

    return skierId;
  }
}
