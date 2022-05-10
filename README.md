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

Initalize your SQL with the InitLumSQL.sql file

Currently, Lum makes the assumtion that it is ran on Linux-like OS (Currently Debian 11) and that the lanuage is not Turkish.
