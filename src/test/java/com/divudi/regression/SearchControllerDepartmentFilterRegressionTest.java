package com.divudi.regression;

import com.divudi.bean.common.SearchController;
import com.divudi.data.SearchKeyWord;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.ejb.BillFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.persistence.TemporalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for SearchController department filtering functionality.
 * Ensures that JPQL parameter names and values are correctly matched and that
 * fromDepartment and toDepartment filters work independently and together.
 * 
 * @author Dr M H B Ariyaratne
 * @since Department Filter Parameter Fix
 */
@DisplayName("SearchController Department Filter Regression Tests")
public class SearchControllerDepartmentFilterRegressionTest {
    
    private SearchController searchController;
    
    @Mock
    private BillFacade billFacade;
    
    @Mock
    private SearchKeyWord searchKeyword;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        searchController = new SearchController();
        searchController.setBillFacade(billFacade);
        searchController.setSearchKeyword(searchKeyword);
        
        // Set up default date range
        when(searchKeyword.getFromDate()).thenReturn(new Date());
        when(searchKeyword.getToDate()).thenReturn(new Date());
    }
    
    @Nested
    @DisplayName("FromDepartment Filter Tests")
    class FromDepartmentFilterTest {
        
        @Test
        @DisplayName("FromDepartment filter should use correct parameter name and value")
        public void testFromDepartmentFilter_ShouldUseCorrectParameterNameAndValue() {
            // Setup
            when(searchKeyword.getFromDepartment()).thenReturn("Emergency");
            when(searchKeyword.getBillNo()).thenReturn(null);
            when(searchKeyword.getStaffName()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute - This should trigger the method with fromDepartment filter
            searchController.createIssueReport();
            
            // Verify the JPQL contains correct parameter name (:fDep) and the parameter map contains correct value
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains("(upper(b.fromDepartment.name) like :fDep )")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.containsKey("fDep") && 
                           paramMap.get("fDep").equals("%EMERGENCY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
        
        @Test
        @DisplayName("FromDepartment filter should work with empty other filters")
        public void testFromDepartmentFilter_ShouldWorkWithEmptyOtherFilters() {
            // Setup - only fromDepartment is set
            when(searchKeyword.getFromDepartment()).thenReturn("Surgery");
            when(searchKeyword.getBillNo()).thenReturn("");
            when(searchKeyword.getStaffName()).thenReturn("");
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createIssueReport();
            
            // Verify only fromDepartment filter is applied
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains(":fDep") && !sql.contains(":billNo") && !sql.contains(":stf")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.size() == 3 && // fromDate, toDate, fDep
                           paramMap.containsKey("fDep") &&
                           paramMap.get("fDep").equals("%SURGERY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
    }
    
    @Nested
    @DisplayName("ToDepartment Filter Tests") 
    class ToDepartmentFilterTest {
        
        @Test
        @DisplayName("ToDepartment filter should use correct parameter name and value")
        public void testToDepartmentFilter_ShouldUseCorrectParameterNameAndValue() {
            // Setup
            when(searchKeyword.getToDepartment()).thenReturn("Pharmacy");
            when(searchKeyword.getFromDepartment()).thenReturn(null);
            when(searchKeyword.getDepartment()).thenReturn(null);
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute - This should trigger a method with toDepartment filter
            searchController.createGrnReport();
            
            // Verify the JPQL contains correct parameter name (:toDept) and the parameter map contains correct value
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains("((b.toDepartment.name) like :toDept )")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.containsKey("toDept") && 
                           paramMap.get("toDept").equals("%PHARMACY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
        
        @Test
        @DisplayName("Department filter should use different parameter name than toDepartment")
        public void testDepartmentFilter_ShouldUseDifferentParameterNameThanToDepartment() {
            // Setup - both toDepartment and department are set
            when(searchKeyword.getToDepartment()).thenReturn("Pharmacy");
            when(searchKeyword.getDepartment()).thenReturn("Laboratory");
            when(searchKeyword.getFromDepartment()).thenReturn(null);
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify both filters use different parameter names
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains(":toDept") && sql.contains(":dept")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.containsKey("toDept") && 
                           paramMap.containsKey("dept") &&
                           paramMap.get("toDept").equals("%PHARMACY%") &&
                           paramMap.get("dept").equals("%LABORATORY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
    }
    
    @Nested
    @DisplayName("Combined Filter Tests")
    class CombinedFilterTest {
        
        @Test
        @DisplayName("FromDepartment and ToDepartment filters should work together (intersect)")
        public void testFromAndToDepartmentFilters_ShouldWorkTogether() {
            // Setup
            when(searchKeyword.getFromDepartment()).thenReturn("Emergency");
            when(searchKeyword.getToDepartment()).thenReturn("Pharmacy");
            when(searchKeyword.getDepartment()).thenReturn(null);
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify both filters are applied with correct parameters
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains("((b.fromDepartment.name) like :frmDept )") && 
                              sql.contains("((b.toDepartment.name) like :toDept )")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.containsKey("frmDept") && 
                           paramMap.containsKey("toDept") &&
                           paramMap.get("frmDept").equals("%EMERGENCY%") &&
                           paramMap.get("toDept").equals("%PHARMACY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
        
        @Test
        @DisplayName("All three department filters should work together without conflicts")
        public void testAllThreeDepartmentFilters_ShouldWorkTogetherWithoutConflicts() {
            // Setup
            when(searchKeyword.getFromDepartment()).thenReturn("Emergency");
            when(searchKeyword.getToDepartment()).thenReturn("Pharmacy");
            when(searchKeyword.getDepartment()).thenReturn("Laboratory");
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify all three filters use unique parameter names
            verify(billFacade).findByJpql(
                argThat(sql -> sql.contains(":frmDept") && 
                              sql.contains(":toDept") && 
                              sql.contains(":dept")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.containsKey("frmDept") && 
                           paramMap.containsKey("toDept") &&
                           paramMap.containsKey("dept") &&
                           paramMap.get("frmDept").equals("%EMERGENCY%") &&
                           paramMap.get("toDept").equals("%PHARMACY%") &&
                           paramMap.get("dept").equals("%LABORATORY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
        
        @Test
        @DisplayName("Filters should intersect results, not overwrite each other")
        public void testFilters_ShouldIntersectResultsNotOverwriteEachOther() {
            // Setup
            when(searchKeyword.getFromDepartment()).thenReturn("ICU");
            when(searchKeyword.getToDepartment()).thenReturn("Pharmacy");
            
            // Mock a scenario where separate filters would return different results
            // but combined they should return intersection
            List<Bill> mockBills = new ArrayList<>();
            Bill bill1 = new Bill();
            bill1.setId(1L);
            mockBills.add(bill1);
            
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify that the SQL query contains AND conditions (intersection)
            verify(billFacade).findByJpql(
                argThat(sql -> {
                    // Count the number of AND conditions
                    String lowerSql = sql.toLowerCase();
                    int andCount = lowerSql.split(" and ").length - 1;
                    // Should have at least one AND for each filter plus date range filters
                    return andCount >= 2 && 
                           sql.contains("frmDept") && 
                           sql.contains("toDept");
                }),
                any(HashMap.class),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
    }
    
    @Nested
    @DisplayName("Parameter Value Format Tests")
    class ParameterValueFormatTest {
        
        @Test
        @DisplayName("Department filter values should be trimmed and uppercased with wildcards")
        public void testDepartmentFilterValues_ShouldBeTrimmedAndUppercasedWithWildcards() {
            // Setup with values that need trimming and case conversion
            when(searchKeyword.getFromDepartment()).thenReturn("  emergency  ");
            when(searchKeyword.getToDepartment()).thenReturn("  Pharmacy  ");
            when(searchKeyword.getDepartment()).thenReturn("  laboratory  ");
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify parameter values are properly formatted
            verify(billFacade).findByJpql(
                anyString(),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return paramMap.get("frmDept").equals("%EMERGENCY%") &&
                           paramMap.get("toDept").equals("%PHARMACY%") &&
                           paramMap.get("dept").equals("%LABORATORY%");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
        
        @Test
        @DisplayName("Empty and null department filters should be ignored")
        public void testEmptyAndNullDepartmentFilters_ShouldBeIgnored() {
            // Setup with empty/null values
            when(searchKeyword.getFromDepartment()).thenReturn("");
            when(searchKeyword.getToDepartment()).thenReturn(null);
            when(searchKeyword.getDepartment()).thenReturn("   ");
            when(searchKeyword.getToInstitution()).thenReturn(null);
            when(searchKeyword.getRefBillNo()).thenReturn(null);
            when(searchKeyword.getNumber()).thenReturn(null);
            
            List<Bill> mockBills = new ArrayList<>();
            when(billFacade.findByJpql(anyString(), any(HashMap.class), eq(TemporalType.TIMESTAMP), eq(50)))
                .thenReturn(mockBills);
            
            // Execute
            searchController.createGrnReport();
            
            // Verify no department filter parameters are included
            verify(billFacade).findByJpql(
                argThat(sql -> !sql.contains(":frmDept") && 
                              !sql.contains(":toDept") && 
                              !sql.contains(":dept")),
                argThat(params -> {
                    HashMap<String, Object> paramMap = (HashMap<String, Object>) params;
                    return !paramMap.containsKey("frmDept") &&
                           !paramMap.containsKey("toDept") &&
                           !paramMap.containsKey("dept");
                }),
                eq(TemporalType.TIMESTAMP),
                eq(50)
            );
        }
    }
}