"use client";
import React, { useEffect, useState } from "react";

export default function HealthPage() {
  const [status, setStatus] = useState({
    backend: false,
    spark: false,
    hdfs: false,
  });

  useEffect(() => {
    Promise.all([
      fetch("/api/health")
        .then((r) => r.ok)
        .catch(() => false),
      fetch("/api/health/spark")
        .then((r) => r.ok)
        .catch(() => false),
      fetch("/api/health/hdfs")
        .then((r) => r.ok)
        .catch(() => false),
    ]).then(([b, s, h]) => setStatus({ backend: b, spark: s, hdfs: h }));
  }, []);

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">System Health</h1>
      <ul>
        <li>Backend: {status.backend ? "OK" : "DOWN"}</li>
        <li>Spark master: {status.spark ? "OK" : "DOWN"}</li>
        <li>HDFS: {status.hdfs ? "OK" : "DOWN"}</li>
      </ul>
    </div>
  );
}
