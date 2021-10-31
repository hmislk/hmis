select *
from Bill
where `CANCELLED`=false and `CANCELLEDBILL_ID` is not null
order by id desc
limit 10;