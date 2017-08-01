package com.bloomberg.bach.agent;

import java.io.IOException;

import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;

/**
 * @author Ronald Macmaster
 * Context Package for Bach Metrics package.
 * Manages a singleton context.
 */
public class BachMetricsContext {

  // singleton context object.
  private static volatile BachMetricsContext INSTANCE = null;

  // metrics reporting via JMX and metrics2
  private static BachMetricsServer server = null;

  private BachMetricsContext() {
    DefaultMetricsSystem.initialize("client");
  }

  private static synchronized BachMetricsContext registerContext() {
    // return singleton context.
    return (INSTANCE == null) ? (INSTANCE = new BachMetricsContext()) : INSTANCE;
  }

  /**
   * Start metrics JSON service on localhost and an arbitrary port.  <br>
   */
  public static void start() throws IOException {
    start(null, null);
  }

  /**
   * Start the metrics JSON service on the given host and port. <br>
   */
  public static void start(String host, Integer port) throws IOException {
    // System.getProperties().list(System.out);		
    server = new BachMetricsServer(host, port);
    server.start();

    // initialize metrics system.
    BachMetricsContext.registerContext();
    System.out.format("Bach Metrics Context: started on  <%s, %d> %n", server.getHost(), server.getPort());
  }

  /**
   * Stop the metrics context. <br>
   */
  public static void stop() throws IOException {
    server.stop();
  }

  /**
   * returns the current port of operation. <br>
   */
  public static Integer getPort() {
    if (server != null) {
      return server.getPort();
    } else {
      throw new IllegalStateException("Metrics Context has not been started!");
    }
  }

  /**
   * returns the current host of operation. <br>
   */
  public static String getHost() {
    if (server != null) {
      return server.getHost();
    } else {
      throw new IllegalStateException("Metrics Context has not been started!");
    }
  }

}
