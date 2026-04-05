package pabloesteban.joangiron.uv.es

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

private const val CAMPINGS_RESOURCE_ID = "2ddaf823-5da4-4459-aa57-5bfe9f9eb474"

interface OpenDataService {
    @Headers("Accept: application/json")
    @GET("api/3/action/datastore_search")
    suspend fun getCampings(
        @Query("id") resourceId: String = CAMPINGS_RESOURCE_ID
    ): OpenDataResponse
}

object OpenDataApi {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://dadesobertes.gva.es/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: OpenDataService = retrofit.create(OpenDataService::class.java)
}

data class OpenDataResponse(
    @Json(name = "result") val result: OpenDataResult
)

data class OpenDataResult(
    @Json(name = "records") val records: List<CampingDto>
)

data class CampingDto(
    @Json(name = "_id") val id: Int?,
    @Json(name = "Signatura") val signatura: String?,
    @Json(name = "Estado") val estado: String?,
    @Json(name = "Modalidad") val modalidad: String?,
    @Json(name = "Nombre") val nombre: String?,
    @Json(name = "Categoria") val categoria: String?,
    @Json(name = "Municipio") val municipio: String?,
    @Json(name = "Provincia") val provincia: String?,
    @Json(name = "CP") val cp: Any?,
    @Json(name = "Direccion") val direccion: String?,
    @Json(name = "Tipo via") val tipoVia: String?,
    @Json(name = "Via") val via: String?,
    @Json(name = "Numero") val numero: String?,
    @Json(name = "Email") val email: String?,
    @Json(name = "Periodo") val periodo: String?,
    @Json(name = "Num. Parcelas") val numParcelas: Any?,
    @Json(name = "Num. Bungalows") val numBungalows: Any?,
    @Json(name = "Plazas") val plazas: Any?,
    @Json(name = "Web") val web: String?
)

fun CampingDto.toCamping(): Camping {
    return Camping(
        id = id ?: 0,
        signatura = signatura.orEmpty(),
        estado = estado.orEmpty(),
        modalidad = modalidad.orEmpty(),
        nombre = nombre ?: "Sin nombre",
        categoria = categoria ?: "Sin categoria",
        municipio = municipio ?: "Sin municipio",
        provincia = provincia ?: "Sin provincia",
        cp = cp.toText(),
        direccion = direccion.orEmpty(),
        tipoVia = tipoVia.orEmpty(),
        via = via.orEmpty(),
        numero = numero.orEmpty(),
        email = email.orEmpty(),
        periodo = periodo.orEmpty(),
        numParcelas = numParcelas.toIntValue(),
        numBungalows = numBungalows.toIntValue(),
        plazas = plazas.toIntValue(),
        web = web.orEmpty()
    )
}

private fun Any?.toText(): String = when (this) {
    null -> ""
    is String -> this
    else -> this.toString()
}

private fun Any?.toIntValue(): Int = when (this) {
    null -> 0
    is Int -> this
    is Double -> this.toInt()
    is Long -> this.toInt()
    is String -> this.toIntOrNull() ?: 0
    else -> this.toString().toIntOrNull() ?: 0
}
