import { useEffect, useState } from 'react'

type ScriptStatus = 'idle' | 'loading' | 'ready' | 'error'

const cache = new Map<string, ScriptStatus>()

/**
 * Load a third-party script once per document (Google Identity Services is
 * the only one we use). Returns its load status so callers can render a
 * disabled control until it is ready.
 */
export function useScript(src: string | null): ScriptStatus {
  const [status, setStatus] = useState<ScriptStatus>(() =>
    src ? (cache.get(src) ?? 'loading') : 'idle',
  )

  useEffect(() => {
    if (!src) {
      setStatus('idle')
      return
    }
    if (cache.get(src) === 'ready') {
      setStatus('ready')
      return
    }

    let element = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`)
    if (!element) {
      element = document.createElement('script')
      element.src = src
      element.async = true
      document.head.append(element)
    }

    const onLoad = () => {
      cache.set(src, 'ready')
      setStatus('ready')
    }
    const onError = () => {
      cache.set(src, 'error')
      setStatus('error')
    }

    element.addEventListener('load', onLoad)
    element.addEventListener('error', onError)
    return () => {
      element?.removeEventListener('load', onLoad)
      element?.removeEventListener('error', onError)
    }
  }, [src])

  return status
}
