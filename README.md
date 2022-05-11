# Lumbot

Lum Discord Bot

## Installation

Make a file in the working path called `.settings.conf` with the following content add your own Discord token and SQL database:

```
DISCORD_TOKEN=
DB_ADDRESS=
DB_PORT=
DB_DATABASELUM=lum
DB_DATABASESHORTURL=shorturls
DB_LOGIN=DB_PASSWORD=
DISCORD_PREFIX=l!
```

also add [localization.json](https://raw.githubusercontent.com/Slaynash/Lumbot-translations/main/localization.json "localization.json") to your working path

Initialize your SQL with the InitLumSQL.sql file

Currently, Lum assumes that it is run on Linux-like OS (Currently Debian 11) and that the language is not Turkish.

## Features

* MelonLoader Log scans
* Scammer remover (Scam Shield)
* Steam Depo change notifications
* Dad Jokes
* Funny Replies
* ABCDEFG game
* Moderation (Ban, Kick, Purge, Reply Purge)
* Remove EXE, ZIP, DLL, and other suspicious messages from non-staff
* Auto Publish announcement channels
* Dump IDs (get all ID from regex on username)
* Custom Rank Colors
* Custom Replies (also via regex) (also reactions)
* Give Role on successful member screening
* Give Role on reaction
* Verifier
* DM message Proxy to Devs
