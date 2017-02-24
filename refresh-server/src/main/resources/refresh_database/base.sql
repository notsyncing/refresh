-- LIGHTFUR { "id": "io.github.notsyncing.refresh", "database": "$", "version": 0 } END

CREATE TABLE clients
(
  machine_id TEXT
    NOT NULL PRIMARY KEY,
  account_id TEXT,
  account_name TEXT,
  current_version TEXT,
  additional_data TEXT,
  last_seen TIMESTAMP
    NOT NULL DEFAULT now()
);

CREATE TABLE client_phases
(
  account_id TEXT
    NOT NULL PRIMARY KEY,
  phase INT
    NOT NULL DEFAULT 0
);
