package com.scc.model;

public record Rental(int rentalId, int userId, String customerName, String vehicleDetails, String rentalType, double totalAmount, String status, String timestamp) {}