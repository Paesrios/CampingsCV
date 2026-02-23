package com.example.campingscv

import android.content.res.Resources
import org.json.JSONObject

data class Camping(
    val name: String,
    val municipality: String,
    val province: String,
    val category: String,
    val places: Int,
    val address: String
)

object CampingRepository {
    fun loadFromRaw(resources: Resources, rawResourceId: Int): List<Camping> {
        val jsonFileContent = readJsonFromRaw(resources, rawResourceId)
        val jsonObject = JSONObject(jsonFileContent)
        val jsonResult = jsonObject.optJSONObject("result") ?: return emptyList()
        val records = jsonResult.optJSONArray("records") ?: return emptyList()

        val campings = ArrayList<Camping>(records.length())
        for (i in 0 until records.length()) {
            val item = records.optJSONObject(i) ?: continue
            val name = item.getStringOrBlank(listOf("Nombre", "nom", "NOM", "nombre", "NOMBRE"))
            val municipality = item.getStringOrBlank(listOf("Municipio", "municipi", "MUNICIPI", "municipio", "MUNICIPIO"))
            val province = item.getStringOrBlank(listOf("Provincia", "provincia", "PROVINCIA"))
            val category = item.getStringOrBlank(listOf("Categoria", "categoria", "CATEGORIA"))
            val places = item.getIntOrZero(listOf("Plazas", "places", "PLACES"))
            val address = item.getStringOrBlank(listOf("Direccion", "direccion", "DIRECCION", "adreca", "ADRECA"))

            campings.add(
                Camping(
                    name = name,
                    municipality = municipality,
                    province = province,
                    category = category,
                    places = places,
                    address = address
                )
            )
        }
        return campings
    }

    private fun readJsonFromRaw(resources: Resources, rawResourceId: Int): String {
        val inputStream = resources.openRawResource(rawResourceId)
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        return String(buffer, Charsets.UTF_8)
    }

    private fun JSONObject.getStringOrBlank(keys: List<String>): String {
        for (key in keys) {
            val value = optString(key).trim()
            if (value.isNotEmpty()) {
                return value
            }
        }
        return ""
    }

    private fun JSONObject.getIntOrZero(keys: List<String>): Int {
        for (key in keys) {
            if (!isNull(key)) {
                val intValue = optInt(key, Int.MIN_VALUE)
                if (intValue != Int.MIN_VALUE) {
                    return intValue
                }
                val textValue = optString(key).trim()
                val parsed = textValue.toIntOrNull()
                if (parsed != null) {
                    return parsed
                }
            }
        }
        return 0
    }
}
