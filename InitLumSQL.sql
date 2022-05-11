-- phpMyAdmin SQL Dump
-- version 5.1.0
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: May 11, 2022 at 10:29 AM
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
CREATE DATABASE IF NOT EXISTS `lum` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `lum`;

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`cmnClientLogger`@`%` PROCEDURE `GetSteamWatch` (IN `ChangeNumber` INT UNSIGNED, IN `GameID` INT UNSIGNED)  BEGIN
	UPDATE `Config` SET `value` = ChangeNumber WHERE `Config`.`setting` = 'LastSteamChange';
	SELECT * FROM `SteamWatch` WHERE `SteamWatch`.GameID = GameID;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `Config`
--

CREATE TABLE IF NOT EXISTS `Config` (
  `setting` varchar(32) NOT NULL,
  `value` int NOT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `SteamWatch`
--

CREATE TABLE IF NOT EXISTS `SteamWatch` (
  `ukey` int NOT NULL AUTO_INCREMENT,
  `GameID` bigint UNSIGNED NOT NULL,
  `ServerID` bigint UNSIGNED NOT NULL,
  `ChannelID` bigint UNSIGNED NOT NULL,
  `publicMention` varchar(1024) DEFAULT NULL,
  `betaMention` varchar(1024) DEFAULT NULL,
  `TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `ukey` (`ukey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `strings`
--

CREATE TABLE IF NOT EXISTS `strings` (
  `string` text NOT NULL,
  `value` text NOT NULL,
  UNIQUE KEY `string` (`string`(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `url` text COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
