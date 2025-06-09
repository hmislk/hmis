select `ID`, `BALANCE`, `DTYPE`, `BILLTYPEATOMIC`, `FROMWEBUSER_ID`, `FROMSTAFF_ID`, `TOWEBUSER_ID`, `TOSTAFF_ID`
from bill 
order by `ID` desc limit 100;

