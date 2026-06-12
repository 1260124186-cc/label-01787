import React, {
  useRef,
  useEffect,
  useState,
  useCallback,
  useImperativeHandle,
  forwardRef,
  useMemo
} from 'react'
import { View, Canvas } from '@tarojs/components'
import Taro, { getEnv, ENV_TYPE } from '@tarojs/taro'
import type {
  InkStroke,
  InkPoint,
  InkBrushConfig,
  InkTool
} from '@/types/ink'
import { generateStrokeId, computeBoundingBox } from '@/types/ink'
import styles from './index.module.scss'

export interface InkCanvasHandle {
  undo: () => void
  redo: () => void
  clear: () => void
  getStrokes: () => InkStroke[]
  setStrokes: (strokes: InkStroke[]) => void
  exportCanvas: () => Promise<string | null>
  getStrokesModified: () => InkStroke[]
  getDeletedStrokeIds: () => string[]
  resetModified: () => void
}

interface InkCanvasProps {
  width: number
  height: number
  brushConfig: InkBrushConfig
  strokes?: InkStroke[]
  onStrokeStart?: () => void
  onStrokeEnd?: (stroke: InkStroke) => void
  onStrokesChange?: (strokes: InkStroke[]) => void
  readonly?: boolean
  zoom?: number
}

const MAX_HISTORY = 50

type EnvType = 'h5' | 'weapp'

const detectEnv = (): EnvType => {
  try {
    const env = getEnv()
    if (env === ENV_TYPE.WEAPP) return 'weapp'
    const info: any = Taro.getSystemInfoSync()
    if (info.environment === 'wxdevtools' || info.platform === 'devtools') return 'weapp'
    return 'h5'
  } catch {
    return 'h5'
  }
}

type AnyCtx = any
type AnyCanvas = any

