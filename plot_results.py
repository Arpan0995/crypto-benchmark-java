import csv
import matplotlib
# Use Agg backend for headless environments
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

"""
This script reads the benchmark results produced by BenchmarkRunner and
generates bar charts comparing the performance and key sizes of AES,
RSA and SPHINCS+.  The resulting PNG files are saved alongside the
CSV file.

Usage:
  python plot_results.py benchmark_results.csv
"""

import sys
import os

def load_results(csv_path):
    with open(csv_path, newline='') as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    return rows

def plot_time_chart(rows, output_path):
    algorithms = [row['algorithm'] for row in rows]
    key_times = [float(row['keyGenTimeMs']) for row in rows]
    enc_times = [float(row['encryptOrSignTimeMs']) for row in rows]
    dec_times = [float(row['decryptOrVerifyTimeMs']) for row in rows]

    x = np.arange(len(algorithms))
    width = 0.25

    fig, ax = plt.subplots(figsize=(8, 4))
    ax.bar(x - width, key_times, width, label='Key Generation')
    ax.bar(x, enc_times, width, label='Encrypt/Sign')
    ax.bar(x + width, dec_times, width, label='Decrypt/Verify')

    ax.set_ylabel('Time (ms)')
    ax.set_title('Performance comparison of cryptographic algorithms')
    ax.set_xticks(x)
    ax.set_xticklabels(algorithms)
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.5)
    fig.tight_layout()
    fig.savefig(output_path)
    print(f"Saved time comparison chart to {output_path}")

def plot_size_chart(rows, output_path):
    algorithms = [row['algorithm'] for row in rows]
    # Use 0 for missing values (symmetric algorithms) to avoid None
    key_bits = [int(row['keySizeBits']) for row in rows]
    public_key = [int(row['publicKeyBytes']) for row in rows]
    private_key = [int(row['privateKeyBytes']) for row in rows]
    signature = [int(row['signatureBytes']) for row in rows]
    symmetric_bytes = [int(row['symmetricKeyBytes']) for row in rows]

    x = np.arange(len(algorithms))
    width = 0.18

    fig, ax = plt.subplots(figsize=(10, 4))
    ax.bar(x - 2*width, key_bits, width, label='Key size (bits)')
    ax.bar(x - width, [b for b in symmetric_bytes], width, label='Symmetric key (bytes)')
    ax.bar(x, public_key, width, label='Public key (bytes)')
    ax.bar(x + width, private_key, width, label='Private key (bytes)')
    ax.bar(x + 2*width, signature, width, label='Signature size (bytes)')

    ax.set_ylabel('Size')
    ax.set_title('Key and signature size comparison')
    ax.set_xticks(x)
    ax.set_xticklabels(algorithms)
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.5)
    fig.tight_layout()
    fig.savefig(output_path)
    print(f"Saved size comparison chart to {output_path}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python plot_results.py <benchmark_results.csv>")
        sys.exit(1)
    csv_path = sys.argv[1]
    rows = load_results(csv_path)
    base = os.path.splitext(csv_path)[0]
    plot_time_chart(rows, base + '_time.png')
    plot_size_chart(rows, base + '_size.png')

if __name__ == '__main__':
    main()
