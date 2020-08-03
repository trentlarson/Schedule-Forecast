// from https://raw.githubusercontent.com/eugenp/tutorials/master/libraries-server/src/main/java/com/baeldung/jetty/JettyServer.java
// from https://www.baeldung.com/jetty-embedded

package com.trentlarson.forecast.core.helper;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

class JettyServer {

  private Server server;

  public static void main(String[] args) throws Exception {
    new JettyServer().start();
  }

  void start() throws Exception {

    int maxThreads = 100;
    int minThreads = 10;
    int idleTimeout = 120;

    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

    server = new Server(threadPool);
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(8090);
    server.setConnectors(new Connector[] { connector });

    ServletHandler servletHandler = new ServletHandler();
    server.setHandler(servletHandler);

    servletHandler.addServletWithMapping(BlockingServlet.class, "/graph");
    servletHandler.addServletWithMapping(DisplayServlet.class, "/display");

    server.start();

  }

  void stop() throws Exception {
    server.stop();
  }
}