const InkCanvas = forwardRef<InkCanvasHandle, InkCanvasProps>((props, ref) => {
  const {
    width,
    height,
    brushConfig,
    strokes: externalStrokes,
    onStrokeStart,
    onStrokeEnd,
    onStrokesChange,
    readonly = false,
    zoom = 1
  } = props

  const canvasRef = useRef<AnyCanvas>(null)
  const ctxRef = useRef<AnyCtx>(null)
  const [strokeList, setStrokeList] = useState<InkStroke[]>([])
  const [history, setHistory] = useState<InkStroke[][]>([[]])
  const [historyIndex, setHistoryIndex] = useState(0)
  const isDrawingRef = useRef(false)
  const currentStrokeRef = useRef<InkStroke | null>(null)
  const lastPointRef = useRef<InkPoint | null>(null)
  const modifiedStrokesRef = useRef<Map<string, InkStroke>>(new Map())
  const deletedStrokeIdsRef = useRef<Set<string>>(new Set())
  const envRef = useRef<EnvType>('h5')
  const dprRef = useRef(1)
  const initializedRef = useRef(false)
  const wrapperRef = useRef<any>(null)

  const canvasId = useMemo(() => {
    const rand = Math.random().toString(36).substring(2, 11)
    return `ink_canvas_${rand}`
  }, [])

  const brushConfigRef = useRef(brushConfig)
  const readonlyRef = useRef(readonly)
  const onStrokeStartRef = useRef(onStrokeStart)
  const onStrokeEndRef = useRef(onStrokeEnd)
  const onStrokesChangeRef = useRef(onStrokesChange)
  const strokeListRef = useRef<InkStroke[]>([])
  const historyRef = useRef<InkStroke[][]>([[]])
  const historyIndexRef = useRef(0)

  useEffect(() => { brushConfigRef.current = brushConfig }, [brushConfig])
  useEffect(() => { readonlyRef.current = readonly }, [readonly])
  useEffect(() => { onStrokeStartRef.current = onStrokeStart }, [onStrokeStart])
  useEffect(() => { onStrokeEndRef.current = onStrokeEnd }, [onStrokeEnd])
  useEffect(() => { onStrokesChangeRef.current = onStrokesChange }, [onStrokesChange])
  useEffect(() => { strokeListRef.current = strokeList }, [strokeList])
  useEffect(() => { historyRef.current = history }, [history])
  useEffect(() => { historyIndexRef.current = historyIndex }, [historyIndex])

  useEffect(() => {
    envRef.current = detectEnv()
    const info = Taro.getSystemInfoSync()
    dprRef.current = info.pixelRatio || 2
  }, [])

  useEffect(() => {
    if (externalStrokes && externalStrokes.length > 0 && !initializedRef.current) {
      setStrokeList(externalStrokes)
      setHistory([externalStrokes])
      setHistoryIndex(0)
      modifiedStrokesRef.current.clear()
      deletedStrokeIdsRef.current.clear()
      initializedRef.current = true
    }
  }, [externalStrokes])

  const getCanvasContext = useCallback((): Promise<AnyCtx> => {
    return new Promise((resolve) => {
      if (ctxRef.current) {
        resolve(ctxRef.current)
        return
      }

      if (envRef.current === 'h5') {
        setTimeout(() => {
          const canvas = canvasRef.current
          if (canvas && canvas.getContext) {
            const ctx = canvas.getContext('2d')
            if (ctx) {
              const dpr = dprRef.current
              if (dpr > 1 && !canvas.__dprScaled) {
                canvas.width = width * dpr
                canvas.height = height * dpr
                ctx.scale(dpr, dpr)
                canvas.__dprScaled = true
              }
              ctxRef.current = ctx
              resolve(ctx)
            }
          }
        }, 16)
      } else {
        const query = Taro.createSelectorQuery()
        query.select('#' + canvasId)
          .fields({ node: true, size: true })
          .exec((res: any[]) => {
            if (res && res[0] && res[0].node) {
              const canvasNode = res[0].node
              const ctx = canvasNode.getContext('2d')
              const dpr = dprRef.current
              canvasNode.width = res[0].width * dpr
              canvasNode.height = res[0].height * dpr
              ctx.scale(dpr, dpr)
              ctxRef.current = ctx
              canvasRef.current = canvasNode
              resolve(ctx)
            } else {
              resolve(null as any)
            }
          })
      }
    })
  }, [canvasId, width, height])

  const clearCanvasArea = useCallback((ctx: AnyCtx) => {
    if (!ctx) return
    ctx.clearRect(0, 0, width, height)
  }, [width, height])

  const drawStrokeOnCanvas = useCallback((ctx: AnyCtx, stroke: InkStroke, scale = 1) => {
    if (!ctx || !stroke.points || stroke.points.length < 2) return

    ctx.save()
    ctx.strokeStyle = stroke.color
    ctx.lineWidth = stroke.lineWidth * scale
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.globalAlpha = stroke.opacity

    if (stroke.strokeType === 'highlighter') {
      ctx.globalAlpha = (stroke.opacity ?? 1) * 0.5
    }

    if (stroke.strokeType === 'eraser') {
      if (envRef.current === 'h5') {
        ctx.globalCompositeOperation = 'destination-out'
      } else {
        ctx.globalCompositeOperation = 'source-over'
      }
      ctx.strokeStyle = '#FFFFFF'
      ctx.lineWidth = stroke.lineWidth * 2 * scale
    } else {
      ctx.globalCompositeOperation = 'source-over'
    }

    ctx.beginPath()
    const points = stroke.points
    ctx.moveTo(points[0].x * scale, points[0].y * scale)

    for (let i = 1; i < points.length - 1; i++) {
      const xc = (points[i].x + points[i + 1].x) / 2 * scale
      const yc = (points[i].y + points[i + 1].y) / 2 * scale
      ctx.quadraticCurveTo(points[i].x * scale, points[i].y * scale, xc, yc)
    }

    if (points.length >= 2) {
      const last = points[points.length - 1]
      ctx.lineTo(last.x * scale, last.y * scale)
    }

    ctx.stroke()
    ctx.restore()
  }, [])

  const renderAllStrokes = useCallback(async () => {
    const ctx = await getCanvasContext()
    if (!ctx) return

    clearCanvasArea(ctx)

    for (const stroke of strokeList) {
      drawStrokeOnCanvas(ctx, stroke, 1)
    }
  }, [getCanvasContext, clearCanvasArea, drawStrokeOnCanvas, strokeList])

  useEffect(() => {
    if (strokeList.length > 0 || initializedRef.current) {
      renderAllStrokes()
    }
  }, [strokeList, renderAllStrokes])

  const getTouchPoint = useCallback((clientX: number, clientY: number, pressure: number = 0.5): InkPoint | null => {
    const canvas = canvasRef.current
    if (!canvas) return null

    let rect: { left: number; top: number }
    if (envRef.current === 'h5' && typeof canvas.getBoundingClientRect === 'function') {
      rect = canvas.getBoundingClientRect()
    } else {
      rect = { left: 0, top: 0 }
    }

    const x = (clientX - rect.left) / zoom
    const y = (clientY - rect.top) / zoom

    return {
      x: Math.max(0, Math.min(width, x)),
      y: Math.max(0, Math.min(height, y)),
      pressure,
      timestamp: Date.now()
    }
  }, [width, height, zoom])

  const drawSegment = useCallback(async (from: InkPoint, to: InkPoint) => {
    const ctx = await getCanvasContext()
    if (!ctx) return

    const currentTool: InkTool = brushConfigRef.current.tool
    if (currentTool === 'none') return

    const isEraser = currentTool === 'eraser'
    ctx.save()

    if (isEraser) {
      if (envRef.current === 'h5') {
        ctx.globalCompositeOperation = 'destination-out'
      } else {
        ctx.globalCompositeOperation = 'source-over'
      }
      ctx.strokeStyle = '#FFFFFF'
      ctx.lineWidth = brushConfigRef.current.lineWidth * 2
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
    } else {
      ctx.globalCompositeOperation = 'source-over'
      ctx.strokeStyle = brushConfigRef.current.color
      ctx.lineWidth = brushConfigRef.current.lineWidth
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.globalAlpha = brushConfigRef.current.opacity ?? 1

      if (currentTool === 'highlighter') {
        ctx.globalAlpha = (brushConfigRef.current.opacity ?? 1) * 0.5
      }
    }

    ctx.beginPath()
    ctx.moveTo(from.x, from.y)
    const xc = (from.x + to.x) / 2
    const yc = (from.y + to.y) / 2
    ctx.quadraticCurveTo(from.x, from.y, xc, yc)
    ctx.stroke()
    ctx.restore()
  }, [getCanvasContext])

  const pushHistory = useCallback((newStrokes: InkStroke[]) => {
    const currentHistory = historyRef.current
    const currentIdx = historyIndexRef.current
    const newHistory = currentHistory.slice(0, currentIdx + 1)
    newHistory.push([...newStrokes])

    if (newHistory.length > MAX_HISTORY) {
      newHistory.shift()
    }

    setHistory(newHistory)
    setHistoryIndex(newHistory.length - 1)
  }, [])

  const doStrokeStart = useCallback(async (clientX: number, clientY: number, pressure: number = 0.5) => {
    if (readonlyRef.current || brushConfigRef.current.tool === 'none') return

    const point = getTouchPoint(clientX, clientY, pressure)
    if (!point) return

    isDrawingRef.current = true
    onStrokeStartRef.current?.()

    const currentTool = brushConfigRef.current.tool
    const strokeType = currentTool === 'eraser' ? 'eraser' : currentTool

    const newStroke: InkStroke = {
      id: generateStrokeId(),
      strokeId: generateStrokeId(),
      strokeType: strokeType as any,
      color: brushConfigRef.current.color,
      lineWidth: brushConfigRef.current.lineWidth,
      opacity: brushConfigRef.current.opacity,
      points: [point],
      boundingBox: { x: point.x, y: point.y, w: 0, h: 0 }
    }

    currentStrokeRef.current = newStroke
    lastPointRef.current = point

    if (currentTool !== 'eraser') {
      const ctx = await getCanvasContext()
      if (ctx) {
        ctx.save()
        ctx.fillStyle = brushConfigRef.current.color
        ctx.globalAlpha = brushConfigRef.current.opacity ?? 1
        if (currentTool === 'highlighter') {
          ctx.globalAlpha = (brushConfigRef.current.opacity ?? 1) * 0.5
        }
        ctx.beginPath()
        ctx.arc(point.x, point.y, brushConfigRef.current.lineWidth / 2, 0, Math.PI * 2)
        ctx.fill()
        ctx.restore()
      }
    }
  }, [getTouchPoint, getCanvasContext])

  const doStrokeMove = useCallback(async (clientX: number, clientY: number, pressure: number = 0.5) => {
    if (!isDrawingRef.current || !currentStrokeRef.current || !lastPointRef.current) return

    const point = getTouchPoint(clientX, clientY, pressure)
    if (!point) return

    const currentStroke = currentStrokeRef.current
    currentStroke.points.push(point)

    const bbox = computeBoundingBox(currentStroke.points)
    currentStroke.boundingBox = bbox

    await drawSegment(lastPointRef.current, point)
    lastPointRef.current = point
  }, [getTouchPoint, drawSegment])

  const doStrokeEnd = useCallback(() => {
    if (!isDrawingRef.current || !currentStrokeRef.current) return

    isDrawingRef.current = false
    const finishedStroke = currentStrokeRef.current

    if (finishedStroke.points.length >= 2) {
      const currentTool = brushConfigRef.current.tool
      const currentList = strokeListRef.current

      if (currentTool === 'eraser') {
        const newStrokes = currentList.filter(s => {
          if (!s.boundingBox || !finishedStroke.boundingBox) return true
          const b1 = s.boundingBox
          const b2 = finishedStroke.boundingBox
          return !(
            b1.x < b2.x + b2.w &&
            b1.x + b1.w > b2.x &&
            b1.y < b2.y + b2.h &&
            b1.y + b1.h > b2.y
          )
        })
        const erased = currentList.length - newStrokes.length
        if (erased > 0) {
          currentList.forEach(s => {
            if (!newStrokes.find(ns => ns.id === s.id)) {
              deletedStrokeIdsRef.current.add(s.strokeId)
              modifiedStrokesRef.current.delete(s.strokeId)
            }
          })
        }
        setStrokeList(newStrokes)
        pushHistory(newStrokes)
        onStrokesChangeRef.current?.(newStrokes)
      } else {
        modifiedStrokesRef.current.set(finishedStroke.strokeId, finishedStroke)
        const newStrokes = [...currentList, finishedStroke]
        setStrokeList(newStrokes)
        pushHistory(newStrokes)
        onStrokeEndRef.current?.(finishedStroke)
        onStrokesChangeRef.current?.(newStrokes)
      }
    }

    currentStrokeRef.current = null
    lastPointRef.current = null
  }, [pushHistory])

  useEffect(() => {
    if (envRef.current !== 'h5') return

    const setupH5Listeners = () => {
      const el = wrapperRef.current
      if (!el) return false

      const domEl = el.$node || el
      if (!domEl || typeof domEl.addEventListener !== 'function') return false

      const onMouseDown = (e: MouseEvent) => {
        e.preventDefault()
        doStrokeStart(e.clientX, e.clientY, 0.5)
      }
      const onMouseMove = (e: MouseEvent) => {
        if (!isDrawingRef.current) return
        e.preventDefault()
        doStrokeMove(e.clientX, e.clientY, 0.5)
      }
      const onMouseUp = () => doStrokeEnd()
      const onMouseLeave = () => doStrokeEnd()

      domEl.addEventListener('mousedown', onMouseDown)
      domEl.addEventListener('mousemove', onMouseMove)
      domEl.addEventListener('mouseup', onMouseUp)
      domEl.addEventListener('mouseleave', onMouseLeave)

      ;(domEl as any).__inkListeners = { onMouseDown, onMouseMove, onMouseUp, onMouseLeave }
      return true
    }

    const timer = setTimeout(() => {
      setupH5Listeners()
    }, 50)

    return () => {
      clearTimeout(timer)
      const el = wrapperRef.current
      const domEl = el?.$node || el
      const listeners = domEl?.__inkListeners
      if (listeners && domEl?.removeEventListener) {
        domEl.removeEventListener('mousedown', listeners.onMouseDown)
        domEl.removeEventListener('mousemove', listeners.onMouseMove)
        domEl.removeEventListener('mouseup', listeners.onMouseUp)
        domEl.removeEventListener('mouseleave', listeners.onMouseLeave)
      }
    }
  }, [doStrokeStart, doStrokeMove, doStrokeEnd])

  const handleTouchStart = useCallback((e: any) => {
    e.preventDefault?.()
    const touch = e.touches?.[0] || e.changedTouches?.[0]
    if (!touch) return
    const pressure = touch.force || touch.pressure || 0.5
    const cx = touch.clientX ?? touch.x ?? 0
    const cy = touch.clientY ?? touch.y ?? 0
    doStrokeStart(cx, cy, pressure)
  }, [doStrokeStart])

  const handleTouchMove = useCallback((e: any) => {
    e.preventDefault?.()
    const touch = e.touches?.[0] || e.changedTouches?.[0]
    if (!touch) return
    const pressure = touch.force || touch.pressure || 0.5
    const cx = touch.clientX ?? touch.x ?? 0
    const cy = touch.clientY ?? touch.y ?? 0
    doStrokeMove(cx, cy, pressure)
  }, [doStrokeMove])

  const handleTouchEnd = useCallback((e: any) => {
    e.preventDefault?.()
    doStrokeEnd()
  }, [doStrokeEnd])

  const undo = useCallback(() => {
    if (historyIndex <= 0) return

    const newIndex = historyIndex - 1
    setHistoryIndex(newIndex)
    const prevStrokes = history[newIndex] || []
    setStrokeList(prevStrokes)
    onStrokesChange?.(prevStrokes)

    prevStrokes.forEach(s => {
      modifiedStrokesRef.current.set(s.strokeId, s)
    })
  }, [historyIndex, history, onStrokesChange])

  const redo = useCallback(() => {
    if (historyIndex >= history.length - 1) return

    const newIndex = historyIndex + 1
    setHistoryIndex(newIndex)
    const nextStrokes = history[newIndex] || []
    setStrokeList(nextStrokes)
    onStrokesChange?.(nextStrokes)

    nextStrokes.forEach(s => {
      modifiedStrokesRef.current.set(s.strokeId, s)
    })
  }, [historyIndex, history, onStrokesChange])

  const clear = useCallback(() => {
    strokeList.forEach(s => {
      deletedStrokeIdsRef.current.add(s.strokeId)
    })
    modifiedStrokesRef.current.clear()

    const newStrokes: InkStroke[] = []
    setStrokeList(newStrokes)
    pushHistory(newStrokes)
    onStrokesChange?.(newStrokes)
  }, [strokeList, pushHistory, onStrokesChange])

  const getStrokes = useCallback((): InkStroke[] => {
    return strokeList
  }, [strokeList])

  const setStrokes = useCallback((newStrokes: InkStroke[]) => {
    setStrokeList(newStrokes)
    setHistory([newStrokes])
    setHistoryIndex(0)
    modifiedStrokesRef.current.clear()
    deletedStrokeIdsRef.current.clear()
    initializedRef.current = true
  }, [])

  const exportCanvas = useCallback(async (): Promise<string | null> => {
    const ctx = await getCanvasContext()
    if (!ctx) return null

    if (envRef.current === 'h5') {
      const canvas = canvasRef.current
      return canvas && typeof canvas.toDataURL === 'function'
        ? canvas.toDataURL('image/png')
        : null
    } else {
      return new Promise((resolve) => {
        const canvas = canvasRef.current
        if (canvas && typeof canvas.toDataURL === 'function') {
          canvas.toDataURL({
            type: 'image/png',
            quality: 1,
            success: (res: any) => resolve(res.tempFilePath || null),
            fail: () => resolve(null)
          })
        } else {
          resolve(null)
        }
      })
    }
  }, [getCanvasContext])

  const getStrokesModified = useCallback((): InkStroke[] => {
    return Array.from(modifiedStrokesRef.current.values())
  }, [])

  const getDeletedStrokeIds = useCallback((): string[] => {
    return Array.from(deletedStrokeIdsRef.current.values())
  }, [])

  const resetModified = useCallback(() => {
    modifiedStrokesRef.current.clear()
    deletedStrokeIdsRef.current.clear()
  }, [])

  useImperativeHandle(ref, () => ({
    undo,
    redo,
    clear,
    getStrokes,
    setStrokes,
    exportCanvas,
    getStrokesModified,
    getDeletedStrokeIds,
    resetModified
  }), [undo, redo, clear, getStrokes, setStrokes, exportCanvas, getStrokesModified, getDeletedStrokeIds, resetModified])

  const canvasStyle = useMemo(() => ({
    width: `${width}px`,
    height: `${height}px`
  }), [width, height])

  if (width <= 0 || height <= 0) {
    return <View className={styles.inkCanvasWrapper} />
  }

  if (envRef.current === 'h5') {
    return (
      <View
        ref={wrapperRef}
        className={styles.inkCanvasWrapper}
        style={canvasStyle}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onTouchCancel={handleTouchEnd}
      >
        <canvas
          ref={canvasRef as any}
          className={styles.inkCanvas}
          style={canvasStyle}
        />
      </View>
    )
  }

  return (
    <View
      ref={wrapperRef}
      className={styles.inkCanvasWrapper}
      style={canvasStyle}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onTouchCancel={handleTouchEnd}
    >
      <Canvas
        id={canvasId}
        type='2d'
        className={styles.inkCanvas}
        style={canvasStyle}
      />
    </View>
  )
})

InkCanvas.displayName = 'InkCanvas'

export default InkCanvas
