FROM amazoncorretto:25-alpine3.21

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
RUN mkdir -p /app/log && chown -R appuser:appgroup /app

COPY --chown=appuser:appgroup build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
