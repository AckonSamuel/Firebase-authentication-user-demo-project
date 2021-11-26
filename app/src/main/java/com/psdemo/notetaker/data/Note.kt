package com.psdemo.notetaker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity
data class Note(
    @PrimaryKey
    @get:Exclude
    var id: String,
    var title: String,
    var body: String,
    var date: Long,
    var announcement: Boolean
)