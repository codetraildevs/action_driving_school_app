/** Preserve exact-number priority, then variant priority, then database ID order. */
export function orderPhoneCandidates<T extends { id: number; phoneNumber: string }>(
  rows: T[], variants: string[],
): T[] {
  const seen = new Set<number>();
  const result: T[] = [];
  for (const variant of variants) {
    for (const row of rows) {
      if (row.phoneNumber === variant && !seen.has(row.id)) {
        seen.add(row.id);
        result.push(row);
      }
    }
  }
  return result;
}
