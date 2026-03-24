package com.forexbot.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.forexbot.R
import com.forexbot.api.SessionManager

class LoginActivity : AppCompatActivity() {

    private var isLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.isLoggedIn(this)) {
            startMain(); return
        }

        setContentView(R.layout.activity_login)

        val etName     = findViewById<EditText>(R.id.et_name)
        val etEmail    = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnAction  = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_action)
        val tvToggle   = findViewById<TextView>(R.id.tv_toggle)
        val tvTitle    = findViewById<TextView>(R.id.tv_title)
        val tvSubtitle = findViewById<TextView>(R.id.tv_subtitle)
        val layoutName = findViewById<View>(R.id.layout_name)

        fun updateMode() {
            if (isLogin) {
                tvTitle.text = "Welcome Back"
                tvSubtitle.text = "Sign in to your trading account"
                btnAction.text = "SIGN IN"
                tvToggle.text = "Don't have an account? Create one"
                layoutName.visibility = View.GONE
            } else {
                tvTitle.text = "Create Account"
                tvSubtitle.text = "Start automated forex trading"
                btnAction.text = "CREATE ACCOUNT"
                tvToggle.text = "Already have an account? Sign in"
                layoutName.visibility = View.VISIBLE
            }
        }
        updateMode()

        tvToggle.setOnClickListener { isLogin = !isLogin; updateMode() }

        btnAction.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass  = etPassword.text.toString().trim()
            val name  = etName.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (!isLogin && name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            val displayName = if (isLogin) email.substringBefore("@").replaceFirstChar { it.uppercase() } else name
            SessionManager.saveLogin(this, displayName, email)
            startMain()
        }
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
