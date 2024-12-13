FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY out/artifacts/music_jar/music.jar app.jar

RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    libxrender \
    libxext && \
    apk add --no-cache zip && \
    zip -d app.jar 'META-INF/*.SF' 'META-INF/*.RSA' || true

ENV PREFIX="ish" \
    TOKEN="" \
    LAVALINK="http://localhost:8080" \
    WEBSOCKET="ws://localhost:5034/animal" \
    CLIENT_ID="1"

CMD ["java", "-jar", "app.jar"]