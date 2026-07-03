FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
ENV JAVA_OPTS=""

COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
