#!/usr/bin/env python3
"""
create_chess_db.py
==================
Converts the Lichess CC0 puzzle CSV into a gzip-compressed SQLite database
ready to be uploaded to GitHub Releases and downloaded by the app.

Requirements:
    pip install zstandard --break-system-packages

Usage:
    # From the .zst file (downloaded from Lichess):
    python create_chess_db.py --zst lichess_db_puzzle.csv.zst --out chess_puzzles.db.gz

    # From an already-decompressed CSV:
    python create_chess_db.py --csv lichess_db_puzzle.csv --out chess_puzzles.db.gz

    # Test run — only process the first N rows:
    python create_chess_db.py --zst lichess_db_puzzle.csv.zst --out test.db.gz --limit 50000

Download the Lichess file:
    curl -L -o lichess_db_puzzle.csv.zst https://database.lichess.org/lichess_db_puzzle.csv.zst

Lichess CSV header:
    PuzzleId, FEN, Moves, Rating, RatingDeviation, Popularity, NbPlays, Themes, GameUrl, OpeningTags
"""

import argparse
import csv
import gzip
import io
import os
import sqlite3
import sys
import tempfile

BATCH_SIZE = 10_000

CREATE_SQL = """
CREATE TABLE IF NOT EXISTS chess_puzzles (
    id              TEXT PRIMARY KEY,
    fen             TEXT NOT NULL,
    moves           TEXT NOT NULL,
    rating          INTEGER NOT NULL,
    ratingDeviation INTEGER NOT NULL DEFAULT 0,
    popularity      INTEGER NOT NULL DEFAULT 0,
    themes          TEXT NOT NULL DEFAULT '',
    openingTags     TEXT NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_rating ON chess_puzzles(rating);
CREATE INDEX IF NOT EXISTS idx_themes ON chess_puzzles(themes);
"""

INSERT_SQL = """
INSERT OR IGNORE INTO chess_puzzles
    (id, fen, moves, rating, ratingDeviation, popularity, themes, openingTags)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
"""


def open_csv_stream(args):
    """Return a text-mode file-like object. Caller is responsible for closing it."""
    if args.zst:
        import zstandard as zstd
        raw = open(args.zst, "rb")
        ctx = zstd.ZstdDecompressor()
        return io.TextIOWrapper(ctx.stream_reader(raw), encoding="utf-8", newline="")
    else:
        return open(args.csv, newline="", encoding="utf-8")


def process(reader, db_path, limit):
    conn = sqlite3.connect(db_path)
    conn.executescript(CREATE_SQL)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")

    batch   = []
    total   = 0
    skipped = 0

    for row in reader:
        if limit and total >= limit:
            break
        try:
            # DictReader gives us named columns; csv.reader gives a list.
            if isinstance(row, dict):
                pid  = row["PuzzleId"]
                fen  = row["FEN"]
                mvs  = row["Moves"]
                rat  = int(row["Rating"])
                dev  = int(row.get("RatingDeviation", 0) or 0)
                pop  = int(row.get("Popularity", 0) or 0)
                thm  = row.get("Themes", "")
                opn  = row.get("OpeningTags", "")
            else:
                pid, fen, mvs = row[0], row[1], row[2]
                rat  = int(row[3])
                dev  = int(row[4]) if len(row) > 4 else 0
                pop  = int(row[5]) if len(row) > 5 else 0
                thm  = row[7]      if len(row) > 7 else ""
                opn  = row[9]      if len(row) > 9 else ""
        except (ValueError, IndexError, KeyError):
            skipped += 1
            continue

        if not pid or not fen or not mvs:
            skipped += 1
            continue

        batch.append((pid, fen, mvs, rat, dev, pop, thm, opn))
        total += 1

        if len(batch) >= BATCH_SIZE:
            conn.executemany(INSERT_SQL, batch)
            conn.commit()
            batch.clear()
            print(f"  Inserted {total:,} rows…", end="\r", flush=True)

    if batch:
        conn.executemany(INSERT_SQL, batch)
        conn.commit()

    conn.close()
    print(f"\nDone. Inserted {total:,} puzzles, skipped {skipped:,}.")


def main():
    parser = argparse.ArgumentParser(description="Build chess_puzzles.db.gz from Lichess CSV")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--csv", help="Path to uncompressed Lichess puzzle CSV")
    source.add_argument("--zst", help="Path to Lichess .csv.zst file")
    parser.add_argument("--out",   default="chess_puzzles.db.gz",
                        help="Output .db.gz path (default: chess_puzzles.db.gz)")
    parser.add_argument("--limit", type=int, default=0,
                        help="Stop after this many rows — 0 means process everything")
    args = parser.parse_args()

    # Verify source exists before doing anything
    src_path = args.zst or args.csv
    if not os.path.exists(src_path):
        print(f"Error: source file not found: {src_path}", file=sys.stderr)
        sys.exit(1)

    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        db_path = tmp.name

    try:
        print(f"Source : {src_path}  ({os.path.getsize(src_path) / 1e6:.0f} MB compressed)")
        print(f"Output : {args.out}")
        if args.limit:
            print(f"Limit  : {args.limit:,} rows")
        print()

        f = open_csv_stream(args)
        try:
            # Lichess puzzle CSV always has a header row
            reader = csv.DictReader(f)
            process(reader, db_path, args.limit)
        finally:
            f.close()

        print(f"\nCompressing → {args.out} …")
        with open(db_path, "rb") as src, gzip.open(args.out, "wb", compresslevel=9) as dst:
            while True:
                chunk = src.read(1 << 20)   # 1 MB chunks
                if not chunk:
                    break
                dst.write(chunk)

        raw_mb = os.path.getsize(db_path) / 1e6
        gz_mb  = os.path.getsize(args.out) / 1e6
        print(f"SQLite : {raw_mb:.0f} MB  →  gzip output : {gz_mb:.1f} MB")
        print()
        print("Next steps:")
        print(f"  1. Upload '{args.out}' to a GitHub Release asset.")
        print("  2. Copy the asset URL into ChessDownloadManager.downloadUrl in the app.")

    finally:
        if os.path.exists(db_path):
            os.unlink(db_path)


if __name__ == "__main__":
    main()
