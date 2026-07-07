FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY shared-lib/pom.xml shared-lib/pom.xml
COPY management-system/be/pom.xml management-system/be/pom.xml

RUN mvn -f shared-lib/pom.xml -B dependency:go-offline

COPY shared-lib shared-lib
RUN mvn -f shared-lib/pom.xml -B clean install -DskipTests

RUN mvn -f management-system/be/pom.xml -B dependency:go-offline

COPY management-system/be management-system/be
RUN mvn -f management-system/be/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /workspace/management-system/be/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
