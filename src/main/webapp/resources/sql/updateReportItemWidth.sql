select  dtype, `RIWIDTH`,count(*) from reportitem group by `DTYPE`,`RIWIDTH`;
-- update reportitem set `RIWIDTH` = 30 WHERE `RIWIDTH` is null;
-- update reportitem set `RIWIDTH` = 30 WHERE `DTYPE`='InvestigationItem';
