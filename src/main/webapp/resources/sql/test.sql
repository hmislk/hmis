SELECT pe.`BHTNO` FROM billitem bi join billfee bf on bi.`PAIDFORBILLFEE_ID`=bf.`ID` join bill b on bf.`BILL_ID`=b.`ID` join patientencounter pe on b.`PATIENTENCOUNTER_ID`=pe.`ID`
WHERE bf.`ID` in
(SELECT bi.`PAIDFORBILLFEE_ID` FROM billitem bi join bill b on bi.`BILL_ID`=b.`ID` join billfee bf on bi.`PAIDFORBILLFEE_ID`=bf.`ID` 
join patientencounter pe on bf.`PATIENENCOUNTER_ID`=pe.`ID`
join bill rb on bi.`REFERENCEBILL_ID`=rb.`ID`
WHERE b.`RETIRED`=false and b.`BILLTYPE`='PaymentBill' and
(rb.`BILLTYPE`='InwardBill' or rb.`BILLTYPE`='InwardProfessional') and 
b.`CANCELLED`=false
group by bi.`PAIDFORBILLFEE_ID` 
having count(bi.`PAIDFORBILLFEE_ID`)>1);


-- SELECT pe.`BHTNO` FROM billitem bi join bill b on bi.`BILL_ID`=b.`ID` join billfee bf on bi.`PAIDFORBILLFEE_ID`=bf.`ID` join patientencounter pe on b.`PATIENTENCOUNTER_ID`=pe.`ID`
-- WHERE bf.`ID`=105877;