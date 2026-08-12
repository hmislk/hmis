# Inpatient Direct Issue Bill — Service Charge/Discount + A4/FiveFive/POS Print Formats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the inpatient pharmacy direct-issue print and the related BHT return/reprint prints, show per-item and bill-level **Gross / Service Charge / Discount / Net** (margin labelled "Service Charge" everywhere), selectable across **A4 / FiveFive / POS** formats via a standard gear/Settings config dialog.

**Architecture:** This is a **print-template + print-config** change only — all four values already exist on `PrintBillData`/`BillItemData` (DTO family) and `Bill`/`BillItem` (entity family), so there are **no query/DTO/facade changes**. New dedicated composites are created so the widely-shared retail/wholesale sale composites are never touched. A gear dialog with three application-wide boolean config keys (A4/FiveFive/POS) drives which composite renders.

**Tech Stack:** JSF 2.x / PrimeFaces composite components (`cc:interface`/`cc:implementation`), Facelets, `configOptionApplicationController` config options, Maven build, Payara local deploy.

## Global Constraints

- **Repository:** https://github.com/hmislk/hmis — PR targets this branch `22035-view-bill-missing-items-bht-return` (updates PR #22054). Do NOT branch from or target `master`.
- **Margin label:** the word **"Service Charge"** must appear at every place margin is shown. Never "Margin" or "Matrix Value".
- **Do NOT regress sale bills:** `pharmacy_bill_retail_sale_native.xhtml` and `pharmacy_bill_wholesale_sale_native.xhtml` share the DTO-native composites `inward_direct_issue_bill_native_five_five_custom_3` and `saleBill_Header_Inward_native`. These existing composites must **not** be edited.
- **Config pattern:** follow `developer_docs/configuration/printer-configuration-system.md` — gear button (privilege `ChangeReceiptPrintingPaperTypes`) → `p:dialog` → one `h:selectBooleanCheckbox` per format (`h:` not `p:`) → each format's `h:panelGroup` renders on its own boolean key.
- **Config scope:** application-wide via `configOptionApplicationController` (to match the existing `Pharmacy Inward Direct Issue Bill is FiveFiveCustom3` / `PosHeaderPaper` keys on these pages).
- **New config keys** (defaults in parentheses):
  - `Pharmacy Inward Direct Issue Bill is A4` (false)
  - `Pharmacy Inward Direct Issue Bill is FiveFive` (true)
  - `Pharmacy Inward Direct Issue Bill is POS` (false)
- **Money-column gating:** wrap the four money columns in the existing guard exactly:
  `#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}`
- **Namespace → folder:** `phi:` = `http://xmlns.jcp.org/jsf/composite/pharmacy` → `src/main/webapp/resources/pharmacy/`. All new composites live in `resources/pharmacy/` and are referenced via the `phi:` prefix. Composite tag name = filename without `.xhtml`.
- **JSF-only note:** composite `.xhtml` and page `.xhtml` changes require **no Java compilation**; only Task 1 (config controller) touches Java and needs a build. Persistence: this branch keeps local JNDI (`jdbc/coop`, `jdbc/ruhunuAudit`) unstaged; swap to `${JDBC_DATASOURCE}` placeholders only right before a push, then restore.
- **Value magnitude:** returns/cancellations may store negative values. Follow the existing per-family convention — DTO family already prints `#{bid.grossValue < 0 ? -bid.grossValue : bid.grossValue}`; entity return bills print positive magnitudes the same way.

## File Structure

**New composites (all in `src/main/webapp/resources/pharmacy/`):**

DTO family (attrs: `bill`=`PrintBillData`, `items`=`List<BillItemData>`, `duplicate`?):
- `inward_direct_issue_bill_native_a4.xhtml`
- `inward_direct_issue_bill_native_five_five.xhtml`
- `inward_direct_issue_bill_native_pos.xhtml`

Entity family (attrs: `bill`=`com.divudi.core.entity.Bill`, `duplicate`?):
- `inward_direct_issue_bill_a4.xhtml`
- `inward_direct_issue_bill_five_five_sc.xhtml`  (`_sc` = "service charge" variant; avoids clashing with existing `_five_five_custom_3`)
- `inward_direct_issue_bill_pos.xhtml`

**Modified pages:**
- `src/main/webapp/inward/pharmacy_bill_issue_bht.xhtml` — DTO family + gear dialog.
- `src/main/webapp/inward/pharmacy_reprint_bill_sale_bht.xhtml` — entity family + gear dialog + relabel "Matrix Value"→"Service Charge" + add Gross/Discount to View Bill table.
- `src/main/webapp/inward/pharmacy_bill_return_bht_issue.xhtml` — entity family on both Sale-Bill and Return-Bill previews + gear dialog.
- `src/main/webapp/inward/pharmacy_reprint_bill_return_bht.xhtml` — entity family + gear dialog.

**Modified Java:**
- `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java` — 3 boolean props + load/save + getters/setters.

**Reused CSS (no new CSS files needed):** `sale_bill_five_five_custom_3.css` (FiveFive), `pharmacypos_header.css` (POS). A4 composites use inline `@media print` styling in the composite.

---

## Task 1: Config properties in PharmacyConfigController

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java`

**Interfaces:**
- Produces: EL-bindable properties `inwardDirectIssueBillA4`, `inwardDirectIssueBillFiveFive`, `inwardDirectIssueBillPos` (each `boolean` with `is`/`set` accessors); action method `saveInwardDirectIssueBillPaperConfig()`.
- Consumes: existing injected `configOptionApplicationController` field (already present in this class — verify with grep before editing).

- [ ] **Step 1: Confirm the app-wide config controller is injected**

Run: `grep -n "configOptionApplicationController" src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java`
Expected: a `@Inject`-ed field `private ConfigOptionApplicationController configOptionApplicationController;` exists (declared near line 20). If it is NOT injected, add:
```java
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
```
(import `com.divudi.bean.common.ConfigOptionApplicationController` if needed.)

- [ ] **Step 2: Add the three fields**

Add near the other paper-type fields (top of class, after existing `private boolean custom3Paper;` group):
```java
    // Inward Direct Issue Bill — Service Charge/Discount print formats (Issue #22035)
    private boolean inwardDirectIssueBillA4;
    private boolean inwardDirectIssueBillFiveFive;
    private boolean inwardDirectIssueBillPos;
