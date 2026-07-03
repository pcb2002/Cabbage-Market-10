package com.example.cabbagemarket10.domain.item.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemBidRequest {

    @NotNull(message = "입찰가는 필수입니다.")
    @PositiveOrZero(message = "입찰가는 0 이상이어야 합니다.")
    private Long bidPrice;

    public ItemBidRequest(Long bidPrice) {
        this.bidPrice = bidPrice;
    }
}
