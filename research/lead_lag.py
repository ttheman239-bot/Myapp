"""Lead-lag analysis of international stock indices.

Computes overnight-drift / time-zone lead-lag metrics between pairs of
indices and a simple backtest of a long-only overnight momentum strategy.

Outputs a JSON file that the Android app bundles as an asset.

Usage (requires internet):
    pip install yfinance pandas numpy
    python research/lead_lag.py --years 5 \
        --out app/src/main/assets/research.json

This script is not executed inside the Claude sandbox (Yahoo Finance is
blocked). It is intended to be run locally or in GitHub Actions, which
have full internet access.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import math
import sys
from dataclasses import asdict, dataclass, field


# Lead -> Lag candidate pairs (symbol, human-readable name, timezone hint)
PAIRS: list[tuple[str, str, str, str]] = [
    # (lead, lag, lead_name, lag_name)
    ("^GSPC", "^AXJO", "S&P 500",  "ASX 200"),
    ("^GSPC", "^N225", "S&P 500",  "Nikkei 225"),
    ("^GSPC", "^KS11", "S&P 500",  "KOSPI"),
    ("^GSPC", "^HSI",  "S&P 500",  "Hang Seng"),
    ("^GSPC", "^TWII", "S&P 500",  "Taiwan Weighted"),
    ("^GSPC", "^SET.BK", "S&P 500", "SET Index"),
    ("^GSPC", "^STI",  "S&P 500",  "Straits Times"),
    ("^N225", "^GSPC", "Nikkei 225", "S&P 500 (next)"),
    ("^N225", "^FTSE", "Nikkei 225", "FTSE 100"),
    ("^FTSE", "^GSPC", "FTSE 100",  "S&P 500 (next)"),
    ("^GDAXI", "^GSPC","DAX",       "S&P 500 (next)"),
]


@dataclass
class PairResult:
    lead_symbol: str
    lag_symbol: str
    lead_name: str
    lag_name: str
    n_obs: int
    beta: float              # regression coefficient r_lag(t+1) = α + β r_lead(t)
    r_squared: float
    t_stat: float
    hit_rate: float          # fraction of days where sign(r_lag(t+1)) == sign(r_lead(t))
    avg_lag_abs_ret: float   # avg |r_lag| (for sizing context)
    backtest_sharpe: float   # annualized Sharpe of long-only overnight momentum
    backtest_hit_rate: float
    backtest_avg_ret: float  # average per-trade return (after tcost)
    equity_curve: list[float] = field(default_factory=list)  # normalized, starts at 1.0


def fetch_series(symbol: str, years: int) -> "pd.Series":
    import yfinance as yf  # imported lazily so the script can be linted without deps
    end = dt.date.today()
    start = end - dt.timedelta(days=int(years * 365.25) + 30)
    df = yf.download(symbol, start=start.isoformat(), end=end.isoformat(),
                     progress=False, auto_adjust=False)
    if df is None or df.empty:
        raise RuntimeError(f"no data for {symbol}")
    # Use adjusted close when available
    col = "Adj Close" if "Adj Close" in df.columns else "Close"
    return df[col].dropna()


def analyse_pair(lead: "pd.Series", lag: "pd.Series",
                 lead_symbol: str, lag_symbol: str,
                 lead_name: str, lag_name: str,
                 tcost_bps: float = 5.0) -> PairResult:
    import numpy as np
    import pandas as pd

    r_lead = np.log(lead).diff().rename("lead")
    r_lag  = np.log(lag).diff().rename("lag")

    # Align on a shifted-by-1 basis: r_lag(t) is predicted by r_lead(t-1)
    df = pd.concat([r_lead.shift(1), r_lag], axis=1).dropna()
    df.columns = ["lead_prev", "lag"]

    x = df["lead_prev"].to_numpy()
    y = df["lag"].to_numpy()
    n = len(x)
    if n < 100:
        raise RuntimeError("not enough overlapping observations")

    # OLS: y = α + β x
    x_mean, y_mean = x.mean(), y.mean()
    xx = ((x - x_mean) ** 2).sum()
    xy = ((x - x_mean) * (y - y_mean)).sum()
    beta = xy / xx if xx > 0 else 0.0
    alpha = y_mean - beta * x_mean
    y_hat = alpha + beta * x
    resid = y - y_hat
    ss_res = (resid ** 2).sum()
    ss_tot = ((y - y_mean) ** 2).sum()
    r_squared = 1.0 - ss_res / ss_tot if ss_tot > 0 else 0.0
    se_beta = math.sqrt(ss_res / (n - 2) / xx) if (n > 2 and xx > 0) else float("nan")
    t_stat = beta / se_beta if se_beta and not math.isnan(se_beta) and se_beta > 0 else 0.0

    hit_rate = float(((x > 0) & (y > 0)).sum() + ((x < 0) & (y < 0)).sum()) / n
    avg_lag_abs = float(np.abs(y).mean())

    # Backtest: go long lag when lead_prev > 0; flat otherwise.
    # PnL = r_lag(t) - transaction cost on days with position flip
    tcost = tcost_bps * 1e-4
    pos = (x > 0).astype(float)
    flip = np.abs(np.diff(pos, prepend=0.0))
    pnl = pos * y - flip * tcost

    bt_hit = float((pnl > 0).sum()) / n
    bt_avg = float(pnl.mean())
    bt_std = float(pnl.std(ddof=1)) if n > 1 else 0.0
    sharpe = (bt_avg / bt_std) * math.sqrt(252) if bt_std > 0 else 0.0

    equity = (1.0 + pnl).cumprod()
    # Downsample equity curve to 60 points for the mobile chart
    step = max(1, len(equity) // 60)
    eq_down = equity[::step].tolist()
    if eq_down[-1] != equity[-1]:
        eq_down.append(float(equity[-1]))

    return PairResult(
        lead_symbol=lead_symbol,
        lag_symbol=lag_symbol,
        lead_name=lead_name,
        lag_name=lag_name,
        n_obs=n,
        beta=round(float(beta), 4),
        r_squared=round(float(r_squared), 4),
        t_stat=round(float(t_stat), 3),
        hit_rate=round(hit_rate, 4),
        avg_lag_abs_ret=round(avg_lag_abs, 5),
        backtest_sharpe=round(float(sharpe), 3),
        backtest_hit_rate=round(bt_hit, 4),
        backtest_avg_ret=round(bt_avg, 5),
        equity_curve=[round(float(v), 4) for v in eq_down],
    )


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--years", type=float, default=5.0)
    ap.add_argument("--out",   default="app/src/main/assets/research.json")
    ap.add_argument("--tcost-bps", type=float, default=5.0)
    args = ap.parse_args()

    try:
        import pandas as pd  # noqa: F401
        import numpy  as np  # noqa: F401
        import yfinance as yf  # noqa: F401
    except ImportError as e:
        print(f"Missing dependency: {e}. Run `pip install yfinance pandas numpy`.",
              file=sys.stderr)
        return 2

    cache: dict[str, "pd.Series"] = {}
    results: list[PairResult] = []

    for lead_sym, lag_sym, lead_name, lag_name in PAIRS:
        try:
            if lead_sym not in cache:
                cache[lead_sym] = fetch_series(lead_sym, args.years)
            if lag_sym not in cache:
                cache[lag_sym] = fetch_series(lag_sym, args.years)
            res = analyse_pair(
                cache[lead_sym], cache[lag_sym],
                lead_sym, lag_sym, lead_name, lag_name,
                tcost_bps=args.tcost_bps,
            )
            results.append(res)
            print(f"OK  {lead_name:12s} -> {lag_name:18s}  "
                  f"β={res.beta:+.3f}  t={res.t_stat:+.2f}  "
                  f"Sharpe={res.backtest_sharpe:+.2f}  hit={res.hit_rate:.1%}")
        except Exception as e:
            print(f"SKIP {lead_name} -> {lag_name}: {e}", file=sys.stderr)

    # Rank by backtest Sharpe (descending)
    results.sort(key=lambda r: r.backtest_sharpe, reverse=True)

    payload = {
        "generated_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "years_of_data":    args.years,
        "tcost_bps":        args.tcost_bps,
        "pairs":            [asdict(r) for r in results],
    }
    with open(args.out, "w") as f:
        json.dump(payload, f, indent=2)
    print(f"\nWrote {len(results)} pairs to {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
