FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y dovecot-core dovecot-imapd && \
    usermod -aG shadow dovecot && \
    groupadd -g 100 users || true && \
    usermod -aG mail admin || true && \
    rm -rf /var/lib/apt/lists/*
EXPOSE 143 2525
CMD ["dovecot", "-F"]
