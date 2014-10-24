-- UPDATE fee SET `FEE` = `FFEE`;
SELECT `FEE`,`FFEE` FROM fee WHERE `DTYPE`="itemfee" and `FEE` !=`FFEE`;