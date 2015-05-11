/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.Times;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffShiftFacade;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class AttendanceUploadController implements Serializable {

    /**
     *
     * EJBs
     *
     */
    @EJB
    StaffFacade staffFacade;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    HumanResourceBean humanResourceBean;
    /**
     * CDIs
     *
     */
    @Inject
    SessionController sessionController;

    /**
     *
     * Uploading File
     *
     */
    private UploadedFile file;
    String uploadedText;
    List<FingerPrintRecord> collectedRecords;

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    /**
     * Creates a new instance of DemographyExcelManager
     */
    public AttendanceUploadController() {

    }

    public FingerPrintRecordFacade getFingerPrintRecordFacade() {
        return fingerPrintRecordFacade;
    }

    public void setFingerPrintRecordFacade(FingerPrintRecordFacade fingerPrintRecordFacade) {
        this.fingerPrintRecordFacade = fingerPrintRecordFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public List<FingerPrintRecord> getCollectedRecords() {

        return collectedRecords;
    }

    public void setCollectedRecords(List<FingerPrintRecord> collectedRecords) {
        this.collectedRecords = collectedRecords;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public String getUploadedText() {
        return uploadedText;
    }

    public void setUploadedText(String uploadedText) {
        this.uploadedText = uploadedText;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

//    public void uploadAttendance() {
//        try {
//            InputStream in;
//            in = file.getInputstream();
//            File f;
//            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
//            FileOutputStream out;
//            out = new FileOutputStream(f);
//            int read;
//            byte[] bytes = new byte[1024];
//            while ((read = in.read(bytes)) != -1) {
//                out.write(bytes, 0, read);
//            }
//            in.close();
//            out.flush();
//            out.close();
//            BufferedReader reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
//            String line;
//            collectedRecords = new ArrayList<>();
//            while ((line = reader.readLine()) != null) {
//
//                line = line.trim();
//
//                String[] strings = line.split("\\s");
//
////                if (strings.length < 3) {
////                    continue;
////                }
//             //   //System.out.println("strings0 = " + strings[0]);
//             //   //System.out.println("strings1 = " + strings[1]);
//             //   //System.out.println("strings2 = " + strings[2]);
//
//                String strCat = strings[0];
//
//                Map m = new HashMap();
//                m.put("d", strCat);
//                String sql = "select s from Staff s "
//                        + " where s.retired=false"
//                        + " and s.code=:d";
//                Staff s = getStaffFacade().findFirstBySQL(sql, m);
//
//                String str = strings[1] + " " + strings[2];
//
//                ////System.out.println("date in string is " + str);
//                if (s != null && !str.trim().equals("")) {
//                    try {
//                        FingerPrintRecord ffr = new FingerPrintRecord();
//                        ffr.setCreatedAt(Calendar.getInstance().getTime());
//                        ffr.setCreater(getSessionController().getLoggedUser());
//                        ffr.setFingerPrintRecordType(FingerPrintRecordType.Logged);
//                        ffr.setStaff(s);
//
//                        Date date;
//                        DateFormat formatter;
//                        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//                        //   formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//                     //   System.err.println("str :" + str);
//
//                        try {
//                            System.err.println(" 1 str = " + str);
//                            date = formatter.parse(str);
//                        } catch (ParseException e) {
//                            System.err.println("error in parsing");
//                            continue;
//                        }
//                        ffr.setRecordTimeStamp(date);
//                        String jpql = "select r from FingerPrintRecord r "
//                                + " where r.retired=false "
//                                + " and r.fingerPrintRecordType=:p "
//                                + " and r.staff=:s "
//                                + " and r.recordTimeStamp=:t";
//                        m = new HashMap();
//                        m.put("s", s);
//                        m.put("t", date);
//                        m.put("p", FingerPrintRecordType.Logged);
//                        FingerPrintRecord rffr = getFingerPrintRecordFacade().findFirstBySQL(jpql, m, TemporalType.TIMESTAMP);
//                        System.err.println(" 2 rffr = " + rffr);
//                        if (rffr == null) {
//                            getFingerPrintRecordFacade().create(ffr);
//                            System.err.println(" 3 rfr = " + ffr);
//                            FingerPrintRecord ffrv = new FingerPrintRecord();
//                            ffrv.setCreatedAt(Calendar.getInstance().getTime());
//                            ffrv.setCreater(getSessionController().getLoggedUser());
//                            ffrv.setFingerPrintRecordType(FingerPrintRecordType.Varified);
//                            ffrv.setStaff(s);
//                            ffrv.setRecordTimeStamp(date);
//                            ffrv.setLoggedRecord(ffr);
//                            //////////////////////////
//                            ffrv.setStaffShift(getHumanResourceBean().fetchStaffShift(ffrv));
//                            ///////////////////////////
//                            getFingerPrintRecordFacade().create(ffrv);
//
//                            ffr.setVerifiedRecord(ffrv);
//                            getFingerPrintRecordFacade().edit(ffr);
//                            collectedRecords.add(ffrv);
//                            System.err.println("4 ffrv");
//                        } else {
//                            // rffr.setStaffShift(searchStaffShift(rffr));
//                            rffr.getVerifiedRecord().setStaffShift(getHumanResourceBean().fetchStaffShift(rffr.getVerifiedRecord()));
//                            getFingerPrintRecordFacade().edit(rffr.getVerifiedRecord());
//
//                            collectedRecords.add(rffr.getVerifiedRecord());
//                        }
//                    } catch (Exception ex) {
//                        Logger.getLogger(AttendanceUploadController.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                }
//
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(AttendanceUploadController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
    public void uploadAttendance() {
        try {
            InputStream in;
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out;
            out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            BufferedReader reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
            String line;
            collectedRecords = new ArrayList<>();
            while ((line = reader.readLine()) != null) {

                line = line.trim();
                String[] strings = line.split("\\s");

                //Fetch Staff From Employee Code
                String empCode = strings[0];
                Staff staff = getHumanResourceBean().fetchStaff(empCode);

                //SETTING TIME STAMP
                String str = strings[1] + " " + strings[2];
                Date date;
                DateFormat formatter;
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    // System.err.println(" 1 str = " + str);
                    date = formatter.parse(str);
                } catch (ParseException e) {
                    System.err.println("error in parsing");
                    continue;
                }

                if (staff != null && !str.trim().equals("")) {

                    FingerPrintRecord fingerPrintRecordLogged = getHumanResourceBean().createFingerPrintRecordLogged(staff, date, getSessionController().getLoggedUser());

                    FingerPrintRecord fingerPrintRecordVarified = null;

                    fingerPrintRecordVarified = getHumanResourceBean().createFingerPrintRecordVarified(getSessionController().getLoggedUser(), fingerPrintRecordLogged);
                    fingerPrintRecordLogged.setVerifiedRecord(fingerPrintRecordVarified);
                    getFingerPrintRecordFacade().edit(fingerPrintRecordLogged);

                    collectedRecords.add(fingerPrintRecordVarified);
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(AttendanceUploadController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void uploadAttendanceAsExcel() {
        try {
            File inputWorkbook;
            Workbook w;
            Cell cell;
            InputStream in;
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out;
            out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            inputWorkbook = new File(f.getAbsolutePath());
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);
            collectedRecords = new ArrayList<>();
            for (int i = 1; i < sheet.getRows(); i++) {
                cell = sheet.getCell(0, i);
                String strCat = cell.getContents();

                Map m = new HashMap();
                m.put("d", strCat);
                String sql = "select s from Staff s where s.retired=false and s.acNo=:d";
                Staff s = getStaffFacade().findFirstBySQL(sql, m);
                cell = sheet.getCell(2, i);
                String str = cell.getContents();

                if (s != null && !str.equals("")) {
//                    //System.err.println("AcNo : " + strCat);
//                    //System.err.println("Staff : " + s);
                    FingerPrintRecord ffr = new FingerPrintRecord();
                    ffr.setCreatedAt(Calendar.getInstance().getTime());
                    ffr.setCreater(getSessionController().getLoggedUser());
                    ffr.setFingerPrintRecordType(FingerPrintRecordType.Logged);
                    ffr.setStaff(s);

                    Date date;
                    DateFormat formatter = new SimpleDateFormat("MM/d/yy HH:mm");
                    //  //System.err.println("str :" + str);
                    date = formatter.parse(str);
                    ffr.setRecordTimeStamp(date);
                    String jpql = "select r from FingerPrintRecord r where r.retired=false and "
                            + " r.fingerPrintRecordType=:p and r.staff=:s and r.recordTimeStamp=:t";
                    m = new HashMap();
                    m.put("s", s);
                    m.put("t", date);
                    m.put("p", FingerPrintRecordType.Logged);
                    FingerPrintRecord rffr = getFingerPrintRecordFacade().findFirstBySQL(jpql, m, TemporalType.TIMESTAMP);
                    if (rffr == null) {
                        getFingerPrintRecordFacade().create(ffr);

                        FingerPrintRecord ffrv = new FingerPrintRecord();
                        ffrv.setCreatedAt(Calendar.getInstance().getTime());
                        ffrv.setCreater(getSessionController().getLoggedUser());
                        ffrv.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                        ffrv.setStaff(s);
                        ffrv.setRecordTimeStamp(date);
                        ffrv.setLoggedRecord(ffr);
                        //////////////////////////
                        ffrv.setStaffShift(getHumanResourceBean().fetchStaffShift(ffrv));
                        ///////////////////////////
                        getFingerPrintRecordFacade().create(ffrv);

                        ffr.setVerifiedRecord(ffrv);
                        getFingerPrintRecordFacade().edit(ffr);
                        collectedRecords.add(ffrv);

                    } else {
                        // rffr.setStaffShift(searchStaffShift(rffr));
                        rffr.getVerifiedRecord().setStaffShift(getHumanResourceBean().fetchStaffShift(rffr.getVerifiedRecord()));
                        getFingerPrintRecordFacade().edit(rffr.getVerifiedRecord());

                        collectedRecords.add(rffr.getVerifiedRecord());

                    }

                }

            }
        } catch (IOException | BiffException | ParseException ex) {
            Logger.getLogger(AttendanceUploadController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Times[] getTimes() {
        return Times.values();
    }

    @EJB
    StaffShiftFacade staffShiftFacade;

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

//    public void onEdit(RowEditEvent event) {
//        FingerPrintRecord tmp = (FingerPrintRecord) event.getObject();
//
//        //System.err.println("1: " + tmp.getTimes());
//        //System.err.println("2: " + tmp.getFingerPrintRecordType());
//        //System.err.println("3: " + tmp.getStaff().getAcNo());
//        //System.err.println("4: " + tmp.getRecordTimeStamp());
//        //System.err.println("5: " + tmp.getStaffShift());
//
//        if (tmp.getTimes() == Times.inTime) {
//            tmp.getStaffShift().setStartRecord(tmp);
//        } else if (tmp.getTimes() == Times.outTime) {
//            tmp.getStaffShift().setEndRecord(tmp);
//        }
//
//        if (tmp.getStaffShift() != null && tmp.getStaffShift().getId() != null) {
//            getStaffShiftFacade().edit(tmp.getStaffShift());
//            //System.err.println("1: ");
//        }
//
//        getFingerPrintRecordFacade().edit(tmp);
//
//    }
    public void onEdit(FingerPrintRecord tmp) {

        //System.err.println("1: " + tmp.getTimes());
        //System.err.println("2: " + tmp.getFingerPrintRecordType());
        //System.err.println("3: " + tmp.getStaff().getAcNo());
        //System.err.println("4: " + tmp.getRecordTimeStamp());
        //System.err.println("5: " + tmp.getStaffShift());
        if (tmp.getTimes() == Times.inTime) {
            tmp.getStaffShift().setStartRecord(tmp);
        } else if (tmp.getTimes() == Times.outTime) {
            tmp.getStaffShift().setEndRecord(tmp);
        }

        if (tmp.getStaffShift() != null && tmp.getStaffShift().getId() != null) {
            getStaffShiftFacade().edit(tmp.getStaffShift());
            //System.err.println("1: ");
        }

        getFingerPrintRecordFacade().edit(tmp);

    }

    private List<StaffShift> staffShiftList(FingerPrintRecord r) {
        Date recordedDate = r.getRecordTimeStamp();

        Map m = new HashMap();
        m.put("s", r.getStaff());
        m.put("d", recordedDate);
        String sql = "Select ss from StaffShift ss where ss.retired=false and ss.staff=:s and "
                + "  ss.shiftDate=:d";
        List<StaffShift> ss = getStaffShiftFacade().findBySQL(sql, m, TemporalType.DATE);
        if (!ss.isEmpty()) {
            //System.err.println("exist : " + ss.get(0).getStaff().getPerson().getName());
            return ss;
        } else {
            return new ArrayList<>();
        }

    }

}
