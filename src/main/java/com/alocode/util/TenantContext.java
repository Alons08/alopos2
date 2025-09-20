package com.alocode.util;

public class TenantContext {
    
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    
    public static void setCurrentTenant(Long tenantId) {
    CURRENT_TENANT.set(tenantId);
    }
    
    public static Long getCurrentTenant() {
    Long tid = CURRENT_TENANT.get();
    return tid;
    }
    
    public static void clear() {
    CURRENT_TENANT.remove();
    }
}