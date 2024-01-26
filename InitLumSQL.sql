-- phpMyAdmin SQL Dump
-- version 5.1.0
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Jan 26, 2024 at 07:39 PM
-- Server version: 8.0.28
-- PHP Version: 8.0.16

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `lum`
--
CREATE DATABASE IF NOT EXISTS `lum` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `lum`;

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`cmnClientLogger`@`%` PROCEDURE `FetchIcon` (IN `name` CHAR(128))  BEGIN
SELECT * FROM Icons WHERE `UnityName` = name;
UPDATE Icons SET `Counter` = `Counter`+1, `LastUsed` = CURRENT_TIMESTAMP WHERE `UnityName` = name;
END$$

CREATE DEFINER=`cmnClientLogger`@`%` PROCEDURE `GetSteamWatch` (IN `ChangeNumber` INT UNSIGNED, IN `GameID` INT UNSIGNED)  BEGIN
	SELECT * FROM `SteamWatch` WHERE `SteamWatch`.GameID = GameID;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `blacklistusername`
--

CREATE TABLE IF NOT EXISTS `blacklistusername` (
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'all usernames must be lowercase',
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `GuildConfigurations`
--

CREATE TABLE IF NOT EXISTS `GuildConfigurations` (
  `GuildID` bigint NOT NULL,
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ScamShield` tinyint(1) NOT NULL DEFAULT '0',
  `ScamShieldBan` tinyint(1) NOT NULL DEFAULT '0',
  `ScamShieldCross` tinyint(1) NOT NULL DEFAULT '0',
  `ScamShieldDm` tinyint(1) NOT NULL DEFAULT '0',
  `MLLogScan` tinyint(1) NOT NULL DEFAULT '0',
  `MLLogReaction` tinyint(1) NOT NULL DEFAULT '0',
  `MLReplies` tinyint(1) NOT NULL DEFAULT '0',
  `MLPartialRemover` tinyint(1) NOT NULL DEFAULT '0',
  `MLGeneralRemover` tinyint(1) NOT NULL DEFAULT '0',
  `DLLRemover` tinyint(1) NOT NULL DEFAULT '0',
  `LumReplies` tinyint(1) NOT NULL DEFAULT '0',
  `DadJokes` tinyint(1) NOT NULL DEFAULT '0',
  `Created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `GuildID` (`GuildID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `Icons`
--

CREATE TABLE IF NOT EXISTS `Icons` (
  `UnityName` char(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'must be all lowercase and no spaces',
  `IconURL` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `PirateURL` text COLLATE utf8mb4_unicode_ci,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `LastUsed` timestamp NULL DEFAULT NULL,
  `Counter` int NOT NULL DEFAULT '0',
  `MLoverride` char(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`UnityName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `Memes`
--

CREATE TABLE IF NOT EXISTS `Memes` (
  `GuildID` bigint UNSIGNED NOT NULL,
  `MemeChannel` bigint NOT NULL,
  `ReportChannel` bigint DEFAULT NULL,
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user` bigint NOT NULL,
  `ID` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `MessagePairs`
--

CREATE TABLE IF NOT EXISTS `MessagePairs` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `OGMessage` bigint UNSIGNED NOT NULL,
  `DevMessage` bigint UNSIGNED NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `Reminders`
--

CREATE TABLE IF NOT EXISTS `Reminders` (
  `ID` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `UserID` bigint UNSIGNED NOT NULL,
  `ServerID` bigint UNSIGNED DEFAULT NULL,
  `ChannelID` bigint UNSIGNED NOT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `TSend` timestamp NOT NULL,
  `Message` text COLLATE utf8mb4_unicode_ci,
  `Color` int DEFAULT '-13223617',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `Replies`
--

CREATE TABLE IF NOT EXISTS `Replies` (
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ukey` int NOT NULL AUTO_INCREMENT,
  `guildID` bigint NOT NULL,
  `regex` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contains` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `equals` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user` bigint DEFAULT NULL,
  `message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bdelete` tinyint(1) NOT NULL DEFAULT '0',
  `bkick` tinyint(1) NOT NULL DEFAULT '0',
  `bban` tinyint(1) NOT NULL DEFAULT '0',
  `bbot` tinyint(1) NOT NULL DEFAULT '0',
  `bedit` tinyint(1) NOT NULL DEFAULT '0',
  `breport` tinyint(1) NOT NULL DEFAULT '0',
  `channel` bigint DEFAULT NULL,
  `ignorerole` bigint DEFAULT NULL,
  `lastedited` bigint DEFAULT NULL,
  PRIMARY KEY (`ukey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `SteamApp`
--

CREATE TABLE IF NOT EXISTS `SteamApp` (
  `GameID` bigint UNSIGNED NOT NULL,
  `Depot` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `GameID` (`GameID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `SteamTags`
--

CREATE TABLE IF NOT EXISTS `SteamTags` (
  `tagid` int UNSIGNED NOT NULL,
  `name` char(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`tagid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `SteamWatch`
--

CREATE TABLE IF NOT EXISTS `SteamWatch` (
  `ukey` int NOT NULL AUTO_INCREMENT,
  `GameID` bigint UNSIGNED NOT NULL,
  `ServerID` bigint UNSIGNED NOT NULL,
  `ChannelID` bigint UNSIGNED NOT NULL,
  `publicMention` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `betaMention` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `otherMention` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `ukey` (`ukey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Database: `shorturls`
--
CREATE DATABASE IF NOT EXISTS `shorturls` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `shorturls`;

-- --------------------------------------------------------

--
-- Table structure for table `shorturls`
--

CREATE TABLE IF NOT EXISTS `shorturls` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` varchar(64) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
