package com.warehouse.bot.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product
{
    private Integer id;
    private String name;
    private Integer category_id;
    private String sku;
    private BigDecimal base_price;
    private Boolean is_active;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private String path_to_photo;
    private Integer num_reserved_goods;
}