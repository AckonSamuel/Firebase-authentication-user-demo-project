package com.psdemo.notetaker

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.psdemo.notetaker.data.Note
import java.util.*

class ListRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<ListRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val notes = ArrayList<Note>()
    private val TAG = ListRecyclerAdapter::class.qualifiedName

    fun addNotes(newNotes: List<Note>) {
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    fun clearNotes(announcements: Boolean) {
        val newNotes = ArrayList<Note>()
        for (note in notes) {
            if (note.announcement != announcements) {
                newNotes.add(note)
            }
        }

        notes.clear()
        notes.addAll(newNotes)
    }

    fun addNote(newNote: Note) {
        notes.add(newNote)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        Log.d(TAG, "Binding note " + note.title)
        holder.textTitle.text = note.title
        holder.textBody.text = note.body
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = note.date
        holder.textDate.text = calendar.time.toString()
        if (note.announcement) {
            holder.cardNote.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.announcement_background_color
                )
            )
        }
    }

    override fun getItemCount() = notes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle = itemView.findViewById<TextView>(R.id.textTitle)
        val textBody = itemView.findViewById<TextView>(R.id.textBody)
        val textDate = itemView.findViewById<TextView>(R.id.textDate)
        val cardNote = itemView.findViewById<CardView>(R.id.cardNote)
    }
}