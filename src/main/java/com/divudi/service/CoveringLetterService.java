package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.clinical.CoveringLetterColumn;
import com.divudi.core.data.clinical.CoveringLetterRecordType;
import com.divudi.core.data.dto.CoveringLetterRowDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.CoveringLetter;
import com.divudi.core.entity.clinical.CoveringLetterItem;
import com.divudi.core.entity.clinical.DocumentTemplate;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.CoveringLetterFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.CommonFunctions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 * Builds, numbers and renders covering letters that list several admissions or
 * OPD credit bills.
 */
@Stateless
public class CoveringLetterService {

    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    @EJB
    private CoveringLetterFacade coveringLetterFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private com.divudi.core.facade.PaymentFacade paymentFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // ---------------------------------------------------------------- queries
    @SuppressWarnings("unchecked")
    public List<CoveringLetterRowDTO> fetchAdmissionRows(Institution creditCompany, Patient patient,
            Date from, Date to, AdmissionType admissionType, Department department, boolean onlyUnsettled) {
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.CoveringLetterRowDTO(")
                .append("pe.id, ")
                .append("pe.bhtNo, pe.dateOfAdmission, per.name, ecc.policyNo, ")
                .append("coalesce(fb.netTotal, pe.netTotal)) ")
                .append("from EncounterCreditCompany ecc ")
                .append("join ecc.patientEncounter pe ")
                .append("left join pe.finalBill fb ")
                .append("left join pe.patient pt ")
                .append("left join pt.person per ")
                .append("where ecc.retired=false and pe.retired=false and pe.discharged=true ")
                .append("and ecc.institution=:cc ")
                .append("and pe.dateOfDischarge between :fd and :td ");
        m.put("cc", creditCompany);
        m.put("fd", from);
        m.put("td", to);
        if (patient != null) {
            jpql.append("and pe.patient=:pt ");
            m.put("pt", patient);
        }
        if (admissionType != null) {
            jpql.append("and pe.admissionType=:at ");
            m.put("at", admissionType);
        }
        if (department != null) {
            jpql.append("and pe.department=:dep ");
            m.put("dep", department);
        }
        jpql.append("order by pe.dateOfAdmission, pe.id");
        List<CoveringLetterRowDTO> list = (List<CoveringLetterRowDTO>) patientEncounterFacade
                .findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        if (list == null) {
            return new ArrayList<>();
        }
        setRecordType(list, CoveringLetterRecordType.Admission);
        return fillAdmissionCompanyAmounts(list, creditCompany, onlyUnsettled);
    }

    @SuppressWarnings("unchecked")
    public List<CoveringLetterRowDTO> fetchOpdBillRows(Institution creditCompany, Patient patient,
            Date from, Date to, boolean onlyUnsettled) {
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.CoveringLetterRowDTO(")
                .append("b.id, ")
                .append("b.deptId, b.createdAt, per.name, '', b.netTotal) ")
                .append("from BilledBill b ")
                .append("left join b.patient pt ")
                .append("left join pt.person per ")
                .append("where b.billType=:btp and b.paymentMethod=:pm ")
                .append("and b.creditCompany=:cc ")
                .append("and b.createdAt between :fd and :td ")
                .append("and b.deptId is not null and b.cancelled=false and b.retired=false ");
        m.put("btp", BillType.OpdBill);
        m.put("pm", PaymentMethod.Credit);
        m.put("cc", creditCompany);
        m.put("fd", from);
        m.put("td", to);
        if (patient != null) {
            jpql.append("and b.patient=:pt ");
            m.put("pt", patient);
        }
        if (onlyUnsettled) {
            jpql.append("and abs(b.netTotal)-abs(coalesce(b.paidAmount, 0))>:val ");
            m.put("val", 0.1);
        }
        jpql.append("order by b.id");
        List<CoveringLetterRowDTO> list = (List<CoveringLetterRowDTO>) billFacade
                .findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        if (list == null) {
            return new ArrayList<>();
        }
        setRecordType(list, CoveringLetterRecordType.OpdBill);
        // An OPD credit bill is charged to the credit company in full.
        for (CoveringLetterRowDTO r : list) {
            r.setCompanyAmount(r.getAmount());
        }
        fillPolicyNos(list);
        return list;
    }

    /**
     * Sets each admission's company amount from the credit company's commitment
     * bill (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY) of the current final
     * bill; admissions without one fall back to the final bill total. When
     * onlyUnsettled is set, drops admissions whose commitment bill for this
     * company is fully paid.
     */
    @SuppressWarnings("unchecked")
    private List<CoveringLetterRowDTO> fillAdmissionCompanyAmounts(List<CoveringLetterRowDTO> rows,
            Institution creditCompany, boolean onlyUnsettled) {
        List<Long> ids = new ArrayList<>();
        for (CoveringLetterRowDTO r : rows) {
            ids.add(r.getId());
        }
        Map<Long, double[]> byEncounter = new HashMap<>();
        if (!ids.isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ids", ids);
            m.put("cc", creditCompany);
            m.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
            String jpql = "select pe.id, b.netTotal, b.paidAmount from Bill b, PatientEncounter pe "
                    + "where b.referenceBill = pe.finalBill and pe.id in :ids "
                    + "and b.billTypeAtomic=:bta and b.creditCompany=:cc "
                    + "and b.retired=false and b.cancelled=false";
            List<Object[]> found = (List<Object[]>) billFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP);
            if (found != null) {
                for (Object[] o : found) {
                    double[] v = byEncounter.computeIfAbsent((Long) o[0], k -> new double[2]);
                    v[0] += o[1] == null ? 0 : ((Number) o[1]).doubleValue();
                    v[1] += o[2] == null ? 0 : ((Number) o[2]).doubleValue();
                }
            }
        }
        List<CoveringLetterRowDTO> result = new ArrayList<>();
        for (CoveringLetterRowDTO r : rows) {
            double[] v = byEncounter.get(r.getId());
            if (v == null) {
                r.setCompanyAmount(r.getAmount());
            } else {
                r.setCompanyAmount(v[0]);
                if (onlyUnsettled && Math.abs(v[0]) - Math.abs(v[1]) <= 0.1) {
                    continue;
                }
            }
            result.add(r);
        }
        return result;
    }

    private void setRecordType(List<CoveringLetterRowDTO> rows, CoveringLetterRecordType type) {
        for (CoveringLetterRowDTO r : rows) {
            r.setRecordType(type);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillPolicyNos(List<CoveringLetterRowDTO> rows) {
        List<Long> ids = new ArrayList<>();
        for (CoveringLetterRowDTO r : rows) {
            if (r.getId() != null) {
                ids.add(r.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> byBill = new HashMap<>();
        // The credit payment is on the bill itself, or (for bills settled from
        // an OPD batch bill) on the batch bill it references.
        putPolicyNos(byBill, "select p.bill.id, p.policyNo from Payment p "
                + "where p.retired=false and p.paymentMethod=:pm and p.bill.id in :ids "
                + "and p.policyNo is not null and p.policyNo<>'' order by p.id", ids);
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) {
            if (!byBill.containsKey(id)) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            putPolicyNos(byBill, "select b.id, p.policyNo from Bill b, Payment p "
                    + "where p.bill = b.backwardReferenceBill and b.id in :ids "
                    + "and p.retired=false and p.paymentMethod=:pm "
                    + "and p.policyNo is not null and p.policyNo<>'' order by p.id", missing);
        }
        for (CoveringLetterRowDTO r : rows) {
            r.setPolicyNo(byBill.get(r.getId()));
        }
    }

    @SuppressWarnings("unchecked")
    private void putPolicyNos(Map<Long, String> byBill, String jpql, List<Long> ids) {
        Map<String, Object> m = new HashMap<>();
        m.put("ids", ids);
        m.put("pm", PaymentMethod.Credit);
        List<Object[]> found = (List<Object[]>) paymentFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (found == null) {
            return;
        }
        for (Object[] o : found) {
            if (o[0] != null && o[1] != null) {
                byBill.put((Long) o[0], o[1].toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void markAlreadyInLetter(List<CoveringLetterRowDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (CoveringLetterRowDTO r : rows) {
            r.setAlreadyInLetterRef(null);
        }
        CoveringLetterRecordType type = rows.get(0).getRecordType();
        List<Long> ids = new ArrayList<>();
        for (CoveringLetterRowDTO r : rows) {
            if (r.getId() != null) {
                ids.add(r.getId());
            }
        }
        if (ids.isEmpty() || type == null) {
            return;
        }
        String idPath = type == CoveringLetterRecordType.Admission ? "i.patientEncounter.id" : "i.bill.id";
        Map<String, Object> m = new HashMap<>();
        m.put("ids", ids);
        String jpql = "select " + idPath + ", i.coveringLetter.referenceNo from CoveringLetterItem i "
                + "where i.retired=false and i.coveringLetter.retired=false and " + idPath + " in :ids "
                + "order by i.id";
        List<Object[]> found = (List<Object[]>) coveringLetterFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (found == null) {
            return;
        }
        Map<Long, String> refs = new HashMap<>();
        for (Object[] o : found) {
            if (o[0] != null && o[1] != null) {
                refs.put((Long) o[0], o[1].toString());
            }
        }
        for (CoveringLetterRowDTO r : rows) {
            r.setAlreadyInLetterRef(refs.get(r.getId()));
        }
    }

    // -------------------------------------------------------------- rendering
    public String render(DocumentTemplate t, List<CoveringLetterRowDTO> rows, CoveringLetterRecordType type,
            Institution issuer, Institution creditCompany, String referenceNo, String dateFormat) {
        if (t == null || t.getContents() == null) {
            return "";
        }
        if (rows == null) {
            rows = new ArrayList<>();
        }
        String input = CommonFunctions.unescapeTemplateHtml(t.getContents());

        double total = 0;
        double companyTotal = 0;
        Date minDate = null;
        Date maxDate = null;
        for (CoveringLetterRowDTO r : rows) {
            if (r.getAmount() != null) {
                total += r.getAmount();
            }
            if (r.getCompanyAmount() != null) {
                companyTotal += r.getCompanyAmount();
            }
            if (r.getDate() != null) {
                if (minDate == null || r.getDate().before(minDate)) {
                    minDate = r.getDate();
                }
                if (maxDate == null || r.getDate().after(maxDate)) {
                    maxDate = r.getDate();
                }
            }
        }

        String creditCompanyName = "";
        String creditCompanyAddress = "";
        String creditCompanyContact = "";
        if (creditCompany != null) {
            creditCompanyName = nz(creditCompany.getName());
            creditCompanyAddress = nz(creditCompany.getAddress());
            if (creditCompany.getContactPerson() != null && creditCompany.getContactPerson().getName() != null) {
                creditCompanyContact = creditCompany.getContactPerson().getName();
            }
            if (creditCompanyContact.trim().isEmpty() && creditCompany.getContactPersonName() != null) {
                creditCompanyContact = creditCompany.getContactPersonName();
            }
        }

        // Data values are HTML-escaped; only the template itself and {rows_table}
        // (whose cells are escaped in buildRowsTable) are inserted as raw HTML.
        String output = input
                .replace("{letter_date}", esc(CommonFunctions.formatDate(new Date(), dateFormat)))
                .replace("{reference_no}", esc(referenceNo))
                .replace("{credit_company}", esc(creditCompanyName))
                .replace("{credit_company_address}", esc(creditCompanyAddress))
                .replace("{credit_company_contact}", esc(creditCompanyContact))
                .replace("{name}", esc(commonValue(rows, true)))
                .replace("{policy_no}", esc(commonValue(rows, false)))
                .replace("{company_total}", String.format("%,.2f", companyTotal))
                .replace("{total}", String.format("%,.2f", total))
                .replace("{count}", String.valueOf(rows.size()))
                .replace("{from_date}", esc(CommonFunctions.formatDate(minDate, dateFormat)))
                .replace("{to_date}", esc(CommonFunctions.formatDate(maxDate, dateFormat)))
                .replace("{issuer}", issuer == null ? "" : esc(issuer.getName()))
                .replace("{issuer_address}", issuer == null ? "" : esc(issuer.getAddress()))
                .replace("{issuer_phone}", issuer == null ? "" : esc(issuer.getPhone()))
                .replace("{issuer_bank}", issuer == null ? "" : esc(issuer.getBankName()))
                .replace("{issuer_branch}", issuer == null ? "" : esc(issuer.getBranchName()))
                .replace("{issuer_account_no}", issuer == null ? "" : esc(issuer.getAccountNo()));
        // Row table last so that row text is never touched by placeholder replacement.
        output = output.replace("{rows_table}", buildRowsTable(t, rows, type, total, companyTotal, dateFormat));
        return output;
    }

    private String commonValue(List<CoveringLetterRowDTO> rows, boolean name) {
        String common = null;
        for (CoveringLetterRowDTO r : rows) {
            String v = name ? r.getPatientName() : r.getPolicyNo();
            if (v == null || v.trim().isEmpty()) {
                return "";
            }
            if (common == null) {
                common = v;
            } else if (!common.equals(v)) {
                return "";
            }
        }
        return common == null ? "" : common;
    }

    private List<CoveringLetterColumn> selectedColumns(DocumentTemplate t) {
        Set<CoveringLetterColumn> chosen = new HashSet<>();
        for (String s : t.getRowColumnList()) {
            try {
                chosen.add(CoveringLetterColumn.valueOf(s));
            } catch (IllegalArgumentException e) {
                // ignore unknown column names
            }
        }
        if (chosen.isEmpty()) {
            chosen.add(CoveringLetterColumn.REF_NO);
            chosen.add(CoveringLetterColumn.DATE);
            chosen.add(CoveringLetterColumn.AMOUNT);
        }
        List<CoveringLetterColumn> ordered = new ArrayList<>();
        for (CoveringLetterColumn c : CoveringLetterColumn.values()) {
            if (chosen.contains(c)) {
                ordered.add(c);
            }
        }
        return ordered;
    }

    private static final String CELL = "border:1px solid #000;padding:3px 6px;";

    private String buildRowsTable(DocumentTemplate t, List<CoveringLetterRowDTO> rows,
            CoveringLetterRecordType type, double total, double companyTotal, String dateFormat) {
        List<CoveringLetterColumn> cols = selectedColumns(t);
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"covering-letter-rows\" style=\"border-collapse:collapse;width:100%\">");
        sb.append("<tr>");
        sb.append("<th style=\"").append(CELL).append("text-align:left\">No</th>");
        for (CoveringLetterColumn c : cols) {
            String label = c.getLabel();
            if (c == CoveringLetterColumn.REF_NO) {
                label = type == CoveringLetterRecordType.OpdBill ? "Bill No" : "BHT No";
            }
            String align = isAmount(c) ? "right" : "left";
            sb.append("<th style=\"").append(CELL).append("text-align:").append(align).append("\">")
                    .append(esc(label)).append("</th>");
        }
        sb.append("</tr>");
        int n = 1;
        for (CoveringLetterRowDTO r : rows) {
            sb.append("<tr>");
            sb.append("<td style=\"").append(CELL).append("\">").append(n++).append("</td>");
            for (CoveringLetterColumn c : cols) {
                String v;
                String align = "left";
                switch (c) {
                    case REF_NO:
                        v = r.getRefNo();
                        break;
                    case DATE:
                        v = CommonFunctions.formatDate(r.getDate(), dateFormat);
                        break;
                    case PATIENT:
                        v = r.getPatientName();
                        break;
                    case POLICY_NO:
                        v = r.getPolicyNo();
                        break;
                    case AMOUNT:
                        v = String.format("%,.2f", r.getAmount() == null ? 0.0 : r.getAmount());
                        align = "right";
                        break;
                    case COMPANY_AMOUNT:
                        v = String.format("%,.2f", r.getCompanyAmount() == null ? 0.0 : r.getCompanyAmount());
                        align = "right";
                        break;
                    default:
                        v = "";
                }
                sb.append("<td style=\"").append(CELL).append("text-align:").append(align).append("\">")
                        .append(esc(v)).append("</td>");
            }
            sb.append("</tr>");
        }
        int firstAmountIdx = -1;
        for (int i = 0; i < cols.size(); i++) {
            if (isAmount(cols.get(i))) {
                firstAmountIdx = i;
                break;
            }
        }
        sb.append("<tr>");
        if (firstAmountIdx >= 0) {
            int span = firstAmountIdx + 1; // "No" cell plus the columns before the first amount
            sb.append("<td colspan=\"").append(span).append("\" style=\"").append(CELL)
                    .append("text-align:right;font-weight:bold\">Total</td>");
            for (int i = firstAmountIdx; i < cols.size(); i++) {
                CoveringLetterColumn c = cols.get(i);
                String v = c == CoveringLetterColumn.AMOUNT ? String.format("%,.2f", total)
                        : c == CoveringLetterColumn.COMPANY_AMOUNT ? String.format("%,.2f", companyTotal) : "";
                sb.append("<td style=\"").append(CELL).append("text-align:right;font-weight:bold\">")
                        .append(v).append("</td>");
            }
        } else {
            sb.append("<td colspan=\"").append(cols.size()).append("\" style=\"").append(CELL)
                    .append("text-align:right;font-weight:bold\">Total</td>");
            sb.append("<td style=\"").append(CELL).append("text-align:right;font-weight:bold\">")
                    .append(String.format("%,.2f", total)).append("</td>");
        }
        sb.append("</tr></table>");
        return sb.toString();
    }

    private static boolean isAmount(CoveringLetterColumn c) {
        return c == CoveringLetterColumn.AMOUNT || c == CoveringLetterColumn.COMPANY_AMOUNT;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // -------------------------------------------------------------- numbering
    private String getNumberDelimiter() {
        String delimiter = configOptionApplicationController.getShortTextValueByKey("Number Delimiter", "/");
        if (delimiter == null || delimiter.trim().isEmpty()) {
            return "/";
        }
        return delimiter.trim();
    }

    /**
     * Assigns serial, letterYear and referenceNo on the letter. Must be called
     * while holding the lock for the same issuer and year (see createLetter).
     */
    public String nextReferenceNo(Institution issuer, int year, CoveringLetter letter) {
        Map<String, Object> m = new HashMap<>();
        String jpql = "select max(l.serial) from CoveringLetter l where l.letterYear=:yr";
        m.put("yr", year);
        if (issuer != null) {
            jpql += " and l.issuingInstitution=:ins";
            m.put("ins", issuer);
        } else {
            jpql += " and l.issuingInstitution is null";
        }
        long serial = coveringLetterFacade.findLongByJpql(jpql, m) + 1;
        String code = issuer != null && issuer.getCode() != null && !issuer.getCode().trim().isEmpty()
                ? issuer.getCode().trim() : "CL";
        String d = getNumberDelimiter();
        String ref = code + d + "CL" + d + String.format("%02d", year % 100) + d + String.format("%04d", serial);
        if (letter != null) {
            letter.setSerial(serial);
            letter.setLetterYear(year);
            letter.setReferenceNo(ref);
        }
        return ref;
    }

    private ReentrantLock lockFor(Institution issuer, int year) {
        String key = (issuer == null || issuer.getId() == null ? "0" : issuer.getId().toString()) + "-" + year;
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * Not transactional itself so that each facade call commits before the
     * per-issuer lock is released; the next caller then sees the new serial.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public CoveringLetter createLetter(DocumentTemplate template, List<CoveringLetterRowDTO> rows,
            CoveringLetterRecordType type, Institution issuer, Institution creditCompany,
            WebUser user, String dateFormat) {
        if (template == null || rows == null || rows.isEmpty() || type == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        ReentrantLock lock = lockFor(issuer, year);
        lock.lock();
        try {
            CoveringLetter l = new CoveringLetter();
            nextReferenceNo(issuer, year, l);
            l.setIssuingInstitution(issuer);
            l.setCreditCompany(creditCompany);
            l.setDocumentTemplate(template);
            l.setRecordType(type);
            l.setCreater(user);
            l.setCreatedAt(new Date());

            double total = 0;
            Date minDate = null;
            Date maxDate = null;
            int order = 1;
            for (CoveringLetterRowDTO r : rows) {
                CoveringLetterItem item = new CoveringLetterItem();
                item.setCoveringLetter(l);
                if (type == CoveringLetterRecordType.Admission) {
                    PatientEncounter pe = patientEncounterFacade.find(r.getId());
                    item.setPatientEncounter(pe);
                } else {
                    Bill b = billFacade.find(r.getId());
                    item.setBill(b);
                }
                double amt = r.getAmount() == null ? 0.0 : r.getAmount();
                item.setAmount(amt);
                item.setOrderNo(order++);
                total += amt;
                if (r.getDate() != null) {
                    if (minDate == null || r.getDate().before(minDate)) {
                        minDate = r.getDate();
                    }
                    if (maxDate == null || r.getDate().after(maxDate)) {
                        maxDate = r.getDate();
                    }
                }
                l.getItems().add(item);
            }
            l.setTotal(total);
            l.setRowCount(rows.size());
            l.setFromDate(minDate);
            l.setToDate(maxDate);
            l.setGeneratedHtml(render(template, rows, type, issuer, creditCompany, l.getReferenceNo(), dateFormat));
            coveringLetterFacade.create(l);
            return l;
        } finally {
            lock.unlock();
        }
    }
}
