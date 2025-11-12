-- Add two-factor columns to admin/user table
ALTER TABLE users
  ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE,
  ADD COLUMN two_factor_secret VARCHAR(255) DEFAULT NULL;
