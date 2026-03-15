package dev.noctud.latte.settings;

import dev.noctud.latte.config.LatteConfiguration;

public class BaseLatteSettings {
    protected LatteConfiguration.Vendor vendor;
    protected String vendorName;

    BaseLatteSettings() {
        this(LatteConfiguration.Vendor.CUSTOM, "");
    }

    BaseLatteSettings(LatteConfiguration.Vendor vendor, String vendorName) {
        this.vendor = vendor;
        this.vendorName = vendorName;
    }

    public LatteConfiguration.Vendor getVendor() {
        return vendor;
    }

    protected BaseLatteSettings setVendor(LatteConfiguration.Vendor vendor) {
        this.vendor = vendor;
        return this;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }
}

