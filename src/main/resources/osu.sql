# Host: localhost  (Version 5.7.20-0ubuntu0.16.04.1)
# Date: 2017-12-08 16:52:50
# Generator: MySQL-Front 6.0  (Build 2.20)


#
# Structure for table "bgfile"
#

CREATE TABLE `bgfile` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `sid` int(11) unsigned NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `data` longblob NOT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

#
# Structure for table "osufile"
#

CREATE TABLE `osufile` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `bid` int(11) unsigned NOT NULL DEFAULT '0',
  `data` longtext NOT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;

#
# Structure for table "resource"
#

CREATE TABLE `resource` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `data` longblob NOT NULL,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=217 DEFAULT CHARSET=utf8;

#
# Structure for table "score"
#

CREATE TABLE `score` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `beatmap_id` int(11) DEFAULT NULL,
  `mode` bit(1) DEFAULT NULL,
  `score_version` int(11) DEFAULT NULL,
  `map_md5` varchar(32) DEFAULT NULL,
  `rep_md5` varchar(32) DEFAULT NULL,
  `size` int(11) DEFAULT '-1',
  `score` bigint(20) DEFAULT NULL,
  `max_combo` int(11) DEFAULT NULL,
  `count50` int(11) DEFAULT NULL,
  `count100` int(11) DEFAULT NULL,
  `count300` int(11) DEFAULT NULL,
  `count_miss` int(11) DEFAULT NULL,
  `count_katu` int(11) DEFAULT NULL,
  `count_geki` int(11) DEFAULT NULL,
  `perfect` int(1) DEFAULT NULL,
  `enabled_mods` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `rank` varchar(2) DEFAULT NULL,
  `pp` decimal(10,2) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `online_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=1279 DEFAULT CHARSET=utf8;

#
# Structure for table "userinfo"
#

CREATE TABLE `userinfo` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `count300` int(11) DEFAULT NULL,
  `count100` int(11) DEFAULT NULL,
  `count50` int(11) DEFAULT NULL,
  `playcount` int(11) DEFAULT NULL,
  `accuracy` decimal(12,2) DEFAULT NULL,
  `pp_raw` decimal(8,3) DEFAULT NULL,
  `ranked_score` bigint(10) DEFAULT NULL,
  `total_score` bigint(10) DEFAULT NULL,
  `level` decimal(10,2) DEFAULT NULL,
  `pp_rank` int(11) DEFAULT NULL,
  `count_rank_ss` int(11) DEFAULT NULL,
  `count_rank_s` int(11) DEFAULT NULL,
  `count_rank_a` int(11) DEFAULT NULL,
  `queryDate` date DEFAULT NULL,
  PRIMARY KEY (`Id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=80966 DEFAULT CHARSET=utf8;

#
# Structure for table "userrole"
#

CREATE TABLE `userrole` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `role` varchar(255) NOT NULL DEFAULT 'creep',
  `qq` bigint(13) DEFAULT '0',
  `legacy_uname` varchar(255) DEFAULT NULL,
  `current_uname` varchar(255) DEFAULT NULL,
  `is_banned` tinyint(1) unsigned DEFAULT '0',
  `repeat_count` bigint(10) unsigned DEFAULT '0',
  `speaking_count` bigint(10) unsigned DEFAULT '0',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `唯一索引` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1744 DEFAULT CHARSET=utf8;
