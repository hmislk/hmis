select * from fee f join item i on f.`ITEM_ID`=i.`ID` WHERE i.`DTYPE`='ServiceSession' and i.`RETIRED`=false and f.`RETIRED`=false and
f.`FEETYPE`='OwnInstitution' and f.`NAME`='Hospital Fee';


-- update fee f join item i on f.`ITEM_ID`=i.`ID` SET f.`FEE`=600 WHERE i.`DTYPE`='ServiceSession' and i.`RETIRED`=false and f.`RETIRED`=false and
-- f.`FEETYPE`='OwnInstitution' and f.`NAME`='Hospital Fee';
