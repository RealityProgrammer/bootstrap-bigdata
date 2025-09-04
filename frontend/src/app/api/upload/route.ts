import { NextResponse } from 'next/server';

export async function POST(req: Request) {
    try {
        const body = await req.formData();
        // In production this should forward to backend (localhost:8080) or use fetch with FormData
        // For now return a stub success to allow FE development without backend.
        return NextResponse.json({ ok: true, message: 'Stub: received upload (FE stub). In production this proxies to backend /api/upload.' });
    } catch (err) {
        return NextResponse.json({ ok: false, error: String(err) }, { status: 500 });
    }
}
