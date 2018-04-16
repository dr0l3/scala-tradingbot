#!/usr/bin/env bash

docker run --name trading-postgres -p 5432:5432 -e POSTGRES_PASSWORD=password -d postgres