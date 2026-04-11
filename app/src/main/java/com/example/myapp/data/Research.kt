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
) {
    /** Loose verdict useful for a quick badge on the ranking list. */
    val verdict: Verdict
        get() = when {
            backtestSharpe >= 1.0 && tStat >= 3.0 -> Verdict.STRONG
            backtestSharpe >= 0.5 && tStat >= 2.0 -> Verdict.OK
            else                                  -> Verdict.WEAK
        }
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
                add(
                    PairResult(
                        leadSymbol     = p.getString("lead_symbol"),
                        lagSymbol      = p.getString("lag_symbol"),
                        leadName       = p.getString("lead_name"),
                        lagName        = p.getString("lag_name"),
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
}
