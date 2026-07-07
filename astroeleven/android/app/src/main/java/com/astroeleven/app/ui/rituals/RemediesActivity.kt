package com.astroeleven.app.ui.rituals

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.astroeleven.app.data.api.ApiClient
import com.astroeleven.app.data.model.Ritual
import com.astroeleven.app.ui.home.RitualsTab
import com.astroeleven.app.ui.theme.CosmicAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemediesActivity : AppCompatActivity() {
    private val ritualsList = mutableStateListOf<Ritual>()
    private var isTamil by mutableStateOf(false)
    private var isLoading by mutableStateOf(true)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        isTamil = intent.getBooleanExtra("isTamil", false)
        fetchRituals()

        setContent {
            CosmicAppTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(if (isTamil) "பரிகாரங்கள்" else "Remedies", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.White
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFFFF7F00)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                RitualsTab(ritualsList, isTamil)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchRituals() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.api.getHomeData()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful && response.body()?.ok == true) {
                        val rituals = response.body()?.data?.rituals ?: emptyList()
                        ritualsList.clear()
                        ritualsList.addAll(rituals)
                    } else {
                        Toast.makeText(this@RemediesActivity, "Failed to load remedies", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(this@RemediesActivity, "Error loading remedies", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
