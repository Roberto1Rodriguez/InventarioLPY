package com.example.inventariolpy

import android.app.Activity
import android.os.Bundle

class NfcSinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // Solo captura la intención y se cierra
    }
}