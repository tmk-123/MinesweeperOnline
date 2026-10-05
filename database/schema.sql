-- ==================================================
-- MINESWEEPER ONLINE - DATABASE SCHEMA (MySQL)
-- ==================================================

CREATE DATABASE IF NOT EXISTS `minesweeper_online`
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `minesweeper_online`;

-- 1. Table: User
-- Note: 'User' is a reserved keyword in some SQL engines, wrapped with backticks
CREATE TABLE IF NOT EXISTS `User` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `totalScore` INT NOT NULL DEFAULT 0,
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Table: Match
-- Note: 'Match' is a reserved keyword in MySQL 8.0.17+, wrapped with backticks
CREATE TABLE IF NOT EXISTS `Match` (
    `id` VARCHAR(64) PRIMARY KEY,
    `status` VARCHAR(20) NOT NULL DEFAULT 'FINISHED',
    `startedAt` TIMESTAMP NULL,
    `endedAt` TIMESTAMP NULL,
    `winnerId` INT NULL,
    `result` VARCHAR(50) NULL,
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_match_winner` FOREIGN KEY (`winnerId`) REFERENCES `User` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Table: MatchPlayer
-- Exactly two MatchPlayers per Match (User 1 - N MatchPlayer, Match 1 - N MatchPlayer)
CREATE TABLE IF NOT EXISTS `MatchPlayer` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `matchId` VARCHAR(64) NOT NULL,
    `userId` INT NOT NULL,
    `result` VARCHAR(20) NOT NULL,
    `openedSafeCells` INT NOT NULL DEFAULT 0,
    `totalActions` INT NOT NULL DEFAULT 0,
    `flagsPlaced` INT NOT NULL DEFAULT 0,
    `score` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_mp_match` FOREIGN KEY (`matchId`) REFERENCES `Match` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mp_user` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uq_match_user` UNIQUE (`matchId`, `userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample initial test data
INSERT INTO `User` (`username`, `password`, `totalScore`)
VALUES 
    ('alice', '123456', 25),
    ('bob', '123456', 15),
    ('charlie', '123456', 10)
ON DUPLICATE KEY UPDATE `username` = `username`;
