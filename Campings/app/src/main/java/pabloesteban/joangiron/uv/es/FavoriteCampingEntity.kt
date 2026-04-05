package pabloesteban.joangiron.uv.es

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_campings")
data class FavoriteCampingEntity(
    @PrimaryKey val id: Int,
    val signatura: String,
    val estado: String,
    val modalidad: String,
    val nombre: String,
    val categoria: String,
    val municipio: String,
    val provincia: String,
    val cp: String,
    val direccion: String,
    val tipoVia: String,
    val via: String,
    val numero: String,
    val email: String,
    val periodo: String,
    val numParcelas: Int,
    val numBungalows: Int,
    val plazas: Int,
    val web: String
)
