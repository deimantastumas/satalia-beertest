FROM openjdk:latest
COPY target/beer-test-0.1.jar /demo.jar
CMD ["java", "-jar", "/demo.jar"]