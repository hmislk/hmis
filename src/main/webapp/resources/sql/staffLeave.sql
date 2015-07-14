
-- select * from form l join staffshift s on l.`STAFFSHIFT_ID`=s.`ID` where l.retired=false and s.`ID`=9489469 and l.`DTYPE`='LeaveFormSystem' ;

select * from staffleave l join staffshift s on l.`STAFFSHIFT_ID`=s.`ID` where l.retired=false and s.`ID`=9489469 ;