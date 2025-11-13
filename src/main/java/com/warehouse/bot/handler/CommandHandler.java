// handler/CommandHandler.java
package com.warehouse.bot.handler;

import com.warehouse.bot.model.Product;
import com.warehouse.bot.model.ProductWithAttributes;
import com.warehouse.bot.model.ServerAttributes;
import com.warehouse.bot.model.Thermocup;
import com.warehouse.bot.model.ThermocupAttributes;
import com.warehouse.bot.service.WarehouseApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CommandHandler {

    private final WarehouseApiService warehouseApiService;
    private Map<Long, String> userStates = new HashMap<>();

    public CommandHandler(WarehouseApiService warehouseApiService) {
        this.warehouseApiService = warehouseApiService;
    }

    public String handleCommand(String message, Long chatId) {
        try {
            if (userStates.containsKey(chatId)) {
                return handleState(message, chatId);
            }

            switch (message) {
                case "/start":
                    return getWelcomeMessage();
                
                case "Get products":
                    return getProductsMenu();
                
                case "All products":
                    return getAllProducts();
                
                case "Products by ID":
                    userStates.put(chatId, "AWAITING_PRODUCT_ID");
                    return "Please enter the product ID:";
                
                case "Thermocups by ID":
                    userStates.put(chatId, "AWAITING_THERMOCUP_ID");
                    return "Please enter the thermocup ID:";
                
                case "Add new products":
                    return getAddProductsMenu();
                
                case "Add new Thermal mug":
                    userStates.put(chatId, "AWAITING_THERMOCUP_CREATE");
                    return getThermocupCreateInstructions();
                
                case "Update products":
                    return getUpdateProductsMenu();
                
                case "Update thermal mug by ID":
                    userStates.put(chatId, "AWAITING_THERMOCUP_UPDATE");
                    return "Please enter thermocup ID and update data in format:\n" +
                           "ID|name|category_id|base_price|SKU|is_active|path_to_photo|volume_ml|color|brand|model|is_hermetic|material\n" +
                           "Example: 123|New Name|1|29.99|SKU123|true|/photos/1.jpg|500|Red|BrandX|ModelY|true|Stainless Steel";
                
                case "Update quantity of reserved product":
                    userStates.put(chatId, "AWAITING_RESERVED_UPDATE");
                    return "Please enter product ID and quantity change in format: ID|QUANTITY\nExample: 123|10";
                
                case "Update product quantity in stock":
                    userStates.put(chatId, "AWAITING_STOCK_UPDATE");
                    return "Please enter product ID, warehouse ID and quantity change in format: PRODUCT_ID|WAREHOUSE_ID|QUANTITY\nExample: 123|1|15";
                
                default:
                    return "Unknown command. Please use the menu buttons or type /start to see available options.";
            }
        } catch (Exception e) {
            log.error("Error handling command: {}", e.getMessage());
            return "An error occurred while processing your request. Please try again.";
        }
    }

    private String handleState(String message, Long chatId) {
        String state = userStates.get(chatId);
        userStates.remove(chatId);

        try {
            switch (state) {
                case "AWAITING_PRODUCT_ID":
                    Long productId = Long.parseLong(message.trim());
                    Product product = warehouseApiService.getProductById(productId);
                    return product != null ? formatProduct(product) : "Product not found!";
                
                case "AWAITING_THERMOCUP_ID":
                    Long thermocupId = Long.parseLong(message.trim());
                    Thermocup thermocup = warehouseApiService.getThermocupById(thermocupId);
                    return thermocup != null ? formatThermocup(thermocup) : "Thermocup not found!";
                
                case "AWAITING_THERMOCUP_CREATE":
                    return createThermocupFromInput(message);
                
                case "AWAITING_THERMOCUP_UPDATE":
                    return updateThermocupFromInput(message);
                
                case "AWAITING_RESERVED_UPDATE":
                    return updateReservedQuantityFromInput(message);
                
                case "AWAITING_STOCK_UPDATE":
                    return updateStockQuantityFromInput(message);
                
                default:
                    return "Invalid state. Please start over.";
            }
        } catch (NumberFormatException e) {
            return "Invalid number format. Please try again with valid numbers.";
        } catch (Exception e) {
            log.error("Error handling state: {}", e.getMessage());
            return "An error occurred while processing your input. Please try again.";
        }
    }

    private String getWelcomeMessage() {
        return "🏭 Welcome to Warehouse Bot! 🏭\n\n" +
               "Please choose an option from the menu:\n\n" +
               "📦 Get products\n" +
               "➕ Add new products\n" +
               "✏️ Update products";
    }

    private String getProductsMenu() {
        return "📦 Get Products Menu:\n\n" +
               "• All products\n" +
               "• Products by ID\n" +
               "• Thermocups by ID";
    }

    private String getAddProductsMenu() {
        return "➕ Add New Products:\n\n" +
               "• Add new Thermal mug";
    }

    private String getUpdateProductsMenu() {
        return "✏️ Update Products:\n\n" +
               "• Update thermal mug by ID\n" +
               "• Update quantity of reserved product\n" +
               "• Update product quantity in stock";
    }

    private String getAllProducts() {
        List<Product> products = warehouseApiService.getProducts(new HashMap<>());
        if (products.isEmpty()) {
            return "No products found.";
        }
        
        StringBuilder sb = new StringBuilder("📦 All Products:\n\n");
        for (int i = 0; i < Math.min(products.size(), 10); i++) {
            Product product = products.get(i);
            sb.append(formatProductShort(product)).append("\n\n");
        }
        
        if (products.size() > 10) {
            sb.append("... and ").append(products.size() - 10).append(" more products");
        }
        
        return sb.toString();
    }

    private String createThermocupFromInput(String input) {
        try {
            String[] parts = input.split("\\|");
            if (parts.length < 13) {
                return "Invalid format. Please provide all required fields.";
            }

            Thermocup thermocup = new Thermocup();
            thermocup.setName(parts[0]);
            thermocup.setCategory_id(Integer.parseInt(parts[1]));
            thermocup.setBase_price(Double.parseDouble(parts[2]));
            thermocup.setStarting_quantity(Integer.parseInt(parts[3]));
            thermocup.setWarehouse_id(Integer.parseInt(parts[4]));
            thermocup.setPath_to_photo(parts[5]);

            Attribute attributes = new Attribute();
            attributes.setVolume_ml(Integer.parseInt(parts[6]));
            attributes.setColor(parts[7]);
            attributes.setBrand(parts[8]);
            attributes.setModel(parts[9]);
            attributes.setIs_hermetic(Boolean.parseBoolean(parts[10]));
            attributes.setMaterial(parts[11]);

            thermocup.setAttributes(attributes);

            return warehouseApiService.createThermocup(thermocup);
        } catch (Exception e) {
            return "Error creating thermocup: " + e.getMessage();
        }
    }

    private String updateThermocupFromInput(String input) {
        try {
            String[] parts = input.split("\\|");
            if (parts.length < 13) {
                return "Invalid format. Please provide all required fields.";
            }

            Long productId = Long.parseLong(parts[0]);
            Thermocup thermocup = new Thermocup();
            thermocup.setName(parts[1]);
            thermocup.setCategory_id(Integer.parseInt(parts[2]));
            thermocup.setBase_price(Double.parseDouble(parts[3]));
            thermocup.setSKU(parts[4]);
            thermocup.setIs_active(Boolean.parseBoolean(parts[5]));
            thermocup.setPath_to_photo(parts[6]);

            Attribute attributes = new Attribute();
            attributes.setVolume_ml(Integer.parseInt(parts[7]));
            attributes.setColor(parts[8]);
            attributes.setBrand(parts[9]);
            attributes.setModel(parts[10]);
            attributes.setIs_hermetic(Boolean.parseBoolean(parts[11]));
            attributes.setMaterial(parts[12]);

            thermocup.setAttributes(attributes);

            return warehouseApiService.updateThermocup(productId, thermocup);
        } catch (Exception e) {
            return "Error updating thermocup: " + e.getMessage();
        }
    }

    private String updateReservedQuantityFromInput(String input) {
        try {
            String[] parts = input.split("\\|");
            if (parts.length != 2) {
                return "Invalid format. Please use: ID|QUANTITY";
            }

            Long productId = Long.parseLong(parts[0]);
            Integer quantityChange = Integer.parseInt(parts[1]);

            return warehouseApiService.updateReservedQuantity(productId, quantityChange);
        } catch (Exception e) {
            return "Error updating reserved quantity: " + e.getMessage();
        }
    }

    private String updateStockQuantityFromInput(String input) {
        try {
            String[] parts = input.split("\\|");
            if (parts.length != 3) {
                return "Invalid format. Please use: PRODUCT_ID|WAREHOUSE_ID|QUANTITY";
            }

            Long productId = Long.parseLong(parts[0]);
            Integer warehouseId = Integer.parseInt(parts[1]);
            Integer quantityChange = Integer.parseInt(parts[2]);

            return warehouseApiService.updateStockQuantity(productId, warehouseId, quantityChange);
        } catch (Exception e) {
            return "Error updating stock quantity: " + e.getMessage();
        }
    }

    private String getThermocupCreateInstructions() {
        return "Please enter thermocup data in the following format:\n\n" +
               "name|category_id|base_price|starting_quantity|warehouse_id|path_to_photo|volume_ml|color|brand|model|is_hermetic|material\n\n" +
               "Example:\n" +
               "Premium Thermo|1|29.99|100|1|/photos/thermo1.jpg|500|Blue|ThermoBrand|PremiumX|true|Stainless Steel";
    }

    private String formatProduct(Product product) {
        return String.format(
            "🆔 ID: %d\n📛 Name: %s\n🏷️ Category: %s\n💰 Price: $%.2f\n📦 Quantity: %d\n🔧 Active: %s\n📸 Photo: %s",
            product.getId(),
            product.getName(),
            product.getCategory(),
            product.getPrice(),
            product.getQuantity(),
            product.getIsActive() ? "Yes" : "No",
            product.getPathToPhoto()
        );
    }

    private String formatProductShort(Product product) {
        return String.format(
            "🆔 %d | 📛 %s | 💰 $%.2f | 📦 %d",
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getQuantity()
        );
    }

    private String formatThermocup(Thermocup thermocup)
    {
        return String.format(
            "📛 Name: %s\n🏷️ Category ID: %d\n💰 Base Price: $%.2f\n📦 Starting Quantity: %d\n🏭 Warehouse ID: %d\n📸 Photo: %s\n\n" +
            "Attributes:\n" +
            "• Volume: %d ml\n• Color: %s\n• Brand: %s\n• Model: %s\n• Hermetic: %s\n• Material: %s",
            thermocup.getName(),
            thermocup.getCategory_id(),
            thermocup.getBase_price(),
            thermocup.getStarting_quantity(),
            thermocup.getWarehouse_id(),
            thermocup.getPath_to_photo(),
            thermocup.getAttributes().getVolume_ml(),
            thermocup.getAttributes().getColor(),
            thermocup.getAttributes().getBrand(),
            thermocup.getAttributes().getModel(),
            thermocup.getAttributes().getIs_hermetic() ? "Yes" : "No",
            thermocup.getAttributes().getMaterial()
        );
    }

    private String formatProductWithAttributes(ProductWithAttributes<?> productWithAttributes)
    {
        Product product = productWithAttributes.getProduct();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
            "🆔 ID: %d\n📛 Name: %s\n🏷️ Category ID: %d\n💰 Price: $%.2f\n📦 Reserved: %d\n🔧 Active: %s\n",
            product.getId(),
            product.getName(),
            product.getCategory_id(),
            product.getBase_price(),
            product.getNum_reserved_goods(),
            product.getIs_active() ? "Yes" : "No"
        ));
        
        // Add attributes based on category
        Object attributes = productWithAttributes.getAttributes();
        if (attributes instanceof ThermocupAttributes) {
            ThermocupAttributes thermocup = (ThermocupAttributes) attributes;
            sb.append("\n🧴 Thermocup Attributes:\n")
            .append(String.format("• Volume: %d ml\n• Color: %s\n• Brand: %s\n• Model: %s\n• Hermetic: %s\n• Material: %s",
                    thermocup.getVolume_ml(), thermocup.getColor(), thermocup.getBrand(),
                    thermocup.getModel(), thermocup.getIs_hermetic() ? "Yes" : "No", thermocup.getMaterial()));
        } else if (attributes instanceof ServerAttributes) {
            ServerAttributes server = (ServerAttributes) attributes;
            sb.append("\n🖥️ Server Attributes:\n")
            .append(String.format("• RAM: %d GB\n• CPU: %s (%d cores)\n• HDD: %d GB\n• SSD: %d GB\n• Form: %s\n• Manufacturer: %s",
                    server.getRam_gb(), server.getCpu_model(), server.getCpu_cores(),
                    server.getHdd_size_gb(), server.getSsd_size_gb(), server.getForm_factor(), server.getManufacturer()));
        }
        
        return sb.toString();
    }
}