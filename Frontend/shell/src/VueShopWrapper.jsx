import React, { useEffect, useRef, useState } from 'react'
import { createApp } from 'vue'
import Shop from '../../microclient-vue/src/components/Shop.vue'

export default function VueShopWrapper() {
  const mountRef = useRef(null)
  const [mounted, setMounted] = useState(false)
  useEffect(() => {
    if (!mountRef.current || mounted) return
    const app = createApp(Shop)
    app.mount(mountRef.current)
    mountRef.current.__vue_app__ = app
    setMounted(true)
    return () => {
      if (mountRef.current && mountRef.current.__vue_app__) {
        mountRef.current.__vue_app__.unmount()
        delete mountRef.current.__vue_app__
      }
    }
  }, [mounted])

  return (
    <div style={{ position:'relative' }}>
      {!mounted && <div style={{position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center', fontSize:12, opacity:.6}}>Lade Vue Shop…</div>}
      <div ref={mountRef} />
    </div>
  )
}
