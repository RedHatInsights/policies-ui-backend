FROM registry.access.redhat.com/ubi7-minimal

LABEL maintainer="hrupp@redhat.com"
RUN microdnf install java-11-openjdk-headless && microdnf clean all

ENV quarkus.http.port=8080 \
    quarkus.datasource.url=jdbc:postgresql://localhost:5432/postgres \
    quarkus.datasource.username=sarah \
    quarkus.datasource.password=connor \
    engine/mp-rest/url=http://localhost:8083


EXPOSE 8080

COPY lib /lib
COPY app-1.0-SNAPSHOT-runner.jar /

CMD "java" "-jar" "app-1.0-SNAPSHOT-runner.jar"