```

- [ ] **Step 3: Load the three keys in loadCurrentConfig()**

Inside `loadCurrentConfig()` (starts line ~211), add after the Paper Type Settings block:
```java
        // Inward Direct Issue Bill print formats (Issue #22035) — application-wide
        inwardDirectIssueBillA4 = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is A4", false);
        inwardDirectIssueBillFiveFive = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is FiveFive", true);
        inwardDirectIssueBillPos = configOptionApplicationController.getBooleanValueByKey("Pharmacy Inward Direct Issue Bill is POS", false);
```

- [ ] **Step 4: Add the dedicated save method**

Add as a new method (e.g. right after `saveConfig()`, around line 393+):
```java
    public void saveInwardDirectIssueBillPaperConfig() {
        try {
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is A4", inwardDirectIssueBillA4);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is FiveFive", inwardDirectIssueBillFiveFive);
            configOptionApplicationController.setBooleanValueByKey("Pharmacy Inward Direct Issue Bill is POS", inwardDirectIssueBillPos);
            JsfUtil.addSuccessMessage("Inward Direct Issue Bill print format settings saved");
            loadCurrentConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving print format settings: " + e.getMessage());
        }
    }
```
(Confirm `com.divudi.core.util.JsfUtil` is already imported in this file — it is used by other save methods; if the import path differs, match the existing import.)

- [ ] **Step 5: Add getters/setters**

Add near the other accessors:
```java
    public boolean isInwardDirectIssueBillA4() { return inwardDirectIssueBillA4; }
    public void setInwardDirectIssueBillA4(boolean inwardDirectIssueBillA4) { this.inwardDirectIssueBillA4 = inwardDirectIssueBillA4; }

    public boolean isInwardDirectIssueBillFiveFive() { return inwardDirectIssueBillFiveFive; }
    public void setInwardDirectIssueBillFiveFive(boolean inwardDirectIssueBillFiveFive) { this.inwardDirectIssueBillFiveFive = inwardDirectIssueBillFiveFive; }

    public boolean isInwardDirectIssueBillPos() { return inwardDirectIssueBillPos; }
    public void setInwardDirectIssueBillPos(boolean inwardDirectIssueBillPos) { this.inwardDirectIssueBillPos = inwardDirectIssueBillPos; }
```

- [ ] **Step 6: Compile**

Run: `mvn -q -o compile` (or the project's configured build). Use the JDK 11 + Maven from `reference_maven_path` memory.
Expected: BUILD SUCCESS, no errors referencing the new symbols.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java
git commit -m "feat(inward): add A4/FiveFive/POS print-format config keys for direct-issue bill (#22035)"
```

---

## Task 2: DTO A4 composite

**Files:**
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_a4.xhtml`

**Interfaces:**
- Consumes: `cc.attrs.bill` (`PrintBillData` — getters `departmentName`, `patientName`, `patientAgeSex`, `bhtNo`, `roomName`, `createdAt`, `billNo`, `total`, `margin`, `discount`, `netTotal`); `cc.attrs.items` (`List<BillItemData>` — getters `itemName`, `qty`, `netRate`, `grossValue`, `marginValue`, `discountValue`, `netValue`).
- Produces: composite tag `phi:inward_direct_issue_bill_native_a4`.

- [ ] **Step 1: Create the A4 composite**

Create the file with this content:
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

    <cc:interface>
        <cc:attribute name="bill"      required="true"  type="com.divudi.core.data.dto.PrintBillData" />
        <cc:attribute name="items"     required="true"  type="java.util.List" />
        <cc:attribute name="duplicate" required="false" />
    </cc:interface>

    <cc:implementation>
        <style>
            .idi-a4 { width: 100%; font-family: Arial, sans-serif; font-size: 12px; color: #000; }
            .idi-a4 h2 { text-align: center; margin: 0 0 4px 0; }
            .idi-a4 .meta td { padding: 2px 8px; }
            .idi-a4 table.items { width: 100%; border-collapse: collapse; margin-top: 8px; }
            .idi-a4 table.items th, .idi-a4 table.items td { border: 1px solid #000; padding: 4px 6px; }
            .idi-a4 table.items th { text-align: right; }
            .idi-a4 table.items th.l, .idi-a4 table.items td.l { text-align: left; }
            .idi-a4 table.items td.r { text-align: right; }
            .idi-a4 table.totals { width: 40%; margin-top: 8px; margin-left: auto; border-collapse: collapse; }
            .idi-a4 table.totals td { padding: 2px 8px; }
            .idi-a4 table.totals td.r { text-align: right; }
            @media print { .idi-a4 { -webkit-print-color-adjust: exact; } }
        </style>

        <div class="idi-a4">
            <h2>
                <h:outputText value="#{cc.attrs.bill.departmentName}"/>
                <h:outputText value=" **Duplicate**" rendered="#{cc.attrs.duplicate eq true}"/>
            </h2>

            <table class="meta">
                <tr>
                    <td>Name</td><td>#{cc.attrs.bill.patientName}</td>
                    <td>BHT</td><td>#{cc.attrs.bill.bhtNo}</td>
                </tr>
                <tr>
                    <td>Age/Sex</td><td>#{cc.attrs.bill.patientAgeSex}</td>
                    <td>Room</td><td>#{cc.attrs.bill.roomName}</td>
                </tr>
                <tr>
                    <td>Bill No</td><td>#{cc.attrs.bill.billNo}</td>
                    <td>Date</td>
                    <td><h:outputText value="#{cc.attrs.bill.createdAt}"><f:convertDateTime pattern="yyyy-MM-dd HH:mm"/></h:outputText></td>
                </tr>
            </table>

            <table class="items">
                <thead>
                    <tr>
                        <th class="l">No</th>
                        <th class="l">Item</th>
                        <th>Qty</th>
                        <th>Rate</th>
                        <th>Gross</th>
                        <th>Service Charge</th>
                        <th>Discount</th>
                        <th>Net</th>
                    </tr>
                </thead>
                <tbody>
                    <ui:repeat value="#{cc.attrs.items}" var="item" varStatus="s">
                        <tr>
                            <td class="l">#{s.index + 1}</td>
                            <td class="l">#{item.itemName}</td>
                            <td class="r"><h:outputText value="#{item.qty}"><f:convertNumber integerOnly="true"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.netRate}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.grossValue lt 0 ? -item.grossValue : item.grossValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.marginValue lt 0 ? -item.marginValue : item.marginValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.discountValue lt 0 ? -item.discountValue : item.discountValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.netValue lt 0 ? -item.netValue : item.netValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                        </tr>
                    </ui:repeat>
                </tbody>
            </table>

            <table class="totals">
                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}">
                    <tr><td>Gross</td><td class="r"><h:outputText value="#{cc.attrs.bill.total}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td>Service Charge</td><td class="r"><h:outputText value="#{cc.attrs.bill.margin}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td>Discount</td><td class="r"><h:outputText value="#{cc.attrs.bill.discount}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td><strong>Net Total</strong></td><td class="r"><strong><h:outputText value="#{cc.attrs.bill.netTotal}"><f:convertNumber pattern="#,##0.00"/></h:outputText></strong></td></tr>
                </h:panelGroup>
                <tr><td>No of Items</td><td class="r"><h:outputText value="#{cc.attrs.items.size()}"><f:convertNumber integerOnly="true"/></h:outputText></td></tr>
            </table>

            <div style="margin-top: 12px; font-size: 11px;">
                Printed By: #{sessionController.loggedUser.name}
                <h:outputText value=" #{sessionController.currentDate}"><f:convertDateTime pattern="#{sessionController.applicationPreference.longDateTimeFormat}"/></h:outputText>
            </div>
        </div>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_a4.xhtml
git commit -m "feat(inward): add A4 DTO print composite with Service Charge/Discount (#22035)"
```

