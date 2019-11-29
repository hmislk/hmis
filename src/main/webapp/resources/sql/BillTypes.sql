select distinct billtype, `DTYPE`, count(billtype) from bill where `BILLDATE` > '20190101 00:00:00.000'  group by `BILLTYPE`, `DTYPE` order by `BILLTYPE`;
