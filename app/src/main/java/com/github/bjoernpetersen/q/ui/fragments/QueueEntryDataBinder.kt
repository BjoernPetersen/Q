package com.github.bjoernpetersen.q.ui.fragments

import android.content.Context
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.bjoernpetersen.jmusicbot.client.ApiException
import com.github.bjoernpetersen.jmusicbot.client.model.QueueEntry
import com.github.bjoernpetersen.q.QueueState
import com.github.bjoernpetersen.q.R
import com.github.bjoernpetersen.q.api.Auth
import com.github.bjoernpetersen.q.api.Connection
import com.github.bjoernpetersen.q.api.Permission
import com.github.bjoernpetersen.q.api.action.MoveSong
import com.github.bjoernpetersen.q.api.action.onMainThread
import com.github.bjoernpetersen.q.tag
import com.github.bjoernpetersen.q.ui.SearchActivity
import com.github.bjoernpetersen.q.ui.asDuration
import com.squareup.picasso.Callback.EmptyCallback
import com.squareup.picasso.Picasso
import com.yqritc.recyclerviewmultipleviewtypesadapter.DataBindAdapter
import com.yqritc.recyclerviewmultipleviewtypesadapter.DataBinder
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_queue.view.*
import java.util.*

class QueueEntryDataBinder(adapter: DataBindAdapter, private val listener: QueueEntryListener?) :
    DataBinder<QueueEntryDataBinder.ViewHolder>(adapter) {

  private var _items: MutableList<QueueEntry> = ArrayList()
  var items: List<QueueEntry>
    get() = Collections.unmodifiableList(_items)
    set(value) {
      val items = _items
      items.clear()
      items.addAll(value)
      notifyDataSetChanged()
    }

  override fun newViewHolder(parent: ViewGroup): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.fragment_queue, parent, false)
    return ViewHolder(view)
  }

  override fun bindViewHolder(holder: ViewHolder, position: Int) {
    val entry = getItem(position)
    if (entry == holder.entry) {
      return
    }
    holder.entry = entry
    val song = entry.song

    val albumArtView = holder.albumArtView
    Picasso.with(holder.context).cancelRequest(holder.albumArtView)
    holder.albumArtView.visibility = View.GONE
    Picasso.with(albumArtView.context)
        .load(song.albumArtUrl)
        .into(holder.albumArtView, object : EmptyCallback() {
          override fun onSuccess() {
            albumArtView.visibility = View.VISIBLE
          }
        })
    holder.titleView.setContent(song.title)
    holder.descriptionView.setContent(song.description)
    holder.durationView.setContent(song.duration.asDuration())
    holder.queuerView.setContent(entry.userName)

    holder.view.setOnClickListener {
      holder.entry?.let { listener?.onClick(it) }
    }

    val menu = PopupMenu(holder.view.context, holder.contextMenu)
    menu.inflate(R.menu.query_item_menu)
    menu.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.move -> {
          val choices = QueueState.queue
              .map { it.song }
              .filter { it != song }
              .map { it.title }
              .toTypedArray()
          AlertDialog.Builder(holder.context)
              .setCancelable(true)
              .setTitle(R.string.move_confirm)
              .setNegativeButton(android.R.string.cancel, { dialog, _ -> dialog.dismiss() })
              .setItems(choices, { _, which ->
                MoveSong(entry, which).defaultAction(holder.context)
              })
              .show()
          true
        }
        R.id.search_related -> {
          val intent = Intent(holder.context, SearchActivity::class.java)
              .putExtra(SearchActivity.INITIAL_QUERY_EXTRA, holder.entry?.song?.description)
              .putExtra(SearchActivity.INITIAL_PROVIDER_EXTRA, holder.entry?.song?.provider?.id)
          holder.context.startActivity(intent)
          true
        }
        R.id.remove_button -> {
          Observable.fromCallable { Auth.apiKey }
              .subscribeOn(Schedulers.io())
              .map { it.raw }
              .map { Connection.dequeue(it, song.id, song.provider.id) }
              .onMainThread()
              .subscribe({
                QueueState.queue = it
              }, {
                Toast.makeText(holder.context, R.string.remove_error, Toast.LENGTH_SHORT).show()
                Log.d(tag(), "Could not remove song from queue", it)
                when (it) {
                  is ApiException -> if (it.code == 403) {
                    Auth.clear()
                  }
                }
              })
          true
        }
        else -> false
      }
    }
    holder.contextMenu.setOnClickListener {
      val apiKey = Auth.apiKeyNoRefresh
      val items = menu.menu
      items.findItem(R.id.move).isVisible = Auth.hasPermissionNoRefresh(Permission.MOVE)
      items.findItem(R.id.search_related).isVisible
      items.findItem(R.id.remove_button).isVisible =
          apiKey != null && apiKey.userName == entry.userName
              || Auth.hasPermissionNoRefresh(Permission.SKIP)
      menu.show()
    }
  }

  private fun TextView.setContent(content: String?) {
    if (content.isNullOrBlank()) {
      visibility = View.GONE
    } else {
      visibility = View.VISIBLE
      text = content
    }
  }

  override fun getItemCount(): Int = items.size

  internal fun getItem(position: Int): QueueEntry = items[position]

  class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val albumArtView: ImageView = view.album_art
    val titleView: TextView = view.song_title
    val descriptionView: TextView = view.song_description
    val durationView: TextView = view.song_duration
    val queuerView: TextView = view.song_queuer
    val contextMenu: ImageButton = view.context_menu
    var entry: QueueEntry? = null
    val context: Context
      get() = view.context

    init {
      this.descriptionView.isSelected = true
      this.titleView.isSelected = true
    }

    override fun toString(): String = "${super.toString()} '${titleView.text}'"
  }

  interface QueueEntryListener {
    fun onClick(entry: QueueEntry)
  }
}
