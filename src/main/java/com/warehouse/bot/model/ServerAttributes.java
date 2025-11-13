package com.warehouse.bot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerAttributes
{
    private Integer product_id;
    private Integer ram_gb;
    private String cpu_model;
    private Integer cpu_cores;
    private Integer hdd_size_gb;
    private Integer ssd_size_gb;
    private String form_factor; // "Rack", "Tower", "Blade"
    private String manufacturer;
}