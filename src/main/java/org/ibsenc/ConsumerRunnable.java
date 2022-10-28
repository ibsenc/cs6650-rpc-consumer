package org.ibsenc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsumerRunnable implements Runnable {

  private static final Integer SKIER_ID_INDEX = 3;
  private ConcurrentHashMap<Integer, List<String>> skierIdToLiftRides;
  private LinkedBlockingQueue channelQueue;

  public ConsumerRunnable(ConcurrentHashMap<Integer, List<String>> skierIdToLiftRides, LinkedBlockingQueue<Channel> channelQueue) {
    this.skierIdToLiftRides = skierIdToLiftRides;
    this.channelQueue = channelQueue;
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
        System.out.println("Processing liftRide with corrId: " + delivery.getProperties().getCorrelationId());

        // Put skierId and associated liftRide in hashmap
        addSkierIdAndLiftRideToHashMap(liftRideJson);

        response += "Processed liftRide with corrId: " + delivery.getProperties().getCorrelationId();
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

  private void addSkierIdAndLiftRideToHashMap(String liftRideJson) {
    Integer skierId = getSkierIdFromLiftRide(liftRideJson);
//    System.out.println(skierId);

    skierIdToLiftRides.computeIfAbsent(skierId, k -> new ArrayList<>());

    List<String> liftRides = skierIdToLiftRides.get(skierId);
    liftRides.add(liftRideJson);
    System.out.println(skierId + ": " + liftRides);

    skierIdToLiftRides.put(skierId, liftRides);
  }

  private Integer getSkierIdFromLiftRide(String liftRideJson) {
    String[] jsonFields = liftRideJson.split(",");
    String[] skierIdLabelAndValue = jsonFields[SKIER_ID_INDEX].split(":");

    Integer skierId = Integer.valueOf(skierIdLabelAndValue[1]);

    return skierId;
  }
}
