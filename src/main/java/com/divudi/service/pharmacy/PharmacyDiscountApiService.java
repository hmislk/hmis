package com.divudi.service.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PharmacyDiscountBulkRequestDto;
import com.divudi.core.data.dto.pharmacy.PharmacyDiscountRequestDto;
import com.divudi.core.data.dto.pharmacy.PharmacyDiscountResponseDto;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.membership.PaymentSchemeDiscount;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.PaymentSchemeDiscountFacade;
import com.divudi.core.facade.PaymentSchemeFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class PharmacyDiscountApiService {

    @EJB
    private PaymentSchemeDiscountFacade discountFacade;

    @EJB
    private CategoryFacade categoryFacade;

    @EJB
    private PaymentSchemeFacade paymentSchemeFacade;

    public List<PharmacyDiscountResponseDto> list(Long paymentSchemeId, String paymentSchemeName,
                                                   String billTypeStr, int limit) {
        StringBuilder jpql = new StringBuilder(
                "select d from PaymentSchemeDiscount d where d.retired = false");
        Map<String, Object> params = new HashMap<>();

        if (paymentSchemeId != null) {
            jpql.append(" and d.paymentScheme.id = :psid");
            params.put("psid", paymentSchemeId);
        } else if (paymentSchemeName != null && !paymentSchemeName.trim().isEmpty()) {
            jpql.append(" and upper(d.paymentScheme.name) like :psname");
            params.put("psname", "%" + paymentSchemeName.trim().toUpperCase() + "%");
        }

        if (billTypeStr != null && !billTypeStr.trim().isEmpty()) {
            BillType bt = parseBillType(billTypeStr);
            if (bt != null) {
                jpql.append(" and d.billType = :bt");
                params.put("bt", bt);
            }
        }

        jpql.append(" order by d.paymentScheme.name, d.category.name");

        List<PaymentSchemeDiscount> rows = discountFacade.findByJpql(jpql.toString(), params, limit);
        List<PharmacyDiscountResponseDto> result = new ArrayList<>();
        if (rows != null) {
            for (PaymentSchemeDiscount d : rows) {
                result.add(toDto(d));
            }
        }
        return result;
    }

    public PharmacyDiscountResponseDto create(PharmacyDiscountRequestDto request, WebUser user)
            throws Exception {
        if (request.getDiscountPercent() == null) {
            throw new Exception("discountPercent is required");
        }

        PaymentScheme scheme = resolveScheme(request.getPaymentSchemeId(), request.getPaymentSchemeName());
        if (scheme == null) {
            throw new Exception("paymentScheme not found — supply paymentSchemeId or paymentSchemeName");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryFacade.find(request.getCategoryId());
            if (category == null) {
                throw new Exception("Category not found: " + request.getCategoryId());
            }
        }

        BillType billType = request.getBillType() != null ? parseBillType(request.getBillType()) : BillType.PharmacySale;
        if (billType == null) { billType = BillType.PharmacySale; }

        PaymentMethod pm = request.getPaymentMethod() != null ? parsePaymentMethod(request.getPaymentMethod()) : null;

        PaymentSchemeDiscount d = new PaymentSchemeDiscount();
        d.setPaymentScheme(scheme);
        d.setCategory(category);
        d.setBillType(billType);
        d.setPaymentMethod(pm);
        d.setDiscountPercent(request.getDiscountPercent());
        d.setCreater(user);
        d.setCreatedAt(new Date());
        d.setRetired(false);
        discountFacade.create(d);
        return toDto(d);
    }

    public Map<String, Object> bulk(PharmacyDiscountBulkRequestDto request, WebUser user)
            throws Exception {
        if (request.getDiscountPercent() == null) {
            throw new Exception("discountPercent is required");
        }

        PaymentScheme scheme = resolveScheme(request.getPaymentSchemeId(), request.getPaymentSchemeName());
        if (scheme == null) {
            throw new Exception("paymentScheme not found — supply paymentSchemeId or paymentSchemeName");
        }

        BillType billType = request.getBillType() != null ? parseBillType(request.getBillType()) : BillType.PharmacySale;
        if (billType == null) { billType = BillType.PharmacySale; }

        PaymentMethod pm = request.getPaymentMethod() != null ? parsePaymentMethod(request.getPaymentMethod()) : null;

        Map<String, Object> catParams = new HashMap<>();
        catParams.put("t", PharmaceuticalItemCategory.class);
        List<Category> categories = categoryFacade.findByJpql(
                "select c from Category c where c.retired = false and type(c) = :t order by c.name",
                catParams);

        int created = 0;
        int updated = 0;

        if (categories != null) {
            for (Category cat : categories) {
                PaymentSchemeDiscount existing = findExisting(scheme, cat, billType, pm);
                if (existing != null) {
                    existing.setDiscountPercent(request.getDiscountPercent());
                    discountFacade.edit(existing);
                    updated++;
                } else {
                    PaymentSchemeDiscount d = new PaymentSchemeDiscount();
                    d.setPaymentScheme(scheme);
                    d.setCategory(cat);
                    d.setBillType(billType);
                    d.setPaymentMethod(pm);
                    d.setDiscountPercent(request.getDiscountPercent());
                    d.setCreater(user);
                    d.setCreatedAt(new Date());
                    d.setRetired(false);
                    discountFacade.create(d);
                    created++;
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("paymentSchemeId", scheme.getId());
        summary.put("paymentSchemeName", scheme.getName());
        summary.put("billType", billType.name());
        summary.put("discountPercent", request.getDiscountPercent());
        summary.put("created", created);
        summary.put("updated", updated);
        summary.put("total", created + updated);
        return summary;
    }

    public PharmacyDiscountResponseDto update(Long id, PharmacyDiscountRequestDto request, WebUser user)
            throws Exception {
        PaymentSchemeDiscount d = discountFacade.find(id);
        if (d == null || d.isRetired()) {
            throw new Exception("Discount row not found or already retired: " + id);
        }
        if (request.getDiscountPercent() != null) {
            d.setDiscountPercent(request.getDiscountPercent());
        }
        discountFacade.edit(d);
        return toDto(d);
    }

    public void retire(Long id, WebUser user) throws Exception {
        PaymentSchemeDiscount d = discountFacade.find(id);
        if (d == null) {
            throw new Exception("Discount row not found: " + id);
        }
        if (d.isRetired()) {
            throw new Exception("Discount row already retired: " + id);
        }
        d.setRetired(true);
        d.setRetirer(user);
        d.setRetiredAt(new Date());
        discountFacade.edit(d);
    }

    private PaymentSchemeDiscount findExisting(PaymentScheme scheme, Category cat,
                                                BillType bt, PaymentMethod pm) {
        Map<String, Object> p = new HashMap<>();
        p.put("ps", scheme);
        p.put("cat", cat);
        p.put("bt", bt);

        String jpql;
        if (pm != null) {
            jpql = "select d from PaymentSchemeDiscount d"
                    + " where d.retired = false"
                    + " and d.paymentScheme = :ps"
                    + " and d.category = :cat"
                    + " and d.billType = :bt"
                    + " and d.paymentMethod = :pm";
            p.put("pm", pm);
        } else {
            jpql = "select d from PaymentSchemeDiscount d"
                    + " where d.retired = false"
                    + " and d.paymentScheme = :ps"
                    + " and d.category = :cat"
                    + " and d.billType = :bt"
                    + " and d.paymentMethod is null";
        }
        return (PaymentSchemeDiscount) discountFacade.findFirstByJpql(jpql, p);
    }

    private PaymentScheme resolveScheme(Long id, String name) {
        if (id != null) {
            return paymentSchemeFacade.find(id);
        }
        if (name != null && !name.trim().isEmpty()) {
            Map<String, Object> p = new HashMap<>();
            p.put("name", name.trim());
            List<PaymentScheme> list = paymentSchemeFacade.findByJpql(
                    "select s from PaymentScheme s where s.retired = false and s.name = :name", p, 1);
            if (list != null && !list.isEmpty()) return list.get(0);
            p.put("name", "%" + name.trim().toUpperCase() + "%");
            list = paymentSchemeFacade.findByJpql(
                    "select s from PaymentScheme s where s.retired = false and upper(s.name) like :name order by s.name", p, 1);
            if (list != null && !list.isEmpty()) return list.get(0);
        }
        return null;
    }

    private BillType parseBillType(String s) {
        if (s == null) return null;
        try { return BillType.valueOf(s.trim()); } catch (IllegalArgumentException e) { return null; }
    }

    private PaymentMethod parsePaymentMethod(String s) {
        if (s == null) return null;
        try { return PaymentMethod.valueOf(s.trim()); } catch (IllegalArgumentException e) { return null; }
    }

    private PharmacyDiscountResponseDto toDto(PaymentSchemeDiscount d) {
        PharmacyDiscountResponseDto dto = new PharmacyDiscountResponseDto();
        dto.setId(d.getId());
        dto.setDiscountPercent(d.getDiscountPercent());
        dto.setRetired(d.isRetired());
        if (d.getBillType() != null) dto.setBillType(d.getBillType().name());
        if (d.getPaymentMethod() != null) dto.setPaymentMethod(d.getPaymentMethod().name());
        if (d.getCategory() != null) {
            dto.setCategoryId(d.getCategory().getId());
            dto.setCategoryName(d.getCategory().getName());
        }
        if (d.getPaymentScheme() != null) {
            dto.setPaymentSchemeId(d.getPaymentScheme().getId());
            dto.setPaymentSchemeName(d.getPaymentScheme().getName());
        }
        return dto;
    }
}
