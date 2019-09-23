FROM openjdk:latest
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
CMD ["java", "-jar", "exp_jar.jar"]
