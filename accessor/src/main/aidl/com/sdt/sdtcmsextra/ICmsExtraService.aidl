// ICmsExtraService.aidl
package com.sdt.sdtcmsextra;

// Declare any non-default types here with import statements
interface ICmsExtraService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    double getCpuUsage();

    double getCpuTemp();

    boolean isHdmiPlugged();

    boolean isHdmiEnabled();

    int setHdmiStatus(boolean isEnable);

    String getHdmiSupportResolution();

    String getHdmiResolutionValue();

    int setHdmiResolutionValue(String mode);

    boolean isBestOutputMode();

    boolean isHdmiCecSupport();

    String getHdmiProductName();

    String getHdmiEdidVersion();

}