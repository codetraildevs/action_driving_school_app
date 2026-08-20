import { NextRequest, NextResponse } from 'next/server';
import { LRUCache } from 'lru-cache';

interface RateLimitOptions {
  interval: number; // in milliseconds
  uniqueTokenPerInterval: number;
}

export function withRateLimit(options: RateLimitOptions) {
  const tokenCache = new LRUCache({
    max: options.uniqueTokenPerInterval || 500,
    ttl: options.interval || 60000,
  });

  return function (handler: any) {
    return async (req: NextRequest, context: any) => {
      // Get IP address from headers
      const forwarded = req.headers.get('x-forwarded-for');
      const realIp = req.headers.get('x-real-ip');
      const ip = forwarded?.split(',')[0] || realIp || 'anonymous';
      
      const tokenCount = (tokenCache.get(ip) as number[]) || [0];
      
      if (tokenCount[0] === 0) {
        tokenCache.set(ip, [1]);
      } else {
        tokenCount[0] += 1;
        tokenCache.set(ip, tokenCount);
      }

      const currentUsage = tokenCount[0];
      const isRateLimited = currentUsage >= (options.uniqueTokenPerInterval || 10);

      if (isRateLimited) {
        return NextResponse.json(
          { error: 'Rate limit exceeded', message: 'Too many requests' },
          { status: 429 }
        );
      }

      return handler(req, context);
    };
  };
}