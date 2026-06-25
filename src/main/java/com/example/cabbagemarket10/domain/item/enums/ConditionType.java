package com.example.cabbagemarket10.domain.item.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConditionType {
    NEW("새상품"),
    USED("중고상품");

    private final String description;
}