package com.example.myapp.data

import android.content.Context
import org.json.JSONObject
import java.io.IOException

/**
 * Research payload produced by `research/lead_lag.py` (or the pre-baked
 * illustrative dataset committed in `app/src/main/assets/research.json`).
 */
data class ResearchData(
    val generatedAtUtc: String,
    val source: String,
    val yearsOfData: Double,
    val tcostBps: Double,
    val note: String,
    val pairs: List<PairResult>,
)

data class PairResult(
    val leadSymbol: String,
    val lagSymbol: String,
    val leadName: String,
    val lagName: String,
    val category: String,
    val nObs: Int,
    val beta: Double,
    val rSquared: Double,
    val tStat: Double,
    val hitRate: Double,
    val avgLagAbsRet: Double,
    val backtestSharpe: Double,
    val backtestHitRate: Double,
    val backtestAvgRet: Double,
    val equityCurve: List<Double>,
    val playbook: Playbook,
) {
    /** Loose verdict useful for a quick badge on the ranking list. */
    val verdict: Verdict
        get() = when {
            kotlin.math.abs(backtestSharpe) >= 1.0 && kotlin.math.abs(tStat) >= 3.0 -> Verdict.STRONG
            kotlin.math.abs(backtestSharpe) >= 0.5 && kotlin.math.abs(tStat) >= 2.0 -> Verdict.OK
            else                                                                    -> Verdict.WEAK
        }
}

data class Playbook(
    val instrument: String,
    val instrumentAlt: String,
    val entryRule: String,
    val entryTime: String,
    val exitRule: String,
    val exitTime: String,
    val positionSize: String,
    val stopLoss: String,
    val capitalMinUsd: Int,
    val tcostBps: Int,
    val expectedWins: String,
    val avgWinPct: Double,
    val avgLossPct: Double,
    val tradesPerYear: Int,
    val exampleTrade: String,
    val risks: List<String>,
    val brokerNotes: String,
) {
    val hasDetail: Boolean get() = instrument.isNotBlank() && instrument != "-"
}

enum class Verdict(val label: String, val short: String) {
    STRONG("น่าเล่น", "STRONG"),
    OK("พอได้", "OK"),
    WEAK("อย่าเล่น", "WEAK"),
}

object ResearchRepository {

    @Volatile private var cached: ResearchData? = null

    fun load(context: Context): ResearchData {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val text = try {
                context.assets.open("research.json").bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                throw IllegalStateException("research.json missing from assets", e)
            }
            val parsed = parse(text)
            cached = parsed
            return parsed
        }
    }

    internal fun parse(json: String): ResearchData {
        val root = JSONObject(json)
        val pairsArr = root.getJSONArray("pairs")
        val pairs = buildList(pairsArr.length()) {
            for (i in 0 until pairsArr.length()) {
                val p = pairsArr.getJSONObject(i)
                val eqArr = p.getJSONArray("equity_curve")
                val eq = buildList(eqArr.length()) {
                    for (j in 0 until eqArr.length()) add(eqArr.getDouble(j))
                }
                val pbObj = p.optJSONObject("playbook")
                val playbook = if (pbObj != null) parsePlaybook(pbObj) else emptyPlaybook()
                add(
                    PairResult(
                        leadSymbol     = p.getString("lead_symbol"),
                        lagSymbol      = p.getString("lag_symbol"),
                        leadName       = p.getString("lead_name"),
                        lagName        = p.getString("lag_name"),
                        category       = p.optString("category", "Equity→Equity"),
                        nObs           = p.getInt("n_obs"),
                        beta           = p.getDouble("beta"),
                        rSquared       = p.getDouble("r_squared"),
                        tStat          = p.getDouble("t_stat"),
                        hitRate        = p.getDouble("hit_rate"),
                        avgLagAbsRet   = p.getDouble("avg_lag_abs_ret"),
                        backtestSharpe = p.getDouble("backtest_sharpe"),
                        backtestHitRate= p.getDouble("backtest_hit_rate"),
                        backtestAvgRet = p.getDouble("backtest_avg_ret"),
                        equityCurve    = eq,
                        playbook       = playbook,
                    )
                )
            }
        }
        return ResearchData(
            generatedAtUtc = root.optString("generated_at_utc", ""),
            source         = root.optString("source", ""),
            yearsOfData    = root.optDouble("years_of_data", 0.0),
            tcostBps       = root.optDouble("tcost_bps", 0.0),
            note           = root.optString("note", ""),
            pairs          = pairs,
        )
    }

    private fun parsePlaybook(p: JSONObject): Playbook {
        val risksArr = p.optJSONArray("risks")
        val risks = if (risksArr == null) emptyList() else buildList(risksArr.length()) {
            for (k in 0 until risksArr.length()) add(risksArr.getString(k))
        }
        return Playbook(
            instrument    = p.optString("instrument", "-"),
            instrumentAlt = p.optString("instrument_alt", "-"),
            entryRule     = p.optString("entry_rule", "-"),
            entryTime     = p.optString("entry_time", "-"),
            exitRule      = p.optString("exit_rule", "-"),
            exitTime      = p.optString("exit_time", "-"),
            positionSize  = p.optString("position_size", "-"),
            stopLoss      = p.optString("stop_loss", "-"),
            capitalMinUsd = p.optInt("capital_min_usd", 0),
            tcostBps      = p.optInt("tcost_bps", 0),
            expectedWins  = p.optString("expected_wins", "-"),
            avgWinPct     = p.optDouble("avg_win_pct", 0.0),
            avgLossPct    = p.optDouble("avg_loss_pct", 0.0),
            tradesPerYear = p.optInt("trades_per_year", 0),
            exampleTrade  = p.optString("example_trade", ""),
            risks         = risks,
            brokerNotes   = p.optString("broker_notes", ""),
        )
    }

    private fun emptyPlaybook(): Playbook = Playbook(
        instrument = "-", instrumentAlt = "-", entryRule = "-", entryTime = "-",
        exitRule = "-", exitTime = "-", positionSize = "-", stopLoss = "-",
        capitalMinUsd = 0, tcostBps = 0, expectedWins = "-",
        avgWinPct = 0.0, avgLossPct = 0.0, tradesPerYear = 0,
        exampleTrade = "", risks = emptyList(), brokerNotes = "",
    )
}
