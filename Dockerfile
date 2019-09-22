# Source: https://github.com/docker-library/openjdk/issues/158
FROM openjdk:latest
RUN apt-get update && apt-get install -y --no-install-recommends openjfx && rm -rf /var/lib/apt/lists/*
COPY target/beer-test-0.1.jar /demo.jar
CMD ["java", "-jar", "/demo.jar"]