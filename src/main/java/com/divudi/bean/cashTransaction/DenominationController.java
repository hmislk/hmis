/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.cashTransaction.Denomination;
import com.divudi.core.facade.DenominationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class DenominationController implements Serializable {

    @EJB
    private DenominationFacade denominationFacade;

    @Inject
    private SessionController sessionController;

    private Denomination denomination;
    private List<Denomination> denominations;

    public DenominationController() {
    }

    private List<Denomination> fetchDenominations() {
        List<Denomination> denos;
        String jpql = "select d "
                + " from Denomination d "
                + " where d.retired=:ret "
                + " order by d.orderNumber";
        Map params = new HashMap<>();
        params.put("ret", false);
        denos = denominationFacade.findByJpql(jpql, params);
        if (denos == null || denos.isEmpty()) {
            denos = createDenominations();
        }
        return denos;
    }

    private List<Denomination> createDenominations() {
        List<Denomination> denos = new ArrayList<>();

        Denomination deno1 = new Denomination();
        deno1.setName("1");
        deno1.setDisplayName("Rs. 1.00");
        deno1.setOrderNumber(1.0);
        deno1.setDenominationValue(1.0);
        denos.add(deno1);

        Denomination deno2 = new Denomination();
        deno2.setName("2");
        deno2.setDisplayName("Rs. 2.00");
        deno2.setOrderNumber(2.0);
        deno2.setDenominationValue(2.0);
        denos.add(deno2);

        Denomination deno3 = new Denomination();
        deno3.setName("5");
        deno3.setDisplayName("Rs. 5.00");
        deno3.setOrderNumber(3.0);
        deno3.setDenominationValue(5.0);
        denos.add(deno3);

        Denomination deno4 = new Denomination();
        deno4.setName("10");
        deno4.setDisplayName("Rs. 10.00");
        deno4.setOrderNumber(4.0);
        deno4.setDenominationValue(10.0);
        denos.add(deno4);

        Denomination deno5 = new Denomination();
        deno5.setName("20");
        deno5.setDisplayName("Rs. 20.00");
        deno5.setOrderNumber(5.0);
        deno5.setDenominationValue(20.0);
        denos.add(deno5);

        Denomination deno6 = new Denomination();
        deno6.setName("50");
        deno6.setDisplayName("Rs. 50.00");
        deno6.setOrderNumber(6.0);
        deno6.setDenominationValue(50.0);
        denos.add(deno6);

        Denomination deno7 = new Denomination();
        deno7.setName("100");
        deno7.setDisplayName("Rs. 100.00");
        deno7.setOrderNumber(7.0);
        deno7.setDenominationValue(100.0);
        denos.add(deno7);

        Denomination deno8 = new Denomination();
        deno8.setName("500");
        deno8.setDisplayName("Rs. 500.00");
        deno8.setOrderNumber(8.0);
        deno8.setDenominationValue(500.0);
        denos.add(deno8);

        Denomination deno9 = new Denomination();
        deno9.setName("1000");
        deno9.setDisplayName("Rs. 1000.00");
        deno9.setOrderNumber(9.0);
        deno9.setDenominationValue(1000.0);
        denos.add(deno9);

        Denomination deno10 = new Denomination();
        deno10.setName("2000");
        deno10.setDisplayName("Rs. 2000.00");
        deno10.setOrderNumber(10.0);
        deno10.setDenominationValue(2000.0);
        denos.add(deno10);

        Denomination deno11 = new Denomination();
        deno11.setName("5000");
        deno11.setDisplayName("Rs. 5000.00");
        deno11.setOrderNumber(11.0);
        deno11.setDenominationValue(5000.0);
        denos.add(deno11);

        denominationFacade.create(deno1);
        denominationFacade.create(deno2);
        denominationFacade.create(deno3);
        denominationFacade.create(deno4);
        denominationFacade.create(deno5);
        denominationFacade.create(deno6);
        denominationFacade.create(deno7);
        denominationFacade.create(deno8);
        denominationFacade.create(deno9);
        denominationFacade.create(deno10);
        denominationFacade.create(deno11);

        return denos;
    }

    public DenominationFacade getDenominationFacade() {
        return denominationFacade;
    }

    public void setDenominationFacade(DenominationFacade denominationFacade) {
        this.denominationFacade = denominationFacade;
    }

    public Denomination getDenomination() {
        return denomination;
    }

    public void setDenomination(Denomination denomination) {
        this.denomination = denomination;
    }

    public List<Denomination> getDenominations() {
        if (denominations == null) {
            denominations = fetchDenominations();
        }
        return denominations;
    }

    public void setDenominations(List<Denomination> denominations) {
        this.denominations = denominations;
    }

    @FacesConverter(forClass = Denomination.class)
    public static class DenominationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DenominationController controller = (DenominationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "denominationController");
            return controller.getDenominationFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Denomination) {
                Denomination o = (Denomination) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Denomination.class.getName());
            }
        }
    }

}
