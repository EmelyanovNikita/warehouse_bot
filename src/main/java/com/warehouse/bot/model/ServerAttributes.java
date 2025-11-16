package com.warehouse.bot.model;

import lombok.Data;

@Data
public class ServerAttributes {
    private Integer product_id;
    private Integer ram_gb;
    private String cpu_model;
    private Integer cpu_cores;
    private Integer hdd_size_gb;
    private Integer ssd_size_gb;
    private String form_factor;
    private String manufacturer;
}