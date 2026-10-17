package com.divudi.bean.channel;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.channel.ChannelShiftCollectionReportDTO;
import com.divudi.core.data.dto.channel.ChannelShiftCollectionRowDTO;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Institution;
import com.divudi.service.ChannelService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Update action helper of the Summary Collection Report (#24248).
 */
class ChannelReportControllerShiftCollectionTest {

    /**
     * Stub service: records the arguments and returns a canned result.
     */
    static class StubChannelService extends ChannelService {

        Long shiftStartBillId;
        Long shiftEndBillId;
        Long cashierId;
        Institution hospital;
        List<Category> categories;
        List<PaymentMethod> methods;
        ChannelReportController.WrapperDtoForChannelFutureIncome result;

        @Override
        public ChannelReportController.WrapperDtoForChannelFutureIncome fetchChannelBookingBillsForShiftEnd(Long shiftStartBillId, Long shiftEndBillId,
                Long createrId, Institution hospital, List<Category> categoryList, List<PaymentMethod> paymentMethods) {
            this.shiftStartBillId = shiftStartBillId;
            this.shiftEndBillId = shiftEndBillId;
            this.cashierId = createrId;
            this.hospital = hospital;
            this.categories = categoryList;
            this.methods = paymentMethods;
            return result;
        }
    }

    private ChannelReportController controller;
    private StubChannelService service;
    private ChannelReportController.WrapperDtoForChannelFutureIncome current;
    private Institution hospital;
    private Date start;
    private Date end;

    @BeforeEach
    void setUp() {
        controller = new ChannelReportController();
        service = new StubChannelService();
        controller.channelService = service;

        hospital = new Institution();
        start = new Date(1_000_000L);
        end = new Date(2_000_000L);
        current = new ChannelReportController.WrapperDtoForChannelFutureIncome();
        current.setShiftStartBillId(100L);
        current.setShiftEndBillId(200L);
        current.setCashierId(7L);
        current.setCashierUserName("cashier1");
        current.setShiftStartAt(start);
        current.setShiftEndAt(end);
        current.setHospital(hospital);
    }

    private static ChannelShiftCollectionRowDTO row(long id, PaymentMethod pm, double doc, double hos, double total) {
        return new ChannelShiftCollectionRowDTO(id, "B" + id, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, pm, new Date(), new Date(),
                "Dr A", "Patient", doc, hos, total, false, false, null, null, null, 1);
    }

    @Test
    void reload_passesShiftContextAndFiltersToService() {
        List<PaymentMethod> methods = Arrays.asList(PaymentMethod.Cash);
        List<Category> categories = Collections.emptyList();
        service.result = new ChannelReportController.WrapperDtoForChannelFutureIncome();

        controller.reloadShiftCollection(current, categories, methods, "pubudu");

        assertEquals(100L, service.shiftStartBillId);
        assertEquals(200L, service.shiftEndBillId);
        assertEquals(7L, service.cashierId);
        assertSame(hospital, service.hospital);
        assertSame(categories, service.categories);
        assertSame(methods, service.methods);
    }

    @Test
    void reload_returnsRowsAndKeepsShiftHeader() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        report.addRow(row(1, PaymentMethod.Cash, 2000, 500, 2500), null);
        report.addRow(row(2, PaymentMethod.Card, 3000, 600, 3600), null);
        ChannelReportController.WrapperDtoForChannelFutureIncome fromService = new ChannelReportController.WrapperDtoForChannelFutureIncome();
        fromService.setShiftCollection(report);
        service.result = fromService;

        ChannelReportController.WrapperDtoForChannelFutureIncome reloaded = controller.reloadShiftCollection(current, null, null, "pubudu");

        assertTrue(reloaded.getShiftCollection().isHasTransactions());
        assertEquals(1, reloaded.getShiftCollection().getCashSection().getRows().size());
        assertEquals(1, reloaded.getShiftCollection().getCardSection().getRows().size());
        assertEquals(6100, reloaded.getShiftCollection().getGrandTotal());
        assertEquals("pubudu", reloaded.getProcessedBy());
        assertEquals("cashier1", reloaded.getCashierUserName());
        assertEquals(start, reloaded.getShiftStartAt());
        assertEquals(end, reloaded.getShiftEndAt());
        assertEquals(100L, reloaded.getShiftStartBillId());
        assertEquals(200L, reloaded.getShiftEndBillId());
        assertEquals(7L, reloaded.getCashierId());
        assertSame(hospital, reloaded.getHospital());
    }

    @Test
    void reload_whenServiceReturnsNull_givesEmptyReportNotNull() {
        service.result = null;

        ChannelReportController.WrapperDtoForChannelFutureIncome reloaded = controller.reloadShiftCollection(current, null, null, "pubudu");

        assertNotNull(reloaded);
        assertNotNull(reloaded.getShiftCollection());
        assertFalse(reloaded.getShiftCollection().isHasTransactions());
        assertEquals(0, reloaded.getShiftCollection().getGrandTotal());
        assertEquals(100L, reloaded.getShiftStartBillId());
    }
}
