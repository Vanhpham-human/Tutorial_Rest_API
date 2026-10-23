package com.fptaptech.catalog.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_sequences")
public class OrderSequence {

    @Id
    @Column(name = "seq_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private long lastValue;

    protected OrderSequence() {
    }

    public OrderSequence(Integer year, long lastValue) {
        this.year = year;
        this.lastValue = lastValue;
    }

    public Integer getYear() {
        return year;
    }

    public long getLastValue() {
        return lastValue;
    }

    public void setLastValue(long lastValue) {
        this.lastValue = lastValue;
    }
}
