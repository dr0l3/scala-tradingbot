#!/usr/bin/env bash

docker run -it --rm --link trading-postgres:postgres postgres psql -h postgres -U postgres