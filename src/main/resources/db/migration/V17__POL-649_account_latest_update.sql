CREATE TABLE account_latest_update (
    account_id TEXT NOT NULL,
    latest TIMESTAMP NOT NULL,
    CONSTRAINT pk_account_latest_update PRIMARY KEY (account_id)
);
