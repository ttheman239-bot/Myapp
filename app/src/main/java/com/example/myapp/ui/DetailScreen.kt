package com.example.myapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapp.data.PairResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(pair: PairResult, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${pair.leadSymbol} → ${pair.lagSymbol}") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← กลับ") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${pair.leadName} → ${pair.lagName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "ตลาด ${pair.leadName} ปิดก่อน ⇒ ข่าวที่เกิดระหว่าง ${pair.leadName} เปิดอยู่จะ" +
                    " สะท้อนเข้า ${pair.lagName} ในเซสชั่นถัดไป",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MetricsCard(pair)

            Text(
                "Equity curve (backtest 5 ปี • long-only overnight momentum)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    EquityChart(curve = pair.equityCurve)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "เริ่มที่ 1.00",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "ปัจจุบัน ${fmt2(pair.equityCurve.last())}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            ExplanationCard(pair)

            if (pair.playbook.hasDetail) {
                PlaybookSection(pair)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlaybookSection(pair: com.example.myapp.data.PairResult) {
    val pb = pair.playbook

    Text(
        "📘 Playbook — เทรดยังไง ขั้นตอนโดยละเอียด",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )

    PlaybookCard("Step 1 — Instrument (เลือก 1 ใน)") {
        Labeled("แนะนำ", pb.instrument)
        if (pb.instrumentAlt != "-" && pb.instrumentAlt.isNotBlank()) {
            Labeled("ทางเลือก", pb.instrumentAlt)
        }
    }

    PlaybookCard("Step 2 — Entry") {
        Labeled("Rule", pb.entryRule)
        Labeled("Time", pb.entryTime)
    }

    PlaybookCard("Step 3 — Exit") {
        Labeled("Rule", pb.exitRule)
        Labeled("Time", pb.exitTime)
    }

    PlaybookCard("Step 4 — Risk / Size") {
        Labeled("Position size", pb.positionSize)
        Labeled("Stop loss", pb.stopLoss)
        if (pb.capitalMinUsd > 0) {
            Labeled("Capital ต่ำสุด", "$${"%,d".format(pb.capitalMinUsd)}")
        }
        if (pb.tcostBps > 0) {
            Labeled("Tcost estimate", "${pb.tcostBps} bps round-trip")
        }
    }

    PlaybookCard("Step 5 — Expected stats") {
        if (pb.expectedWins != "-") Labeled("Win rate", pb.expectedWins)
        if (pb.tradesPerYear > 0)   Labeled("Trades/year", "${pb.tradesPerYear}")
        if (pb.avgWinPct != 0.0)    Labeled("Avg win", "${"%+.2f".format(pb.avgWinPct)}%")
        if (pb.avgLossPct != 0.0)   Labeled("Avg loss", "${"%+.2f".format(pb.avgLossPct)}%")
    }

    if (pb.exampleTrade.isNotBlank()) {
        PlaybookCard("Example trade (sample)") {
            Text(
                pb.exampleTrade,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }

    if (pb.risks.isNotEmpty()) {
        PlaybookCard("⚠ ความเสี่ยงเฉพาะคู่นี้") {
            pb.risks.forEach { r ->
                Text(
                    "• $r",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }

    if (pb.brokerNotes.isNotBlank() && pb.brokerNotes != "-") {
        PlaybookCard("Broker notes") {
            Text(
                pb.brokerNotes,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PlaybookCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricsCard(pair: PairResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "สถิติหลัก",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            MetricRow("β (pass-through)",         fmt3(pair.beta))
            MetricRow("t-stat",                   fmt2(pair.tStat))
            MetricRow("R²",                       pct(pair.rSquared))
            MetricRow("Hit rate (direction)",     pct(pair.hitRate))
            MetricRow("N (observations)",         "${pair.nObs} days")
            Spacer(Modifier.height(14.dp))
            Text(
                "Backtest (หัก tcost)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            MetricRow("Sharpe (annualised)",      fmt2(pair.backtestSharpe))
            MetricRow("Win rate",                 pct(pair.backtestHitRate))
            MetricRow("Avg per trade",            bps(pair.backtestAvgRet))
            MetricRow("Avg |r| ของ lag",          bps(pair.avgLagAbsRet))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ExplanationCard(pair: PairResult) {
    val text = buildString {
        append("**สรุปสั้น:** ")
        when {
            pair.backtestSharpe >= 1.0 -> {
                append("คู่นี้มีสัญญาณ lead-lag ที่แข็งแรงเป็นพิเศษ ")
                append("ทิศทางการเคลื่อนไหวของ ${pair.leadName} เมื่อวาน ทำนาย ")
                append("${pair.lagName} ได้ถูก ${"%.0f".format(pair.hitRate * 100)}% ของวัน ")
                append("ด้วย Sharpe หลังหัก tcost ≈ ${"%.2f".format(pair.backtestSharpe)} ซึ่งถือว่าน่าเล่น")
            }
            pair.backtestSharpe >= 0.5 -> {
                append("คู่นี้มี edge พอใช้ได้ ")
                append("hit rate ${"%.0f".format(pair.hitRate * 100)}% สูงกว่า 50% อย่างมีนัยสำคัญ ")
                append("แต่ต้องระวัง tcost และ slippage จะกิน edge ได้")
            }
            else -> {
                append("คู่นี้ไม่น่าเล่น — t-stat ต่ำกว่า 2 หมายถึงสัญญาณไม่มีนัยสำคัญทางสถิติ ")
                append("Sharpe ต่ำกว่า 0.5 แปลว่าไม่คุ้มค่าเมื่อเทียบกับ transaction cost")
            }
        }
        append("\n\n")
        append("**วิธีเทรด:** เมื่อ ${pair.leadName} ปิด > 0 ให้เปิด long ETF/futures ของ ${pair.lagName} ")
        append("ก่อนเปิดตลาด home market วันถัดไป และปิดตอน close ของวันนั้น")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Text(
            text = text.replace("**", ""),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
