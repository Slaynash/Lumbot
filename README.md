# Lumbot

Lum Discord Bot

## Installation

Make a file in the working path called `.settings.conf` with the following content add your own Discord token and SQL database:

```
DISCORD_TOKEN=
MAIN_BOT=
PING_URL=

DB_ADDRESS=
DB_PORT=
DB_DATABASELUM=lum
DB_DATABASESHORTURL=shorturls
DB_LOGIN=
DB_PASSWORD=

DISCORD_PREFIX=l!
CURSEFORGE_API_KEY=
GitHub_API_KEY=
ANIMESCHEDULE_API_KEY=
```

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

---

## Licence

This work is licensed under a
[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License][cc-by-nc-sa].

[![CC BY-NC-SA 4.0][cc-by-nc-sa-image]][cc-by-nc-sa]

[cc-by-nc-sa]: http://creativecommons.org/licenses/by-nc-sa/4.0/
[cc-by-nc-sa-image]: https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png
[cc-by-nc-sa-shield]: https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg