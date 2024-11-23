FROM openjdk:17-jdk-slim

WORKDIR /app

COPY out/artifacts/music_jar/music.jar app.jar

ENV PREFIX="ish" \
    TOKEN="" \
    LAVALINK="http://localhost:8080" \
    WEBSOCKET="ws://localhost:5034/animal" \
    CLIENT_ID="1"

RUN apt-get update && \
    apt-get install -y zip && \
    zip -d app.jar 'META-INF/*.SF' 'META-INF/*.RSA' && \
    apt-get clean && rm -rf /var/lib/apt/lists/*


CMD ["java", "-jar", "app.jar"]
