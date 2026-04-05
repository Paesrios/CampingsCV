package pabloesteban.joangiron.uv.es

import kotlinx.coroutines.flow.Flow

class FavoriteCampingRepository(
    private val dao: FavoriteCampingDao
) {
    val favoritesFlow: Flow<List<FavoriteCampingEntity>> = dao.getAllFavoritesFlow()

    fun isFavoriteFlow(campingId: Int): Flow<Boolean> = dao.isFavoriteFlow(campingId)

    suspend fun addFavorite(camping: Camping) {
        dao.insertFavorite(camping.toFavoriteEntity())
    }

    suspend fun removeFavorite(campingId: Int) {
        dao.deleteFavoriteById(campingId)
    }
}

fun Camping.toFavoriteEntity(): FavoriteCampingEntity = FavoriteCampingEntity(
    id = id,
    signatura = signatura,
    estado = estado,
    modalidad = modalidad,
    nombre = nombre,
    categoria = categoria,
    municipio = municipio,
    provincia = provincia,
    cp = cp,
    direccion = direccion,
    tipoVia = tipoVia,
    via = via,
    numero = numero,
    email = email,
    periodo = periodo,
    numParcelas = numParcelas,
    numBungalows = numBungalows,
    plazas = plazas,
    web = web
)

fun FavoriteCampingEntity.toCamping(): Camping = Camping(
    id = id,
    signatura = signatura,
    estado = estado,
    modalidad = modalidad,
    nombre = nombre,
    categoria = categoria,
    municipio = municipio,
    provincia = provincia,
    cp = cp,
    direccion = direccion,
    tipoVia = tipoVia,
    via = via,
    numero = numero,
    email = email,
    periodo = periodo,
    numParcelas = numParcelas,
    numBungalows = numBungalows,
    plazas = plazas,
    web = web
)
