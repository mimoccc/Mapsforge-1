--
-- PostgreSQL database dump
--


SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;


--
-- tablespace definition
--
CREATE TABLESPACE osm LOCATION '/var/postgresql-8.3/osm';

--
-- user definition
--
CREATE USER osm WITH PASSWORD 'osm';

