--liquibase formatted sql

--changeset adriana.laos:001-1 comment:Extensiones (unaccent para búsquedas sin tildes)
CREATE EXTENSION IF NOT EXISTS unaccent;
--rollback DROP EXTENSION IF EXISTS unaccent;

--changeset adriana.laos:001-2 splitStatements:false comment:Función inmutable para normalizar texto en índices y búsquedas
CREATE OR REPLACE FUNCTION fn_normalizar(texto text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT btrim(regexp_replace(lower(public.unaccent('public.unaccent'::regdictionary, coalesce(texto, ''))), '\s+', ' ', 'g'))
$$;
COMMENT ON FUNCTION fn_normalizar(text) IS 'Minúsculas, sin tildes y con espacios unificados. Inmutable para poder usarse en índices (detección de duplicados RN02).';
--rollback DROP FUNCTION fn_normalizar(text);
