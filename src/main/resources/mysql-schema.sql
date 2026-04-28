-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- 主机： localhost
-- 生成日期： 2026-04-28 21:59:17
-- 服务器版本： 8.0.36
-- PHP 版本： 8.0.26

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `reactor_netdisk`
--
CREATE DATABASE IF NOT EXISTS `reactor_netdisk` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `reactor_netdisk`;

-- --------------------------------------------------------

--
-- 表的结构 `file`
--

DROP TABLE IF EXISTS `file`;
CREATE TABLE IF NOT EXISTS `file` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `user_id` int NOT NULL,
                                      `folder_id` bigint DEFAULT NULL COMMENT '文件夹id，若文件在根目录则为null',
                                      `name` varchar(255) NOT NULL COMMENT '上传时文件名',
    `public_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '此文件是否公开',
    `description` varchar(255) DEFAULT NULL,
    `path_name` varchar(255) NOT NULL COMMENT '文件存储路径',
    `size` bigint NOT NULL COMMENT '文件大小，字节',
    `mime_type` varchar(255) DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_user_folder` (`user_id`,`folder_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- 表的结构 `file_token`
--

DROP TABLE IF EXISTS `file_token`;
CREATE TABLE IF NOT EXISTS `file_token` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `file_id` mediumtext NOT NULL,
                                            `token` varchar(50) NOT NULL,
    `expire_at` datetime NOT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `file_token_token_index` (`token`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- 表的结构 `folder`
--

DROP TABLE IF EXISTS `folder`;
CREATE TABLE IF NOT EXISTS `folder` (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `user_id` int NOT NULL,
                                        `parent_id` bigint DEFAULT NULL COMMENT '父文件夹id，若文件夹在根目录则为null',
                                        `name` varchar(255) NOT NULL,
    `public_flag` tinyint(1) DEFAULT '0',
    `description` varchar(255) DEFAULT NULL COMMENT '描述',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `folder_user_parent_name_index` (`user_id`, `parent_id`, `name`) COMMENT '同一用户同一目录下不允许同名文件夹',
    KEY `idx_user_folder` (`user_id`,`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- 表的结构 `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
                                       `id` int NOT NULL AUTO_INCREMENT,
                                       `username` varchar(20) NOT NULL,
    `password` varchar(20) DEFAULT NULL,
    `email` varchar(50) DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`),
    KEY `email_index` (`email`),
    KEY `username_index` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- 表的结构 `user_token`
--

DROP TABLE IF EXISTS `user_token`;
CREATE TABLE IF NOT EXISTS `user_token` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `user_id` int NOT NULL,
                                            `token` varchar(50) NOT NULL,
    `expire_at` datetime NOT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `user_id_index` (`user_id`),
    KEY `user_token_token_index` (`token`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
