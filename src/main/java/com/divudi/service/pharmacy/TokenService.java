package com.divudi.service.pharmacy;

import com.divudi.core.data.TokenType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Token;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.facade.TokenFacade;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class TokenService {

    @EJB
    private TokenFacade tokenFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;

    public void markInprogress(Token token, Bill bill) {
        if (token == null) {
            return;
        }
        token.setBill(bill);
        token.setCalled(false);
        token.setCalledAt(null);
        token.setInProgress(true);
        token.setCompleted(false);
        tokenFacade.edit(token);
    }

    public void markToken(Token token, Bill bill) {
        if (token == null) {
            return;
        }
        token.setBill(bill);
        token.setCalled(true);
        token.setCalledAt(new Date());
        token.setInProgress(false);
        token.setCompleted(false);
        tokenFacade.edit(token);
    }

    public Token settlePharmacyToken(TokenType tokenType,
            Department department,
            Department counter,
            Patient patient,
            Institution institution,
            Bill bill) {
        Token token = new Token();
        token.setTokenType(tokenType);
        token.setDepartment(department);
        token.setFromDepartment(department);
        token.setPatient(patient);
        token.setInstitution(institution);
        token.setFromInstitution(institution);
        token.setCounter(counter);
        if (counter != null) {
            token.setToDepartment(counter.getSuperDepartment());
            if (counter.getSuperDepartment() != null) {
                token.setToInstitution(counter.getSuperDepartment().getInstitution());
            }
        }
        tokenFacade.create(token);
        token.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(token.getFromDepartment(), null, null, tokenType));
        token.setTokenDate(new Date());
        token.setTokenAt(new Date());
        token.setBill(bill);
        tokenFacade.edit(token);
        return token;
    }
}
