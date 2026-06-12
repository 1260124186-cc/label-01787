export type InkTool = 'pen' | 'highlighter' | 'eraser' | 'none'

export type InkToolType = 'pen' | 'highlighter' | 'eraser'

export interface InkPoint {
  x: number
  y: number
  pressure?: number
  timestamp?: number
}

export interface InkStroke {
  id: string
  strokeId: string
  strokeType: InkToolType
  color: string
  lineWidth: number
  opacity: number
  points: InkPoint[]
  boundingBox?: {
    x: number
    y: number
    w: number
    h: number
  }
  createdAt?: string
  updatedAt?: string
}

export interface InkStrokeDTO {
  id?: number
  bookId: number
  pageNum: number
  strokeId: string
  strokeType: InkToolType
  color: string
  lineWidth: number
  opacity: number
  points: string
  boundingBox?: string
}

export interface InkBrushConfig {
  tool: InkTool
  color: string
  lineWidth: number
  opacity: number
}

export interface InkPageData {
  bookId: number
  pageNum: number
  strokes: InkStroke[]
}

export interface InkBatchSyncRequest {
  bookId: number
  pageNum: number
  strokes: InkStrokeDTO[]
  deletedStrokeIds: string[]
}

export interface InkBatchSyncResult {
  saved: number
  deleted: number
  strokes: InkStroke[]
}

export const DEFAULT_INK_CONFIG: InkBrushConfig = {
  tool: 'pen',
  color: '#1A1A1A',
  lineWidth: 2,
  opacity: 1
}

export const INK_COLORS = [
  '#1A1A1A',
  '#E53935',
  '#FB8C00',
  '#FDD835',
  '#43A047',
  '#1E88E5',
  '#8E24AA',
  '#00ACC1',
  '#FFFFFF'
]

export const INK_LINE_WIDTHS = [1, 2, 3, 4, 6, 8, 12]

export const HIGHLIGHTER_COLORS = [
  { color: '#FFF176', opacity: 0.5, label: '黄' },
  { color: '#81C784', opacity: 0.5, label: '绿' },
  { color: '#64B5F6', opacity: 0.5, label: '蓝' },
  { color: '#F48FB1', opacity: 0.5, label: '粉' },
  { color: '#FFB74D', opacity: 0.5, label: '橙' },
  { color: '#CE93D8', opacity: 0.5, label: '紫' }
]

export function generateStrokeId(): string {
  return `stroke_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

export function serializePoints(points: InkPoint[]): string {
  return JSON.stringify(points.map(p => [p.x, p.y, p.pressure ?? 0.5]))
}

export function deserializePoints(str: string): InkPoint[] {
  try {
    const arr = JSON.parse(str)
    return arr.map((p: number[]) => ({
      x: p[0],
      y: p[1],
      pressure: p[2] ?? 0.5
    }))
  } catch {
    return []
  }
}

export function serializeBoundingBox(bb?: InkStroke['boundingBox']): string | undefined {
  if (!bb) return undefined
  return `${bb.x},${bb.y},${bb.w},${bb.h}`
}

export function deserializeBoundingBox(str?: string): InkStroke['boundingBox'] | undefined {
  if (!str) return undefined
  const parts = str.split(',').map(Number)
  if (parts.length === 4) {
    return { x: parts[0], y: parts[1], w: parts[2], h: parts[3] }
  }
  return undefined
}

export function strokeToDTO(stroke: InkStroke, bookId: number, pageNum: number): InkStrokeDTO {
  return {
    strokeId: stroke.strokeId,
    bookId,
    pageNum,
    strokeType: stroke.strokeType,
    color: stroke.color,
    lineWidth: stroke.lineWidth,
    opacity: stroke.opacity,
    points: serializePoints(stroke.points),
    boundingBox: serializeBoundingBox(stroke.boundingBox)
  }
}

export function dtoToStroke(dto: any): InkStroke {
  return {
    id: dto.strokeId,
    strokeId: dto.strokeId,
    strokeType: dto.strokeType as InkToolType || 'pen',
    color: dto.color,
    lineWidth: Number(dto.lineWidth) || 2,
    opacity: Number(dto.opacity) ?? 1,
    points: typeof dto.points === 'string' ? deserializePoints(dto.points) : (dto.points || []),
    boundingBox: deserializeBoundingBox(dto.boundingBox),
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt
  }
}

export function computeBoundingBox(points: InkPoint[]): InkStroke['boundingBox'] {
  if (points.length === 0) {
    return { x: 0, y: 0, w: 0, h: 0 }
  }
  let minX = Infinity, minY = Infinity
  let maxX = -Infinity, maxY = -Infinity
  for (const p of points) {
    if (p.x < minX) minX = p.x
    if (p.y < minY) minY = p.y
    if (p.x > maxX) maxX = p.x
    if (p.y > maxY) maxY = p.y
  }
  return {
    x: minX,
    y: minY,
    w: maxX - minX,
    h: maxY - minY
  }
}
