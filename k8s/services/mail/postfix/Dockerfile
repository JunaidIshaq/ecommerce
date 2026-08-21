FROM alpine:3.20
RUN apk add --no-cache postfix postfix-pcre cyrus-sasl cyrus-sasl-login opendkim
EXPOSE 25 587 465

# --- DKIM -------------------------------------------------------------------
# Signing key for shopfast.live (selector: shopfast2026).
# Public key is published as a TXT record at shopfast2026._domainkey.shopfast.live
RUN mkdir -p /etc/opendkim/keys /var/spool/postfix/opendkim \
    && chown -R opendkim:opendkim /etc/opendkim \
    && chown opendkim:postfix /var/spool/postfix/opendkim \
    && chmod 0755 /etc/opendkim /var/spool/postfix/opendkim \
    && chmod 0775 /var/spool/postfix/opendkim

COPY shopfast2026.private /etc/opendkim/keys/shopfast2026.private
# opendkim runs as the opendkim user (key-dir security check requires the key dir
# to be owned by the executing uid). The chroot socket dir is group postfix so the
# postfix cleanup process can connect to it.
RUN chmod 0640 /etc/opendkim/keys/shopfast2026.private \
    && chown opendkim:opendkim /etc/opendkim/keys/shopfast2026.private

# opendkim signing table + key table + trusted hosts
RUN printf '*@shopfast.live shopfast2026._domainkey.shopfast.live\n' > /etc/opendkim/SigningTable \
    && printf 'shopfast2026._domainkey.shopfast.live shopfast.live:shopfast2026:/etc/opendkim/keys/shopfast2026.private\n' > /etc/opendkim/KeyTable \
    && printf '127.0.0.1\n10.0.0.0/8\n172.16.0.0/12\n192.168.0.0/16\n::1\n' > /etc/opendkim/TrustedHosts \
    && chown -R opendkim:opendkim /etc/opendkim

COPY opendkim.conf /etc/opendkim/opendkim.conf

# postfix: pass mail through the opendkim milter for signing
# Socket lives inside postfix's chroot (/var/spool/postfix) so cleanup/qmgr can reach it.
RUN postconf -M 2>/dev/null; \
    postconf 'smtpd_milters=unix:/var/spool/postfix/opendkim/opendkim.sock' \
             'non_smtpd_milters=unix:/var/spool/postfix/opendkim/opendkim.sock' \
             'milter_default_action=accept' \
             'milter_protocol=6'

CMD ["/bin/sh", "-c", "opendkim -x /etc/opendkim/opendkim.conf & sleep 2 && postfix start-fg"]
