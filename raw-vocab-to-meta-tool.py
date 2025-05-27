# This script converts a vocabulary text file (with lines of "word<TAB>definition") 
"""
raw-to-meta.py

This script converts a vocabulary text file (with lines formatted as "word<TAB>definition") 
into a meta JSON format, or appends new entries from a vocab file to an existing meta JSON file.

Features:
- Generates a meta JSON file containing a list of entries, each with a unique id, title, body, and links.
- The first entry is a meta entry with empty title/body and links to the last entry.
- Each vocab entry links to the previous entry via the 'previous' field.
- When adding entries to an existing meta JSON, only unique (title, body) pairs are appended.
- Updates the meta entry's 'previous' field to point to the last entry.

Functions:
- generate_unique_id(existing_ids): Generates a unique 8-character hexadecimal ID not in existing_ids.
- read_vocab_file(vocab_path): Reads a vocab file and returns a list of (word, definition) tuples.
- create_meta_file(vocab_path, output_path=None): Creates a new meta JSON file from a vocab file.
- add_entries_to_meta(meta_path, vocab_path): Adds new unique entries from a vocab file to an existing meta JSON.

Usage:
	python raw-to-meta.py vocab.txt [--output output.json]
	python raw-to-meta.py vocab.txt --meta existing_meta.json

Arguments:
	vocab           Path to vocab txt file (word<TAB>definition per line)
	--meta          Path to existing meta JSON file to add entries to
	--output        Output path for new meta JSON file

Example:
	# Create a new meta JSON file from vocab.txt
	python raw-to-meta.py vocab.txt --output vocab.json

	# Add new entries from vocab2.txt to an existing meta JSON file
	python raw-to-meta.py vocab2.txt --meta vocab.json
"""
# into a meta JSON format, or adds new entries from a vocab file to an existing meta JSON.
# 
# Usage:
#   python raw-to-meta.py vocab.txt [--output output.json]
#   python raw-to-meta.py vocab.txt --meta existing_meta.json
#
# - The meta JSON contains a list of entries, each with a unique id, title, body, and links.
# - The first entry is a meta entry with empty title/body and links to the last entry.
# - Each vocab entry links to the previous entry via the 'previous' field.
# - When adding entries, new entries are appended and the meta entry's 'previous' is updated.

import os
import sys
import json
import random
import argparse

def generate_unique_id(existing_ids):
	while True:
		new_id = f"{random.getrandbits(32):08x}"
		if new_id not in existing_ids:
			return new_id

def read_vocab_file(vocab_path):
	entries = []
	with open(vocab_path, 'r', encoding='utf-8') as f:
		for line in f:
			line = line.strip()
			if not line or '\t' not in line:
				continue
			word, definition = line.split('\t', 1)
			entries.append((word.strip(), definition.strip()))
	return entries

def create_meta_file(vocab_path, output_path=None):
	vocab_entries = read_vocab_file(vocab_path)
	language_name = os.path.splitext(os.path.basename(vocab_path))[0]
	meta_entries = []
	existing_ids = set()
	existing_title_body = set()

	# Entry 0: meta data
	meta_id = ""
	existing_ids.add(meta_id)
	meta_entry = {
		"title": "",
		"body": "",
		"id": meta_id,
		"level": 0,
		"previous": "",  # will be set later
		"state": True
	}
	meta_entries.append(meta_entry)
	existing_title_body.add(("", ""))

	# Entries for vocab, skip duplicates
	prev_id = meta_id
	for word, definition in vocab_entries:
		if (word, definition) in existing_title_body:
			continue
		entry_id = generate_unique_id(existing_ids)
		existing_ids.add(entry_id)
		existing_title_body.add((word, definition))
		entry = {
			"title": word,
			"body": definition,
			"id": entry_id,
			"level": 0,
			"previous": prev_id,
			"state": True
		}
		meta_entries.append(entry)
		prev_id = entry_id

	# Set 'previous' for meta_entry to the last entry's id
	if len(meta_entries) > 1:
		meta_entries[0]['previous'] = meta_entries[-1]['id']

	data = [language_name, meta_entries]
	output_path = output_path or os.path.splitext(vocab_path)[0] + ".json"
	with open(output_path, 'w', encoding='utf-8') as f:
		json.dump(data, f, ensure_ascii=False, indent=2)
	print(f"Meta file created: {output_path}")

if __name__ == "__main__":

	parser = argparse.ArgumentParser(description="Convert vocab file to meta JSON or add entries.")
	parser.add_argument("vocab", help="Path to vocab txt file (word<TAB>definition per line)")
	parser.add_argument("--output", help="Output path for new meta JSON file")
	args = parser.parse_args()
	# arabicphrase.txt --output arabicphrase.json
	
	create_meta_file(args.vocab, args.output)