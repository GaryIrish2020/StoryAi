package com.GirishDevelopment.storyai.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    fun hasUser(): Boolean {
        return Firebase.auth.currentUser != null
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { 
                if (it.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(it.exception?.message ?: "Sign in failed.")
                }
            }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        Firebase.auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { 
                if (it.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(it.exception?.message ?: "Sign up failed.")
                }
            }
    }
}