package pabloesteban.joangiron.uv.es

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCampingDao {
    @Query("SELECT * FROM favorite_campings ORDER BY nombre ASC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteCampingEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_campings WHERE id = :campingId)")
    fun isFavoriteFlow(campingId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(camping: FavoriteCampingEntity)

    @Query("DELETE FROM favorite_campings WHERE id = :campingId")
    suspend fun deleteFavoriteById(campingId: Int)
}
