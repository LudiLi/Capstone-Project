package edu.cmu.capstone16fall.pe.dataservice;

import android.net.wifi.ScanResult;

/**
 * Created by Zheng on 10/8/16.
 */

/**
 * A helper class to store all detected wifi information and the wifi information with the
 * strongest signal.
 *
 */
public class WifiReturnType {
    ScanResult[] scanResult;
    ScanResult maxScanResult;

    public WifiReturnType(ScanResult[] scanResult, ScanResult maxScanResult) {
        this.scanResult = scanResult;
        this.maxScanResult = maxScanResult;
    }
}
