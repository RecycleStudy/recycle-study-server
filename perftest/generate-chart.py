"""
성능 테스트 결과 비교 차트 생성

Usage:
  python perftest/generate-chart.py
  python perftest/generate-chart.py --output docs/images/performance-comparison.png
"""

import argparse
import os

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

# 한글 폰트 설정 (Windows: Malgun Gothic, Mac: AppleGothic)
plt.rcParams["font.family"] = "Malgun Gothic"
plt.rcParams["axes.unicode_minus"] = False

# 데이터 (500건 기준)
labels = [
    "기본값\n(core-size=8)",
    "스레드 풀 튜닝\n(core-size=27)",
    "Virtual Thread\n(Java 25)",
]
throughput = [2.49, 8.85, 125.0]
duration = [200.6, 56.5, 4.0]
multiplier = ["1x", "3.6x", "50.2x"]
colors = ["#6c757d", "#0d6efd", "#198754"]


def create_chart(output_path):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5))

    # --- 왼쪽: 처리량 (건/초) ---
    bars1 = ax1.barh(labels, throughput, color=colors, height=0.5, edgecolor="white")
    ax1.set_xlabel("처리량 (건/초)", fontsize=11)
    ax1.set_title("처리량 비교 (500건 기준)", fontsize=13, fontweight="bold", pad=12)
    ax1.set_xlim(0, max(throughput) * 1.25)
    ax1.xaxis.set_major_locator(ticker.MultipleLocator(25))
    ax1.grid(axis="x", alpha=0.3)

    for bar, val, mult in zip(bars1, throughput, multiplier):
        ax1.text(
            val + max(throughput) * 0.02,
            bar.get_y() + bar.get_height() / 2,
            f"{val} 건/초 ({mult})",
            va="center",
            fontsize=10,
            fontweight="bold",
        )

    # --- 오른쪽: 처리 시간 (초) ---
    bars2 = ax2.barh(labels, duration, color=colors, height=0.5, edgecolor="white")
    ax2.set_xlabel("처리 시간 (초)", fontsize=11)
    ax2.set_title("처리 시간 비교 (500건 기준)", fontsize=13, fontweight="bold", pad=12)
    ax2.set_xlim(0, max(duration) * 1.25)
    ax2.xaxis.set_major_locator(ticker.MultipleLocator(50))
    ax2.grid(axis="x", alpha=0.3)

    # 1분 기준선
    ax2.axvline(x=60, color="red", linestyle="--", linewidth=1, alpha=0.7)
    ax2.text(62, 2.3, "1분 기준", color="red", fontsize=9, alpha=0.8)

    for bar, val in zip(bars2, duration):
        ax2.text(
            val + max(duration) * 0.02,
            bar.get_y() + bar.get_height() / 2,
            f"{val}초",
            va="center",
            fontsize=10,
            fontweight="bold",
        )

    plt.tight_layout(pad=2.0)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    fig.savefig(output_path, dpi=150, bbox_inches="tight", facecolor="white")
    plt.close()
    print(f"Chart saved: {output_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output",
        default="docs/images/performance-comparison.png",
        help="Output image path",
    )
    args = parser.parse_args()
    create_chart(args.output)