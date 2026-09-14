package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

@Data
public class BorrowRequest {
    private Integer bookId;
    private Integer userId;
}
