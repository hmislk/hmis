-- select id, name from department
-- order by name;
-- select `ID`, `ITEMBATCH_ID`
-- from stock 
-- where stock > 0
-- and `DEPARTMENT_ID` = 2;
-- select `ITEM_ID` , `DATEOFEXPIRE` from itembatch ;
-- select name from item;
SELECT 
    s.`ID`,
    i.name AS item_name,
    s.stock,
    ib.DATEOFEXPIRE AS expiry
FROM 
    stock s
JOIN 
    itembatch ib ON s.ITEMBATCH_ID = ib.ID
JOIN 
    item i ON ib.ITEM_ID = i.ID
WHERE 
    s.stock > 0
    AND s.DEPARTMENT_ID = 2;

-- UPDATE stock s
-- JOIN itembatch ib ON s.ITEMBATCH_ID = ib.ID
-- JOIN item i ON ib.ITEM_ID = i.ID
-- SET s.stock = 0
-- WHERE s.DEPARTMENT_ID = 2;

