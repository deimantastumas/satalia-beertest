# Beertest - SATALIA
Coding challenge

You are a crazy beer-fanatic millionaire. It's the weekend. Your location is LONG/LAT. You have a perfect helicopter with enough fuel to fly 2000 kilometers. You have an idea to visit as many beer factories within that distance as you can and collect as many beer types as you can for your party this weekend.

Program accepts following inputs:
 - Home location (latitude, longitude)
 - Priority (breweries or beer types)
 
 Program uses Docker to run MySQL inside a container. 
 
# Requirements:
 - docker-compose https://docs.docker.com/compose/
 - JDK*
 
# Usage
Install docker launcher, clone the project and run following commands:
 - "docker-compose up -d" - runs mysql server inside a docker container. Initially loads .sql dump.
 
 - "java -jar beer-test.jar"
 
Source code also has a GUI version, however it had some problems so I decided to stick with console.

 *Project was intended to run without JDK (using another docker container containing OpenJDK). Sadly it failed to connect to mysql server :(
 
 