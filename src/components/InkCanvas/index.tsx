import React, { useRef, useEffect, useState, useCallback, useImperativeHandle, forwardRef, useMemo } from 'react'
import { View, Canvas } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
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

  const canvasRef = useRef<any>(null)
  const ctxRef = useRef<CanvasRenderingContext2D | any>(null)
  const [strokes, setStrokes] = useState<InkStroke[]>([])
  const [history, setHistory] = useState<InkStroke[][]>([[]])
  const [historyIndex, setHistoryIndex] = useState(0)
  const [isDrawing, setIsDrawing] = useState(false)
  const currentStrokeRef = useRef<InkStroke | null>(null)
  const lastPointRef = useRef<InkPoint | null>(null)
  const modifiedStrokesRef = useRef<Map<string, InkStroke>>(new Map())
  const deletedStrokeIdsRef = useRef<Set<string>>(new Set())
  const envRef = useRef<'h5' | 'weapp'>('h5')
  const dprRef = useRef(1)
  const canvasId = useMemo(() => `ink_canvas_${Math.random().toString(36).substr(2, 9)}`, [])

  useEffect(() => {
    const info = Taro.getSystemInfoSync()
    envRef.current = info.platform === 'devtools' || info.platform === 'android' || info.platform === 'ios' ? 'weapp' : 'h5'
    dprRef.current = info.pixelRatio || 1
  }, [])

  useEffect(() => {
    if (externalStrokes) {
      setStrokes(externalStrokes)
      setHistory([externalStrokes])
      setHistoryIndex(0)
      modifiedStrokesRef.current.clear()
      deletedStrokeIdsRef.current.clear()
    }
  }, [externalStrokes])

  const getCanvasContext = useCallback((): Promise<CanvasRenderingContext2D | any> => {
    return new Promise((resolve) => {
      if (ctxRef.current) {
        resolve(ctxRef.current)
        return
      }

      if (envRef.current === 'h5') {
        setTimeout(() => {
          const canvas = canvasRef.current
          if (canvas) {
            const ctx = canvas.getContext('2d')
            if (ctx) {
              ctxRef.current = ctx
              resolve(ctx)
              return
            }
          }
          resolve(null)
        }, 50)
      } else {
        const query = Taro.createSelectorQuery()
        query.select(`#${canvasId}`)
          .fields({ node: true, size: true })
          .exec((res) => {
            if (res && res[0]) {
              const canvas = res[0].node
              const ctx = canvas.getContext('2d')
              const dpr = dprRef.current
              canvas.width = width * dpr
              canvas.height = height * dpr
              ctx.scale(dpr, dpr)
              ctxRef.current = ctx
              resolve(ctx)
            } else {
              resolve(null)
            }
          })
      }
    })
  }, [canvasId, width, height])

  const clearCanvas = useCallback((ctx: CanvasRenderingContext2D | any) => {
    if (!ctx) return
    if (envRef.current === 'h5') {
      const canvas = canvasRef.current
      if (canvas) {
        ctx.clearRect(0, 0, canvas.width, canvas.height)
      }
    } else {
      ctx.clearRect(0, 0, width, height)
    }
  }, [width, height])

  const renderStroke = useCallback((ctx: CanvasRenderingContext2D | any, stroke: InkStroke, scale = 1) => {
    if (!ctx || !stroke.points || stroke.points.length === 0) return

    const isEraser = stroke.strokeType === 'eraser'
    ctx.save()

    if (isEraser) {
      ctx.globalCompositeOperation = envRef.current === 'h5' ? 'destination-out' : 'source-over'
      ctx.strokeStyle = '#FFFFFF'
      ctx.lineWidth = stroke.lineWidth * 2 * scale
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
    } else {
      ctx.globalCompositeOperation = 'source-over'
      ctx.strokeStyle = stroke.color
      ctx.lineWidth = stroke.lineWidth * scale
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.globalAlpha = stroke.opacity ?? 1

      if (stroke.strokeType === 'highlighter') {
        ctx.globalAlpha = (stroke.opacity ?? 1) * 0.5
      }
    }

    const points = stroke.points
    if (points.length === 1) {
      const p = points[0]
      ctx.beginPath()
      ctx.arc(p.x * scale, p.y * scale, stroke.lineWidth * scale / 2, 0, Math.PI * 2)
      ctx.fillStyle = isEraser ? '#FFFFFF' : stroke.color
      ctx.fill()
    } else {
      ctx.beginPath()
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
    }

    ctx.restore()
  }, [])

  const renderAll = useCallback(async () => {
    const ctx = await getCanvasContext()
    if (!ctx) return
    clearCanvas(ctx)
    for (const stroke of strokes) {
      renderStroke(ctx, stroke)
    }
  }, [getCanvasContext, clearCanvas, renderStroke, strokes])

  useEffect(() => {
    renderAll()
  }, [renderAll])

  useDidShow(() => {
    renderAll()
  })

  const getTouchPoint = (e: any): InkPoint | null => {
    let touch: any = null
    if (e.touches && e.touches.length > 0) {
      touch = e.touches[0]
    } else if (e.changedTouches && e.changedTouches.length > 0) {
      touch = e.changedTouches[0]
    } else if (e.clientX !== undefined) {
      touch = e
    }
    if (!touch) return null

    let x: number, y: number
    const rect = touch.target?.getBoundingClientRect ? touch.target.getBoundingClientRect() : null

    if (envRef.current === 'h5' && rect) {
      x = (touch.clientX - rect.left) / zoom
      y = (touch.clientY - rect.top) / zoom
    } else if (touch.x !== undefined) {
      x = touch.x / zoom
      y = touch.y / zoom
    } else if (touch.pageX !== undefined) {
      x = touch.pageX / zoom
      y = touch.pageY / zoom
    } else {
      x = touch.clientX / zoom
      y = touch.clientY / zoom
    }

    const pressure = (touch as any).force || (touch as any).pressure || 0.5

    return {
      x: Math.max(0, Math.min(width, x)),
      y: Math.max(0, Math.min(height, y)),
      pressure,
      timestamp: Date.now()
    }
  }

  const drawPreviewSegment = async (from: InkPoint, to: InkPoint) => {
    const ctx = await getCanvasContext()
    if (!ctx) return

    const tool: InkTool = brushConfig.tool
    if (tool === 'none') return

    const isEraser = tool === 'eraser'
    ctx.save()

    if (isEraser) {
      ctx.globalCompositeOperation = envRef.current === 'h5' ? 'destination-out' : 'source-over'
      ctx.strokeStyle = '#FFFFFF'
      ctx.lineWidth = brushConfig.lineWidth * 2
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
    } else {
      ctx.globalCompositeOperation = 'source-over'
      ctx.strokeStyle = brushConfig.color
      ctx.lineWidth = brushConfig.lineWidth
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.globalAlpha = brushConfig.opacity ?? 1

      if (tool === 'highlighter') {
        ctx.globalAlpha = (brushConfig.opacity ?? 1) * 0.5
      }
    }

    ctx.beginPath()
    ctx.moveTo(from.x, from.y)
    const xc = (from.x + to.x) / 2
    const yc = (from.y + to.y) / 2
    ctx.quadraticCurveTo(from.x, from.y, xc, yc)
    ctx.lineTo(to.x, to.y)
    ctx.stroke()
    ctx.restore()
  }

  const handleStart = async (e: any) => {
    if (readonly || brushConfig.tool === 'none') return
    e.stopPropagation?.()
    e.preventDefault?.()

    const point = getTouchPoint(e)
    if (!point) return

    onStrokeStart?.()
    setIsDrawing(true)

    const stroke: InkStroke = {
      id: generateStrokeId(),
      strokeId: generateStrokeId(),
      strokeType: (brushConfig.tool === 'none' ? 'pen' : brushConfig.tool) as any,
      color: brushConfig.color,
      lineWidth: brushConfig.lineWidth,
      opacity: brushConfig.opacity,
      points: [point]
    }
    currentStrokeRef.current = stroke
    lastPointRef.current = point
  }

  const handleMove = async (e: any) => {
    if (!isDrawing || !currentStrokeRef.current) return
    e.stopPropagation?.()
    e.preventDefault?.()

    const point = getTouchPoint(e)
    if (!point) return

    const stroke = currentStrokeRef.current
    stroke.points.push(point)

    if (lastPointRef.current) {
      await drawPreviewSegment(lastPointRef.current, point)
    }
    lastPointRef.current = point
  }

  const handleEnd = async (e: any) => {
    if (!isDrawing || !currentStrokeRef.current) return
    e.stopPropagation?.()
    e.preventDefault?.()

    const stroke = currentStrokeRef.current
    stroke.boundingBox = computeBoundingBox(stroke.points)

    if (stroke.points.length === 0) {
      currentStrokeRef.current = null
      lastPointRef.current = null
      setIsDrawing(false)
      return
    }

    const tool: InkTool = brushConfig.tool
    if (tool === 'eraser') {
      const eraserBBox = stroke.boundingBox
      const newStrokes: InkStroke[] = []
      const eraserRadius = stroke.lineWidth

      for (const existing of strokes) {
        if (strokeIntersects(existing, eraserBBox, eraserRadius)) {
          const erased = eraseFromStroke(existing, stroke.points, eraserRadius)
          for (const s of erased) {
            newStrokes.push(s)
            modifiedStrokesRef.current.set(s.strokeId, s)
          }
          if (erased.length === 0 || existing.strokeId !== erased[0]?.strokeId) {
            deletedStrokeIdsRef.current.add(existing.strokeId)
          }
        } else {
          newStrokes.push(existing)
        }
      }

      setStrokes(newStrokes)
      pushToHistory(newStrokes)
      onStrokesChange?.(newStrokes)
    } else {
      modifiedStrokesRef.current.set(stroke.strokeId, stroke)
      const newStrokes = [...strokes, stroke]
      setStrokes(newStrokes)
      pushToHistory(newStrokes)
      onStrokeEnd?.(stroke)
      onStrokesChange?.(newStrokes)
    }

    currentStrokeRef.current = null
    lastPointRef.current = null
    setIsDrawing(false)
    await renderAll()
  }

  const strokeIntersects = (s: InkStroke, bbox: { x: number; y: number; w: number; h: number }, radius: number): boolean => {
    if (!s.boundingBox) return false
    const a = s.boundingBox
    const expanded = {
      x: bbox.x - radius,
      y: bbox.y - radius,
      w: bbox.w + radius * 2,
      h: bbox.h + radius * 2
    }
    return !(
      a.x > expanded.x + expanded.w ||
      a.x + a.w < expanded.x ||
      a.y > expanded.y + expanded.h ||
      a.y + a.h < expanded.y
    )
  }

  const eraseFromStroke = (stroke: InkStroke, eraserPath: InkPoint[], radius: number): InkStroke[] => {
    const points = stroke.points
    const segments: InkPoint[][] = []
    let currentSegment: InkPoint[] = []

    for (const p of points) {
      let erased = false
      for (const ep of eraserPath) {
        const dx = p.x - ep.x
        const dy = p.y - ep.y
        if (Math.sqrt(dx * dx + dy * dy) < radius + stroke.lineWidth / 2) {
          erased = true
          break
        }
      }

      if (erased) {
        if (currentSegment.length > 1) {
          segments.push(currentSegment)
        }
        currentSegment = []
      } else {
        currentSegment.push(p)
      }
    }

    if (currentSegment.length > 1) {
      segments.push(currentSegment)
    }

    if (segments.length === 1 && segments[0].length === points.length) {
      return [stroke]
    }

    return segments.map((seg, idx) => ({
      ...stroke,
      strokeId: `${stroke.strokeId}_s${idx}`,
      id: `${stroke.id}_s${idx}`,
      points: seg,
      boundingBox: computeBoundingBox(seg)
    }))
  }

  const pushToHistory = (newStrokes: InkStroke[]) => {
    const newHistory = history.slice(0, historyIndex + 1)
    newHistory.push(newStrokes)
    if (newHistory.length > 50) {
      newHistory.shift()
    }
    setHistory(newHistory)
    setHistoryIndex(newHistory.length - 1)
  }

  const undo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1
      setHistoryIndex(newIndex)
      setStrokes(history[newIndex])
      onStrokesChange?.(history[newIndex])
    }
  }

  const redo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1
      setHistoryIndex(newIndex)
      setStrokes(history[newIndex])
      onStrokesChange?.(history[newIndex])
    }
  }

  const clear = () => {
    for (const s of strokes) {
      deletedStrokeIdsRef.current.add(s.strokeId)
    }
    setStrokes([])
    pushToHistory([])
    onStrokesChange?.([])
    renderAll()
  }

  const getStrokes = (): InkStroke[] => strokes

  const setStrokes = (newStrokes: InkStroke[]) => {
    setStrokes(newStrokes)
    pushToHistory(newStrokes)
    modifiedStrokesRef.current.clear()
    deletedStrokeIdsRef.current.clear()
    for (const s of newStrokes) {
      modifiedStrokesRef.current.set(s.strokeId, s)
    }
  }

  const getStrokesModified = (): InkStroke[] => {
    return Array.from(modifiedStrokesRef.current.values())
  }

  const getDeletedStrokeIds = (): string[] => {
    return Array.from(deletedStrokeIdsRef.current)
  }

  const resetModified = () => {
    modifiedStrokesRef.current.clear()
    deletedStrokeIdsRef.current.clear()
  }

  const exportCanvas = async (): Promise<string | null> => {
    return new Promise(async (resolve) => {
      if (envRef.current === 'h5') {
        const canvas = canvasRef.current
        if (canvas) {
          resolve(canvas.toDataURL('image/png'))
        } else {
          resolve(null)
        }
      } else {
        const ctx = await getCanvasContext()
        if (!ctx) {
          resolve(null)
          return
        }
        Taro.canvasToTempFilePath({
          canvasId,
          fileType: 'png',
          success: (res) => resolve(res.tempFilePath),
          fail: () => resolve(null)
        } as any)
      }
    })
  }

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
  }))

  return (
    <View
      className={styles.inkCanvasWrap}
      style={{
        width: `${width}px`,
        height: `${height}px`,
        position: 'relative'
      }}
    >
      <Canvas
        id={canvasId}
        ref={canvasRef}
        type={envRef.current === 'weapp' ? '2d' : undefined}
        className={styles.inkCanvas}
        style={{
          width: `${width}px`,
          height: `${height}px`,
          touchAction: 'none',
          cursor: readonly ? 'default' : (brushConfig.tool === 'none' ? 'default' : 'crosshair')
        }}
        onTouchStart={handleStart}
        onTouchMove={handleMove}
        onTouchEnd={handleEnd}
        onTouchCancel={handleEnd}
        onMouseDown={handleStart}
        onMouseMove={handleMove}
        onMouseUp={handleEnd}
        onMouseLeave={handleEnd}
      />
    </View>
  )
})

InkCanvas.displayName = 'InkCanvas'

export default InkCanvas
