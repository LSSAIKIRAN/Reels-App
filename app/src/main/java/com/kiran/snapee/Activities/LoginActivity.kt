package com.kiran.snapee.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.kiran.snapee.MainActivity
import com.kiran.snapee.R
import com.kiran.snapee.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.SignupTxt.setOnClickListener {
            startActivity(Intent(this,SignUpActivity::class.java))
            finish()
        }


        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        binding.SigninBtn.setOnClickListener {
            login()
        }


    }

    private fun setInProgress(inProgress : Boolean){
        if(inProgress){
            binding.progressBar.visibility = View.VISIBLE
            binding.SigninBtn.visibility = View.GONE
        }else{
            binding.progressBar.visibility = View.GONE
            binding.SigninBtn.visibility = View.VISIBLE
        }
    }


    private fun login() {
        val email = binding.Email.text.toString()
        val password = binding.Password.text.toString()

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.Email.setError("Enter a valid email")
            return;
        }
        if(password.length<6){
            binding.Password.setError("incorrect")
            return
        }

        loginWithFirebase(email,password)
    }

    private fun loginWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
            email,
            password
        ).addOnSuccessListener {
            Toast.makeText(this, "Successfully", Toast.LENGTH_SHORT).show()
            setInProgress(false)
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            setInProgress(false)
        }
    }
}