import { NextResponse } from 'next/server';

export async function GET() {
    return NextResponse.json({ files: ['/tmp/analysis_summary.csv', '/tmp/predictions_discount_010.csv'] });
}
