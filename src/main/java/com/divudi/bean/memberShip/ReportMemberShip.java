package com.divudi.bean.memberShip;

import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.data.memberShip.IpaCreditInstitution;
import com.divudi.data.PaymentMethod;
import com.divudi.data.memberShip.IpaMemberShip;
import com.divudi.data.memberShip.IpaMemberShipCreditInstitution;
import com.divudi.data.memberShip.IpaPaymentMethod;
import com.divudi.data.memberShip.OpdMemberShip;
import com.divudi.entity.Institution;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.memberShip.MembershipScheme;
import com.divudi.facade.PriceMatrixFacade;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@RequestScoped
public class ReportMemberShip {

    
    List<IpaPaymentMethod> ipaPaymentMethods;
    List<IpaCreditInstitution> ipaCreditInstitutions;
    private List<IpaMemberShip> ipaMemberShip;
    List<IpaMemberShipCreditInstitution> ipaMemberShipCreditInstitution;
    List<OpdMemberShip> opdMemberShip;
    @Inject
    EnumController enumController;
    @Inject
    InstitutionController institutionController;
    @Inject
    InwardMemberShipDiscount inwardMemberShipDiscount;
    @Inject
    MembershipSchemeController membershipSchemeController;
    @EJB
    PriceMatrixFacade inwardPriceAdjustmentFacade;
    @Inject
    PriceMatrixController priceMatrixController;

    
    
    
    public List<OpdMemberShip> getOpdMemberShip() {
        return opdMemberShip;
    }

