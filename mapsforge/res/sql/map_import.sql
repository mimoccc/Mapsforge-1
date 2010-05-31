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
-- Name: filtered_pois; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE filtered_pois (
    poi_id bigint,
    tile_x integer,
    tile_y integer,
    zoom_level smallint
);


ALTER TABLE public.filtered_pois OWNER TO osm;

--
-- Name: filtered_ways; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE filtered_ways (
    way_id bigint,
    tile_x integer,
    tile_y integer,
    tile_bitmask smallint,
    zoom_level smallint
);


ALTER TABLE public.filtered_ways OWNER TO osm;

--
-- Name: metadata; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE metadata (
    import_version integer,
    date bigint,
    maxlat integer,
    minlon integer,
    minlat integer,
    maxlon integer,
    zoom smallint,
    tile_size smallint
);


ALTER TABLE public.metadata OWNER TO osm;

--
-- Name: multipolygons; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE multipolygons (
    outer_way_id bigint NOT NULL,
    inner_way_sequence smallint NOT NULL,
    latitude integer,
    longitude integer,
    waynode_sequence smallint
);


ALTER TABLE public.multipolygons OWNER TO osm;

--
-- Name: pois; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE pois (
    id bigint NOT NULL,
    latitude integer,
    longitude integer,
    name_length smallint,
    name text DEFAULT ''::text,
    tags_amount smallint,
    layer smallint,
    elevation smallint,
    housenumber text DEFAULT ''::text,
    tags text
);


ALTER TABLE public.pois OWNER TO osm;

--
-- Name: pois_tags; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE pois_tags (
    poi_id bigint,
    tag character varying(50)
);


ALTER TABLE public.pois_tags OWNER TO osm;

--
-- Name: pois_to_tiles; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE pois_to_tiles (
    poi_id bigint,
    tile_x integer,
    tile_y integer,
    zoom_level smallint,
    size integer
);


ALTER TABLE public.pois_to_tiles OWNER TO osm;

--
-- Name: waynodes; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE waynodes (
    way_id bigint NOT NULL,
    waynode_sequence smallint,
    latitude integer,
    longitude integer
);


ALTER TABLE public.waynodes OWNER TO osm;

--
-- Name: waynodes_diff; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE waynodes_diff (
    way_id bigint NOT NULL,
    waynode_sequence smallint NOT NULL,
    diff_lat bigint,
    diff_lon bigint
);


ALTER TABLE public.waynodes_diff OWNER TO osm;

--
-- Name: ways; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE ways (
    id bigint NOT NULL,
    name_length smallint,
    name text DEFAULT ''::text,
    tags_amount smallint,
    layer smallint,
    waynodes_amount integer,
    way_type smallint DEFAULT (1)::smallint,
    tags text,
    convexness smallint,
    label_pos_lat integer,
    label_pos_lon integer,
    inner_way_amount smallint
);


ALTER TABLE public.ways OWNER TO osm;

--
-- Name: ways_tags; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE ways_tags (
    way_id bigint,
    tag character varying(50)
);


ALTER TABLE public.ways_tags OWNER TO osm;

--
-- Name: ways_to_tiles; Type: TABLE; Schema: public; Owner: osm; Tablespace: 
--

CREATE TABLE ways_to_tiles (
    way_id bigint,
    tile_x integer,
    tile_y integer,
    tile_bitmask smallint,
    zoom_level smallint,
    size integer
);


ALTER TABLE public.ways_to_tiles OWNER TO osm;

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
-- Name: pkey; Type: CONSTRAINT; Schema: public; Owner: osm; Tablespace: 
--

ALTER TABLE ONLY waynodes_diff
    ADD CONSTRAINT pkey PRIMARY KEY (way_id, waynode_sequence);


--
-- Name: multipolygons_outer_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX multipolygons_outer_idx ON multipolygons USING btree (outer_way_id);


--
-- Name: pois_tags_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX pois_tags_idx ON pois_tags USING btree (tag);


--
-- Name: waynodes_id_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX waynodes_id_idx ON waynodes USING btree (way_id);


--
-- Name: ways_tags_idx; Type: INDEX; Schema: public; Owner: osm; Tablespace: 
--

CREATE INDEX ways_tags_idx ON ways_tags USING btree (tag);


--
-- Name: fk_multipolygons; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY multipolygons
    ADD CONSTRAINT fk_multipolygons FOREIGN KEY (outer_way_id) REFERENCES ways(id) ON DELETE CASCADE;


--
-- Name: fk_pois; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY filtered_pois
    ADD CONSTRAINT fk_pois FOREIGN KEY (poi_id) REFERENCES pois(id);


--
-- Name: fk_pois_tiles; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY pois_to_tiles
    ADD CONSTRAINT fk_pois_tiles FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE CASCADE;


--
-- Name: fk_waynodes; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY waynodes
    ADD CONSTRAINT fk_waynodes FOREIGN KEY (way_id) REFERENCES ways(id) ON DELETE CASCADE;


--
-- Name: fk_ways; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY filtered_ways
    ADD CONSTRAINT fk_ways FOREIGN KEY (way_id) REFERENCES ways(id);


--
-- Name: fk_waystotiles; Type: FK CONSTRAINT; Schema: public; Owner: osm
--

ALTER TABLE ONLY ways_to_tiles
    ADD CONSTRAINT fk_waystotiles FOREIGN KEY (way_id) REFERENCES ways(id) ON DELETE CASCADE;


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

