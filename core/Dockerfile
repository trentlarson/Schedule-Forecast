# 'latest' is latest LTS
FROM ubuntu:latest
LABEL org.opencontainers.image.authors="trent@trentlarson.com"

RUN apt -y update
RUN apt -y upgrade
RUN DEBIAN_FRONTEND=noninteractive \
    TZ=America/Denver \
    apt install -y git maven
RUN git clone https://github.com/trentlarson/Schedule-Forecast.git

WORKDIR Schedule-Forecast/core
RUN mvn package
CMD java -classpath target/forecast-core-0.1.0.jar:lib/commons-logging-1.1.1.jar:lib/hibernate-3.2.6.ga.jar:lib/hsqldb-1.8.0.jar:lib/log4j-1.2.15.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-server/9.4.3.v20170317/jetty-server-9.4.3.v20170317.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-servlet/9.4.3.v20170317/jetty-servlet-9.4.3.v20170317.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-util/9.4.3.v20170317/jetty-util-9.4.3.v20170317.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-http/9.4.3.v20170317/jetty-http-9.4.3.v20170317.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-io/9.4.3.v20170317/jetty-io-9.4.3.v20170317.jar:$HOME/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:$HOME/.m2/repository/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar com.trentlarson.forecast.core.helper.JettyServer
