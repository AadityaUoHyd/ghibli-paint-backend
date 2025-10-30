FROM eclipse-temurin:24-jdk-alpine
WORKDIR /app
RUN apk update && apk upgrade --no-cache && rm -rf /var/cache/apk/*
COPY target/ghibli-paint-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]