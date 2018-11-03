FROM openjdk:11-slim

RUN mkdir -p /opt/app

WORKDIR /opt/app

COPY ./shrinkwrap-start.sh ./target/scala-2.12/app-assembly.jar ./

ENTRYPOINT ["./shrinkwrap-start.sh"]