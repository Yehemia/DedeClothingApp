package com.dedeclothingstore.model;
import javafx.beans.property.*;

public class Product {
    private final IntegerProperty productId;
    private final StringProperty name;
    private final StringProperty sku;
    private final StringProperty category;
    private final StringProperty size;
    private final StringProperty color;
    private final DoubleProperty price;
    private final IntegerProperty stockQuantity;
    private final StringProperty supplier;
    private final StringProperty entryDate;

    public Product(int productId, String name, String category, double price, int stockQuantity) {
        this.productId = new SimpleIntegerProperty(productId);
        this.name = new SimpleStringProperty(name);
        this.sku = new SimpleStringProperty("");
        this.category = new SimpleStringProperty(category);
        this.size = new SimpleStringProperty("");
        this.color = new SimpleStringProperty("");
        this.price = new SimpleDoubleProperty(price);
        this.stockQuantity = new SimpleIntegerProperty(stockQuantity);
        this.supplier = new SimpleStringProperty("");
        this.entryDate = new SimpleStringProperty("");
    }
    public Product(int productId, String name, String sku, String category, String size,
                   String color, double price, int stockQuantity, String supplier,
                   String entryDate) {
        this.productId = new SimpleIntegerProperty(productId);
        this.name = new SimpleStringProperty(name);
        this.sku = new SimpleStringProperty(sku);
        this.category = new SimpleStringProperty(category);
        this.size = new SimpleStringProperty(size);
        this.color = new SimpleStringProperty(color);
        this.price = new SimpleDoubleProperty(price);
        this.stockQuantity = new SimpleIntegerProperty(stockQuantity);
        this.supplier = new SimpleStringProperty(supplier);
        this.entryDate = new SimpleStringProperty(entryDate);
    }

    public int getProductId() { return productId.get(); }
    public String getName() { return name.get(); }
    public String getSku() { return sku.get(); }
    public String getCategory() { return category.get(); }
    public String getSize() { return size.get(); }
    public String getColor() { return color.get(); }
    public double getPrice() { return price.get(); }
    public int getStockQuantity() { return stockQuantity.get(); }
    public String getSupplier() { return supplier.get(); }
    public String getEntryDate() { return entryDate.get(); }

    public IntegerProperty productIdProperty() { return productId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty skuProperty() { return sku; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty sizeProperty() { return size; }
    public StringProperty colorProperty() { return color; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty stockQuantityProperty() { return stockQuantity; }
    public StringProperty supplierProperty() { return supplier; }
    public StringProperty entryDateProperty() { return entryDate; }

    // Dummy property for action column
    public String getDummy() { return ""; }
}