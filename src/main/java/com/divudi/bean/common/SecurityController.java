/*
 * Author : Dr. M H B Ariyaratne
 *
 * MO(Health Information), Department of Health Services, Southern Province
 * and
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import java.io.Serializable;
import javax.inject.Named; import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class SecurityController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of HOSecurity
     */
    public SecurityController() {
    }

    public String encrypt(String word) {
        BasicTextEncryptor en = new BasicTextEncryptor();
        en.setPassword("health");
        try {
            return en.encrypt(word);
        } catch (Exception ex) {
            return null;
        }
    }

    public String hash(String word) {
        try {
            BasicPasswordEncryptor en = new BasicPasswordEncryptor();
            return en.encryptPassword(word);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean matchPassword(String planePassword, String encryptedPassword) {
        BasicPasswordEncryptor en = new BasicPasswordEncryptor();
        return en.checkPassword(planePassword, encryptedPassword);
    }

    public String decrypt(String word) {
        BasicTextEncryptor en = new BasicTextEncryptor();
        en.setPassword("health");
        try {
            return en.decrypt(word);
        } catch (Exception ex) {
            return null;
        }
    }
}
