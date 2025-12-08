package com.sentinel.user_management_service.enums;

public enum UserPlan {
    FREE(3, 5, 10, 100),
    STANDARD(5, 10, 20, 500),
    PRO(10, 25, 50, 1000),
    ENTERPRISE(999, 999, 999, 999999);

    private final int maxTenants;
    private final int maxProjectsPerTenant;
    private final int maxUsersPerTenant;
    private final int maxScansPerMonth;

    UserPlan(int maxTenants, int maxProjectsPerTenant, int maxUsersPerTenant, int maxScansPerMonth) {
        this.maxTenants = maxTenants;
        this.maxProjectsPerTenant = maxProjectsPerTenant;
        this.maxUsersPerTenant = maxUsersPerTenant;
        this.maxScansPerMonth = maxScansPerMonth;
    }

    public int getMaxTenants() {
        return maxTenants;
    }

    public int getMaxProjectsPerTenant() {
        return maxProjectsPerTenant;
    }

    public int getMaxUsersPerTenant() {
        return maxUsersPerTenant;
    }

    public int getMaxScansPerMonth() {
        return maxScansPerMonth;
    }
}