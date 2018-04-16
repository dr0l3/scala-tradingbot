# Thoughts

## General requirements

- Want all parts of the application to be scalable
- Thus we also want all parts of the application to be as parallel as possible
- We would like the game to be strictly consistent when all players are update for a date
- But we are okay with progressing in an "eventually consistent manner" meaning some players might be behind other players
- We would like to in theory be able to move the application nodes between hosts during play

## Data strategy

Given that we have two kinds of data
- Data that need to be persisted between runs
- Data that is not needed to be persisted between runs

It makes sense to save some of it in truly persistent storage (postgres).
While other parts are saved in less persistent storage (redis).

The general algorithm for ticks will thus be

1) Fetch all users in Postgres
2) Fetch their holdings in Redis
3) Calculate new holdings
4) Update Redis

This also means we can just use binary serialization for the parts that go in Redis thus simplifying the persistence.

## Persistent data

The following data is saved between runs and never updated during games.

- Strategies
- User data
- Prices

## Transient data

The following data is not saved between run and is updated. Thus

- Holdings
- Capital

## Redis persistence

We save ids of users and strategies in postgres.
These are then used as keys in Redis to look up transient game data.