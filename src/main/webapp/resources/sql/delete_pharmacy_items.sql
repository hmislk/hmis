SET FOREIGN_KEY_CHECKS=0;
DELETE from item where `DTYPE` in ('Amp','Vmp','Atm','Vtm','Ampp','Vmpp');
DELETE from category where `DTYPE` in ('','');
SET FOREIGN_KEY_CHECKS=1;