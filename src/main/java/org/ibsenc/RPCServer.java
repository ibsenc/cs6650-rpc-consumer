package org.ibsenc;

import com.rabbitmq.client.*;

public class RPCServer {

  private static final String RPC_QUEUE_NAME = "rpc_queue";
  private static final String hostName = "localhost";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(hostName);

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
    channel.queuePurge(RPC_QUEUE_NAME);
    channel.basicQos(1);

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

        // Do something with the delivery body (put in hashmap)
        System.out.println(liftRideJson);

        response += "Processed liftRide with corrId: " + delivery.getProperties().getCorrelationId();
      } catch (RuntimeException e) {
        System.out.println("!!!Exception: " + e);
      } finally {
        channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
    };

    channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
  }
}
