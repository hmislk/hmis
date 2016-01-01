update bill set `BILLTYPE` = 'ChannelAgentOld' where `BILLTYPE` = 'ChannelAgent';
update bill set `BILLTYPE` = 'ChannelCashOld' where `BILLTYPE` = 'ChannelCash';
update bill set `BILLTYPE` = 'ChannelOnCallOld' where `BILLTYPE` = 'ChannelOnCall';
update bill set `BILLTYPE` = 'ChannelPaidOld' where `BILLTYPE` = 'ChannelPaid';
update bill set `BILLTYPE` = 'ChannelStaffOld' where `BILLTYPE` = 'ChannelStaff';
