ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS asn_number;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS organization;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS country;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS city;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS latitude;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS longitude;
ALTER TABLE ip_artifacts DROP COLUMN IF EXISTS data_source;

