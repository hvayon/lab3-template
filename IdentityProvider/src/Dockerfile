FROM maven:3.8-amazoncorretto-19 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:19 as builder
WORKDIR identity_provider
ARG JAR_FILE=target/*.jar
COPY --from=build ${JAR_FILE} flight_service.jar
RUN java -Djarmode=layertools -jar flight_service.jar extract
LABEL authors="Губеева Наталья ИУ7-11МВ"

FROM amazoncorretto:19
ENV PORT=9090
WORKDIR identity_provider
COPY --from=builder identity_provider/spring-boot-loader/ ./
COPY --from=builder identity_provider/dependencies/ ./
COPY --from=builder identity_provider/snapshot-dependencies/ ./
COPY --from=builder identity_provider/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]