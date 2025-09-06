import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',
  trailingSlash: false,
  reactStrictMode: true,
  swcMinify: true,
}

export default nextConfig;
