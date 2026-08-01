/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.lab;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class PatientReportItemValue implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private PatientEncounter patientEncounter;
    @ManyToOne
    private InvestigationItem investigationItem;
    @ManyToOne
    private PatientReport patientReport;
    private String codeSystem;
    private String codeSystemCode;
    private String strValue;
    @Lob
    private String lobValue;
    @Lob
    private byte[] baImage;
    private String fileName;
    private String fileType;
    private Double doubleValue;

    @ManyToOne
    private PatientReportGroup patientReportGroup;

    @Transient
    private String value;
    @Transient
    private String displayValue;

    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    private boolean allowToExportChart;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public String getLobValue() {
        return lobValue;
    }

    public void setLobValue(String lobValue) {
        this.lobValue = lobValue;
    }

    /**
     * One "blank unit" that a rich-text editor (or plain typing) leaves
     * behind for an empty line: an empty paragraph/div (optionally
     * containing just a &lt;br&gt; or &amp;nbsp;), a bare &lt;br&gt;,
     * &amp;nbsp;, a literal "\n" escape sequence, or real whitespace.
     */
    private static final String LOB_VALUE_BLANK_UNIT
            = "(?:<p[^>]*>(?:\\s|&nbsp;|<br\\s*/?>)*</p>"
            + "|<div[^>]*>(?:\\s|&nbsp;|<br\\s*/?>)*</div>"
            + "|<br\\s*/?>"
            + "|&nbsp;"
            + "|\\\\n"
            + "|\\s)";

    /**
     * lobValue with leading/trailing blank lines removed, so printed reports
     * do not show empty gaps caused by blank lines (typed as literal "\n",
     * real newline characters, or empty rich-text paragraphs/breaks) left
     * over from data entry.
     */
    public String getLobValuePrintable() {
        if (lobValue == null) {
            return null;
        }
        String trimmed = lobValue.replaceAll("(?i)^(?:" + LOB_VALUE_BLANK_UNIT + ")+", "");
        trimmed = trimmed.replaceAll("(?i)(?:" + LOB_VALUE_BLANK_UNIT + ")+$", "");
        return trimmed;
    }

    /**
     * lobValuePrintable restructured into an auto-aligned label/value grid.
     * Each line is split on its first top-level ":" into a label (before)
     * and a value (after); rendered with the .micLineGrid CSS rules in
     * microbiology_patient_report.xhtml, the colons of every line in this
     * memo line up in one column no matter how long the longest label is -
     * the column width is not hardcoded, it is sized by the browser to the
     * widest label actually present. Lines without a ":" are left as a
     * single full-width line, untouched. List blocks (ol/ul) are left
     * unsplit since their marker layout is not compatible with the
     * label/value column split.
     */
    public String getLobValueLinesAligned() {
        String html = getLobValuePrintable();
        if (html == null) {
            return null;
        }
        // Normalize legacy plain "\n" (real or literal backslash-n), typed
        // before the rich-text editor existed, into <br/> line breaks.
        String normalized = html.replaceAll("\\\\n|\\r\\n|\\r|\\n", "<br/>");
        Document doc = Jsoup.parseBodyFragment(normalized);

        StringBuilder out = new StringBuilder("<div class=\"micLineGrid\">");
        for (MicLine line : splitIntoLines(new ArrayList<>(doc.body().childNodes()))) {
            appendLine(out, line);
        }
        out.append("</div>");
        return out.toString();
    }

    private static final class MicLine {

        final List<Node> nodes;
        final String extraClass;

        MicLine(List<Node> nodes, String extraClass) {
            this.nodes = nodes;
            this.extraClass = extraClass;
        }
    }

    private static List<MicLine> splitIntoLines(List<Node> topLevelNodes) {
        List<MicLine> lines = new ArrayList<>();
        List<Node> current = new ArrayList<>();
        for (Node node : topLevelNodes) {
            if (node instanceof Element) {
                Element el = (Element) node;
                String tag = el.tagName().toLowerCase();
                if ("br".equals(tag)) {
                    lines.add(new MicLine(current, null));
                    current = new ArrayList<>();
                    continue;
                }
                if ("ol".equals(tag) || "ul".equals(tag)) {
                    if (!current.isEmpty()) {
                        lines.add(new MicLine(current, null));
                        current = new ArrayList<>();
                    }
                    List<Node> single = new ArrayList<>();
                    single.add(el);
                    lines.add(new MicLine(single, null));
                    continue;
                }
                if ("p".equals(tag) || "div".equals(tag)) {
                    if (!current.isEmpty()) {
                        lines.add(new MicLine(current, null));
                        current = new ArrayList<>();
                    }
                    String indentClass = extractIndentClass(el);
                    boolean first = true;
                    for (List<Node> sub : splitOnBr(new ArrayList<>(el.childNodes()))) {
                        lines.add(new MicLine(sub, first ? indentClass : null));
                        first = false;
                    }
                    continue;
                }
            }
            current.add(node);
        }
        if (!current.isEmpty()) {
            lines.add(new MicLine(current, null));
        }
        return lines;
    }

    private static List<List<Node>> splitOnBr(List<Node> nodes) {
        List<List<Node>> result = new ArrayList<>();
        List<Node> current = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof Element && "br".equalsIgnoreCase(((Element) node).tagName())) {
                result.add(current);
                current = new ArrayList<>();
            } else {
                current.add(node);
            }
        }
        result.add(current);
        return result;
    }

    private static String extractIndentClass(Element el) {
        for (String cls : el.classNames()) {
            if (cls.startsWith("ql-indent-")) {
                return cls;
            }
        }
        return null;
    }

    private static void appendLine(StringBuilder out, MicLine line) {
        if (line.nodes.isEmpty()) {
            out.append("<div class=\"micLineFull\"></div>");
            return;
        }
        ColonSplit split = splitAtColon(line.nodes);
        String cls = line.extraClass == null ? "" : " " + line.extraClass;
        if (split.found) {
            out.append("<div class=\"micLineLabel").append(cls).append("\">")
                    .append(renderNodes(split.before)).append("</div>");
            out.append("<div class=\"micLineSep\">:</div>");
            out.append("<div class=\"micLineValue\">")
                    .append(renderNodes(split.after)).append("</div>");
        } else {
            out.append("<div class=\"micLineFull").append(cls).append("\">")
                    .append(renderNodes(line.nodes)).append("</div>");
        }
    }

    private static String renderNodes(List<Node> nodes) {
        // A detached Element has no owner Document, so it falls back to
        // Jsoup's default OutputSettings, which pretty-prints block tags
        // (e.g. ol/li) with indentation newlines between them. .micMemoValue
        // renders with white-space: pre-wrap to preserve intentional
        // spacing, so those injected newlines were showing up as a big gap
        // between list lines. Giving the wrapper an owner Document with
        // prettyPrint disabled avoids that.
        Document shell = Document.createShell("");
        shell.outputSettings().prettyPrint(false);
        Element wrapper = shell.body().appendElement("span");
        for (Node n : nodes) {
            wrapper.appendChild(n);
        }
        return wrapper.html();
    }

    private static final class ColonSplit {

        final List<Node> before = new ArrayList<>();
        final List<Node> after = new ArrayList<>();
        boolean found;
    }

    /**
     * Splits a line's node list at the first top-level ":" text character,
     * recursing into inline elements (strong/em/span/...) so formatting
     * that wraps the split point (e.g. "&lt;strong&gt;Organism:&lt;/strong&gt; E.coli")
     * is preserved on both sides rather than producing unbalanced HTML.
     */
    private static ColonSplit splitAtColon(List<Node> nodes) {
        ColonSplit split = new ColonSplit();
        for (Node node : nodes) {
            if (split.found) {
                split.after.add(node.clone());
                continue;
            }
            if (node instanceof TextNode) {
                String text = ((TextNode) node).getWholeText();
                int idx = text.indexOf(':');
                if (idx >= 0) {
                    split.found = true;
                    String beforeText = text.substring(0, idx);
                    String afterText = text.substring(idx + 1);
                    if (!beforeText.isEmpty()) {
                        split.before.add(new TextNode(beforeText));
                    }
                    if (!afterText.isEmpty()) {
                        split.after.add(new TextNode(afterText));
                    }
                } else {
                    split.before.add(node.clone());
                }
            } else if (node instanceof Element) {
                Element el = (Element) node;
                ColonSplit childSplit = splitAtColon(new ArrayList<>(el.childNodes()));
                if (childSplit.found) {
                    split.found = true;
                    Element beforeEl = el.shallowClone();
                    for (Node c : childSplit.before) {
                        beforeEl.appendChild(c);
                    }
                    if (beforeEl.childNodeSize() > 0) {
                        split.before.add(beforeEl);
                    }
                    Element afterEl = el.shallowClone();
                    for (Node c : childSplit.after) {
                        afterEl.appendChild(c);
                    }
                    if (afterEl.childNodeSize() > 0) {
                        split.after.add(afterEl);
                    }
                } else {
                    split.before.add(node.clone());
                }
            } else {
                split.before.add(node.clone());
            }
        }
        return split;
    }

    public byte[] getBaImage() {
        return baImage;
    }

    public void setBaImage(byte[] baImage) {
        this.baImage = baImage;
    }

    public String getFileName() {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        if (!fileName.toLowerCase().matches(".*\\.(bmp|png|jpg|jpeg|jpe)$")) {
            if (fileType != null && !fileType.isEmpty()) {
                return fileName + "." + fileType.toLowerCase();
            }
        }

        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Patient getPatient() {
        //////// // System.out.println("");
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public InvestigationItem getInvestigationItem() {
        return investigationItem;
    }

    public void setInvestigationItem(InvestigationItem investigationItem) {
        this.investigationItem = investigationItem;
    }

    public PatientReport getPatientReport() {
        return patientReport;
    }

    public void setPatientReport(PatientReport patientReport) {
        this.patientReport = patientReport;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof PatientReportItemValue)) {
            return false;
        }
        PatientReportItemValue other = (PatientReportItemValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.PatientInvestigationItemValue[ id=" + id + " ]";
    }

    public String getValue() {
        if (this.investigationItem == null || this.investigationItem.ixItemValueType == null) {
            return "";
        }

        String value = "";
        String formatString = this.investigationItem.formatString;

        switch (this.investigationItem.ixItemValueType) {
            case Double:
            case Long:
                if (this.doubleValue != null) {
                    if (formatString != null) {
                        DecimalFormat decimalFormat = new DecimalFormat(formatString);
                        value = decimalFormat.format(this.doubleValue);
                    } else {
                        value = Double.toString(this.doubleValue);
                    }
                }
                break;
            case Varchar:
                value = this.strValue;
                break;
            case Memo:
                value = this.lobValue;
                break;
            default:
                value = this.investigationItem.ixItemValueType.toString();
                break;
        }

        return value;
    }

    public String getDisplayValue() {
//        if (this.strValue != null && !this.strValue.trim().equals("")) {
//            displayValue = this.strValue;
//        } else {
//            displayValue = getValue();
//        }
//        return displayValue;
        return getValue();
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodeSystemCode() {
        return codeSystemCode;
    }

    public void setCodeSystemCode(String codeSystemCode) {
        this.codeSystemCode = codeSystemCode;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public PatientReportGroup getPatientReportGroup() {
        return patientReportGroup;
    }

    public void setPatientReportGroup(PatientReportGroup patientReportGroup) {
        this.patientReportGroup = patientReportGroup;
    }

    @Override
    public PatientReportItemValue clone() {
        PatientReportItemValue clone = new PatientReportItemValue();

        clone.setPatient(this.getPatient());
        clone.setPatientEncounter(this.getPatientEncounter());
        clone.setInvestigationItem(this.getInvestigationItem());
        clone.setPatientReport(this.getPatientReport());
        clone.setCodeSystem(this.getCodeSystem());
        clone.setCodeSystemCode(this.getCodeSystemCode());
        clone.setStrValue(this.getStrValue());
        clone.setLobValue(this.getLobValue());

        if (this.baImage != null) {
            clone.setBaImage(this.baImage.clone());
        }

        clone.setFileName(this.getFileName());
        clone.setFileType(this.getFileType());
        clone.setDoubleValue(this.getDoubleValue());
        clone.setPatientReportGroup(this.getPatientReportGroup());
        clone.setValue(this.getValue());
        clone.setDisplayValue(this.getDisplayValue());

        clone.setRetired(this.isRetired());
        clone.setRetirer(this.getRetirer());
        clone.setRetiredAt(this.getRetiredAt());
        clone.setRetireComments(this.getRetireComments());

        return clone;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public boolean isAllowToExportChart() {
        return allowToExportChart;
    }

    public void setAllowToExportChart(boolean allowToExportChart) {
        this.allowToExportChart = allowToExportChart;
    }

}
