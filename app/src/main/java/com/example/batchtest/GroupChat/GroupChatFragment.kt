package com.example.batchtest.GroupChat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.batchtest.*
import com.example.batchtest.EditGroupProfile.GroupInfoViewModel
import com.example.batchtest.databinding.FragmentGroupChatBinding
import com.example.batchtest.myGroupsTab.MyGroupFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class GroupChatFragment : Fragment() {

    //binding variables
    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    //ArrayList for messages
    private var messagesArrayList: ArrayList<Message> = ArrayList()

    //groups that are a part of this chat
    private var myGroupName: String? = null
    private lateinit var theirGroupName: String

    //Firebase auth
    private var currUser: FirebaseUser? = null

    //Chat Reference Firebase
    private var chatId: String? = null

    //max number of messages allowed in recyclerview
    private val maximumNumberOfMessages: Int = 300

    //access share view model for group name
    private val sharedViewModel: GroupInfoViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currUser = Firebase.auth.currentUser
        // Use the Kotlin extension in the fragment-ktx artifact
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)

        val db = Firebase.firestore

        val groupChatRV = binding.fragmentGroupChatRecyclerView
        //This will set the recyclerview layout to be like a chat
        //Starting bottom moving up
        groupChatRV.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
                reverseLayout = false
            }
        }
        //groupChatRV.layoutManager = LinearLayoutManager(context)
        groupChatRV.setHasFixedSize(true)

        //This will receive the information of which group you clicked on from the matchedGroupFragment
        val result = arguments?.getString("groupName")
        theirGroupName = result!!
        Log.i(TAG, theirGroupName)

        //Retrieves our group name for matched group chat
        myGroupName = arguments?.getString("myGroupName")
        Log.i(TAG, "Retrieved $myGroupName")

        //---------------------------------------------------
        /**
         * identify which fragment came previously for query
         */
        val previousFragmentName = arguments?.getString("currentFragmentName") //get the previous fragment that navigated here
        Log.i(TAG, "what fragment: $previousFragmentName") //get the group name that was selected to enter chat
        val currentGroupName = sharedViewModel.groupName.value.toString()
        Log.i(TAG, "group name: $currentGroupName")

        //--------------------------------------------------
        //QUERY THE CHAT FROM FIRESTORE!!!!!!!!!!!!!!!!!!!!!
        //if the previous fragment came from my group, query my groups
        if (previousFragmentName == "MyGroupFragment"){
            queryChatFromMyGroups(db, groupChatRV, currentGroupName)
        }
        else{
            //set the group chat title
            setGroupChatTitle(myGroupName!!)
            queryChatFromMatchedGroups(db, myGroupName!!, groupChatRV)
        }
        //QUERY THE CHAT FROM FIRESTORE!!!!!!!!!!!!!!!!!!!!!
        //--------------------------------------------------

        binding.fragmentGroupChatSendBtn.setOnClickListener {
            //create message Object from the edit text
            val message = Message(
                content = binding.fragmetGroupChatMessageEt.text.toString(),
                username = currUser?.email,
                createdDate = Date()
            )
            //if the message is nonempty, go ahead and write it to the database
            if(message.content?.isEmpty() != true) {
                //add the message object to our messages array
                messagesArrayList.add(message)
                writeMessageToFirestore(db)
            }
            //clear the edit text after sending a message
            binding.fragmetGroupChatMessageEt.text.clear()
            //scroll down to the newest message
            groupChatRV.scrollToPosition(messagesArrayList.size - 1)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * QUERY CHAT FROM myGroups
     * retrieve messages for specific group and display them in recyclerview
     *
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun queryChatFromMyGroups(db: FirebaseFirestore, groupChatRV: RecyclerView, currentGroupName: String) {
        db.collection("chats")
                .whereEqualTo("group1Name", currentGroupName)
                .whereEqualTo("group2Name", "")
                .addSnapshotListener { doc, exception ->
                    if (exception != null){
                        // handle the error
                        Log.i(TAG, "Listen failed.", exception)
                        return@addSnapshotListener
                    }

                    messagesArrayList.clear()
                    setMyGroupChatTitle(currentGroupName)

                    for (d in doc!!){
                        chatId = d.id
                        val chat: Chat = d.toObject<Chat>()
                        if(chat.messages.size > maximumNumberOfMessages) {
                            chat.messages.removeAt(0)
                        }
                        messagesArrayList.addAll(chat.messages)
                        if(groupChatRV.adapter == null) {
                            Log.i(TAG, "recycler view is null")
                            // attach adapter and send groups
                            val groupChatAdapter = GroupChatAdapter(messagesArrayList, requireActivity())
                            groupChatRV.adapter = groupChatAdapter
                            groupChatRV.scrollToPosition(messagesArrayList.size - 1)
                        }
                        else {
                            Log.i(TAG, "recycler view is not null")
                            groupChatRV.adapter?.notifyDataSetChanged()
                            groupChatRV.scrollToPosition(messagesArrayList.size - 1)
                        }
                    }
                }
    }

    /**
     * GET CHAT FOR matched group
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun queryChatFromMatchedGroups(db: FirebaseFirestore, myGroupName: String, groupChatRV: RecyclerView) {
        var chat = Chat()
        val chatsRef = db.collection("chats")
        chatsRef.whereIn("group1Name", listOf(myGroupName, theirGroupName))
            .whereIn("group2Name", listOf(myGroupName, theirGroupName))
            .addSnapshotListener { doc, e ->
                if(e != null) {
                    Log.i(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }
                messagesArrayList.clear()
                for (d in doc!!) {
                    chatId = d.id
                    chat = d.toObject<Chat>()
                    if(chat.messages.size > maximumNumberOfMessages) {
                        chat.messages.removeAt(0)
                    }
                    messagesArrayList.addAll(chat.messages)
                    Log.i(TAG, messagesArrayList.toString())
                    if(groupChatRV.adapter == null) {
                        // attach adapter and send groups
                        val groupChatAdapter = GroupChatAdapter(messagesArrayList, requireActivity())
                        groupChatRV.adapter = groupChatAdapter
                        groupChatRV.scrollToPosition(messagesArrayList.size - 1)
                    }
                    else {
                        Log.i(TAG, "recycler view is not null")
                        groupChatRV.adapter?.notifyDataSetChanged()
                        groupChatRV.scrollToPosition(messagesArrayList.size - 1)
                    }
                }
            }
    }

    //when user clicks send, it will write the message to firestore
    private fun writeMessageToFirestore(db: FirebaseFirestore) {
        val chatDocRef = chatId?.let { db.collection("chats").document(it) }
        chatDocRef?.update("messages", messagesArrayList)
    }

    //SET GROUP CHAT TITLE FOR MATCHED GROUP
    @SuppressLint("SetTextI18n")
    private fun setGroupChatTitle(myGroupName: String) {
        if(_binding != null) {
            binding.fragmentGroupChatTb.text = "$myGroupName/$theirGroupName"
        }
    }

    //SET GROUP CHAT TITLE FOR MY GROUP
    private fun setMyGroupChatTitle(myGroupName: String) {
        if(_binding != null) {
            binding.fragmentGroupChatTb.text = myGroupName
        }
    }

    companion object {
        const val TAG = "GroupChatFragment"
    }

}