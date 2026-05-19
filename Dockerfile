FROM amazoncorretto:25-alpine3.21

WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
RUN mkdir -p /app/log && chown -R appuser:appgroup /app

COPY --chown=appuser:appgroup build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=UTC", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=60.0", "-XX:InitialRAMPercentage=60.0", "-XX:MaxMetaspaceSize=150m", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/log/heapdump.hprof", "-jar", "app.jar"]
