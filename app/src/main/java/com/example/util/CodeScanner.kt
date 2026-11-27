package com.example.util

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Camera scanner for IMEI and product barcodes (Google code scanner: no camera permission needed). */
object CodeScanner {
    fun scan(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
        try {
            GmsBarcodeScanning.getClient(context)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val value = barcode.rawValue?.trim().orEmpty()
                    if (value.isNotEmpty()) onResult(value)
                }
                .addOnFailureListener { e -> onError(e.message ?: "") }
        } catch (e: Exception) {
            onError(e.message ?: "")
        }
    }
}
