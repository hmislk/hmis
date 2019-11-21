set forein_key_checks=0;
select * from patientencounter order by id limit 1;
select * from patientencounter where `BHTNO` = '1239';
select id, `DTYPE`, `BILLDATE`, `BILLTIME`, `PATIENTENCOUNTER_ID`,`RETIRED` from Bill where `BILLTYPE` = 'InwardProfessional' and `PATIENTENCOUNTER_ID` = 27196697;
-- set forein_key_checks=1;