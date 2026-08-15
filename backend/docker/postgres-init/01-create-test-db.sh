#!/bin/sh
# Runs once, on first container init, via /docker-entrypoint-initdb.d.
# POSTGRES_DB only creates the dev database; the test profile needs its own
# database so `mvn test` never touches dev data.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE ${POSTGRES_DB}_test'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${POSTGRES_DB}_test')\gexec
EOSQL
