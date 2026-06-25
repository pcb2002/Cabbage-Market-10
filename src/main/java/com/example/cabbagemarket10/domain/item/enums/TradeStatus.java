package com.example.cabbagemarket10.domain.item.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeStatus {
    ON_SALE("판매중"),
    RESERVED("예약중"),
    SOLD_OUT("판매완료");

    private final String description;
}