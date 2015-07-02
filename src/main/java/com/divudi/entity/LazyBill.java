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

public class LazyBill extends LazyDataModel<Bill> {

    private final List<Bill> datasource;

    public LazyBill(List<Bill> datasource) {
        this.datasource = datasource;
    }

    @Override
    public Bill getRowData(String rowKey) {
        for (Bill bill : datasource) {
            if (bill.getId().equals(rowKey)) {
                //System.err.println("Gett Bills"+bill);
                return bill;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Bill bill) {
        //System.err.println("GEt Row Key"+bill);
        return bill.getId();
    }

//    @Override
//    public List<Bill> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
//        List<Bill> data = new ArrayList<>();
//
//        //filter  
//        for (Bill bill : datasource) {
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
////                    ////System.out.println("Property " + property);
////                    ////System.out.println("String Value " + stringValue);
////                    ////System.out.println("Arr Size: " + arr.length);
//                    if (arr.length == 1) {
//                        //       //System.err.println("1 :" + bill.getClass().getSuperclass().getDeclaredField(property).get(bill));
//                        if (bill.getClass().getSuperclass() != null && bill.getClass().getSuperclass() != Object.class) {
//                            fieldValue = String.valueOf(bill.getClass().getSuperclass().getDeclaredField(property).get(bill));
//                        } else {
//                            fieldValue = String.valueOf(bill.getClass().getDeclaredField(property).get(bill));
//                        }
//                    } else if (arr.length == 2) {
//                        Object f;
//                        if (bill.getClass().getSuperclass() != null && bill.getClass().getSuperclass() != Object.class) {
//                            f = bill.getClass().getSuperclass().getDeclaredField(arr[0]).get(bill);
//                        } else {
//                            f = bill.getClass().getDeclaredField(arr[0]).get(bill);
//                        }
//                        //  //System.err.println("2 :" + f.getClass().getDeclaredField(arr[1]).get(f));
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            fieldValue = String.valueOf(f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f));
//                        } else {
//                            fieldValue = String.valueOf(f.getClass().getDeclaredField(arr[1]).get(f));
//                        }
//
//                    } else if (arr.length == 3) {
//                        Object f;
//                        if (bill.getClass().getSuperclass() != null && bill.getClass().getSuperclass() != Object.class) {
//                            f = bill.getClass().getSuperclass().getDeclaredField(arr[0]).get(bill);
//                        } else {
//                            f = bill.getClass().getDeclaredField(arr[0]).get(bill);
//                        }
//                        
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//                     
//                        //   //System.err.println("f2 :" + f2);
//                        fieldValue = String.valueOf(f2.getClass().getDeclaredField(arr[2]).get(f2));
//
//                    } else if (arr.length == 4) {
//                         Object f;
//                        if (bill.getClass().getSuperclass() != null && bill.getClass().getSuperclass() != Object.class) {
//                            f = bill.getClass().getSuperclass().getDeclaredField(arr[0]).get(bill);
//                        } else {
//                            f = bill.getClass().getDeclaredField(arr[0]).get(bill);
//                        }
//                        
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//                        
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        //  //System.err.println("4 :" + f3.getClass().getDeclaredField(arr[3]).get(f3));
//                        fieldValue = String.valueOf(f3.getClass().getDeclaredField(arr[3]).get(f3));
//                    } else if (arr.length == 5) {
//                       Object f;
//                        if (bill.getClass().getSuperclass() != null && bill.getClass().getSuperclass() != Object.class) {
//                            f = bill.getClass().getSuperclass().getDeclaredField(arr[0]).get(bill);
//                        } else {
//                            f = bill.getClass().getDeclaredField(arr[0]).get(bill);
//                        }
//                        
//                        Object f2;
//                        if (f.getClass().getSuperclass() != null && f.getClass().getSuperclass() != Object.class) {
//                            f2 = f.getClass().getSuperclass().getDeclaredField(arr[1]).get(f);
//                        } else {
//                            f2 = f.getClass().getDeclaredField(arr[1]).get(f);
//                        }
//                        Object f3 = f2.getClass().getDeclaredField(arr[2]).get(f2);
//                        Object f4 = f3.getClass().getDeclaredField(arr[3]).get(f3);
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
//                data.add(bill);
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
//    public List<Bill> load(){
//    
//    }
}