---

## Task 3: DTO FiveFive + POS composites

**Files:**
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_five_five.xhtml`
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_pos.xhtml`

**Interfaces:**
- Consumes: same `PrintBillData` + `List<BillItemData>` getters as Task 2.
- Produces: tags `phi:inward_direct_issue_bill_native_five_five`, `phi:inward_direct_issue_bill_native_pos`.

- [ ] **Step 1: Create the FiveFive composite**

Base it on the existing `inward_direct_issue_bill_native_five_five_custom_3.xhtml` (reuse its header/`sale_bill_five_five_custom_3.css`), but change the item table + totals to include all four money columns. Create the file:
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

    <cc:interface>
        <cc:attribute name="bill"      required="true"  type="com.divudi.core.data.dto.PrintBillData" />
        <cc:attribute name="items"     required="true"  type="java.util.List" />
        <cc:attribute name="duplicate" required="false" />
    </cc:interface>

    <cc:implementation>
        <h:outputStylesheet library="css" name="sale_bill_five_five_custom_3.css" />

        <div class="receipt-container">
            <div class="hospital-name">
                <h:outputLabel value="#{cc.attrs.bill.departmentName}"/>
                <h:outputLabel value="**Duplicate**" rendered="#{cc.attrs.duplicate eq true}" />
            </div>
            <div class="separator"></div>

            <table class="info-table">
                <h:panelGroup rendered="#{cc.attrs.bill.patientName ne null}">
                    <tr><td style="padding: 2px;">Name</td><td style="padding: 2px;" colspan="5"><h:outputLabel value="#{cc.attrs.bill.patientName}"/></td></tr>
                </h:panelGroup>
                <h:panelGroup rendered="#{cc.attrs.bill.bhtNo ne null}">
                    <tr>
                        <td style="padding: 2px;">BHT</td><td style="padding: 2px;">#{cc.attrs.bill.bhtNo}</td>
                        <td style="padding: 2px;" class="spacer"></td>
                        <td style="padding: 2px;">Room</td><td style="padding: 2px;">#{cc.attrs.bill.roomName}</td>
                    </tr>
                </h:panelGroup>
                <tr>
                    <td style="padding: 2px;">Bill No</td><td style="padding: 2px;">#{cc.attrs.bill.billNo}</td>
                    <td style="padding: 2px;" class="spacer"></td>
                    <td style="padding: 2px;">Date</td>
                    <td style="padding: 2px;"><h:outputLabel value="#{cc.attrs.bill.createdAt}"><f:convertDateTime pattern="yyyy-MM-dd HH:mm"/></h:outputLabel></td>
                </tr>
            </table>
            <div class="separator"></div>

            <table class="receipt-table">
                <thead>
                    <tr>
                        <th>No</th>
                        <th>Item</th>
                        <th style="text-align: right">Qty</th>
                        <th style="text-align: right">Gross</th>
                        <th style="text-align: right">Serv.Chg</th>
                        <th style="text-align: right">Disc</th>
                        <th style="text-align: right">Net</th>
                    </tr>
                </thead>
                <tbody>
                    <ui:repeat value="#{cc.attrs.items}" var="item" varStatus="s">
                        <tr>
                            <td>#{s.index + 1}</td>
                            <td>#{item.itemName}</td>
                            <td style="text-align: right"><h:outputLabel value="#{item.qty}"><f:convertNumber integerOnly="true"/></h:outputLabel></td>
                            <td style="text-align: right"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.grossValue lt 0 ? -item.grossValue : item.grossValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                            <td style="text-align: right"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.marginValue lt 0 ? -item.marginValue : item.marginValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                            <td style="text-align: right"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.discountValue lt 0 ? -item.discountValue : item.discountValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                            <td style="text-align: right"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.netValue lt 0 ? -item.netValue : item.netValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                        </tr>
                    </ui:repeat>
                </tbody>
            </table>
            <div class="separator"></div>

            <table class="total-table">
                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}">
                    <tr><td>Gross:</td><td style="text-align: right;"><h:outputLabel value="#{cc.attrs.bill.total}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td>Service Charge:</td><td style="text-align: right;"><h:outputLabel value="#{cc.attrs.bill.margin}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td>Discount:</td><td style="text-align: right;"><h:outputLabel value="#{cc.attrs.bill.discount}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td>Net Total:</td><td style="text-align: right;"><h:outputLabel value="#{cc.attrs.bill.netTotal}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                </h:panelGroup>
                <tr><td>No of Items:</td><td><h:outputLabel value="#{cc.attrs.items.size()}"><f:convertNumber integerOnly="true"/></h:outputLabel></td></tr>
            </table>

            <table>
                <tr><td>
                    <h:outputLabel value="Printed By :"/>
                    <h:outputLabel value="#{sessionController.loggedUser.name}"/>
                    <h:outputLabel value="&nbsp;&nbsp;"/>
                    <h:outputLabel value="#{sessionController.currentDate}"><f:convertDateTime pattern="#{sessionController.applicationPreference.longDateTimeFormat}"/></h:outputLabel>
                </td></tr>
            </table>
        </div>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Create the POS composite**

