package com.example.campingscv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campingscv.ui.theme.CampingsCVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val campings = CampingRepository.loadFromRaw(resources, R.raw.campings)
        setContent {
            CampingsCVTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CampingsScreen(
                        campings = campings,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    PLACES_DESC
}

@Composable
fun CampingsScreen(campings: List<Camping>, modifier: Modifier = Modifier) {
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    val sortedCampings = remember(campings, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> campings.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> campings.sortedByDescending { it.name.lowercase() }
            SortOption.PLACES_DESC -> campings.sortedByDescending { it.places }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SortBar(
            selected = sortOption,
            onSortChange = { sortOption = it }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sortedCampings) { camping ->
                CampingItem(camping = camping)
            }
        }
    }
}

@Composable
private fun SortBar(selected: SortOption, onSortChange: (SortOption) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortButton(
            label = "Name A-Z",
            selected = selected == SortOption.NAME_ASC,
            onClick = { onSortChange(SortOption.NAME_ASC) }
        )
        SortButton(
            label = "Name Z-A",
            selected = selected == SortOption.NAME_DESC,
            onClick = { onSortChange(SortOption.NAME_DESC) }
        )
        SortButton(
            label = "Places",
            selected = selected == SortOption.PLACES_DESC,
            onClick = { onSortChange(SortOption.PLACES_DESC) }
        )
    }
}

@Composable
private fun SortButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}

@Composable
fun CampingItem(camping: Camping, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = camping.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${camping.municipality} - ${camping.province}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Category: ${camping.category} | Places: ${camping.places}")
            if (camping.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = camping.address)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampingsPreview() {
    val sample = listOf(
        Camping(
            name = "Camping Costa Blanca",
            municipality = "Denia",
            province = "Alicante",
            category = "3",
            places = 250,
            address = "Carretera Les Marines, km 5"
        ),
        Camping(
            name = "Camping Sierra Verde",
            municipality = "Morella",
            province = "Castellon",
            category = "2",
            places = 120,
            address = "Partida El Saso s/n"
        )
    )

    CampingsCVTheme {
        CampingsScreen(campings = sample)
    }
}