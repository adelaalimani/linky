# Stage 1: Build the application
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests -B

# Stage 2: Custom Java 25 runtime
FROM eclipse-temurin:25-jdk-alpine AS jre-build

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.sql,java.naming,java.desktop,java.management,java.instrument,java.security.jgss,java.xml,java.logging,java.net.http,java.prefs,java.security.sasl,java.transaction.xa,jdk.unsupported,jdk.crypto.ec,java.compiler \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-6 \
    --output /javaruntime
# Stage 3: Run the application
FROM alpine:3.21

RUN apk add --no-cache ca-certificates

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=jre-build /javaruntime $JAVA_HOME

WORKDIR /opt/app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]