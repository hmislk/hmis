package com.divudi.core.entity;

import com.divudi.core.data.OtpChannel;
import com.divudi.core.data.OtpPurpose;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A one-time code sent to a user. Only an HMAC of the code is stored, never
 * the code itself. A code can be used once: it is closed by consumedAt (used)
 * or invalidatedAt (replaced by a newer code).
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Entity
public class OtpCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private WebUser webUser;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    private OtpChannel channel;

    // Where the code was sent (mobile number as used for sending)
    private String identifier;

    @Column(length = 64)
    private String codeHash;

    private int attempts = 0;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date consumedAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date invalidatedAt;

    private String requestIp;
    @Column(length = 500)
    private String userAgent;

    private boolean sentSuccessfully;

    @ManyToOne
    private Sms sms;

    public boolean isActive() {
        return consumedAt == null && invalidatedAt == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public OtpChannel getChannel() {
        return channel;
    }

    public void setChannel(OtpChannel channel) {
        this.channel = channel;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Date consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Date getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Date invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isSentSuccessfully() {
        return sentSuccessfully;
    }

    public void setSentSuccessfully(boolean sentSuccessfully) {
        this.sentSuccessfully = sentSuccessfully;
    }

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OtpCode)) {
            return false;
        }
        OtpCode other = (OtpCode) object;
        if (this.id == null || other.id == null) {
            return this == other;
        }
        return this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.OtpCode[ id=" + id + " ]";
    }
}
