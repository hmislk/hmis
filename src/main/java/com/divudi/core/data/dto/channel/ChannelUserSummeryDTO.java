package com.divudi.core.data.dto.channel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.divudi.core.data.PaymentMethod;

public class ChannelUserSummeryDTO {

        private double doctorFee;
        private double hosFee;
        private double total;
        private double cashTotal;
        private double cardTotal;
        private long billedCount;
        private long cancelledCount;
        private long refundCount;
        private long totalCount;

        private List<ChannelUserSummeryByDateDTO> entriesByDate;

        public ChannelUserSummeryDTO() {
            entriesByDate = new ArrayList<>();
            doctorFee = 0.0;
            hosFee = 0.0;
            total = 0.0;
            cashTotal = 0.0;
            cardTotal = 0.0;
            billedCount = 0;
            cancelledCount = 0;
            refundCount = 0;
            totalCount = 0;
        }

        public List<ChannelUserSummeryByDateDTO> getEntriesByDate() {
            return entriesByDate;
        }

        public void setEntriesByDate(List<ChannelUserSummeryByDateDTO> entriesByDate) {
            this.entriesByDate = entriesByDate;
        }

        public double getDoctorFee() {
            return doctorFee;
        }
            
        public void setDoctorFee(double doctorFee) {
            this.doctorFee = doctorFee;
        }
            
        public double getHosFee() {
            return hosFee;
        }
            
        public void setHosFee(double hosFee) {
            this.hosFee = hosFee;
        }
            
        public double getTotal() {
            return total;
        }
            
        public void setTotal(double total) {
            this.total = total;
        }
            
        public double getCashTotal() {
            return cashTotal;
        }
            
        public void setCashTotal(double cashTotal) {
            this.cashTotal = cashTotal;
        }
            
        public double getCardTotal() {
            return cardTotal;
        }
            
        public void setCardTotal(double cardTotal) {
            this.cardTotal = cardTotal;
        }
            
        public long getBilledCount() {
            return billedCount;
        }
            
        public void setBilledCount(long billedCount) {
            this.billedCount = billedCount;
        }
            
        public long getCancelledCount() {
            return cancelledCount;
        }
            
        public void setCancelledCount(long cancelledCount) {
            this.cancelledCount = cancelledCount;
        }
            
        public long getRefundCount() {
            return refundCount;
        }
            
        public void setRefundCount(long refundCount) {
            this.refundCount = refundCount;
        }
            
        public long getTotalCount() {
            return totalCount;
        }
            
        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public static class ChannelUserSummeryByDateDTO {
            private Date billedDate;
            private String user;

            private PaymentMethod paymentMethod;

            private double doctorFee;
            private double hosFee;
            private double total;
            private double cashTotal;
            private double cardTotal;
            private long billedCount;
            private long cancelledCount;
            private long refundCount;
            private long totalCount;

            public ChannelUserSummeryByDateDTO(Date billedDate, String user, PaymentMethod paymentMethod, Double doctorFee, Double hosFee, Double total, Long cancelledCount, Long refundedCount, Long billedCount) {
                this.billedDate = billedDate;
                this.user = user;
                this.paymentMethod = paymentMethod;
                
                this.doctorFee = doctorFee != null ? doctorFee.doubleValue() : 0;
                this.hosFee = hosFee != null ? hosFee.doubleValue() : 0;
                this.total = total != null ? total.doubleValue() : 0;
                this.cashTotal = 0.0;
                this.cardTotal = 0.0;

                this.cancelledCount = cancelledCount != null ? cancelledCount.longValue() : 0;
                this.refundCount = refundedCount != null ? refundedCount.longValue() : 0;
                this.billedCount = billedCount != null ? billedCount.longValue() : 0;
                this.totalCount = 0;
            }

            // Getters and Setters
            public Date getBilledDate() {
                return billedDate;
            }
            
            public void setBilledDate(Date billedDate) {
                this.billedDate = billedDate;
            }
            
            public String getUser() {
                return user;
            }
            
            public void setUser(String user) {
                this.user = user;
            }
            
            public PaymentMethod getPaymentMethod() {
                return paymentMethod;
            }
            
            public void setPaymentMethod(PaymentMethod paymentMethod) {
                this.paymentMethod = paymentMethod;
            }
            
            public double getDoctorFee() {
                return doctorFee;
            }
            
            public void setDoctorFee(double doctorFee) {
                this.doctorFee = doctorFee;
            }
            
            public double getHosFee() {
                return hosFee;
            }
            
            public void setHosFee(double hosFee) {
                this.hosFee = hosFee;
            }
            
            public double getTotal() {
                return total;
            }
            
            public void setTotal(double total) {
                this.total = total;
            }
            
            public double getCashTotal() {
                return cashTotal;
            }
            
            public void setCashTotal(double cashTotal) {
                this.cashTotal = cashTotal;
            }
            
            public double getCardTotal() {
                return cardTotal;
            }
            
            public void setCardTotal(double cardTotal) {
                this.cardTotal = cardTotal;
            }
            
            public long getBilledCount() {
                return billedCount;
            }
            
            public void setBilledCount(long billedCount) {
                this.billedCount = billedCount;
            }
            
            public long getCancelledCount() {
                return cancelledCount;
            }
            
            public void setCancelledCount(long cancelledCount) {
                this.cancelledCount = cancelledCount;
            }
            
            public long getRefundCount() {
                return refundCount;
            }
            
            public void setRefundCount(long refundCount) {
                this.refundCount = refundCount;
            }
            
            public long getTotalCount() {
                return totalCount;
            }
            
            public void setTotalCount(long totalCount) {
                this.totalCount = totalCount;
            }
        }
    }
