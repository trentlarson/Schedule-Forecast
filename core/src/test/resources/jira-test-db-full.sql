--
-- See README.txt for usage.
--
-- MySQL dump 10.13  Distrib 5.1.50, for apple-darwin10.3.0 (i386)
--
-- Host: localhost    Database: test_forecast_jira
-- ------------------------------------------------------
-- Server version	5.1.50

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `component`
--

DROP TABLE IF EXISTS `component`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `component` (
  `ID` decimal(18,0) NOT NULL,
  `PROJECT` decimal(18,0) DEFAULT NULL,
  `cname` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `description` text CHARACTER SET utf8,
  `URL` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `LEAD` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `ASSIGNEETYPE` decimal(18,0) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `component`
--

LOCK TABLES `component` WRITE;
/*!40000 ALTER TABLE `component` DISABLE KEYS */;
INSERT INTO `component` VALUES ('10024','10010','Linux Admin',NULL,NULL,'jeremy','1'),('10025','10010','Dev - Admin',NULL,NULL,'jeremy','1'),('10033','10010','UI - Back-Office',NULL,NULL,'jeremy','1'),('10021','10010','Dev - Back-Office',NULL,NULL,'jeremy','1'),('10034','10010','UI - Capture Pages',NULL,NULL,'jeremy','1'),('10020','10010','Dev - Replicated Pages',NULL,NULL,'jeremy','1'),('10032','10010','UI - Admin',NULL,NULL,'jeremy','1'),('10031','10010','InfoTrax',NULL,NULL,'jeremy','1'),('10035','10010','UI - Replicated Pages',NULL,NULL,'jeremy','1'),('10040','10021','Max.com',NULL,NULL,NULL,'2'),('10041','10021','Text Mesaging System',NULL,NULL,NULL,'2'),('10042','10021','Philippines',NULL,NULL,NULL,'2'),('10043','10021','Max Living Site',NULL,NULL,NULL,'2'),('10044','10021','Mobile Device Support',NULL,NULL,NULL,'2'),('10045','10021','Email',NULL,NULL,NULL,'2'),('10046','10010','Text messaging System',NULL,NULL,'jeremy','1'),('10047','10010','Mobile Device Support',NULL,NULL,'jeremy','1'),('10060','10021','InHouse','Applications for corporate use only',NULL,'jeff','2'),('10080','10020','Reports',NULL,NULL,NULL,'0'),('10081','10020','Data Mining',NULL,NULL,NULL,'0'),('10022','10010','Dev - Capture Pages',NULL,NULL,'jeremy','1'),('10082','10021','New Zealand',NULL,NULL,NULL,'0'),('20100','20100','PHB Beauticians',NULL,NULL,NULL,NULL),('20101','20100','Wally Cup Holders',NULL,NULL,NULL,NULL),('20102','20100','Alice Face Straighteners',NULL,NULL,NULL,NULL),('20103','20100','Alice Limb Holders',NULL,NULL,NULL,NULL),('20104','20200','Catbert Form Handlers',NULL,NULL,NULL,NULL),('20105','20200','Catbert Mood Regulaters',NULL,NULL,NULL,NULL),('20106','20200','Dogbert Psychiatric Handlers',NULL,NULL,NULL,NULL),('20300','20300','PHB Beauticians',NULL,NULL,NULL,NULL),('20301','20300','Wally Cup Holders',NULL,NULL,NULL,NULL),('20302','20300','Alice Face Straighteners',NULL,NULL,NULL,NULL),('20303','20300','Alice Limb Holders',NULL,NULL,NULL,NULL),('20304','20400','Catbert Form Handlers',NULL,NULL,NULL,NULL),('20305','20400','Catbert Mood Regulaters',NULL,NULL,NULL,NULL),('20306','20400','Dogbert Psychiatric Handlers',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `component` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customfieldvalue`
--

DROP TABLE IF EXISTS `customfieldvalue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customfieldvalue` (
  `ID` decimal(18,0) NOT NULL,
  `ISSUE` decimal(18,0) DEFAULT NULL,
  `CUSTOMFIELD` decimal(18,0) DEFAULT NULL,
  `PARENTKEY` varchar(255) DEFAULT NULL,
  `STRINGVALUE` varchar(255) DEFAULT NULL,
  `NUMBERVALUE` decimal(18,6) DEFAULT NULL,
  `TEXTVALUE` longtext,
  `DATEVALUE` datetime DEFAULT NULL,
  `VALUETYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `cfvalue_issue` (`ISSUE`,`CUSTOMFIELD`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customfieldvalue`
--

LOCK TABLES `customfieldvalue` WRITE;
/*!40000 ALTER TABLE `customfieldvalue` DISABLE KEYS */;
/*!40000 ALTER TABLE `customfieldvalue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issuelink`
--

DROP TABLE IF EXISTS `issuelink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `issuelink` (
  `ID` decimal(18,0) NOT NULL,
  `LINKTYPE` decimal(18,0) DEFAULT NULL,
  `SOURCE` decimal(18,0) DEFAULT NULL,
  `DESTINATION` decimal(18,0) DEFAULT NULL,
  `SEQUENCE` decimal(18,0) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issuelink`
--

LOCK TABLES `issuelink` WRITE;
/*!40000 ALTER TABLE `issuelink` DISABLE KEYS */;
INSERT INTO `issuelink` VALUES ('10281','10002','11960','11961',NULL),('10283','10002','11960','11962',NULL),('10286','10002','11960','11963',NULL),('10291','10004','11965','11960',NULL);
/*!40000 ALTER TABLE `issuelink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issuelinktype`
--

DROP TABLE IF EXISTS `issuelinktype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `issuelinktype` (
  `ID` decimal(18,0) NOT NULL,
  `LINKNAME` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `INWARD` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `OUTWARD` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `pstyle` varchar(60) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issuelinktype`
--

LOCK TABLES `issuelinktype` WRITE;
/*!40000 ALTER TABLE `issuelinktype` DISABLE KEYS */;
INSERT INTO `issuelinktype` VALUES ('10002','Sub-Task','is part of master task','has subtask of',NULL),('10004','Sequence','must be completed before starting','cannot start until completing',NULL);
/*!40000 ALTER TABLE `issuelinktype` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jiraissue`
--

DROP TABLE IF EXISTS `jiraissue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jiraissue` (
  `ID` decimal(18,0) NOT NULL,
  `pkey` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `PROJECT` decimal(18,0) DEFAULT NULL,
  `REPORTER` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `ASSIGNEE` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `issuetype` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `SUMMARY` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `DESCRIPTION` longtext CHARACTER SET utf8,
  `ENVIRONMENT` longtext CHARACTER SET utf8,
  `PRIORITY` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `RESOLUTION` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `issuestatus` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `CREATED` datetime DEFAULT NULL,
  `UPDATED` datetime DEFAULT NULL,
  `DUEDATE` datetime DEFAULT NULL,
  `RESOLUTIONDATE` datetime DEFAULT NULL,
  `VOTES` decimal(18,0) DEFAULT NULL,
  `TIMEORIGINALESTIMATE` decimal(18,0) DEFAULT NULL,
  `TIMEESTIMATE` decimal(18,0) DEFAULT NULL,
  `TIMESPENT` decimal(18,0) DEFAULT NULL,
  `WORKFLOW_ID` decimal(18,0) DEFAULT NULL,
  `SECURITY` decimal(18,0) DEFAULT NULL,
  `FIXFOR` decimal(18,0) DEFAULT NULL,
  `COMPONENT` decimal(18,0) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jiraissue`
--

LOCK TABLES `jiraissue` WRITE;
/*!40000 ALTER TABLE `jiraissue` DISABLE KEYS */;
INSERT INTO `jiraissue` VALUES ('11960','FOURU-1002','10010','trent','trent','1','test issue - master',NULL,NULL,'3',NULL,'1','2010-03-31 12:44:43','2010-04-07 17:53:27',NULL,NULL,'0','0','0',NULL,'12210',NULL,NULL,NULL),('11961','FOURU-1003','10010','trent','trent','1','test issue - first',NULL,NULL,'6',NULL,'1','2010-03-31 12:44:59','2010-03-31 12:50:11',NULL,NULL,'0','864000','864000',NULL,'12211',NULL,NULL,NULL),('11962','FOURU-1004','10010','trent','trent','1','test issue - second',NULL,NULL,'6',NULL,'1','2010-03-31 12:45:33','2010-03-31 12:46:34',NULL,NULL,'0','0','0',NULL,'12212',NULL,NULL,NULL),('11963','FOURU-1005','10010','trent','trent','1','test issue - another',NULL,NULL,'6',NULL,'1','2010-03-31 12:46:50','2010-03-31 12:50:23',NULL,NULL,'0','432000','432000',NULL,'12213',NULL,NULL,NULL),('11964','FOURU-1006','10010','trent','trent','1','test issue - another independant',NULL,NULL,'2',NULL,'1','2010-03-31 12:47:22','2010-06-10 14:27:06',NULL,NULL,'0','604800','604800',NULL,'12214',NULL,NULL,NULL),('11965','FOURU-1007','10010','trent','trent','1','test issue - finalize',NULL,NULL,'2',NULL,'1','2010-03-31 12:49:04','2010-05-25 09:59:33','2010-05-05 00:00:00',NULL,'0','86400','86400',NULL,'12215',NULL,NULL,NULL),('20101','PR1-20101','20100',NULL,NULL,NULL,'one team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20102','PR1-20102','20100',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20103','PR1-20103','20100',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20104','PR1-20104','20100',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20105','PR1-20105','20100',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20106','PR1-20106','20100',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20107','PR1-20107','20100',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20108','PR1-20108','20100',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20109','PR1-20109','20100',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20110','PR1-20110','20100',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20301','PR3-20301','20300',NULL,NULL,NULL,'one team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20302','PR3-20302','20300',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20303','PR3-20303','20300',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20304','PR3-20304','20300',NULL,NULL,NULL,'another team',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20305','PR3-20305','20300',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20306','PR3-20306','20300',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20307','PR3-20307','20300',NULL,NULL,NULL,'another team too',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20308','PR3-20308','20300',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20309','PR3-20309','20300',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL),('20310','PR3-20310','20300',NULL,NULL,NULL,'another team three',NULL,NULL,'4',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'57600',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `jiraissue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nodeassociation`
--

DROP TABLE IF EXISTS `nodeassociation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nodeassociation` (
  `SOURCE_NODE_ID` decimal(18,0) NOT NULL,
  `SOURCE_NODE_ENTITY` varchar(60) CHARACTER SET utf8 NOT NULL,
  `SINK_NODE_ID` decimal(18,0) NOT NULL,
  `SINK_NODE_ENTITY` varchar(60) CHARACTER SET utf8 NOT NULL,
  `ASSOCIATION_TYPE` varchar(60) CHARACTER SET utf8 NOT NULL,
  `SEQUENCE` decimal(9,0) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nodeassociation`
--

LOCK TABLES `nodeassociation` WRITE;
/*!40000 ALTER TABLE `nodeassociation` DISABLE KEYS */;
INSERT INTO `nodeassociation` VALUES ('11960','Issue','10022','Component','IssueComponent',NULL),('11961','Issue','10022','Component','IssueComponent',NULL),('11962','Issue','10022','Component','IssueComponent',NULL),('11963','Issue','10022','Component','IssueComponent',NULL),('11964','Issue','10022','Component','IssueComponent',NULL),('11965','Issue','10022','Component','IssueComponent',NULL),('20101','Issue','20100','Component','IssueComponent',NULL),('20102','Issue','20101','Component','IssueComponent',NULL),('20103','Issue','20101','Component','IssueComponent',NULL),('20104','Issue','20101','Component','IssueComponent',NULL),('20105','Issue','20102','Component','IssueComponent',NULL),('20106','Issue','20102','Component','IssueComponent',NULL),('20107','Issue','20102','Component','IssueComponent',NULL),('20108','Issue','20103','Component','IssueComponent',NULL),('20109','Issue','20103','Component','IssueComponent',NULL),('20110','Issue','20103','Component','IssueComponent',NULL),('20301','Issue','20300','Component','IssueComponent',NULL),('20302','Issue','20301','Component','IssueComponent',NULL),('20303','Issue','20301','Component','IssueComponent',NULL),('20304','Issue','20301','Component','IssueComponent',NULL),('20305','Issue','20302','Component','IssueComponent',NULL),('20306','Issue','20302','Component','IssueComponent',NULL),('20307','Issue','20302','Component','IssueComponent',NULL),('20308','Issue','20303','Component','IssueComponent',NULL),('20309','Issue','20303','Component','IssueComponent',NULL),('20310','Issue','20303','Component','IssueComponent',NULL);
/*!40000 ALTER TABLE `nodeassociation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `priority`
--

DROP TABLE IF EXISTS `priority`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `priority` (
  `ID` varchar(60) CHARACTER SET utf8 NOT NULL,
  `SEQUENCE` decimal(18,0) DEFAULT NULL,
  `pname` varchar(60) CHARACTER SET utf8 DEFAULT NULL,
  `DESCRIPTION` text CHARACTER SET utf8,
  `ICONURL` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `STATUS_COLOR` varchar(60) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `priority`
--

LOCK TABLES `priority` WRITE;
/*!40000 ALTER TABLE `priority` DISABLE KEYS */;
INSERT INTO `priority` VALUES ('4','5','Minor','Minor loss of function, or other problem where easy workaround is present.','/images/icons/priority_minor.gif','#009900'),('1','1','Blocker','Blocks development and/or testing work, production could not run.','/images/icons/priority_blocker.gif','#ff0000'),('5','6','Trivial','Cosmetic problem like misspelt words or misaligned text.','/images/icons/priority_trivial.gif','#003300'),('6','4','Medium','Soon to become major.','/images/icons/priority_medium.gif','#66ff00'),('3','3','Major','Major need.','/images/icons/priority_major.gif','#ffff00'),('2','2','Critical','Crashes, loss of data, severe memory leak.','/images/icons/priority_critical.gif','#ffcc00');
/*!40000 ALTER TABLE `priority` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` decimal(18,0) NOT NULL,
  `pname` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `lead` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `pkey` varchar(255) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES ('10010','Max4U','jeremy','FOURU'),('10020','Data Warehouse','jeremy','DW'),('10021','Corporate','jeremy','CORP'),('20100','Project 1',NULL,'PR1'),('20200','Project 2',NULL,'PR2'),('20300','Project 3',NULL,'PR3'),('20400','Project 4',NULL,'PR4');
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `team` (
  `id` int(11) NOT NULL,
  `created` date DEFAULT NULL,
  `updated` date DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `team`
--

LOCK TABLES `team` WRITE;
/*!40000 ALTER TABLE `team` DISABLE KEYS */;
INSERT INTO `team` VALUES (20100,NULL,NULL,'Dilbert\'s Posse on 20100',20100),(20101,NULL,NULL,'Dogbert\'s Brood on 20200',20200),(20300,NULL,NULL,'Dilbert\'s Posse on 20300',20300),(20301,NULL,NULL,'Dogbert\'s Brood on 20300',20300);
/*!40000 ALTER TABLE `team` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `team_hours`
--

DROP TABLE IF EXISTS `team_hours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `team_hours` (
  `id` int(11) NOT NULL,
  `created` date DEFAULT NULL,
  `updated` date DEFAULT NULL,
  `team_id` int(11) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `start_of_week` date DEFAULT NULL,
  `hours_available` decimal(10,0) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `team_hours`
--

LOCK TABLES `team_hours` WRITE;
/*!40000 ALTER TABLE `team_hours` DISABLE KEYS */;
INSERT INTO `team_hours` VALUES (20100,NULL,NULL,20100,NULL,'2000-01-01','30'),(20101,NULL,NULL,20101,NULL,'2000-01-01','30'),(20300,NULL,NULL,20300,NULL,'2000-01-01','30'),(20301,NULL,NULL,20301,NULL,'2000-01-01','30');
/*!40000 ALTER TABLE `team_hours` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `team_hours_seq`
--

DROP TABLE IF EXISTS `team_hours_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `team_hours_seq` (
  `id` int(11) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `team_hours_seq`
--

LOCK TABLES `team_hours_seq` WRITE;
/*!40000 ALTER TABLE `team_hours_seq` DISABLE KEYS */;
INSERT INTO `team_hours_seq` VALUES (0);
/*!40000 ALTER TABLE `team_hours_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `team_seq`
--

DROP TABLE IF EXISTS `team_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `team_seq` (
  `id` int(11) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `team_seq`
--

LOCK TABLES `team_seq` WRITE;
/*!40000 ALTER TABLE `team_seq` DISABLE KEYS */;
INSERT INTO `team_seq` VALUES (0);
/*!40000 ALTER TABLE `team_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userbase`
--

DROP TABLE IF EXISTS `userbase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userbase` (
  `ID` decimal(18,0) NOT NULL,
  `username` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `PASSWORD_HASH` varchar(255) CHARACTER SET utf8 DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userbase`
--

LOCK TABLES `userbase` WRITE;
/*!40000 ALTER TABLE `userbase` DISABLE KEYS */;
INSERT INTO `userbase` VALUES ('10010','trent','GOkmx0nTzU+oqhUMQ3YBEJKh5Q5YK6UBGDt7yi2gNra0Fkfq92ixJJLso7DUfjyXC33IrD+BGWnfKVfAWX+fNg=='),('10012','jeremy','GOkmx0nTzU+oqhUMQ3YBEJKh5Q5YK6UBGDt7yi2gNra0Fkfq92ixJJLso7DUfjyXC33IrD+BGWnfKVfAWX+fNg==');
/*!40000 ALTER TABLE `userbase` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-11-07 18:45:56
