package com.scc.model;

public record Rental(
        int rentalId,
        int userId,
        int vehicleId,
        String customerName,
        String vehicleDetails,
        String rentalType,
        int rentalDays,
        double dailyRate,
        double totalAmount,
        String returnDate,
        String status,
        String timestamp
) {
    public static double calculateTotal(double dailyRate, int rentalDays) {
        return Math.max(0, dailyRate) * Math.max(1, rentalDays);
    }
}