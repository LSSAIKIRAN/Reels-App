package com.kiran.snapee.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kiran.snapee.MainActivity
import com.kiran.snapee.ModelClasses.UserModel
import com.kiran.snapee.R
import com.kiran.snapee.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.SigninTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        binding.SignupBtn.setOnClickListener {
            signup()
        }

    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.SignupBtn.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.SignupBtn.visibility = View.VISIBLE
        }
    }

    private fun signup() {
        val email = binding.Email.text.toString()
        val password = binding.Password.text.toString()
        val confirmPassword = binding.ConfirmPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.Email.setError("Enter a valid email")
            return
        }
        if (password.length < 6) {
            binding.Password.setError("Minimum need 6 character")
            return
        }
        if (password != confirmPassword) {
            binding.ConfirmPassword.setError("Password doesn't matched")
            return
        }

        signupWithFirebase(email, password)


    }

    private fun signupWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(
            email, password
        ).addOnSuccessListener {
            it.user?.let { user ->
                val userModel = UserModel(user.uid, email, email.substringBefore("@"))
                Firebase.firestore.collection("users")
                    .document(user.uid)
                    .set(userModel).addOnSuccessListener {
                        Toast.makeText(this, "Successfully", Toast.LENGTH_SHORT).show()
                        setInProgress(false)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            setInProgress(false)
        }
    }
}