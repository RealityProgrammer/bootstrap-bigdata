"use client";
import React, { useEffect, useState } from "react";

export default function ResultsPage() {
  const [files, setFiles] = useState<string[]>([]);

  useEffect(() => {
    fetch("/api/results")
      .then((r) => r.json())
      .then((j) => setFiles(j.files || []))
      .catch(() => setFiles([]));
  }, []);

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Exported Results</h1>
      <ul>
        {files.map((f) => (
          <li key={f} className="mb-2">
            <a
              className="text-blue-600 underline"
              href={`/api/results/download?path=${encodeURIComponent(f)}`}
              target="_blank"
              rel="noreferrer"
            >
              {f}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
