package com.fptaptech.catalog.report;

import com.fptaptech.catalog.common.CatalogException;
import com.fptaptech.catalog.order.Order;
import com.fptaptech.catalog.order.OrderRepository;
import com.fptaptech.catalog.order.OrderService;
import com.fptaptech.catalog.order.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orders;
    private final Clock clock;

    public ReportService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    public DailyRevenueReport daily(RevenueQuery query) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = query.from() == null ? today.minusDays(29) : query.from();
        LocalDate to = query.to() == null ? today : query.to();
        if (from.isAfter(to)) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "Invalid date range",
                    "from phải nhỏ hơn hoặc bằng to");
        }
        var delivered = orders.findByStatusInAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(
                EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED),
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        List<DailyRevenuePoint> days = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            LocalDate current = day;
            List<Order> ofDay = delivered.stream()
                    .filter(order -> current.equals(order.getDeliveredAt().atZone(ZoneOffset.UTC).toLocalDate()))
                    .toList();
            BigDecimal amount = ofDay.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            days.add(new DailyRevenuePoint(current, amount, ofDay.size()));
            total = total.add(amount);
        }
        return new DailyRevenueReport(from, to, OrderService.CURRENCY, total, days);
    }
}
