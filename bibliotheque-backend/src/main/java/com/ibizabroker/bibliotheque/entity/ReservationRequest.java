package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

@Data
public class ReservationRequest {
    private Integer bookId;
    private Integer adherentId;
}
