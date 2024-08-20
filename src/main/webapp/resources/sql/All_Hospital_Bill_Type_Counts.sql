(SELECT 'asiri' AS DatabaseName, BillType, COUNT(*) AS Count FROM asiri.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'demo' AS DatabaseName, BillType, COUNT(*) AS Count FROM demo.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'digasiri' AS DatabaseName, BillType, COUNT(*) AS Count FROM digasiri.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'digasiri1' AS DatabaseName, BillType, COUNT(*) AS Count FROM digasiri1.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'emr1' AS DatabaseName, BillType, COUNT(*) AS Count FROM emr1.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'fhck' AS DatabaseName, BillType, COUNT(*) AS Count FROM fhck.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'horizon' AS DatabaseName, BillType, COUNT(*) AS Count FROM horizon.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'kml' AS DatabaseName, BillType, COUNT(*) AS Count FROM kml.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'lifecare' AS DatabaseName, BillType, COUNT(*) AS Count FROM lifecare.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'metro' AS DatabaseName, BillType, COUNT(*) AS Count FROM metro.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'prabodha' AS DatabaseName, BillType, COUNT(*) AS Count FROM prabodha.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'rh_hrt' AS DatabaseName, BillType, COUNT(*) AS Count FROM rh_hrt.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'rmh' AS DatabaseName, BillType, COUNT(*) AS Count FROM rmh.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'ruhunu' AS DatabaseName, BillType, COUNT(*) AS Count FROM ruhunu.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'ruhunuNew' AS DatabaseName, BillType, COUNT(*) AS Count FROM ruhunuNew.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'sahana' AS DatabaseName, BillType, COUNT(*) AS Count FROM sahana.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'sethma' AS DatabaseName, BillType, COUNT(*) AS Count FROM sethma.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'southernlanka' AS DatabaseName, BillType, COUNT(*) AS Count FROM southernlanka.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'suwani' AS DatabaseName, BillType, COUNT(*) AS Count FROM suwani.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType)
UNION ALL
(SELECT 'ucfm' AS DatabaseName, BillType, COUNT(*) AS Count FROM ucfm.BILL WHERE createdAt >= '2024-05-01' AND createdAt < '2024-06-01' GROUP BY BillType);
