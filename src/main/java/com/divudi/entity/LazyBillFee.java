/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

public class LazyBillFee extends LazyDataModel<BillFee> {

    private final List<BillFee> datasource;

    public LazyBillFee(List<BillFee> datasource) {
        this.datasource = datasource;
    }

    @Override
    public BillFee getRowData(String rowKey) {
        for (BillFee billFee : datasource) {
            if (billFee.getId().equals(rowKey)) {
                return billFee;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(BillFee billFee) {
        return billFee.getId();
    }

//    @Override
//    public List<BillFee> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
//        List<BillFee> data = new ArrayList<>();
//
//        //filter  
//        for (BillFee billFee : datasource) {
//            boolean match = true;
//
//            for (Iterator<String> it = filters.keySet().iterator(); it.hasNext();) {
//                try {
//                    String property = it.next();
//                    String stringValue = filters.get(property).toLowerCase();
//                    String fieldValue = "";
//
//                    String[] arr = property.split("\\.");
//
//                    ////System.out.println("Property " + property);
//                    ////System.out.println("String Value " + stringValue);
//                    ////System.out.println("Arr Size: " + arr.length);
//                    if (arr.length == 1) {
//                        fieldValue = String.valueOf(billFee.getClass().getDeclaredField(property).get(billFee));
//                    } else if (arr.length == 2) {
//                        Object f = billFee.getClass().getDeclaredField(arr[0]).get(billFee);
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            fieldValue = String.valueOf(f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f));
//                        } else {
//                            fieldValue = String.valueOf(f.getClass().getDeclaredField(arr[1]).get(f));
//                        }
//
//                    } else if (arr.length == 3) {
//                        Object f = billFee.getClass().getDeclaredField(arr[0]).get(billFee);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//
//                        if (f2.getClass().getSuperclass() != null && f2.getClass().getSuperclass() != Object.class) {
//                            fieldValue = String.valueOf(f2.getClass().getSuperclass().getDeclaredField(arr[2]).get(f2));
//                        } else {
//                            fieldValue = String.valueOf(f2.getClass().getDeclaredField(arr[2]).get(f2));
//                        }
//
//                    } else if (arr.length == 4) {
//                        Object f = billFee.getClass().getDeclaredField(arr[0]).get(billFee);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        fieldValue = String.valueOf(f3.getClass().getDeclaredField(arr[3]).get(f3));
//                    } else if (arr.length == 5) {
//                        Object f = billFee.getClass().getDeclaredField(arr[0]).get(billFee);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        Object f4 = f3.getClass().getDeclaredField(arr[3]).get(f3);
//                        fieldValue = String.valueOf(f4.getClass().getDeclaredField(arr[4]).get(f4));
//                    }
//
//                    if (stringValue == "" || fieldValue.toLowerCase().contains(stringValue)) {
//                        match = true;
//                    } else {
//                        match = false;
//                        break;
//                    }
//                } catch (Exception e) {
//
//                    match = false;
//                }
//            }
//
//            if (match) {
//                data.add(billFee);
//            }
//        }
//
//        //   sort  
//        if (sortField != null) {
//            //  Collections.sort(data, new LazySorter(sortField, sortOrder));
//        }
//
//        //rowCount  
//        int dataSize = data.size();
//        this.setRowCount(dataSize);
//
//        //paginate  
//        if (dataSize > pageSize) {
//            try {
//                return data.subList(first, first + pageSize);
//            } catch (IndexOutOfBoundsException e) {
//                return data.subList(first, first + (dataSize % pageSize));
//            }
//        } else {
//            return data;
//        }
//    }

//    @Override
//    public List<BillFee> load(){
//    
//    }
}
