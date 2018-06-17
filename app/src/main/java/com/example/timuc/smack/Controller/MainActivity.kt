package com.example.timuc.smack.Controller

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.example.timuc.smack.Model.Channel
import com.example.timuc.smack.R
import com.example.timuc.smack.Services.AuthService
import com.example.timuc.smack.Services.MessageService
import com.example.timuc.smack.Services.UserDataService
import com.example.timuc.smack.Utilities.BROADCAST_USER_DATA_CHANGE
import com.example.timuc.smack.Utilities.SOCKET_URL
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity(){

    val socket = IO.socket(SOCKET_URL)
    lateinit var channelAdapter: ArrayAdapter<Channel>

    private fun setupAdapters(){
        channelAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, MessageService.channels)
        channel_list.adapter = channelAdapter
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        socket.connect()
        socket.on("channelCreated", onNewChannel)
        setupAdapters()


    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(userDataChangeReciver, IntentFilter(BROADCAST_USER_DATA_CHANGE))
        super.onResume()

    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userDataChangeReciver)
        socket.disconnect()
        super.onDestroy()
    }

    private val userDataChangeReciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if(AuthService.isLoggedIn){
                usernameNavHeader.text = UserDataService.name
                useremailNavHeader.text = UserDataService.email
                val resourceId = resources.getIdentifier(UserDataService.avatarName, "drawable", packageName)
                userimageNavHeader.setImageResource(resourceId)
                userimageNavHeader.setBackgroundColor(UserDataService.returnAvatarColor(UserDataService.avatarColor))
                loginBtnNavHeader.text = "Logout"

                MessageService.getChannels(context){complete ->
                    if (complete){
                        channelAdapter.notifyDataSetChanged()
                    }

                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun loginBtnNavClicked(view: View){

        if(AuthService.isLoggedIn){

            UserDataService.logout()
            usernameNavHeader.text= "Please Login"
            useremailNavHeader.text= ""
            userimageNavHeader.setImageResource(R.drawable.profiledefault)
            userimageNavHeader.setBackgroundColor(Color.TRANSPARENT)
            loginBtnNavHeader.text = "Login"

        }else{
            val loginIntend = Intent(this, LoginActivity::class.java)
            startActivity(loginIntend)
        }
    }

    fun addChannelClicked(view: View){

        if(AuthService.isLoggedIn){
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.add_channel_dialog, null)

            builder.setView(dialogView)
                    .setPositiveButton("add"){dialog, which ->
                        //perform some logic when Cliced
                        val nameTextField = dialogView.findViewById<EditText>(R.id.addChannelNameTxt)
                        val descTextField = dialogView.findViewById<EditText>(R.id.addChannelDescTxt)
                        val channelName = nameTextField.text.toString()
                        val channelDesc = descTextField.text.toString()

                        // Create channel wiuth the channel name and description
                        socket.emit("newChannel", channelName, channelDesc)
                    }
                    .setNegativeButton("Cancel"){dialog, which ->
                        // Cancel and close the dalog

                    }
                    .show()
        } else {
            Toast.makeText(this, "please login before creating a channel", Toast.LENGTH_LONG).show()
        }

    }

    private val onNewChannel = Emitter.Listener{args ->
        runOnUiThread {
            val channelName = args[0] as String
            val channelDescription = args[1] as String
            val channelId = args[2] as String

            MessageService.channels.add(Channel(channelName, channelDescription, channelId))
        }
    }


    fun sendMessageBtnClicked(view: View){
        hideKeyboard()
    }

    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if(inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

}
