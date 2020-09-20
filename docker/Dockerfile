FROM curlimages/curl:latest AS download
ARG VERSION
# We use a non-trusted CA, so we have to fall back to curl instead of directly using 'ADD ...'
RUN curl -k -o /tmp/app.jar https://colossus.kruemel.home/nexus/repository/imageserver/ch/guengel/imageserver/$VERSION/imageserver-$VERSION.jar

FROM --platform=linux/arm64 openjdk:11-jre-slim

COPY --from=download /tmp/app.jar /
USER 424242

ENTRYPOINT ["/usr/local/openjdk-11/bin/java"]
CMD ["-Dlogback.configurationFile=logback-graylog.xml", "-jar", "/app.jar"]