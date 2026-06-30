package com.example.cabbagemarket10.domain.chat.entity;

import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    ENTRANCE_LOG,
    TEXT,
    IMAGE,
    EXIT_LOG;




    @JsonCreator
    public static MessageType from(String value) {
        for (MessageType messageType : values()) {
            if (messageType.name().equalsIgnoreCase(value)) {
                return messageType;
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @JsonValue
    public String getValue() {
        return name();
    }
}
