FROM openjdk:11
ADD target/soft-iot-dlt-aca-py-1.0-jar-with-dependencies.jar soft-iot-dlt-aca-py-1.0-jar-with-dependencies.jar
ENTRYPOINT ["java", "-jar","soft-iot-dlt-aca-py-1.0-jar-with-dependencies.jar"]