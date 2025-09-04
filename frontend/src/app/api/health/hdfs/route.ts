import { NextResponse } from 'next/server';

export async function GET() {
    return NextResponse.json({ ok: true, hdfs: 'stub: check hdfs at http://localhost:9870 or hdfs://localhost:9000' });
}
