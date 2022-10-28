package org.ibsenc;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class RPCServer {
  private static final String hostName = "localhost";
  private static final Integer NUM_OF_CONSUMER_THREADS = 10;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(hostName);
    Connection connection = factory.newConnection();

    LinkedBlockingQueue<Channel> channelQueue = generateQueueWithChannels(10, connection);

    ConcurrentHashMap<Integer, List<String>> skierIdToLiftRides = new ConcurrentHashMap<>();
    List<Thread> threads = new ArrayList<>();
    ConsumerRunnable consumerRunnable = new ConsumerRunnable(skierIdToLiftRides, channelQueue);

    // Create consumer threads
    for (int i = 0; i < NUM_OF_CONSUMER_THREADS; i++) {
      Thread thread = new Thread(consumerRunnable);
      threads.add(thread);
    }

    System.out.println(threads);

    // Start threads
    for (Thread thread : threads) {
      thread.start();
      System.out.println(channelQueue);
    }

    System.out.println(channelQueue);

    // Main thread does not continue until all threads below have "joined"
    try {
      for (Thread thread : threads) {
        thread.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static LinkedBlockingQueue generateQueueWithChannels(Integer numOfChannels,
      Connection connection) {
    LinkedBlockingQueue<Channel> channelQueue = new LinkedBlockingQueue<>();

    System.out.println("Connection open? " + connection.isOpen());
    for (int i = 0; i < numOfChannels; i++) {
      try {
        channelQueue.add(connection.createChannel());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return channelQueue;
  }
}
