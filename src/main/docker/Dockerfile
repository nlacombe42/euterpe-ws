FROM alpine:3.12
RUN apk update && apk add openjdk11 bash

COPY startApp.sh /app/startApp.sh
RUN chmod u+x /app/startApp.sh
ENTRYPOINT ["/app/startApp.sh"]
ENV PORT 8008

ADD *.jar /app/app.jar
