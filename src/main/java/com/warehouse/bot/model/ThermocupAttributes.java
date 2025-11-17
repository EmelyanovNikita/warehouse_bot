package com.warehouse.bot.model;

import lombok.Data;

@Data
public class ThermocupAttributes
{
    private Integer product_id;
    private Integer volume_ml;
    private String color;
    private String brand;
    private String model;
    private Boolean is_hermetic;
    private String material;
}