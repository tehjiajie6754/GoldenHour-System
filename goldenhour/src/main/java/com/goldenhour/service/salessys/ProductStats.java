package com.goldenhour.service.salessys;

public class ProductStats {
    
    String modelCode;
    int totalQty;
    double totalRevenue;

    public ProductStats(String modelCode, int totalQty, double totalRevenue) {
        this.modelCode = modelCode;
        this.totalQty = totalQty;
        this.totalRevenue = totalRevenue;
    }
    
    public String getModelCode() { return modelCode; }
    public int getTotalQty() { return totalQty; }
    public double getTotalRevenue() { return totalRevenue; }
}


