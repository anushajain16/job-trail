/**
 * Saves a blob already in memory (e.g. from {@link import('@/lib/api-client').authFetchFile})
 * as a file, via a throwaway object URL and an invisible, immediately-clicked
 * `<a download>` — the standard way to trigger a browser download from
 * script rather than a real navigation.
 */
export function downloadFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
