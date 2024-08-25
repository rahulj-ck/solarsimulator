CREATE TABLE power_plant (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    setup_on DATE,
    operational BOOLEAN
);

CREATE UNIQUE INDEX unique_operational_name ON power_plant (name) WHERE operational = TRUE;