Base it on `saleBill_Header_Inward_native.xhtml` (reuse `pharmacypos_header.css` + its logo/header block), replacing the item/totals section with the four money columns. Create the file:
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

    <cc:interface>
        <cc:attribute name="bill"      required="true"  type="com.divudi.core.data.dto.PrintBillData" />
        <cc:attribute name="items"     required="true"  type="java.util.List" />
        <cc:attribute name="duplicate" required="false" />
    </cc:interface>

    <cc:implementation>
        <h:outputStylesheet library="css" name="pharmacypos_header.css"/>
        <div id="printAllNativeSc" class="posbillheader" style="page-break-after: always!important; max-width: 72mm; width: 72mm;">
            <div style="text-align: center; font-size: 12px;">
                <h:outputLabel value="#{cc.attrs.bill.institutionName}"/><br/>
                <h:outputLabel style="font-size: 10px!important;" value="#{cc.attrs.bill.institutionAddress}"/><br/>
                <h:outputLabel value="#{cc.attrs.bill.departmentPrintingName}"/><br/>
            </div>
            <div style="text-align: center; font-weight: bold;">
                ISSUE BILL
                <h:outputLabel value=" **Duplicate**" rendered="#{cc.attrs.duplicate eq true}"/>
            </div>
            <div style="font-size: 11px;">
                <div>Bill No : #{cc.attrs.bill.billNo}</div>
                <div>Date : <h:outputLabel value="#{cc.attrs.bill.createdAt}"><f:convertDateTime pattern="yyyy-MM-dd HH:mm"/></h:outputLabel></div>
                <h:panelGroup rendered="#{cc.attrs.bill.patientName ne null}"><div>Patient : #{cc.attrs.bill.patientName}</div></h:panelGroup>
                <h:panelGroup rendered="#{cc.attrs.bill.bhtNo ne null}"><div>BHT : #{cc.attrs.bill.bhtNo}</div></h:panelGroup>
                <h:panelGroup rendered="#{cc.attrs.bill.roomName ne null}"><div>Room : #{cc.attrs.bill.roomName}</div></h:panelGroup>
            </div>
            <hr/>

            <table width="100%" style="width: 100%; font-size: 10px;">
                <tr>
                    <td style="text-align:left;">Item</td>
                    <td style="text-align:right;">Qty</td>
                    <td style="text-align:right;">Gross</td>
                    <td style="text-align:right;">S.Chg</td>
                    <td style="text-align:right;">Disc</td>
                    <td style="text-align:right;">Net</td>
                </tr>
                <ui:repeat value="#{cc.attrs.items}" var="bip">
                    <tr><td colspan="6" style="text-transform: capitalize;">#{bip.itemName}</td></tr>
                    <tr>
                        <td></td>
                        <td style="text-align:right;"><h:outputLabel value="#{bip.qty}"><f:convertNumber integerOnly="true"/></h:outputLabel></td>
                        <td style="text-align:right;"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{bip.grossValue lt 0 ? -bip.grossValue : bip.grossValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                        <td style="text-align:right;"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{bip.marginValue lt 0 ? -bip.marginValue : bip.marginValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                        <td style="text-align:right;"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{bip.discountValue lt 0 ? -bip.discountValue : bip.discountValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                        <td style="text-align:right;"><h:outputLabel rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{bip.netValue lt 0 ? -bip.netValue : bip.netValue}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td>
                    </tr>
                </ui:repeat>
            </table>
            <hr/>

            <table style="width: 100%; font-size: 11px;">
                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}">
                    <tr><td style="text-align:left;">Gross</td><td style="text-align:right;"><h:outputLabel value="#{cc.attrs.bill.total}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td style="text-align:left;">Service Charge</td><td style="text-align:right;"><h:outputLabel value="#{cc.attrs.bill.margin}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td style="text-align:left;">Discount</td><td style="text-align:right;"><h:outputLabel value="#{cc.attrs.bill.discount}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                    <tr><td style="text-align:left; font-weight:bold;">Net Total</td><td style="text-align:right; font-weight:bold;"><h:outputLabel value="#{cc.attrs.bill.netTotal}"><f:convertNumber pattern="#,##0.00"/></h:outputLabel></td></tr>
                </h:panelGroup>
                <tr><td style="text-align:left;">No of Items</td><td style="text-align:right;"><h:outputLabel value="#{cc.attrs.items.size()}"><f:convertNumber integerOnly="true"/></h:outputLabel></td></tr>
            </table>

            <div style="text-align:center; margin-top:6px;">
                THANK YOU !<br/>
                <h:outputLabel style="font-size: 9px" value="Powered by CareCode (Pvt) Ltd."/>
            </div>
        </div>
    </cc:implementation>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_five_five.xhtml src/main/webapp/resources/pharmacy/inward_direct_issue_bill_native_pos.xhtml
git commit -m "feat(inward): add FiveFive + POS DTO print composites with Service Charge/Discount (#22035)"
```

---

## Task 4: Wire DTO composites + gear dialog into pharmacy_bill_issue_bht.xhtml

**Files:**
- Modify: `src/main/webapp/inward/pharmacy_bill_issue_bht.xhtml` (preview header ~lines 552-566; `gpBillPreview` ~lines 623-636)

**Interfaces:**
- Consumes: `inpatientDirectIssueNativeSqlController.printBill` (`PrintBillData`), `.printBillItems` (`List<BillItemData>`); `pharmacyConfigController` properties from Task 1; the three new DTO composites from Tasks 2-3.

- [ ] **Step 1: Add the gear button to the preview panel header**

In the `<div class="d-flex gap-2">` block that holds the Print button (right after the opening of that div, before `btnPrint`), add:
```xhtml
                                    <p:commandButton
                                        rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
                                        icon="fas fa-cog"
                                        styleClass="ui-button-secondary ui-button-outlined"
                                        type="button"
                                        title="Print Format Settings"
                                        onclick="PF('idiBillPaperConfigDialog').show();" />
```

- [ ] **Step 2: Replace the two hard-wired preview panelGroups with the three new-format panelGroups**

Inside `<h:panelGroup id="gpBillPreview">`, ADD these three panelGroups (leave the existing FiveFiveCustom3 / PosHeader panelGroups in place for backward compatibility — they default to their own keys):
```xhtml
                            <h:panelGroup id="gpBillPreviewA4New"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is A4', false)}">
                                <phi:inward_direct_issue_bill_native_a4
                                    bill="#{inpatientDirectIssueNativeSqlController.printBill}"
                                    items="#{inpatientDirectIssueNativeSqlController.printBillItems}"/>
                            </h:panelGroup>
                            <h:panelGroup id="gpBillPreviewFiveFiveNew"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is FiveFive', true)}">
                                <phi:inward_direct_issue_bill_native_five_five
                                    bill="#{inpatientDirectIssueNativeSqlController.printBill}"
                                    items="#{inpatientDirectIssueNativeSqlController.printBillItems}"/>
                            </h:panelGroup>
                            <h:panelGroup id="gpBillPreviewPosNew"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is POS', false)}">
                                <phi:inward_direct_issue_bill_native_pos
                                    bill="#{inpatientDirectIssueNativeSqlController.printBill}"
                                    items="#{inpatientDirectIssueNativeSqlController.printBillItems}"/>
                            </h:panelGroup>
```
Then change the two existing panelGroups' default keys so they are **off by default** to avoid double-printing: set `FiveFiveCustom3` default from `true` to `false` and leave `PosHeaderPaper` at `false`. i.e. edit line ~625 to `...getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is FiveFiveCustom3',false)`.

- [ ] **Step 3: Add the gear dialog at the end of the form (before `</h:form>` at line ~642)**

```xhtml
            <p:dialog id="idiBillPaperConfigDialog"
                      header="Direct Issue Bill - Print Format Settings"
                      widgetVar="idiBillPaperConfigDialog"
                      modal="true" width="500" resizable="false" closeOnEscape="true">
                <p:ajax event="open" listener="#{pharmacyConfigController.loadCurrentConfig}" update="idiBillPaperConfigForm" />
                <h:form id="idiBillPaperConfigForm">
                    <div class="mb-3">
                        <h:selectBooleanCheckbox id="cbA4" value="#{pharmacyConfigController.inwardDirectIssueBillA4}" />
                        <h:outputLabel for="cbA4" value="A4 Paper" class="ms-2" />
                    </div>
                    <div class="mb-3">
                        <h:selectBooleanCheckbox id="cbFiveFive" value="#{pharmacyConfigController.inwardDirectIssueBillFiveFive}" />
                        <h:outputLabel for="cbFiveFive" value="5 x 5 Paper" class="ms-2" />
                    </div>
                    <div class="mb-3">
                        <h:selectBooleanCheckbox id="cbPos" value="#{pharmacyConfigController.inwardDirectIssueBillPos}" />
                        <h:outputLabel for="cbPos" value="POS Paper" class="ms-2" />
                    </div>
                    <p:messages showDetail="true" closable="true" />
                    <div class="d-flex gap-2">
                        <p:commandButton value="Apply &amp; Close" icon="fas fa-save"
                                         styleClass="ui-button-success" ajax="false"
                                         action="#{pharmacyConfigController.saveInwardDirectIssueBillPaperConfig}" />
                        <p:commandButton value="Cancel" icon="fas fa-times" styleClass="ui-button-secondary"
                                         onclick="PF('idiBillPaperConfigDialog').hide(); return false;" type="button" />
                    </div>
                </h:form>
            </p:dialog>
```

- [ ] **Step 4: Deploy and smoke-test**

Rebuild/redeploy locally (Maven + asadmin per `reference_payara_install_path`). Then via Playwright per `developer_docs/testing/playwright-e2e-workflow.md`: login → select a pharmacy department → open `pharmacy_bill_issue_bht.xhtml`, issue 2 items to an inpatient, Settle. In preview: open gear, verify all three checkboxes load; toggle to A4 → Apply → confirm A4 layout shows Gross/Service Charge/Discount/Net per item + bill totals; repeat for FiveFive and POS.
Expected: exactly one format renders per selection; "Service Charge" label present; no double bill.

- [ ] **Step 5: Commit**

```bash
git add src/main/webapp/inward/pharmacy_bill_issue_bht.xhtml
git commit -m "feat(inward): wire A4/FiveFive/POS Service Charge print formats + config dialog on direct-issue page (#22035)"
```

---

## Task 5: Entity A4 + FiveFive + POS composites (issue bills)

**Files:**
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_a4.xhtml`
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_five_five_sc.xhtml`
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_bill_pos.xhtml`

**Interfaces:**
- Consumes: `cc.attrs.bill` (`com.divudi.core.entity.Bill`) — via `bill.department.name`, `bill.patient.person.nameWithTitle`, `bill.patient.person.ageAsShortString`, `bill.patient.person.sex.label`, `bill.patientEncounter.bhtNo`, `bill.patientEncounter.currentPatientRoom.roomFacilityCharge.name`, `bill.createdAt`, `bill.deptId`, `bill.billItems` (each `BillItem`: `item.name`, `qty`, `netRate`, `grossValue`, `marginValue`, `discount`, `netValue`), `bill.total`, `bill.margin`, `bill.discount`, `bill.netTotal`, `bill.cancelled`.
- Produces: tags `phi:inward_direct_issue_bill_a4`, `phi:inward_direct_issue_bill_five_five_sc`, `phi:inward_direct_issue_bill_pos`.

- [ ] **Step 1: Create the entity A4 composite**

Mirror Task 2's A4 markup but bound to the `Bill` entity. Key differences from the DTO version: `type="com.divudi.core.entity.Bill"`, no `items` attribute (iterate `cc.attrs.bill.billItems`), item fields `item.item.name`, `item.qty`, `item.netRate`, `item.grossValue`, `item.marginValue`, `item.discount`, `item.netValue`; header uses entity getters listed above; bill totals `cc.attrs.bill.total|margin|discount|netTotal`. Add `**Cancelled**` label when `cc.attrs.bill.cancelled eq true`. Use the same `.idi-a4` inline `<style>` block from Task 2 and the same money-column guard and abs() convention. Full file:
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">
    <cc:interface>
        <cc:attribute name="bill"      required="true"  type="com.divudi.core.entity.Bill" />
        <cc:attribute name="duplicate" required="false" />
    </cc:interface>
    <cc:implementation>
        <style>
            .idi-a4 { width: 100%; font-family: Arial, sans-serif; font-size: 12px; color: #000; }
            .idi-a4 h2 { text-align: center; margin: 0 0 4px 0; }
            .idi-a4 .meta td { padding: 2px 8px; }
            .idi-a4 table.items { width: 100%; border-collapse: collapse; margin-top: 8px; }
            .idi-a4 table.items th, .idi-a4 table.items td { border: 1px solid #000; padding: 4px 6px; }
            .idi-a4 table.items th { text-align: right; }
            .idi-a4 table.items th.l, .idi-a4 table.items td.l { text-align: left; }
            .idi-a4 table.items td.r { text-align: right; }
            .idi-a4 table.totals { width: 40%; margin-top: 8px; margin-left: auto; border-collapse: collapse; }
            .idi-a4 table.totals td { padding: 2px 8px; }
            .idi-a4 table.totals td.r { text-align: right; }
        </style>
        <div class="idi-a4">
            <h2>
                <h:outputText value="#{cc.attrs.bill.department.name}"/>
                <h:outputText value=" **Duplicate**" rendered="#{cc.attrs.duplicate eq true}"/>
                <h:outputText value=" **Cancelled**" rendered="#{cc.attrs.bill.cancelled eq true}"/>
            </h2>
            <table class="meta">
                <tr>
                    <td>Name</td><td>#{cc.attrs.bill.patient.person.nameWithTitle}</td>
                    <td>BHT</td><td>#{cc.attrs.bill.patientEncounter.bhtNo}</td>
                </tr>
                <tr>
                    <td>Bill No</td><td>#{cc.attrs.bill.deptId}</td>
                    <td>Date</td>
                    <td><h:outputText value="#{cc.attrs.bill.createdAt}"><f:convertDateTime pattern="yyyy-MM-dd HH:mm"/></h:outputText></td>
                </tr>
            </table>
            <table class="items">
                <thead>
                    <tr>
                        <th class="l">No</th><th class="l">Item</th><th>Qty</th><th>Rate</th>
                        <th>Gross</th><th>Service Charge</th><th>Discount</th><th>Net</th>
                    </tr>
                </thead>
                <tbody>
                    <ui:repeat value="#{cc.attrs.bill.billItems}" var="item" varStatus="s">
                        <tr>
                            <td class="l">#{s.index + 1}</td>
                            <td class="l">#{item.item.name}</td>
                            <td class="r"><h:outputText value="#{item.qty}"><f:convertNumber integerOnly="true"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.netRate}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.grossValue lt 0 ? -item.grossValue : item.grossValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.marginValue lt 0 ? -item.marginValue : item.marginValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.discount lt 0 ? -item.discount : item.discount}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                            <td class="r"><h:outputText rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}" value="#{item.netValue lt 0 ? -item.netValue : item.netValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td>
                        </tr>
                    </ui:repeat>
                </tbody>
            </table>
            <table class="totals">
                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')}">
                    <tr><td>Gross</td><td class="r"><h:outputText value="#{cc.attrs.bill.total lt 0 ? -cc.attrs.bill.total : cc.attrs.bill.total}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td>Service Charge</td><td class="r"><h:outputText value="#{cc.attrs.bill.margin lt 0 ? -cc.attrs.bill.margin : cc.attrs.bill.margin}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td>Discount</td><td class="r"><h:outputText value="#{cc.attrs.bill.discount lt 0 ? -cc.attrs.bill.discount : cc.attrs.bill.discount}"><f:convertNumber pattern="#,##0.00"/></h:outputText></td></tr>
                    <tr><td><strong>Net Total</strong></td><td class="r"><strong><h:outputText value="#{cc.attrs.bill.netTotal lt 0 ? -cc.attrs.bill.netTotal : cc.attrs.bill.netTotal}"><f:convertNumber pattern="#,##0.00"/></h:outputText></strong></td></tr>
                </h:panelGroup>
                <tr><td>No of Items</td><td class="r"><h:outputText value="#{cc.attrs.bill.billItems.size()}"><f:convertNumber integerOnly="true"/></h:outputText></td></tr>
            </table>
        </div>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Create the entity FiveFive composite (`_sc`)**

Copy Task 3's FiveFive markup, change `type` to `com.divudi.core.entity.Bill`, drop the `items` attribute, iterate `cc.attrs.bill.billItems`, and use entity field paths (`item.item.name`, `item.qty`, `item.grossValue`, `item.marginValue`, `item.discount`, `item.netValue`) and header entity getters (`bill.department.name`, `bill.patient.person.nameWithTitle`, `bill.patientEncounter.bhtNo`, `bill.patientEncounter.currentPatientRoom.roomFacilityCharge.name`, `bill.deptId`, `bill.createdAt`). Bill totals: `bill.total|margin|discount|netTotal`. Keep the same money-column guard, abs() convention, and "Service Charge" labels. Reuse `sale_bill_five_five_custom_3.css`.

- [ ] **Step 3: Create the entity POS composite**

Copy Task 3's POS markup, same entity substitutions as Step 2, container id `printAllScPos`. Header uses `bill.department.name`/institution getters available on the entity (`bill.institution.name`, `bill.institution.address`, `bill.department.printingName` — verify getter names via grep on `Bill.java`/`Department.java`; fall back to `bill.department.name` if a printing-name getter is absent). Reuse `pharmacypos_header.css`.

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/resources/pharmacy/inward_direct_issue_bill_a4.xhtml src/main/webapp/resources/pharmacy/inward_direct_issue_bill_five_five_sc.xhtml src/main/webapp/resources/pharmacy/inward_direct_issue_bill_pos.xhtml
git commit -m "feat(inward): add entity A4/FiveFive/POS print composites with Service Charge/Discount (#22035)"
```

---

## Task 6: Entity return-bill composites

**Files:**
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_a4.xhtml`
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_five_five_sc.xhtml`
- Create: `src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_pos.xhtml`

**Interfaces:**
- Consumes: same `Bill` entity interface as Task 5, applied to a **return** bill (`bhtIssueReturnController.returnBill` / `pharmacyBillSearch.bill` on the reprint-return page). Values are negative on return bills → abs() convention (already in Task 5 markup) prints positive magnitudes.
- Produces: tags `phi:inward_direct_issue_return_bill_a4`, `phi:inward_direct_issue_return_bill_five_five_sc`, `phi:inward_direct_issue_return_bill_pos`.

- [ ] **Step 1: Create the three return composites**

These are identical to Task 5's three entity composites except the header title reads "RETURN BILL" (POS) / adds a "(Return)" suffix to the department heading (A4/FiveFive). Because Task 5's composites already print abs() magnitudes and label margin as "Service Charge", the only content change is the title text. Create all three files by copying the Task 5 equivalents and changing the heading text accordingly.

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_a4.xhtml src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_five_five_sc.xhtml src/main/webapp/resources/pharmacy/inward_direct_issue_return_bill_pos.xhtml
git commit -m "feat(inward): add entity return-bill A4/FiveFive/POS composites with Service Charge/Discount (#22035)"
```

---

## Task 7: Wire reprint page (#22035) — View Bill relabel + entity composites + gear dialog

**Files:**
- Modify: `src/main/webapp/inward/pharmacy_reprint_bill_sale_bht.xhtml`

**Interfaces:**
- Consumes: `pharmacyBillSearch.bill` (`Bill`); entity issue composites from Task 5; `pharmacyConfigController` config props from Task 1.

- [ ] **Step 1: Fix the on-screen View Bill table (the literal #22035 bug + relabel)**

In the left `tbl` dataTable (lines ~126-211): the reported bug is missing Item/Qty — verify the Item Name column (line ~144 `#{bip.item.name}`) and QTY column (line ~151) render (they are currently gated only by `ShowInwardFee`; keep them always visible within that table). Relabel the "Matrix Value" column header (line ~171) to **"Service Charge"**. Add a "Discount" column (using `#{bip.discount}`) between Gross Value and Service Charge so the on-screen table shows Gross / Discount / Service Charge / Net, matching the printed bill. Update the footer `colspan`s accordingly.

- [ ] **Step 2: Add the three entity-format panelGroups into `gpBillPreview`**

Inside `<h:panelGroup id="gpBillPreview">` (lines ~228-246), ADD:
```xhtml
                                        <h:panelGroup id="gpBillPreviewA4Sc" rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is A4', false)}">
                                            <phi:inward_direct_issue_bill_a4 bill="#{pharmacyBillSearch.bill}" duplicate="true" />
                                        </h:panelGroup>
                                        <h:panelGroup id="gpBillPreviewFiveFiveSc" rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is FiveFive', true)}">
                                            <phi:inward_direct_issue_bill_five_five_sc bill="#{pharmacyBillSearch.bill}" duplicate="true" />
                                        </h:panelGroup>
                                        <h:panelGroup id="gpBillPreviewPosSc" rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is POS', false)}">
                                            <phi:inward_direct_issue_bill_pos bill="#{pharmacyBillSearch.bill}" duplicate="true" />
                                        </h:panelGroup>
```
Change the two existing composite panelGroups (`FiveFiveCustom3` default `true`, `PosHeader` default `true` on this page — lines ~238, ~242) to default `false` so only the new formats print by default.

- [ ] **Step 3: Extend the existing gear dialog with A4/FiveFive/POS**

The page already has `reprintBhtIssuePaperConfigDialog` (lines ~254-292) bound to the deprecated `pharmacyBillSearch` paper-type props. **Do NOT modify that dialog** (it drives the legacy POS/5x5 preview and touches `departmentPreference`). Instead add a **separate** dialog `idiBillPaperConfigDialog` (identical to Task 4 Step 3) plus a second gear button, keeping the two config mechanisms cleanly decoupled. Add the gear button next to the existing one (line ~25-31 area):
```xhtml
                                    <p:commandButton
                                        rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
                                        icon="fas fa-cog"
                                        styleClass="ui-button-secondary ui-button-outlined"
                                        type="button"
                                        title="Service Charge Print Format Settings"
                                        onclick="PF('idiBillPaperConfigDialog').show();" />
```
And add the `idiBillPaperConfigDialog` dialog (copy verbatim from Task 4 Step 3) after the existing `reprintBhtIssuePaperConfigDialog` (before `</ui:define>`).

- [ ] **Step 4: Deploy and E2E test the #22035 scenario**

Redeploy. Via Playwright: navigate to a settled direct-issue bill's reprint page → confirm the View Bill table shows Item + Qty (bug fixed), header reads "Service Charge" not "Matrix Value", Discount column present. Open the new gear → toggle A4/FiveFive/POS → Reprint → confirm the printed preview matches selection with all four money columns + "Service Charge".
Expected: Item/Qty visible; correct labels; one format prints.

- [ ] **Step 5: Commit**

```bash
git add src/main/webapp/inward/pharmacy_reprint_bill_sale_bht.xhtml
git commit -m "fix(inward): show Item/Qty + Service Charge/Discount on reprint View Bill and add A4/FiveFive/POS formats (#22035)"
```

---

## Task 8: Wire return page + reprint-return page

**Files:**
- Modify: `src/main/webapp/inward/pharmacy_bill_return_bht_issue.xhtml`
- Modify: `src/main/webapp/inward/pharmacy_reprint_bill_return_bht.xhtml`

**Interfaces:**
- Consumes: `bhtIssueReturnController.bill` (issue `Bill`) and `.returnBill` (return `Bill`); `pharmacyBillSearch.bill` on reprint-return; entity issue composites (Task 5) for the Sale-Bill preview, entity return composites (Task 6) for the Return-Bill preview.

- [ ] **Step 1: Return page — Sale Bill preview (`gpBillPreview2`)**

Inside `gpBillPreview2` (the Sale Bill Preview panel, lines ~270+), ADD three panelGroups rendering the **issue** composites (Task 5) bound to `#{bhtIssueReturnController.bill}`, keyed on the three new config keys (A4 default false / FiveFive default true / POS default false), mirroring Task 7 Step 2. Set the existing `FiveFiveCustom3Original` panelGroup default to `false`.

- [ ] **Step 2: Return page — Return Bill preview (`gpBillPreview1`)**

Inside `gpBillPreview1` (lines ~318+), ADD three panelGroups rendering the **return** composites (Task 6) bound to `#{bhtIssueReturnController.returnBill}`, keyed on the same three keys. Set the existing `FiveFiveCustom3` (return) and `PosHeader` panelGroups defaults to `false`.

- [ ] **Step 3: Return page — gear button + dialog**

Add the gear button in the page's top action area and the `idiBillPaperConfigDialog` (copy from Task 4 Step 3, unique widgetVar per page e.g. `idiReturnBillPaperConfigDialog` — update both the `onclick` and dialog `id`/`widgetVar` to match).

- [ ] **Step 4: Reprint-return page**

In `pharmacy_reprint_bill_return_bht.xhtml`, inside its `gpBillPreviewPosHeader`/`gpBillPreviewOther` area (lines ~164-167), ADD the three return-composite panelGroups bound to `#{pharmacyBillSearch.bill}` keyed on the three new keys; set existing defaults to false. Add gear button + `idiBillPaperConfigDialog` (unique widgetVar, e.g. `idiReprintReturnPaperConfigDialog`).

- [ ] **Step 5: Deploy and E2E test returns**

Redeploy. Via Playwright: from a settled issue bill, "To Return Item", return 1 item, settle the return. On the return page confirm both Sale Bill and Return Bill previews show all four money columns + "Service Charge"; return values print as positive magnitudes. Toggle formats via gear. Then open the reprint-return page for that return and confirm the same.
Expected: both previews correct in all three formats.

- [ ] **Step 6: Commit**

```bash
git add src/main/webapp/inward/pharmacy_bill_return_bht_issue.xhtml src/main/webapp/inward/pharmacy_reprint_bill_return_bht.xhtml
git commit -m "feat(inward): add A4/FiveFive/POS Service Charge formats to BHT return + reprint-return pages (#22035)"
```

---

## Task 9: Regression check + PR update

**Files:** none (verification only).

- [ ] **Step 1: Sale-bill regression (shared composites untouched)**

Via Playwright: open `pharmacy_bill_retail_sale_native.xhtml`, make a small retail sale, settle, print. Confirm its bill is **unchanged** (the shared `inward_direct_issue_bill_native_five_five_custom_3` / `saleBill_Header_Inward_native` composites were not edited).
Expected: identical to pre-change output.

- [ ] **Step 2: Verify no stray double-printing**

On each of the 4 pages, with default config (FiveFive on, A4/POS off), confirm exactly one preview renders. Confirm old-composite keys default to false everywhere they were changed.

- [ ] **Step 3: Persistence swap + push**

Per CLAUDE.md rule #17: change `persistence.xml` to `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}`, preserving `<!-- Do NOT Remove -->` props. Commit that swap, `git push` to `22035-view-bill-missing-items-bht-return`, then immediately restore local JNDI (`jdbc/coop` / `jdbc/ruhunuAudit`) and leave it **unstaged**.

- [ ] **Step 4: Update PR #22054 description**

Append to PR #22054 body a summary of the added print-format work (Service Charge/Discount per item + bill level; A4/FiveFive/POS gear config across issue, reprint, return, and reprint-return pages). Note the original #22035 View-Bill fix is included.

- [ ] **Step 5: Confirm CI green + request review**

Watch the PR's CI checks; once green, request CodeRabbit review. Address any comments via the `/review-pr` workflow.

---

## Notes for the implementer

- **JSF-only tasks (2,3,5,6 and the page-wiring tasks) need no `mvn compile`** — only Task 1 touches Java. Redeploy the exploded webapp / WAR to see composite changes.
- Verify every entity getter path in Tasks 5-6 against `Bill.java`/`BillItem.java`/`Department.java` with grep before finalizing (e.g. confirm `getDeptId`, `getNetRate`, `getMarginValue`, `getPrintingName`) — automated composite copies are the most likely place for a wrong getter name.
- Keep "Service Charge" spelled exactly, everywhere.
- Do not edit `inward_direct_issue_bill_native_five_five_custom_3.xhtml` or `saleBill_Header_Inward_native.xhtml` (shared with sale pages).
