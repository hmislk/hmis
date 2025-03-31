-- Set the database name
SET @database_name = 'mpdatacleaning';

-- Query to get the data size of each table in MB
SELECT 
    TABLE_NAME, 
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE TABLE_SCHEMA = @database_name
ORDER BY size_mb DESC;
