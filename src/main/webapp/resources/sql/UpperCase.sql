SELECT CONCAT('RENAME TABLE ', table_name, ' TO temp_', table_name, '; ', 
              'RENAME TABLE temp_', table_name, ' TO ', UPPER(table_name), ';') 
FROM information_schema.tables 
WHERE table_schema = 'mp';
