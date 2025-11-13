// service/WarehouseApiService.java
package com.warehouse.bot.service;

import com.warehouse.bot.config.BotConfig;
import com.warehouse.bot.model.Product;
import com.warehouse.bot.model.ProductWithAttributes;
import com.warehouse.bot.model.ServerAttributes;
import com.warehouse.bot.model.Thermocup;
import com.warehouse.bot.model.ThermocupAttributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WarehouseApiService {

    private final RestTemplate restTemplate;
    private final BotConfig botConfig;

    public WarehouseApiService(RestTemplate restTemplate, BotConfig botConfig) {
        this.restTemplate = restTemplate;
        this.botConfig = botConfig;
    }

    public List<Product> getProducts(Map<String, String> filters) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(botConfig.getWarehouseServiceUrl() + "/products");

            filters.forEach(builder::queryParam);

            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Product>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting products: {}", e.getMessage());
            return List.of();
        }
    }

    public Product getProductById(Long productId) {
        try {
            String url = botConfig.getWarehouseServiceUrl() + "/products/" + productId;
            return restTemplate.getForObject(url, Product.class);
        } catch (Exception e) {
            log.error("Error getting product by ID: {}", e.getMessage());
            return null;
        }
    }

    public Thermocup getThermocupById(Long productId) {
        try {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermohelmets/" + productId;
            return restTemplate.getForObject(url, Thermocup.class);
        } catch (Exception e) {
            log.error("Error getting thermocup by ID: {}", e.getMessage());
            return null;
        }
    }

    public String createThermocup(Thermocup thermocup) {
        try {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/create";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Thermocup> request = new HttpEntity<>(thermocup, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Thermocup created successfully!";
            } else {
                return "Error creating thermocup: " + response.getBody();
            }
        } catch (Exception e) {
            log.error("Error creating thermocup: {}", e.getMessage());
            return "Error creating thermocup: " + e.getMessage();
        }
    }

    public String updateThermocup(Long productId, Thermocup thermocup) {
        try {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/update/" + productId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Thermocup> request = new HttpEntity<>(thermocup, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Thermocup updated successfully!";
            } else {
                return "Error updating thermocup: " + response.getBody();
            }
        } catch (Exception e) {
            log.error("Error updating thermocup: {}", e.getMessage());
            return "Error updating thermocup: " + e.getMessage();
        }
    }

    public String updateReservedQuantity(Long productId, Integer quantityChange) {
        try {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/update/" + productId + "/reserved";
            
            Map<String, Integer> requestBody = new HashMap<>();
            requestBody.put("quantity_change", quantityChange);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Reserved quantity updated successfully!";
            } else {
                return "Error updating reserved quantity: " + response.getBody();
            }
        } catch (Exception e) {
            log.error("Error updating reserved quantity: {}", e.getMessage());
            return "Error updating reserved quantity: " + e.getMessage();
        }
    }

    public String updateStockQuantity(Long productId, Integer warehouseId, Integer quantityChange)
    {
        try
        {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/update/" + productId + "/stock";
            
            Map<String, Integer> requestBody = new HashMap<>();
            requestBody.put("warehouse_id", warehouseId);
            requestBody.put("quantity_change", quantityChange);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return "Stock quantity updated successfully!";
            } else {
                return "Error updating stock quantity: " + response.getBody();
            }
        }
        catch (Exception e)
        {
            log.error("Error updating stock quantity: {}", e.getMessage());
            return "Error updating stock quantity: " + e.getMessage();
        }
    }

    // Get product with attributes based on category
    public ProductWithAttributes<?> getProductWithAttributes(Integer productId)
    {
        try
        {
            // First get the main product
            Product product = getProductById(productId);
            if (product == null) return null;
            
            // Then get attributes based on category
            Object attributes = null;
            if (product.getCategory_id() == 1)
            {
                attributes = getThermocupAttributes(productId);
            }
            else if (product.getCategory_id() == 2)
            {
                attributes = getServerAttributes(productId);
            }
            
            ProductWithAttributes<Object> result = new ProductWithAttributes<>();
            result.setProduct(product);
            result.setAttributes(attributes);
            return result;
        }
        catch (Exception e)
        {
            log.error("Error getting product with attributes: {}", e.getMessage());
            return null;
        }
    }

    // Get thermocup attributes
    public ThermocupAttributes getThermocupAttributes(Integer productId)
    {
        try
        {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/" + productId;
            return restTemplate.getForObject(url, ThermocupAttributes.class);
        }
        catch (Exception e)
        {
            log.error("Error getting thermocup attributes: {}", e.getMessage());
            return null;
        }
    }

    // Get server attributes  
    public ServerAttributes getServerAttributes(Integer productId)
    {
        try
        {
            String url = botConfig.getWarehouseServiceUrl() + "/products/servers/" + productId;
            return restTemplate.getForObject(url, ServerAttributes.class);
        }
        catch (Exception e)
        {
            log.error("Error getting server attributes: {}", e.getMessage());
            return null;
        }
    }

    // Create thermocup (both product and attributes)
    public String createThermocup(Product product, ThermocupAttributes attributes)
    {
        try
        {
            // First create the main product
            String productUrl = botConfig.getWarehouseServiceUrl() + "/products";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Product> productRequest = new HttpEntity<>(product, headers);
            ResponseEntity<Product> productResponse = restTemplate.postForEntity(productUrl, productRequest, Product.class);
            
            if (productResponse.getStatusCode() != HttpStatus.OK)
            {
                return "Error creating product: " + productResponse.getBody();
            }
            
            // Then create thermocup attributes
            Integer newProductId = productResponse.getBody().getId();
            attributes.setProduct_id(newProductId);
            
            String attributesUrl = botConfig.getWarehouseServiceUrl() + "/products/thermocups/create";
            HttpEntity<ThermocupAttributes> attributesRequest = new HttpEntity<>(attributes, headers);
            ResponseEntity<String> attributesResponse = restTemplate.postForEntity(attributesUrl, attributesRequest, String.class);
            
            if (attributesResponse.getStatusCode() == HttpStatus.OK)
            {
                return "Thermocup created successfully with ID: " + newProductId;
            }
            else
            {
                return "Error creating thermocup attributes: " + attributesResponse.getBody();
            }
        }
        catch (Exception e)
        {
            log.error("Error creating thermocup: {}", e.getMessage());
            return "Error creating thermocup: " + e.getMessage();
        }
    }
}