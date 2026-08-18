# RainLoop webmail UI, served under the /mail subpath.
#
#   docker build -t localhost:5000/shopfast/mail:v4 k8s/services/mail
#   docker push localhost:5000/shopfast/mail:v4
#
# The container listens on :80 with nginx + php-fpm (supervisord). RainLoop is
# installed at /var/www/rainloop and its basePath is set to /mail/ so it emits
# /mail/* URLs; the ingress rewrites /mail/* -> / at this pod.
FROM php:8.3-fpm-alpine AS php

FROM nginx:1.27-alpine

# php-fpm + unzip (to fetch RainLoop) + the extensions RainLoop needs.
RUN apk add --no-cache \
        php83-fpm php83-phar php83-ctype php83-curl php83-dom php83-gd \
        php83-intl php83-mbstring php83-openssl php83-session php83-xml \
        php83-zip php83-fileinfo php83-posix php83-iconv \
        supervisor curl unzip tzdata

# RainLoop app code.
ENV RAINLOOP_VERSION=1.17.0
RUN mkdir -p /var/www/rainloop \
    && curl -fsSL "https://github.com/RainLoop/rainloop-webmail/releases/download/v${RAINLOOP_VERSION}/rainloop-legacy-${RAINLOOP_VERSION}.zip" -o /tmp/rainloop.zip \
    && unzip -q /tmp/rainloop.zip -d /var/www/rainloop \
    && rm -f /tmp/rainloop.zip \
    && chown -R nginx:nginx /var/www/rainloop \
    && chmod -R 755 /var/www/rainloop

# Bake the subpath config so RainLoop generates /mail/* URLs.
COPY rainloop-config/ /var/www/rainloop/data/_data_/_default_/configs/
RUN chown -R nginx:nginx /var/www/rainloop/data

# php-fpm over TCP (127.0.0.1:9000) to avoid socket chown issues under
# dropped capabilities; drop the default pool so only ours runs.
# NOTE: on the Alpine nginx image the include dir is /etc/nginx/conf.d/.
COPY php-fpm.conf /etc/php83/php-fpm.d/zz-rainloop.conf
RUN rm -f /etc/php83/php-fpm.d/www.conf
COPY nginx.conf   /etc/nginx/conf.d/default.conf
COPY supervisord.conf /etc/supervisord.conf

EXPOSE 80
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
