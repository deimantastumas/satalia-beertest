FROM openjdk:latest
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
CMD ["java", "-jar", "beertest-nogui.jar"]

#FROM  phusion/baseimage:0.9.17
#MAINTAINER  Author Name <author@email.com>
#RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
#RUN apt-get -y update
