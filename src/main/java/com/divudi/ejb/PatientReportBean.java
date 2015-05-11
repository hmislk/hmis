/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import java.util.TimeZone;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.entity.Patient;
import com.divudi.entity.lab.Antibiotic;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValueFlag;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.AntibioticFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFlagFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class PatientReportBean {

    @EJB
    private InvestigationItemFacade ixItemFacade;
    @EJB
    private PatientReportItemValueFacade ptRivFacade;
    @EJB
    private PatientReportFacade prFacade;

    public PatientReport patientReportFromPatientIx(PatientInvestigation pi) {
        String sql;
        PatientReport r;
        if (pi == null) {
            return null;
        }
        if (pi.getId() == null || pi.getId() == 0) {
            return null;
        }
        sql = "Select r from PatientReport r where r.retired=false and r.patientInvestigation.id = " + pi.getId();
        r = getPrFacade().findFirstBySQL(sql);
        if (r == null) {
            r = new PatientReport();
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setItem(pi.getInvestigation());
            r.setPatientInvestigation(pi);
            getPrFacade().create(r);
        }
        return r;
    }

    public PatientReport newPatientReportFromPatientIx(PatientInvestigation pi, Investigation ix) {
        PatientReport r;
        if (pi == null) {
            return null;
        }
        if (pi.getId() == null || pi.getId() == 0) {
            return null;
        }
        r = new PatientReport();
        r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        r.setItem(ix);
        r.setPatientInvestigation(pi);
        getPrFacade().create(r);
        return r;
    }

    public List<InvestigationItem> getInvestigationItemsForInvestigation(Investigation ix) {
        List<InvestigationItem> ii;
        if (ix == null || ix.getId() == null) {
            ii = null;
        } else {
            String sql;
            sql = "select ii from InvestigationItem ii where ii.retired = false and ii.item.id = " + ix.getId() + " order by ii.cssTop, ii.cssLeft ";
            ii = getIxItemFacade().findBySQL(sql);
        }
        if (ii == null) {
            ii = new ArrayList<InvestigationItem>();
        }
        return ii;
    }

//    Label,
//    Value,
//    Calculation,
//    Flag,
//    List,
    public List<InvestigationItem> getInvestigationItemsOfValueTypeForInvestigation(Investigation ix) {
        List<InvestigationItem> ii;
        if (ix == null || ix.getId() == null) {
            ii = null;
        } else {
            String sql;
            sql = "select ii from InvestigationItem ii where ii.retired = false and ii.ixItemType = com.divudi.data.InvestigationItemType.Value and ii.item.id = " + ix.getId() + " order by ii.cssTop, ii.cssLeft ";
            ////System.out.println(sql);
            ii = getIxItemFacade().findBySQL(sql);
            ////System.out.println("ii is " + ii + " and the cou");
        }
        if (ii == null) {
            ii = new ArrayList<InvestigationItem>();
        }
        return ii;
    }

    public Double getDefaultDoubleValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return 0.0;
    }

    public String getDefaultVarcharValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return "";
    }

    public String getDefaultMemoValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return "";
    }

    public byte[] getDefaultImageValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return null;
    }

    public void addPatientReportItemValuesForReport(PatientReport ptReport) {
        String sql = "";
        //System.out.println("going to add patient report item values for report");
        Investigation temIx = (Investigation) ptReport.getItem();
        //System.out.println("Items getting for ix is - " + temIx.getName());
        for (ReportItem ii : temIx.getReportItems()) {
            //System.out.println("report items is " + ii.getName());
            PatientReportItemValue val = null;
            if ((ii.getIxItemType() == InvestigationItemType.Value || ii.getIxItemType() == InvestigationItemType.Calculation || ii.getIxItemType() == InvestigationItemType.Flag) && ii.isRetired() == false) {
                if (ptReport.getId() == null || ptReport.getId() == 0) {

                    val = new PatientReportItemValue();
                    if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                        val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                        val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
                        val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
                        val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else {
                    }
                    val.setInvestigationItem((InvestigationItem) ii);
                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                    val.setPatientReport(ptReport);
                    // ptReport.getPatientReportItemValues().add(val);
                    //System.out.println("New value added to pr teport" + ptReport);

                } else {
                    sql = "select i from PatientReportItemValue i where i.patientReport=:ptRp"
                            + " and i.investigationItem=:inv ";
                    HashMap hm = new HashMap();
                    hm.put("ptRp", ptReport);
                    hm.put("inv", ii);
                    val = getPtRivFacade().findFirstBySQL(sql, hm);
                    //System.out.println("val is " + val);
                    if (val == null) {
                        //System.out.println("val is null");
                        val = new PatientReportItemValue();
                        if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                            val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                            val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
                            val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
                            val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else {
                        }
                        val.setInvestigationItem((InvestigationItem) ii);
                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                        val.setPatientReport(ptReport);
                        //ptReport.getPatientReportItemValues().add(val);
                        //System.out.println("value added to pr teport" + ptReport);

                    }

                }
            } else if (ii.getIxItemType() == InvestigationItemType.DynamicLabel && ii.isRetired() == false) {
                if (ptReport.getId() == null || ptReport.getId() == 0) {

                    val = new PatientReportItemValue();
                    val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    val.setInvestigationItem((InvestigationItem) ii);
                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                    val.setPatientReport(ptReport);
                    // ptReport.getPatientReportItemValues().add(val);
                    //System.out.println("New value added to pr teport" + ptReport);

                } else {
                    sql = "select i from PatientReportItemValue i where i.patientReport.id = " + ptReport.getId() + " and i.investigationItem.id = " + ii.getId() + " and i.investigationItem.ixItemType = com.divudi.data.InvestigationItemType.Value";
                    val = getPtRivFacade().findFirstBySQL(sql);
                    //System.out.println("val is " + val);
                    if (val == null) {
                        //System.out.println("val is null");
                        val = new PatientReportItemValue();
                        val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        val.setInvestigationItem((InvestigationItem) ii);
                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                        val.setPatientReport(ptReport);
                        // ptReport.getPatientReportItemValues().add(val);
                        //System.out.println("value added to pr teport" + ptReport);

                    }

                }
            }

            if (val != null) {

                getPtRivFacade().create(val);

                ptReport.getPatientReportItemValues().add(val);
                System.err.println("sss: " + val);
            }
        }
        System.err.println("items :" + ptReport.getPatientReportItemValues());
    }

    public void addMicrobiologyReportItemValuesForReport(PatientReport ptReport) {
        String sql = "";
//        //System.out.println("going to add microbiology report item values for report");
        Investigation temIx = (Investigation) ptReport.getItem();
//        //System.out.println("Items getting for ix is - " + temIx.getName());
        for (ReportItem ii : temIx.getReportItems()) {
//            //System.out.println("report items is " + ii.getName());
            if (ii.isRetired()) {
//                //System.out.println("retired = " + ii.isRetired());
                continue;
            }
            PatientReportItemValue val = null;
            if ((ii.getIxItemType() == InvestigationItemType.Value || ii.getIxItemType() == InvestigationItemType.DynamicLabel)) {
                if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
//                    System.err.println("1");
                    sql = "select i from PatientReportItemValue i where i.patientReport=:ptRp"
                            + " and i.investigationItem=:inv ";
                    HashMap hm = new HashMap();
//                    ReportItem r = new ReportItem();
//                    r.isRetired()
                    hm.put("ptRp", ptReport);
                    hm.put("inv", ii);
                    val = getPtRivFacade().findFirstBySQL(sql, hm);
//                    //System.out.println("val is " + val);
                    if (val == null) {
                        val = new PatientReportItemValue();
                        val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        val.setInvestigationItem((InvestigationItem) ii);
                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                        val.setPatientReport(ptReport);

                        //added by safrin
                        getPtRivFacade().create(val);
                        ptReport.getPatientReportItemValues().add(val);

//                        //System.out.println("value added to pr teport" + ptReport);
                    }

                }
            }

        }

        //Add Antibiotics
        List<Antibiotic> abs = getAntibioticFacade().findBySQL("select a from Antibiotic a where a.retired=false order by a.name");

        for (Antibiotic a : abs) {
            System.err.println("*** " + a.getName());
            InvestigationItem ii = investigationItemForAntibiotic(a, ptReport.getPatientInvestigation().getInvestigation());
            PatientReportItemValue val;
            sql = "select i from PatientReportItemValue i where i.patientReport=:ptRp"
                    + " and i.investigationItem=:inv";
            HashMap hm = new HashMap();
            hm.put("ptRp", ptReport);
            hm.put("inv", ii);

            val = getPtRivFacade().findFirstBySQL(sql, hm);
            System.err.println("ID " + val);
            if (val == null) {
                val = new PatientReportItemValue();
                val.setStrValue("");
                val.setInvestigationItem((InvestigationItem) ii);
                val.setPatient(ptReport.getPatientInvestigation().getPatient());
                val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                System.err.println("Repor " + ptReport);
                val.setPatientReport(ptReport);

                //Added by Safrin
                getPtRivFacade().create(val);
                ptReport.getPatientReportItemValues().add(val);
            }

        }
        //System.err.println("items :" + ptReport.getPatientReportItemValues());

    }

    @EJB
    AntibioticFacade antibioticFacade;
    @EJB
    InvestigationItemFacade iiFacade;

    public InvestigationItem investigationItemForAntibiotic(Antibiotic a, Investigation i) {
        Map m = new HashMap();
        String sql;
        sql = "select ii from InvestigationItem ii where ii.item=:i and ii.name=:a and ii.retired=false and ii.ixItemType=:iit and ii.ixItemValueType=:iivt";
        m.put("i", i);
        m.put("a", a.getName());
        m.put("iit", InvestigationItemType.Value);
        m.put("iivt", InvestigationItemValueType.Varchar);
        InvestigationItem ii = getIiFacade().findFirstBySQL(sql, m);
        if (ii == null) {
            //System.out.println("ii is null");
            ii = new InvestigationItem();
            ii.setName(a.getName());
            ii.setItem(i);
            ii.setIxItemType(InvestigationItemType.Value);
            ii.setIxItemValueType(InvestigationItemValueType.Varchar);
            ii.setCssTop("90%");
            getIiFacade().create(ii);
            i.getReportItems().add(ii);
            getIxFacade().edit(i);
        } else {
            //System.out.println("ii was found and it is " + ii.getItem().getName() + " and " + ii.getName());
        }
        return ii;
    }

    @EJB
    InvestigationFacade ixFacade;

    public InvestigationFacade getIxFacade() {
        return ixFacade;
    }

    public void setIxFacade(InvestigationFacade ixFacade) {
        this.ixFacade = ixFacade;
    }

    public AntibioticFacade getAntibioticFacade() {
        return antibioticFacade;
    }

    public void setAntibioticFacade(AntibioticFacade antibioticFacade) {
        this.antibioticFacade = antibioticFacade;
    }

    @EJB
    CommonFunctions commonFunctions;

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }
    @EJB
    InvestigationItemValueFlagFacade iivfFacade;

    public InvestigationItemValueFlagFacade getIivfFacade() {
        return iivfFacade;
    }

    public void setIivfFacade(InvestigationItemValueFlagFacade iivfFacade) {
        this.iivfFacade = iivfFacade;
    }

    public String getPatientDynamicLabel(InvestigationItem ii, Patient p) {
        String dl;
        String sql;
        dl = ii.getName();
        long ageInDays = commonFunctions.calculateAgeInDays(p.getPerson().getDob(), Calendar.getInstance().getTime());
        sql = "select f from InvestigationItemValueFlag f where  f.fromAge < " + ageInDays + " and f.toAge > " + ageInDays + " and f.investigationItemOfLabelType.id = " + ii.getId();
        List<InvestigationItemValueFlag> fs = getIivfFacade().findBySQL(sql);
        for (InvestigationItemValueFlag f : fs) {
            if (f.getSex() == p.getPerson().getSex()) {
                dl = f.getFlagMessage();
            }
        }
        return dl;
    }

    public InvestigationItemFacade getIxItemFacade() {
        return ixItemFacade;
    }

    public void setIxItemFacade(InvestigationItemFacade ixItemFacade) {
        this.ixItemFacade = ixItemFacade;
    }

    public PatientReportItemValueFacade getPtRivFacade() {
        return ptRivFacade;
    }

    public void setPtRivFacade(PatientReportItemValueFacade ptRivFacade) {
        this.ptRivFacade = ptRivFacade;
    }

    public PatientReportFacade getPrFacade() {
        return prFacade;
    }

    public void setPrFacade(PatientReportFacade prFacade) {
        this.prFacade = prFacade;
    }

    public InvestigationItemFacade getIiFacade() {
        return iiFacade;
    }

    public void setIiFacade(InvestigationItemFacade iiFacade) {
        this.iiFacade = iiFacade;
    }

}
