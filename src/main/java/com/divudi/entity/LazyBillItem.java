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

public class LazyBillItem extends LazyDataModel<BillItem> {

    private final List<BillItem> datasource;

    public LazyBillItem(List<BillItem> datasource) {
        this.datasource = datasource;
    }

    @Override
    public BillItem getRowData(String rowKey) {
        for (BillItem billItem : datasource) {
            if (billItem.getId().equals(rowKey)) {
                return billItem;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(BillItem billItem) {
        return billItem.getId();
    }

//    @Override
//    public List<BillItem> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
//        List<BillItem> data = new ArrayList<>();
//
//        //filter  
//        for (BillItem billItem : datasource) {
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
//                        fieldValue = String.valueOf(billItem.getClass().getDeclaredField(property).get(billItem));
//                    } else if (arr.length == 2) {
//                        Object f = billItem.getClass().getDeclaredField(arr[0]).get(billItem);
//                        //System.err.println("21");
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            //System.err.println("22");
//                            if (f.getClass().getSuperclass().getSuperclass() != null && f.getClass().getSuperclass().getSuperclass() != Object.class) {
//                                //System.err.println("23");
//                                fieldValue = String.valueOf(f.getClass().getSuperclass().getSuperclass().getDeclaredField(arr[1]).get(f));
//                            } else {
//                                //System.err.println("24");
//                                fieldValue = String.valueOf(f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f));
//                            }
//                        } else {
//                            //System.err.println("15");
//                            fieldValue = String.valueOf(f.getClass().getDeclaredField(arr[1]).get(f));
//                        }
//
//                    } else if (arr.length == 3) {
//                        Object f = billItem.getClass().getDeclaredField(arr[0]).get(billItem);
//                        //System.err.println("f " + f);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//
//                        //System.err.println("f2 :" + f2);
//                        if (f2.getClass().getSuperclass() != null && f2.getClass().getSuperclass() != Object.class) {
//                            fieldValue = String.valueOf(f2.getClass().getSuperclass().getDeclaredField(arr[2]).get(f2));
//                        } else {
//                            fieldValue = String.valueOf(f2.getClass().getDeclaredField(arr[2]).get(f2));
//                        }
//
//                    } else if (arr.length == 4) {
//                        Object f = billItem.getClass().getDeclaredField(arr[0]).get(billItem);
//                        //System.err.println("f " + f);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//                        //System.err.println("f2 " + f2);
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        //System.err.println("f3 " + f3);
//                        //  //System.err.println("4 :" + f3.getClass().getDeclaredField(arr[3]).get(f3));
//                        fieldValue = String.valueOf(f3.getClass().getDeclaredField(arr[3]).get(f3));
//                    } else if (arr.length == 5) {
//                        Object f = billItem.getClass().getDeclaredField(arr[0]).get(billItem);
//                        //System.err.println("f " + f);
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//                        //System.err.println("f2 " + f2);
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        //System.err.println("f3 " + f3);
//                        Object f4 = f3.getClass().getDeclaredField(arr[3]).get(f3);
//                        //System.err.println("f4 " + f4);
//                        //   //System.err.println("5 :" + f4.getClass().getDeclaredField(arr[4]).get(f4));
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
//                data.add(billItem);
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
//    public List<BillItem> load(){
//    
//    }
}
