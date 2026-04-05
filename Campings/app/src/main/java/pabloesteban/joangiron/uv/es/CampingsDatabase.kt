package pabloesteban.joangiron.uv.es

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteCampingEntity::class], version = 1, exportSchema = false)
abstract class CampingsDatabase : RoomDatabase() {
    abstract fun favoriteCampingDao(): FavoriteCampingDao

    companion object {
        @Volatile
        private var INSTANCE: CampingsDatabase? = null

        fun getDatabase(context: Context): CampingsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampingsDatabase::class.java,
                    "campings_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
