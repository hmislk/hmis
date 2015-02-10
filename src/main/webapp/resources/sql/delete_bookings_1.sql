SET FOREIGN_KEY_CHECKS=0;
DELETE from pharmaceuticalbillitem where `ID` > 0;
delete from bill where id > 0;
delete from billcomponent where id > 0;
delete from billentry where id > 0;
delete from billfee where id > 0;
delete from billnumber where id > 0;
delete from billsession where id > 0;
SET FOREIGN_KEY_CHECKS=1;