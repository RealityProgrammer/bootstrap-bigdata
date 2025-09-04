import { NextResponse } from 'next/server';

export async function GET() {
    return NextResponse.json({ ok: true, summary: 'Stubbed analysis result. Replace with backend proxy.' });
}
