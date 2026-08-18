FROM alpine:3.20
RUN apk add --no-cache postfix postfix-pcre libsasl2
EXPOSE 25 587 465
CMD ["postfix", "start-fg"]
