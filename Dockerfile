FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-wqy-zenhei fonts-noto-cjk fontconfig \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/ai-job-agent-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=render
ENV PORT=8080

EXPOSE 8080

CMD ["sh", "-c", "java -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE} --server.port=${PORT}"]
