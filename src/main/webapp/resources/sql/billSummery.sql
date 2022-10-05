/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  Buddhika
 * Created: 6 Oct 2022
 */
SELECT `BILLTYPE`,  `BILLCLASSTYPE`, count(`BILLTYPE`), sum(`NETTOTAL`) 
FROM bill 
group by `BILLTYPE`, `BILLCLASSTYPE`;

