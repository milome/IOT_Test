create schema IOT_WATERTREATMENT_REAL;
set schema IOT_WATERTREATMENT_REAL;

DROP TABLE "IOT_WATERTREATMENT_REAL"."SENSOR_DATA_SERIES";
DROP TABLE "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO";
DROP TABLE "IOT_WATERTREATMENT_REAL"."ALARM";
---- Channel in table ----
CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" ("CHANNEL_CODE" INTEGER CS_INT NOT NULL , "CHNANEL_NAME" NVARCHAR(50), "TYPE" NVARCHAR(50),
"SAMPLE_PERIOD" INTEGER, "COEFFICIENTS"  DECIMAL(9,2), "UNIT" NVARCHAR(50),"LATITUDE" NVARCHAR(50),
"LONGTITUDE" NVARCHAR(50), "STATION_ID" INTEGER, PRIMARY KEY ("CHANNEL_CODE")) UNLOAD PRIORITY 5 AUTO MERGE;

---- insert the sensors of the two places, with various latitude and longtitude ----
---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(1, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.492582','97.549942',1);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(2, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.492582','97.549942',1);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(3, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.492582','97.549942',1);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(4, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.492582','97.549942',1);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(5, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.454060','97.553236',2);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(6, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.454060','97.553236',2);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(7, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.454060','97.553236',2);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(8, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.454060','97.553236',2);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(9, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.419265','97.564472',3);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(10, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.419265','97.564472',3);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(11, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.419265','97.564472',3);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(12, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.419265','97.564472',3);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(13, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.380110','97.654222',4);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(14, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.380110','97.654222',4);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(15, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.380110','97.654222',4);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(16, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.380110','97.654222',4);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(17, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.309292','97.738654',5);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(18, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.309292','97.738654',5);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(19, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.309292','97.738654',5);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(20, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.309292','97.738654',5);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(21, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.217819','97.767189',6);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(22, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.217819','97.767189',6);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(23, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.217819','97.767189',6);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(24, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.217819','97.767189',6);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(25, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.216698','97.644935',7);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(26, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.216698','97.644935',7);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(27, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.216698','97.644935',7);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(28, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.216698','97.644935',7);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(29, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.225722','97.509877',8);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(30, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.225722','97.509877',8);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(31, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.225722','97.509877',8);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(32, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.225722','97.509877',8);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(33, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.305303','97.405726',9);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(34, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.305303','97.405726',9);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(35, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.305303','97.405726',9);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(36, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.305303','97.405726',9);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(37, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.382703','97.454217',10);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(38, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.382703','97.454217',10);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(39, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.382703','97.454217',10);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(40, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.382703','97.454217',10);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(41, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.399234','97.409640',11);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(42, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.399234','97.409640',11);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(43, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.399234','97.409640',11);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(44, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.399234','97.409640',11);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(45, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.410083','97.362801',12);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(46, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.410083','97.362801',12);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(47, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.410083','97.362801',12);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(48, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.410083','97.362801',12);

---- PH值传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(49, 'PH', 'sensor', 4000, 1.0, 'mol/L', '38.411212','97.309550',13);
---- 水温传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(50, 'TEMPERATURE', 'sensor', 4000, 1.0, 'mol/L', '38.411212','97.309550',13);
---- 水流传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(51, 'FLOW', 'sensor', 4000, 1.0, 'L/H', '38.411212','97.309550',13);
---- 水位传感器 ----
insert into  "IOT_WATERTREATMENT_REAL"."CHANNEL_INFO" values(52, 'LEVEL', 'sensor', 4000, 1.0, 'm', '38.411212','97.309550',13);


CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."SLUICE" ("ID" INTEGER CS_INT NOT NULL , "LATITUDE" NVARCHAR(50),
"LONGTITUDE" NVARCHAR(50), PRIMARY KEY ("ID")) UNLOAD PRIORITY 5 AUTO MERGE;

CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."SLUICE_STATE" (SLUICE_ID int not null references "IOT_WATERTREATMENT_REAL"."SLUICE" , "TS" timestamp, "STATE" VARCHAR (32) NOT NULL) UNLOAD PRIORITY 5 AUTO MERGE;

CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."STATION" ("ID" INTEGER CS_INT NOT NULL , "LATITUDE" NVARCHAR(50),
"LONGTITUDE" NVARCHAR(50), PRIMARY KEY ("ID")) UNLOAD PRIORITY 5 AUTO MERGE;

CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."STATION_STATE" (STATION_ID int not null references "IOT_WATERTREATMENT_REAL"."STATION" , "TS" timestamp, "STATE" VARCHAR (32) NOT NULL) UNLOAD PRIORITY 5 AUTO MERGE;

CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."ALARM" ("STATION_ID" INTEGER CS_INT NOT NULL ,
	 "SENSOR_ID" INTEGER CS_INT NOT NULL ,
	 "STATE" VARCHAR(32),
	 "DESCRIPTION" VARCHAR(1000),
	 "SEVERITY" VARCHAR(32),
	 "TYPE" VARCHAR(32),
	 "TIMESTAMP" LONGDATE CS_LONGDATE,
	 "VALUE" VARCHAR(50)) UNLOAD PRIORITY 5 AUTO MERGE;
----insert into "IOT_WATERTREATMENT_REAL"."ALARM" (STATION_ID, SENSOR_ID, STATE, DESCRIPTION, SEVERITY, TYPE, TIMESTAMP, VALUE) values('1','2','active','test desc', 'critical', 'alarm', '2015-01-30 12:00:12', '4.8000');
---- 水闸继电器1 ----
insert into  "IOT_WATERTREATMENT_REAL"."SLUICE" values(1, '38.401798','97.574211');
---- 水闸继电器2 ----
insert into  "IOT_WATERTREATMENT_REAL"."SLUICE" values(2, '38.203923','97.781647');
---- 水闸继电器3 ----
insert into  "IOT_WATERTREATMENT_REAL"."SLUICE" values(3, '38.202014','97.652712');
---- 水闸继电器4 ----
insert into  "IOT_WATERTREATMENT_REAL"."SLUICE" values(4, '38.376246','97.468591');

---- STATION 1 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(1, '38.492582','97.549942');
---- STATION 2 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(2, '38.454060','97.553236');
---- STATION 3 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(3, '38.419265','97.564472');
---- STATION 4 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(4, '38.380110','97.654222');
---- STATION 5 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(5, '38.309292','97.738654');
---- STATION 6 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(6, '38.217819','97.767189');
---- STATION 7 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(7, '38.216698','97.644935');
---- STATION 8 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(8, '38.225722','97.509877');
---- STATION 9 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(9, '38.305303','97.405726');
---- STATION 10 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(10, '38.382703','97.454217');
---- STATION 11 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(11, '38.399234','97.409640');
---- STATION 12 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(12, '38.410083','97.362801');
---- STATION 13 ----
insert into  "IOT_WATERTREATMENT_REAL"."STATION" values(13, '38.411212','97.309550');

---- create the series data table ----
CREATE COLUMN TABLE "IOT_WATERTREATMENT_REAL"."SENSOR_DATA_SERIES" (CHANNEL_CODE int not null references IOT_WATERTREATMENT_REAL.CHANNEL_INFO , ts timestamp, val VARCHAR (50))
series (
      series key(CHANNEL_CODE)
      period for series(ts, null)
      equidistant increment by interval 1 second
);
---- insert a sample series of data with built-in function ----
insert into  "IOT_WATERTREATMENT_REAL"."SENSOR_DATA_SERIES" select 1, GENERATED_PERIOD_START, 18 from series_generate_timestamp('INTERVAL 1 SECOND','2015-01-31','2015-02-01',null,null,null);
insert into  "IOT_WATERTREATMENT_REAL"."SENSOR_DATA_SERIES" values(3, '2015-01-30 12:00:12', '7');
select * from "IOT_WATERTREATMENT_REAL"."SENSOR_DATA_SERIES" where CHANNEL_CODE = 3;
---- example of series_generate_timestamp ----
select * from series_generate_timestamp('interval 60 second','2014-12-25 00:00:00','2015-01-29 12:00:00');
