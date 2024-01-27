package com.example.retrofitassignment

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.retrofitassignment.ui.theme.RetrofitAssignmentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// President data class and data provider
data class President(val name: String, val startDuty: Int, val endDuty: Int, val description: String)

object DataProvider {
    val presidents: MutableList<President> = ArrayList()

    init {
        presidents.add(President("Kaarlo Stahlberg", 1919, 1925, "Eka presidentti"))
        presidents.add(President("Lauri Relander", 1925, 1931, "Toka presidentti"))
        presidents.add(President("P. E. Svinhufvud", 1931, 1937, "Kolmas presidentti"))
        presidents.add(President("Kyösti Kallio", 1937, 1940, "Neljas presidentti"))
        presidents.add(President("Risto Ryti", 1940, 1944, "Viides presidentti"))
        presidents.add(President("Carl Gustaf Emil Mannerheim", 1944, 1946, "Kuudes presidentti"))
        presidents.add(President("Juho Kusti Paasikivi", 1946, 1956, "Äkäinen ukko"))
        presidents.add(President("Urho Kekkonen", 1956, 1982, "Pelimies"))
        presidents.add(President("Mauno Koivisto", 1982, 1994, "Manu"))
        presidents.add(President("Martti Ahtisaari", 1994, 2000, "Mahtisaari"))
        presidents.add(President("Tarja Halonen", 2000, 2012, "Eka naispresidentti"))
        presidents.add(President("Sauli Niinistö", 2012, 2024, "Ensimmäisen koiran, Oskun, omistaja"))
    }
}

// Retrofit API service and data models for Wikipedia API
interface WikiApiService {
    @GET("w/api.php")
    suspend fun getHitCount(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("list") list: String = "search",
        @Query("srsearch") srsearch: String
    ): WikiResponse
}

data class WikiResponse(val query: WikiQuery)
data class WikiQuery(val searchinfo: WikiSearchInfo)
data class WikiSearchInfo(val totalhits: Int)

// Repository for making API calls
class WikiRepository {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service: WikiApiService = retrofit.create(WikiApiService::class.java)

    suspend fun hitCountCheck(name: String): WikiResponse {
        return service.getHitCount(srsearch = name)
    }
}

// ViewModel to handle the logic
class MyViewModel : ViewModel() {
    private val repository: WikiRepository = WikiRepository()
    var wikiUiState: MutableState<Int> = mutableStateOf(0)

    fun getHits(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val serverResp = repository.hitCountCheck(name)
            wikiUiState.value = serverResp.query.searchinfo.totalhits
        }
    }
}

// Main activity and composable functions
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetrofitAssignmentTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "presidentsList") {
                    composable("presidentsList") {
                        PresidentsListScreen(navController)
                    }
                    composable("presidentDetail/{presidentName}", arguments = listOf(navArgument("presidentName") { type = NavType.StringType })) { backStackEntry ->
                        val presidentName = backStackEntry.arguments?.getString("presidentName")
                        val president = DataProvider.presidents.find { it.name == presidentName }
                        president?.let { PresidentDetailScreen(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun PresidentsListScreen(navController: NavController) {
    val viewModel = viewModel<MyViewModel>()
    LazyColumn {
        items(DataProvider.presidents) { president ->
            Text(text = president.name, modifier = Modifier.clickable {
                navController.navigate("presidentDetail/${president.name}")
            })
            Divider()
        }
    }
}

@Composable
fun PresidentDetailScreen(president: President) {
    val viewModel = viewModel<MyViewModel>()
    LaunchedEffect(president.name) {
        viewModel.getHits(president.name)
    }
    Column {
        Text("Name: ${president.name}")
        Text("Start Duty: ${president.startDuty}")
        Text("End Duty: ${president.endDuty}")
        Text("Description: ${president.description}")
        Text("Wikipedia Hits: ${viewModel.wikiUiState.value}")
    }
}