--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: metadata; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE metadata (
    importversion integer,
    date bigint,
    maxlat integer,
    minlon integer,
    minlat integer,
    maxlon integer,
    zoom smallint,
    tilesize smallint
);


ALTER TABLE public.metadata OWNER TO osm;

--
-- Name: multipolygons; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE multipolygons (
    outerwayid bigint NOT NULL,
    innerwaysequence smallint NOT NULL,
    latitude integer,
    longitude integer,
    waynodesequence smallint
);


ALTER TABLE public.multipolygons OWNER TO osm;

--
-- Name: pois; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE pois (
    id bigint NOT NULL,
    latitude integer,
    longitude integer,
    namelength smallint,
    name text DEFAULT ''::text,
    tagsamount smallint,
    layer smallint,
    elevation smallint,
    housenumber text DEFAULT ''::text,
    tags text
);


ALTER TABLE public.pois OWNER TO osm;

--
-- Name: poistotiles; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE poistotiles (
    poiid bigint,
    tilex integer,
    tiley integer,
    copy boolean
);


ALTER TABLE public.poistotiles OWNER TO osm;

--
-- Name: waynodes; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE waynodes (
    wayid bigint NOT NULL,
    waynodesequence smallint,
    latitude integer,
    longitude integer
);


ALTER TABLE public.waynodes OWNER TO osm;

--
-- Name: ways; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE ways (
    id bigint NOT NULL,
    namelength smallint,
    name text DEFAULT ''::text,
    tagsamount smallint,
    layer smallint,
    waynodesamount integer,
    waytype smallint DEFAULT (1)::smallint,
    tags text,
    convexness smallint
);


ALTER TABLE public.ways OWNER TO osm;

--
-- Name: waystotiles; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE waystotiles (
    wayid bigint,
    tilex integer,
    tiley integer,
    tilebitmask smallint
);


ALTER TABLE public.waystotiles OWNER TO osm;

--
-- Name: pk_poi_id; Type: CONSTRAINT; Schema: public; Owner: osm; Tablespace: 
--

ALTER TABLE ONLY pois
    ADD CONSTRAINT pk_poi_id PRIMARY KEY (id);


--
-- Name: pk_ways_id; Type: CONSTRAINT; Schema: public; Owner: osm; Tablespace: 
--

ALTER TABLE ONLY ways
    ADD CONSTRAINT pk_ways_id PRIMARY KEY (id);


--
-- Name: multipolygons_outer_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX multipolygons_outer_idx ON multipolygons USING btree (outerwayid);


--
-- Name: pois_tags_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX pois_tags_idx ON pois USING btree (tags) WHERE (tags = ''::text);


--
-- Name: waynodes_id_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX waynodes_id_idx ON waynodes USING btree (wayid);


--
-- Name: ways_tags_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX ways_tags_idx ON ways USING btree (tags) WHERE (tags = ''::text);


--
-- Name: fk_multipolygons; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY multipolygons
    ADD CONSTRAINT fk_multipolygons FOREIGN KEY (outerwayid) REFERENCES ways(id) ON DELETE CASCADE;


--
-- Name: fk_poistotiles; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY poistotiles
    ADD CONSTRAINT fk_poistotiles FOREIGN KEY (poiid) REFERENCES pois(id) ON DELETE CASCADE;


--
-- Name: fk_waynodes; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY waynodes
    ADD CONSTRAINT fk_waynodes FOREIGN KEY (wayid) REFERENCES ways(id) ON DELETE CASCADE;


--
-- Name: fk_waystotiles; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY waystotiles
    ADD CONSTRAINT fk_waystotiles FOREIGN KEY (wayid) REFERENCES ways(id) ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--