    public void setOpdMemberShip(List<OpdMemberShip> opdMemberShip) {
        this.opdMemberShip = opdMemberShip;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    
    

    public void createPaymentMethodInwardPriceAdjustments() {
        ipaPaymentMethods = new ArrayList<>();
        for (PaymentMethod pm : getEnumController().getPaymentMethods()) {
            IpaPaymentMethod subTable = new IpaPaymentMethod();
            subTable.setPaymentMethod(pm);
            subTable.setPriceMatrixs(getPriceMatrixController().getInwardMemberShipDiscounts(pm));
            if (!subTable.getPriceMatrixs().isEmpty()) {
                ipaPaymentMethods.add(subTable);
            }
        }
    }

    public void createOpdPriceAdjustments() {
        opdMemberShip = new ArrayList<>();
        for (MembershipScheme pm : getMembershipSchemeController().getItems()) {
            System.err.println("Mem " + pm.getName());
            OpdMemberShip subTable = new OpdMemberShip();
            subTable.setMembershipScheme(pm);
            List<PriceMatrix> list = getPriceMatrixController().getOpdMemberShipDiscountsDepartment(pm);
            list.addAll(getPriceMatrixController().getOpdMemberShipDiscountsCategory(pm));
            subTable.setPriceMatrixs(list);
            if (!subTable.getPriceMatrixs().isEmpty()) {
                opdMemberShip.add(subTable);
            }
        }
    }

    public void createCreditInstitutionInwardPriceAdjustments() {
        ipaCreditInstitutions = new ArrayList<>();
        for (Institution ins : getInstitutionController().getCreditCompanies()) {
            IpaCreditInstitution subTable1 = new IpaCreditInstitution();
            subTable1.setInstitution(ins);
            for (PaymentMethod pm : getEnumController().getPaymentMethods()) {
                IpaPaymentMethod subTable2 = new IpaPaymentMethod();
                subTable2.setPaymentMethod(pm);
                subTable2.setPriceMatrixs(getPriceMatrixController().getInwardMemberShipDiscounts(ins, pm));

                if (!subTable2.getPriceMatrixs().isEmpty()) {
                    subTable1.getIpaPaymentMethods().add(subTable2);
                }
            }

            if (!subTable1.getIpaPaymentMethods().isEmpty()) {
                ipaCreditInstitutions.add(subTable1);
            }
        }
    }

    public void createMemberShipCreditInstitution() {
        ipaMemberShipCreditInstitution = new ArrayList<>();
        for (Institution ins : getInstitutionController().getCreditCompanies()) {
            IpaMemberShipCreditInstitution subTable1 = new IpaMemberShipCreditInstitution();
            subTable1.setInstitution(ins);
            for (MembershipScheme mem : getMembershipSchemeController().getItems()) {
                IpaMemberShip subTable2 = new IpaMemberShip();
                subTable2.setMembershipScheme(mem);
                for (PaymentMethod pm : getEnumController().getPaymentMethods()) {
                    IpaPaymentMethod subTable3 = new IpaPaymentMethod();
                    subTable3.setPaymentMethod(pm);
                    subTable3.setPriceMatrixs(getPriceMatrixController().getInwardMemberShipDiscounts(ins, mem, pm));

                    if (!subTable3.getPriceMatrixs().isEmpty()) {
                        subTable2.getIpaPaymentMethods().add(subTable3);
                    }
                }

                if (!subTable2.getIpaPaymentMethods().isEmpty()) {
                    subTable1.getIpaMemberShips().add(subTable2);
                }
            }

            if (!subTable1.getIpaMemberShips().isEmpty()) {
                ipaMemberShipCreditInstitution.add(subTable1);
            }

        }
    }

    public void createMemberShipInwardPriceAdjustments() {
        ipaMemberShip = new ArrayList<>();
        for (MembershipScheme mem : getMembershipSchemeController().getItems()) {
            IpaMemberShip subTable1 = new IpaMemberShip();
            subTable1.setMembershipScheme(mem);
            for (PaymentMethod pm : getEnumController().getPaymentMethods()) {
                IpaPaymentMethod subTable2 = new IpaPaymentMethod();
                subTable2.setPaymentMethod(pm);
                subTable2.setPriceMatrixs(getPriceMatrixController().getInwardMemberShipDiscounts(mem, pm));

                if (!subTable2.getPriceMatrixs().isEmpty()) {
                    subTable1.getIpaPaymentMethods().add(subTable2);
                }
            }

            if (!subTable1.getIpaPaymentMethods().isEmpty()) {
                ipaMemberShip.add(subTable1);
            }
        }
    }

    public List<IpaMemberShipCreditInstitution> getIpaMemberShipCreditInstitution() {
        return ipaMemberShipCreditInstitution;
    }

    public void setIpaMemberShipCreditInstitution(List<IpaMemberShipCreditInstitution> ipaMemberShipCreditInstitution) {
        this.ipaMemberShipCreditInstitution = ipaMemberShipCreditInstitution;
    }

    public MembershipSchemeController getMembershipSchemeController() {
        return membershipSchemeController;
    }

    public void setMembershipSchemeController(MembershipSchemeController membershipSchemeController) {
        this.membershipSchemeController = membershipSchemeController;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public InwardMemberShipDiscount getInwardMemberShipDiscount() {
        return inwardMemberShipDiscount;
    }

    public void setInwardMemberShipDiscount(InwardMemberShipDiscount inwardMemberShipDiscount) {
        this.inwardMemberShipDiscount = inwardMemberShipDiscount;
    }

    public List<IpaPaymentMethod> getIpaPaymentMethods() {
        return ipaPaymentMethods;
    }

    public void setIpaPaymentMethods(List<IpaPaymentMethod> ipaPaymentMethods) {
        this.ipaPaymentMethods = ipaPaymentMethods;
    }

    public List<IpaCreditInstitution> getIpaCreditInstitutions() {
        return ipaCreditInstitutions;
    }

    public void setIpaCreditInstitutions(List<IpaCreditInstitution> ipaCreditInstitutions) {
        this.ipaCreditInstitutions = ipaCreditInstitutions;
    }

    public EnumController getEnumController() {
        return enumController;
    }

    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    public PriceMatrixFacade getInwardPriceAdjustmentFacade() {
        return inwardPriceAdjustmentFacade;
    }

    public void setInwardPriceAdjustmentFacade(PriceMatrixFacade inwardPriceAdjustmentFacade) {
        this.inwardPriceAdjustmentFacade = inwardPriceAdjustmentFacade;
    }

    /**
     * Creates a new instance of ReportMemberShip
     */
    public ReportMemberShip() {
    }

    public List<IpaMemberShip> getIpaMemberShip() {
        return ipaMemberShip;
    }

    public void setIpaMemberShip(List<IpaMemberShip> ipaMemberShip) {
        this.ipaMemberShip = ipaMemberShip;
    }

}
