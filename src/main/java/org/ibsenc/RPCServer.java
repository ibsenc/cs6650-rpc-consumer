package org.ibsenc;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class RPCServer {
  private static final String LOCAL_HOST_NAME = "localhost";
  private static final String REMOTE_HOST_NAME_PUBLIC = "100.20.70.143";
  private static final String REMOTE_HOST_NAME_PRIVATE = "172.31.31.103";
  private static final Integer NUM_OF_CONSUMER_THREADS = 1000;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    String rabbitMQHostName = REMOTE_HOST_NAME_PUBLIC;
    factory.setHost(rabbitMQHostName);
    factory.setUsername("ibsenc");
    factory.setPassword("password");
    factory.setVirtualHost("cherry_broker");
    Connection connection = factory.newConnection();

    LinkedBlockingQueue<Channel> channelQueue = generateQueueWithChannels(connection);

    ConcurrentHashMap<Integer, List<String>> skierIdToLiftRides = new ConcurrentHashMap<>();
    List<Thread> threads = new ArrayList<>();
    ConsumerRunnable consumerRunnable = new ConsumerRunnable(skierIdToLiftRides, channelQueue);

    // Create consumer threads
    for (int i = 0; i < NUM_OF_CONSUMER_THREADS; i++) {
      Thread thread = new Thread(consumerRunnable);
      threads.add(thread);
    }

    // Start threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Main thread does not continue until all threads below have "joined"
    try {
      for (Thread thread : threads) {
        thread.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static LinkedBlockingQueue generateQueueWithChannels(Connection connection) {
    LinkedBlockingQueue<Channel> channelQueue = new LinkedBlockingQueue<>();

    if (!connection.isOpen()) {
      throw new RuntimeException("Connection is not open. Cannot create channels.");
    }

    for (int i = 0; i < NUM_OF_CONSUMER_THREADS; i++) {
      try {
        channelQueue.add(connection.createChannel());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return channelQueue;
  }
}
