package com.example.calenderapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calenderapp.R
import com.google.api.services.calendar.model.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(var listEvent: List<Event>): RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvEventDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(itemView)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newEventList: List<Event>) {
        listEvent = newEventList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val currentItem = listEvent[position]
        holder.tvEventTitle.text = currentItem.summary
        holder.tvEventDescription.text = currentItem.description


        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
        val startDate = dateFormat.format(currentItem.start.dateTime.value)
        holder.tvEventDate.text = startDate


        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTime = timeFormat.format(currentItem.start.dateTime.value)
        val endTime = timeFormat.format(currentItem.end.dateTime.value)
        val eventTime = "$startTime - $endTime"
        holder.tvEventTime.text = eventTime
    }

    override fun getItemCount(): Int {
        return listEvent.size
    }
}