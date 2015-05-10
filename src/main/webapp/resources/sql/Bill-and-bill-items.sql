select billitem.id as 'Bill Item Id', bill.`ID` as 'Bill Id' ,
bill.`BILLTYPE`, bill.`CREATEDAT`, billitem.`NETVALUE` as 'BillItem Net Value' ,
bill.`NETTOTAL` as 'bill Net Total',billitem.`PAIDFORBILLFEE_ID`,billitem.`REFERANCEBILLITEM_ID`,billitem.`REFERENCEBILL_ID`,
bill.`REFERENCEBILL_ID`,bill.`CANCELLEDBILL_ID`
from billitem 
inner join bill
on billitem.`BILL_ID`=bill.`ID`
order by billItem.id desc limit 20;
