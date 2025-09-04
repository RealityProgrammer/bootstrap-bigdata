import { NextResponse } from 'next/server';

export async function GET() {
    return NextResponse.json({ ok: true, spark: 'stub: check spark at http://localhost:9090' });
}
