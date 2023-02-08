package com.example.batchtest

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.example.batchtest.databinding.FragmentLoginBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.Executor

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth //authentication variable
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var email: String //email
    private lateinit var password: String //password

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)

        val user = auth.currentUser
        if (user != null) {
            Log.i("print", "user is signed in")
            Log.i("print", user.email.toString())
        } else {
            Log.i("print", "user is signed out")
        }

        binding.fragmentLoginLoginBtn.setOnClickListener {
            email = binding.fragmentLoginUsernameEt.text.toString()
            password = binding.fragmentLoginPasswordEt.text.toString()
            /* The sign In function seems to work but is having trouble connecting to firebase
            For now, the login button continues to the next fragment without logging in.
             */
            signIn(email, password)
        }
        binding.fragmentLoginSignOutBtn.setOnClickListener {
            auth.signOut()
            val user = auth.currentUser
            if (user != null) {
                Log.i("print", "user is signed in")
                Log.i("print", user.email.toString())
            } else {
                Log.i("print", "user is signed out")
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*
    This is the sign in function but is not working correctly yet
     */
    private fun signIn(email: String, password: String) {
        activity?.let {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(it) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i("print", "signInWithEmail:success")
                        findNavController().navigate(R.id.action_loginFragment_to_matchTabFragment)
                        val user = auth.currentUser
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.i("print", "signInWithEmail:failure", task.exception)
                        Toast.makeText(activity, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


}