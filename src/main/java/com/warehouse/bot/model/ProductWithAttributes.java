package com.warehouse.bot.model;

import lombok.Data;

@Data
public class ProductWithAttributes<T>
{
    private Product product;
    private T attributes; // Can be ThermocupAttributes or ServerAttributes
}