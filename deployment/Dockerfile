FROM maven:3.9.6-eclipse-temurin-11 as builder

COPY . /tmp

RUN cd /tmp && mvn package

FROM alpine:3.19.0 as dependencies

ENV MYSQL_DRIVER_VERSION=mysql-connector-j-8.2.0

RUN apk update && apk add curl unzip
WORKDIR /tmp/mysql
RUN curl --location --output "${MYSQL_DRIVER_VERSION}.zip" "https://dev.mysql.com/get/Downloads/Connector-J/${MYSQL_DRIVER_VERSION}.zip"
RUN unzip "${MYSQL_DRIVER_VERSION}.zip" && cp ${MYSQL_DRIVER_VERSION}/*.jar .

FROM payara/server-full:5.2020.3-jdk11

ENV GLASSFISH_HOME=/opt/payara/appserver
ENV PREBOOT_COMMANDS=/opt/custom/create-datasource.asadmin

COPY deployment/create-datasource.asadmin "${PREBOOT_COMMANDS}"
COPY --from=dependencies /tmp/mysql/*.jar "${GLASSFISH_HOME}/glassfish/lib/"
COPY --from=builder /tmp/target/*.war "${GLASSFISH_HOME}/../deployments/hmis.war"

EXPOSE 8080/tcp
EXPOSE 4848/tcp