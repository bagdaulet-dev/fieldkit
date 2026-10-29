CREATE TABLE workspaces (
 id CHAR(64) PRIMARY KEY,
 is_demo BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP(6) NOT NULL,
 last_seen_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX workspace_expiry ON workspaces(last_seen_at);
CREATE TABLE equipment (
 id CHAR(36) PRIMARY KEY,
 workspace_id CHAR(64) NOT NULL,
 name VARCHAR(120) NOT NULL,
 model VARCHAR(120) NOT NULL DEFAULT '',
 category VARCHAR(20) NOT NULL,
 location VARCHAR(120) NOT NULL DEFAULT '',
 description VARCHAR(2000) NOT NULL DEFAULT '',
 total_quantity INT NOT NULL,
 available_quantity INT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT equipment_owner FOREIGN KEY(workspace_id) REFERENCES workspaces(id),
 CONSTRAINT equipment_owned_id UNIQUE(id, workspace_id),
 CONSTRAINT valid_inventory CHECK(total_quantity BETWEEN 1 AND 1000 AND available_quantity BETWEEN 0 AND total_quantity)
);
CREATE INDEX equipment_workspace ON equipment(workspace_id, category);
CREATE TABLE borrowers (
 id CHAR(36) PRIMARY KEY,
 workspace_id CHAR(64) NOT NULL,
 name VARCHAR(100) NOT NULL,
 team VARCHAR(100) NOT NULL DEFAULT '',
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT borrower_owner FOREIGN KEY(workspace_id) REFERENCES workspaces(id),
 CONSTRAINT borrower_owned_id UNIQUE(id, workspace_id)
);
CREATE INDEX borrower_workspace ON borrowers(workspace_id, name);
CREATE TABLE loans (
 id CHAR(36) PRIMARY KEY,
 workspace_id CHAR(64) NOT NULL,
 equipment_id CHAR(36) NOT NULL,
 borrower_id CHAR(36) NOT NULL,
 quantity INT NOT NULL,
 due_date DATE NOT NULL,
 notes VARCHAR(2000) NOT NULL DEFAULT '',
 checked_out_at TIMESTAMP(6) NOT NULL,
 returned_at TIMESTAMP(6),
 CONSTRAINT loan_owner FOREIGN KEY(workspace_id) REFERENCES workspaces(id),
 CONSTRAINT loan_owned_equipment FOREIGN KEY(equipment_id, workspace_id) REFERENCES equipment(id, workspace_id),
 CONSTRAINT loan_owned_borrower FOREIGN KEY(borrower_id, workspace_id) REFERENCES borrowers(id, workspace_id),
 CONSTRAINT valid_loan_quantity CHECK(quantity BETWEEN 1 AND 1000)
);
CREATE INDEX loan_workspace_due ON loans(workspace_id, returned_at, due_date);
CREATE INDEX loan_equipment ON loans(equipment_id, workspace_id);
CREATE INDEX loan_borrower ON loans(borrower_id, workspace_id);
