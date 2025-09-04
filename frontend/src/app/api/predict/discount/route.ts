import { NextResponse } from 'next/server';

export async function GET(req: Request) {
    const url = new URL(req.url);
    const rate = url.searchParams.get('rate') || '0.10';
    return NextResponse.json({ ok: true, path: `/tmp/predictions_discount_${rate.replace('.', '')}.csv` });
}
