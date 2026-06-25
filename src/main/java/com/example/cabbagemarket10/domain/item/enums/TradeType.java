package com.example.cabbagemarket10.domain.item.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeType {
    DIRECT("직거래"),
    DELIVERY("택배거래"),
    AUCTION("경매");

    private final String description;
}
