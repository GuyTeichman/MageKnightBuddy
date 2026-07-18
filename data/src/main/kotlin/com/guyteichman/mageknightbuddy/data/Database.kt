package com.guyteichman.mageknightbuddy.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createDatabase(context: Context): MageKnightBuddyDatabase =
    Room.databaseBuilder(context, MageKnightBuddyDatabase::class.java, "mageknightbuddy.db")
        .setDriver(BundledSQLiteDriver())
        .build()
