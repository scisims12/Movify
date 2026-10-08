package com.ivor.movify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ivor.movify.data.local.dao.CustomListDao
import com.ivor.movify.data.local.dao.DownloadDao
import com.ivor.movify.data.local.dao.HiddenTitleDao
import com.ivor.movify.data.local.dao.WatchLaterDao
import com.ivor.movify.data.local.dao.IdMappingDao
import com.ivor.movify.data.local.dao.ProfileDao
import com.ivor.movify.data.local.dao.WatchProgressDao
import com.ivor.movify.data.local.entity.CustomListEntity
import com.ivor.movify.data.local.entity.CustomListItemEntity
import com.ivor.movify.data.local.entity.DownloadEntity
import com.ivor.movify.data.local.entity.HiddenTitleEntity
import com.ivor.movify.data.local.entity.IdMappingEntity
import com.ivor.movify.data.local.entity.ProfileEntity
import com.ivor.movify.data.local.entity.WatchLaterEntity
import com.ivor.movify.data.local.entity.WatchProgressEntity

@Database(
    entities = [
        WatchLaterEntity::class,
        DownloadEntity::class,
        IdMappingEntity::class,
        WatchProgressEntity::class,
        HiddenTitleEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        ProfileEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun downloadDao(): DownloadDao
    abstract fun idMappingDao(): IdMappingDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun hiddenTitleDao(): HiddenTitleDao
    abstract fun customListDao(): CustomListDao
    abstract fun profileDao(): ProfileDao
}
