package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        CONFIRM: ADMIN, PENDING → CONFIRMED.
        CANCEL: CUSTOMER chỉ khi PENDING; ADMIN khi PENDING, hoặc CONFIRMED nếu có reason.
        SHIP: ADMIN hoặc đối tác, CONFIRMED → SHIPPING.
        FAIL_DELIVERY: ADMIN hoặc đối tác, SHIPPING → CONFIRMED (giao lại).
        DELIVER: ADMIN hoặc đối tác, SHIPPING → DELIVERED.
        REQUEST_RETURN: CUSTOMER hoặc ADMIN, DELIVERED → RETURN_REQUESTED, trong 7 ngày kể từ khi giao.
        ACCEPT_RETURN: ADMIN, RETURN_REQUESTED → RETURNED.
        """)
public enum OrderAction {
    CONFIRM,
    CANCEL,
    SHIP,
    FAIL_DELIVERY,
    DELIVER,
    REQUEST_RETURN,
    ACCEPT_RETURN
}
