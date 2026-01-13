package com.example.dynamicmodule

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ModuleApi {
    fun getDisplayString(deviceName: String): String {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return "$deviceName - $date"
    }
}
