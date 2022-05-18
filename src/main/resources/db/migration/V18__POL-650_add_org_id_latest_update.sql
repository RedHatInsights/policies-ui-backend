CREATE TABLE org_id_latest_update (
    org_id TEXT NOT NULL,
    latest TIMESTAMP NOT NULL,
    CONSTRAINT pk_org_id_latest_update PRIMARY KEY (org_id)
);
