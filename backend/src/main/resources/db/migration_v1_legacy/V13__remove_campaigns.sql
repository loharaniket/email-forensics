-- Migration V13: Safely remove threat campaign tables
DROP TABLE IF EXISTS campaign_members;
DROP TABLE IF EXISTS campaigns;

