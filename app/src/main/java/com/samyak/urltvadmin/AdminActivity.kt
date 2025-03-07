package com.samyak.urltvadmin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.samyak.urltvadmin.databinding.ActivityAdminBinding
import com.samyak.urltvadmin.repository.ChannelRepository

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var adapter: ChannelAdapter
    private val channelList = mutableListOf<Channel>()
    private val allChannels = mutableListOf<Channel>()
    private val channelRepository = ChannelRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Manage Channels"

        // Setup RecyclerView
        adapter = ChannelAdapter(channelList, this)
        binding.recyclerViewChannels.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = this@AdminActivity.adapter
        }

        // Setup FAB click listener
        binding.fabAddChannel.setOnClickListener {
            startActivity(Intent(this, AddChannleActivity::class.java))
        }

        // Fetch channels
        fetchChannels()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        setupSearchView(searchView)
        return true
    }

    private fun setupSearchView(searchView: SearchView) {
        searchView.queryHint = getString(R.string.search_channels)
        
        // Make search text white
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.WHITE)
        searchEditText.setHintTextColor(Color.WHITE.withAlpha(0.7f))
        
        // Make close button white
        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.WHITE)
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChannels(newText)
                return true
            }
        })
    }

    private fun filterChannels(query: String?) {
        if (query.isNullOrBlank()) {
            channelList.clear()
            channelList.addAll(allChannels)
        } else {
            val filteredList = allChannels.filter { channel ->
                channel.name.contains(query, ignoreCase = true) ||
                channel.category.contains(query, ignoreCase = true) ||
                channel.link.contains(query, ignoreCase = true)
            }
            channelList.clear()
            channelList.addAll(filteredList)
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchChannels() {
        channelRepository.getChannelReference().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allChannels.clear()
                channelList.clear()
                
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(Channel::class.java)?.let { channel ->
                        allChannels.add(channel)
                        channelList.add(channel)
                    }
                }
                adapter.notifyDataSetChanged()
                
                // Show empty state if needed
                showEmptyState(channelList.isEmpty())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showError("Error fetching data")
            }
        })
    }

    fun editChannel(channel: Channel) {
        val dialog = EditChannelDialog(this, channel) { updatedChannel ->
            channelRepository.updateChannel(updatedChannel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Channel updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update channel", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        dialog.show()
    }

    private fun Int.withAlpha(alpha: Float): Int {
        return Color.argb((alpha * 255).toInt(), Color.red(this), Color.green(this), Color.blue(this))
    }

    private fun showLoading(show: Boolean) {
        // Add loading indicator if needed
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            // Show empty state view
            binding.recyclerViewChannels.visibility = View.GONE
            // binding.emptyStateView.visibility = View.VISIBLE
        } else {
            binding.recyclerViewChannels.visibility = View.VISIBLE
            // binding.emptyStateView.visibility = View.GONE
        }
    }
}