SET FOREIGN_KEY_CHECKS=0;
DELETE from billfee where `ID` > 0;
DELETE from bill where `ID` > 0;
DELETE from billitem where `ID` > 0;
DELETE from bill where `ID` > 0;
SET FOREIGN_KEY_CHECKS=1;