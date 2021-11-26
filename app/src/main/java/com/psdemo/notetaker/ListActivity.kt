package com.psdemo.notetaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.psdemo.notetaker.data.Note
import com.psdemo.notetaker.data.NoteViewModel
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.content_list.*
import java.util.*

class ListActivity : AppCompatActivity() {
    private val TAG = ListActivity::class.qualifiedName
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var adapter: ListRecyclerAdapter
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var firestoreNotesListener: ListenerRegistration
    private lateinit var announcementsCollection: CollectionReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        setSupportActionBar(toolbar)

        userId = intent.getStringExtra(MainActivity.USER_ID)

        fab.setOnClickListener {
            val activityIntent = Intent(this, NewNoteActivity::class.java)
            startActivityForResult(activityIntent, NEW_NOTE_ACTIVITY_REQUEST_CODE)
        }

        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_sync -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NEW_NOTE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val id = UUID.randomUUID().toString()
            val title = data!!.getStringExtra(NewNoteActivity.NEW_TITLE)
            val body = data.getStringExtra(NewNoteActivity.NEW_BODY)

            val note = Note(id, title, body, Calendar.getInstance().timeInMillis, false)

            if (userId == "-1") {
                noteViewModel.insert(note)
            } else {
                addNoteToFirestore(note, firestoreDB.collection(userId))
            }

            Toast.makeText(
                applicationContext,
                R.string.saved,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                applicationContext,
                R.string.not_saved,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //region data code
    private fun parseDocument(document: DocumentSnapshot): Note {
        return Note(
            document.id,
            document.getString("title")!!,
            document.getString("body")!!,
            document.getLong("date")!!,
            document.getBoolean("announcement")!!
        )
    }

    private fun addNoteToFirestore(note: Note, collection: CollectionReference) {
        collection
            .add(note)
            .addOnSuccessListener { result ->
                Log.d(TAG, "Note added with ID:" + result.id)
                if (note.announcement) {
                    adapter.addNote(note)
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Error adding note", e) }
    }

    private fun loadData() {
        noteViewModel = ViewModelProviders.of(this).get(NoteViewModel::class.java)
        adapter = ListRecyclerAdapter(this)
        listNotes.layoutManager = LinearLayoutManager(this)
        listNotes.adapter = adapter

        firestoreDB = FirebaseFirestore.getInstance()
        announcementsCollection = firestoreDB.collection("announcements")

        announcementsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving announcements")
                val announcementsList = ArrayList<Note>()
                for (document in result) {
                    announcementsList.add(parseDocument(document))
                }

                if (announcementsList.size == 0) {
                    seedAnnouncements()
                } else {
                    adapter.addNotes(announcementsList)
                    loadNotes()
                }

            }
            .addOnFailureListener { e -> Log.e(TAG, "Error getting announcements", e) }

    }

    private fun seedAnnouncements() {
        var note =
            Note(
                "",
                "Welcome to Note Taker",
                "This is a great way to learn about Firebase Authentication",
                Calendar.getInstance().timeInMillis,
                true
            )

        addNoteToFirestore(note, announcementsCollection)

        note = Note(
            "",
            "Pluralsight",
            "This is one of many great Pluralsight courses on Android",
            Calendar.getInstance().timeInMillis - 10000,
            true
        )

        addNoteToFirestore(note, announcementsCollection)
    }

    private fun loadNotes() {
        if (userId == "-1") {
            noteViewModel.allNotes.observe(this, Observer { notes ->
                notes?.let {
                    adapter.clearNotes(false)
                    adapter.addNotes(notes)
                }
            })
        } else {
            firestoreNotesListener = firestoreDB.collection(userId)
                .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for new notes", e)
                        return@EventListener
                    }

                    for (dc in snapshots!!.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            adapter.addNote(parseDocument(dc.document))
                        }
                    }
                })
        }
    }

    //endregion

    companion object {
        private const val NEW_NOTE_ACTIVITY_REQUEST_CODE = 1


    }
}
