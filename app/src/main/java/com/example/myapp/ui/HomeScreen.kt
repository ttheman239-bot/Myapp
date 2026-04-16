package com.example.myapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapp.data.PairResult
import com.example.myapp.data.ResearchData
import com.example.myapp.data.Verdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    data: ResearchData,
    onPairClick: (PairResult) -> Unit,
    onAboutClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trade Delay Cap — วิจัย") },
                actions = {
                    TextButton(onClick = onAboutClick) { Text("About") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                HeaderCard(
                    totalPairs = data.pairs.size,
                    years = data.yearsOfData,
                    note = data.note,
                )
            }
            item {
                Text(
                    "อันดับ คู่ตลาดที่น่าเล่นที่สุด (ranked by backtest Sharpe)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(
                items = data.pairs,
                key = { _, p -> "${p.leadSymbol}->${p.lagSymbol}" },
            ) { index, pair ->
                PairRow(
                    pair = pair,
                    rank = index + 1,
                    onClick = { onPairClick(pair) },
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "* ไม่ใช่คำแนะนำการลงทุน • ข้อมูลย้อนหลังไม่รับประกันอนาคต",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(totalPairs: Int, years: Double, note: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Time-Zone Arbitrage Research",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "วิเคราะห์ $totalPairs คู่ • ข้อมูล ${years.toInt()} ปี • overnight drift strategy",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun PairRow(pair: PairResult, rank: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${pair.leadName} → ${pair.lagName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "β=${fmt3(pair.beta)}  •  t=${fmt2(pair.tStat)}  •  Sharpe=${fmt2(pair.backtestSharpe)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    pair.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }
            VerdictBadge(pair.verdict)
        }
    }
}

@Composable
private fun VerdictBadge(v: Verdict) {
    val (bg, fg) = when (v) {
        Verdict.STRONG -> Color(0xFF1B5E20) to Color.White
        Verdict.OK     -> Color(0xFFF9A825) to Color.Black
        Verdict.WEAK   -> Color(0xFF757575) to Color.White
    }
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            v.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun fmt2(v: Double): String = String.format("%.2f", v)
internal fun fmt3(v: Double): String = String.format("%.3f", v)
internal fun pct(v: Double): String = String.format("%.1f%%", v * 100.0)
internal fun bps(v: Double): String = String.format("%+.1f bps", v * 10000.0)
