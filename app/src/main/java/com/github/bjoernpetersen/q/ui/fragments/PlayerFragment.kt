package com.github.bjoernpetersen.q.ui.fragments


import android.os.Bundle
import android.os.Looper
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.chauthai.swipereveallayout.SwipeRevealLayout
import com.github.bjoernpetersen.jmusicbot.client.ApiException
import com.github.bjoernpetersen.jmusicbot.client.model.PlayerState
import com.github.bjoernpetersen.q.R
import com.github.bjoernpetersen.q.api.ApiKeyListener
import com.github.bjoernpetersen.q.api.Auth
import com.github.bjoernpetersen.q.api.Connection
import com.github.bjoernpetersen.q.api.Permission
import com.github.bjoernpetersen.q.star.StarredAccess
import com.github.bjoernpetersen.q.tag
import com.github.bjoernpetersen.q.ui.ObserverUser
import com.github.bjoernpetersen.q.ui.isVisible
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_player.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class PlayerFragment : Fragment(), ObserverUser {

  override lateinit var observers: MutableList<WeakReference<Disposable>>
  private var apiKeyListener: ApiKeyListener? = null
  private var currentState: PlayerState? = null

  override fun initObservers() {
    observers = ArrayList()
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    // Inflate the layout for this fragment
    return inflater!!.inflate(R.layout.fragment_player, container, false)
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    skip_button.setOnClickListener { skip() }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    song_title.isSelected = true
    song_description.isSelected = true
    song_queuer.isSelected = true
  }

  override fun onStart() {
    super.onStart()
    initObservers()

    val apiKeyListener: ApiKeyListener = {
      view?.post {
        val canSkip = it?.permissions?.contains(Permission.SKIP) == true
        skip_frame.isVisible = canSkip
      }
    }
    Auth.registerListener(apiKeyListener)
    apiKeyListener(Auth.apiKeyNoRefresh)
    this.apiKeyListener = apiKeyListener
    star_button.setOnClickListener { starCurrent(); root.close(true) }
  }

  override fun onStop() {
    disposeObservers()
    apiKeyListener?.let { Auth.unregisterListener(it) }
    super.onStop()
  }

  private fun invokeStateUpdate() {
    val mainLooper = Looper.getMainLooper()
    val scheduler = Schedulers.newThread()
    scheduler.schedulePeriodicallyDirect({
      Observable.fromCallable { Connection.getPlayerState() }
          .subscribeOn(scheduler)
          .observeOn(AndroidSchedulers.from(mainLooper))
          .subscribe(
              { if (isVisible) updatedState(it) else scheduler.shutdown() },
              { Log.v(tag(), "Error updating player fragment", it) }
          )
    }, 0, 2, TimeUnit.SECONDS)
  }

  override fun onResume() {
    super.onResume()
    invokeStateUpdate()
  }

  private fun pause() {
    Observable.fromCallable { Connection.pausePlayer() }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe({ updatedState(it) }, { Log.d(tag(), "Could not pause player", it) })
        .store()
  }

  private fun play() {
    Observable.fromCallable { Connection.resumePlayer() }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe({ updatedState(it) }, { Log.d(tag(), "Could not resume player", it) })
        .store()
  }

  private fun skip() {
    val close: (SwipeRevealLayout) -> Unit = {
      skip_progress.clearAnimation()
      skip_progress.isVisible = false
      skip_button.isVisible = true
      it.setLockDrag(false)
    }

    val swipeView = root
    skip_button.isVisible = false
    skip_progress.isVisible = true
    skip_progress.animate()

    swipeView.open(true)
    swipeView.setLockDrag(true)

    Single.fromCallable { Auth.apiKey }
        .subscribeOn(Schedulers.io())
        .map { it.raw }
        .map { Connection.nextSong(it) }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          updatedState(it)
          close(swipeView)
        }, {
          Log.d(this@PlayerFragment.tag(), "Skipping song failed", it)
          Toast.makeText(context, R.string.skip_error, Toast.LENGTH_SHORT).show()
          if (it is ApiException && it.code == 403) Auth.clear()
          close(swipeView)
        }) // TODO store?
  }

  private fun starCurrent() {
    currentState?.let { state ->
      StarredAccess.instance(context.applicationContext)
          .add(state.songEntry.song)
          .subscribe({
            Log.d(tag(), "Success")
          }, {
            Log.d(tag(), "Error", it)
          })
    }
  }

  private fun updatedState(playerState: PlayerState) {
    currentState = playerState
    // update play/pause button
    val state = playerState.state
    val playPause = play_pause
    if (state == PlayerState.StateEnum.PLAY) {
      // show pause button
      playPause.setImageResource(R.drawable.ic_pause_circle_filled)
      playPause.setOnClickListener { pause() }
    } else {
      // show play button
      playPause.setImageResource(R.drawable.ic_play_circle_filled)
      playPause.setOnClickListener { play() }
    }

    // update current entry info
    val title = song_title
    val description = song_description
    val duration = song_duration
    val queuer = song_queuer

    val songEntry = playerState.songEntry
    if (songEntry == null) {
      title.text = ""
      description.text = ""
      duration.text = ""
      queuer.text = ""
      return
    }

    val song = songEntry.song
    if (title.text != song.title) {
      title.text = song.title
    }
    val songDescription = song.description
    if (description.text != songDescription) {
      description.text = songDescription
    }
    if (songDescription.isEmpty()) {
      description.visibility = View.GONE
    } else {
      description.visibility = View.VISIBLE
    }
    val durationSeconds = song.duration!!
    if (durationSeconds > 0) {
      duration.visibility = View.VISIBLE
      val seconds = durationSeconds % 60
      val minutes = (durationSeconds - seconds) / 60
      duration.text = String.format(Locale.US, "%d:%02d", minutes, seconds)
    } else {
      duration.visibility = View.GONE
    }

    val userName = songEntry.userName
    if (queuer.text != userName) {
      if (userName == null) {
        queuer.text = getString(R.string.suggested)
      } else {
        queuer.text = userName
      }
    }

    val albumArtUrl = song.albumArtUrl
    val albumArt = album_art
    Picasso.with(context)
        .load(albumArtUrl)
        .placeholder(R.drawable.ic_broken_image)
        .into(albumArt)
  }
}
