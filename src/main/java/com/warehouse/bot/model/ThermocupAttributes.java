package com.warehouse.bot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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