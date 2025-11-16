package com.warehouse.bot.service;

import com.warehouse.bot.config.BotConfig;
import com.warehouse.bot.model.*;
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
public class WarehouseApiService
{

    private final RestTemplate restTemplate;
    private final BotConfig botConfig;

    public WarehouseApiService(RestTemplate restTemplate, BotConfig botConfig)
    {
        this.restTemplate = restTemplate;
        this.botConfig = botConfig;
    }

    public List<Product> getProducts(Map<String, String> filters)
    {
        try
        {
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
        }
        catch (Exception e)
        {
            log.error("Error getting products: {}", e.getMessage());
            return List.of();
        }
    }

    public Product getProductById(Long productId)
    {
        try
        {
            System.out.println("getProductByIdAAAAAAAAAAAAAAAA");
            String url = botConfig.getWarehouseServiceUrl() + "/products/" + productId;
            return restTemplate.getForObject(url, Product.class);
        }
        catch (Exception e)
        {
            log.error("Error getting product by ID: {}", e.getMessage());
            return null;
        }
    }

    public ProductWithAttributes<?> getProductWithAttributes(Long productId)
    {
        // We can update method of getting all info from db if will use defferent sku for different categories
        // for example: thermos - T-xxxxxxx : there are T-thermos and xxxxxxxx - is number of product
        // it's better way than using two requests
        try
        {
            // First get the main product
            Product product = getProductById(productId);
            System.out.println("getProductWithAttributesAAAAAAAAAAAAAAAA");
            if (product == null) return null;
            
            // Then get attributes based on category
            Object attributes = null;
            if (product.getCategory() == "Thermocups") // 1 == Thermocup
            {
                attributes = getThermocupAttributes(productId);
            }
            else if (product.getCategory() == "Server") // 2 == Server
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

    public ThermocupAttributes getThermocupAttributes(Long productId)
    {
        try
        {
            System.out.println("getThermocupAttributesAAAAAAAAAAAAAAAA");
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/" + productId;
            return restTemplate.getForObject(url, ThermocupAttributes.class);
        }
        catch (Exception e)
        {
            log.error("Error getting thermocup attributes: {}", e.getMessage());
            return null;
        }
    }

    public ServerAttributes getServerAttributes(Long productId)
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

    public String updateThermocup(Long productId, Product productUpdate, ThermocupAttributes attributesUpdate)
    {
        try
        {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/update/" + productId;
            
            // Combine product and attributes updates
            Map<String, Object> updateRequest = new HashMap<>();
            if (productUpdate != null)
            {
                updateRequest.put("product", productUpdate);
            }
            if (attributesUpdate != null)
            {
                updateRequest.put("attributes", attributesUpdate);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateRequest, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK)
            {
                return "Thermocup updated successfully!";
            }
            else
            {
                return "Error updating thermocup: " + response.getBody();
            }
        }
        catch (Exception e)
        {
            log.error("Error updating thermocup: {}", e.getMessage());
            return "Error updating thermocup: " + e.getMessage();
        }
    }

    public String updateReservedQuantity(Long productId, Integer quantityChange)
    {
        try
        {
            String url = botConfig.getWarehouseServiceUrl() + "/products/thermocups/update/" + productId + "/reserved";
            
            Map<String, Integer> requestBody = new HashMap<>();
            requestBody.put("quantity_change", quantityChange);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK)
            {
                return "Reserved quantity updated successfully!";
            }
            else
            {
                return "Error updating reserved quantity: " + response.getBody();
            }
        }
        catch (Exception e)
        {
            log.error("Error updating reserved quantity: {}", e.getMessage());
            return "Error updating reserved quantity: " + e.getMessage();
        }
    }

    public String updateStockQuantity(Long productId, Integer warehouseId, Integer quantityChange) {
        try {
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
        } catch (Exception e) {
            log.error("Error updating stock quantity: {}", e.getMessage());
            return "Error updating stock quantity: " + e.getMessage();
        }
    }
}