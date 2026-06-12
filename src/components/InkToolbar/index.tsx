import React, { useState } from 'react'
import { View, Text } from '@tarojs/components'
import type { InkBrushConfig, InkTool } from '@/types/ink'
import { INK_COLORS, INK_LINE_WIDTHS, HIGHLIGHTER_COLORS } from '@/types/ink'
import styles from './index.module.scss'

export interface InkToolbarProps {
  config: InkBrushConfig
  onChange: (config: InkBrushConfig) => void
  onUndo?: () => void
  onRedo?: () => void
  onClear?: () => void
  onToggleAnnotation?: () => void
  canUndo?: boolean
  canRedo?: boolean
  compact?: boolean
}

const IconPen = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 19l7-7 3 3-7 7-3-3z" />
      <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z" />
      <path d="M2 2l7.586 7.586" />
      <circle cx="11" cy="11" r="2" />
    </svg>
  </View>
)

const IconHighlighter = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 11l-4.5 4.5a2.121 2.121 0 0 1-3-3L6 8" />
      <path d="M3 21l3-3" />
      <path d="M14 6l4 4 6-6-4-4z" />
      <path d="M9 11l6 6" />
    </svg>
  </View>
)

const IconEraser = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 20H7L3 16c-1-1-1-3 0-4l10-10c1-1 3-1 4 0l6 6c1 1 1 3 0 4L11 24" />
      <path d="M18 13L8 3" />
    </svg>
  </View>
)

const IconNone = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
    </svg>
  </View>
)

const IconUndo = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 7v6h6" />
      <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6.7 3L3 13" />
    </svg>
  </View>
)

const IconRedo = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 7v6h-6" />
      <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6.7 3L21 13" />
    </svg>
  </View>
)

const IconTrash = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    </svg>
  </View>
)

const IconNote = () => (
  <View className={styles.iconSvg}>
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="8" y1="13" x2="16" y2="13" />
      <line x1="8" y1="17" x2="14" y2="17" />
    </svg>
  </View>
)

const InkToolbar: React.FC<InkToolbarProps> = ({
  config,
  onChange,
  onUndo,
  onRedo,
  onClear,
  onToggleAnnotation,
  canUndo = false,
  canRedo = false,
  compact = false
}) => {
  const [showColorPicker, setShowColorPicker] = useState(false)
  const [showWidthPicker, setShowWidthPicker] = useState(false)
  const [showHighlighterPicker, setShowHighlighterPicker] = useState(false)

  const tools: { tool: InkTool; icon: React.ReactNode; label: string; hint?: string }[] = [
    { tool: 'none', icon: <IconNone />, label: '选择', hint: '浏览' },
    { tool: 'pen', icon: <IconPen />, label: '画笔', hint: '圈注' },
    { tool: 'highlighter', icon: <IconHighlighter />, label: '荧光笔', hint: '高亮' },
    { tool: 'eraser', icon: <IconEraser />, label: '橡皮擦', hint: '擦除' }
  ]

  const handleToolClick = (tool: InkTool) => {
    if (tool === 'highlighter') {
      setShowHighlighterPicker(!showHighlighterPicker)
      setShowColorPicker(false)
      setShowWidthPicker(false)
    } else {
      setShowHighlighterPicker(false)
    }
    if (tool === 'pen') {
      setShowColorPicker(!showColorPicker)
      setShowWidthPicker(!showWidthPicker && !showColorPicker)
      setShowHighlighterPicker(false)
    } else if (tool !== 'highlighter') {
      setShowColorPicker(false)
      setShowWidthPicker(false)
    }
    if (tool === 'eraser') {
      setShowWidthPicker(!showWidthPicker)
    }
    onChange({ ...config, tool })
  }

  const handleColorChange = (color: string) => {
    onChange({ ...config, color })
  }

  const handleHighlighterChange = (item: { color: string; opacity: number }) => {
    onChange({ ...config, color: item.color, opacity: item.opacity })
  }

  const handleWidthChange = (lineWidth: number) => {
    onChange({ ...config, lineWidth })
  }

  return (
    <View className={`${styles.toolbar} ${compact ? styles.compact : ''}`}>
      <View className={styles.toolRow}>
        {tools.map(({ tool, icon, label }) => (
          <View
            key={tool}
            className={`${styles.toolBtn} ${config.tool === tool ? styles.active : ''} ${tool === 'pen' ? `${styles.hasSubmenu} ${showColorPicker || showWidthPicker ? styles.submenuOpen : ''}` : ''} ${tool === 'highlighter' ? `${styles.hasSubmenu} ${showHighlighterPicker ? styles.submenuOpen : ''}` : ''} ${tool === 'eraser' ? `${styles.hasSubmenu} ${showWidthPicker ? styles.submenuOpen : ''}` : ''}`}
            onClick={() => handleToolClick(tool)}
          >
            {icon}
            {!compact && <Text className={styles.toolLabel}>{label}</Text>}
          </View>
        ))}

        <View className={styles.divider} />

        <View
          className={`${styles.actionBtn} ${!canUndo ? styles.disabled : ''}`}
          onClick={() => canUndo && onUndo?.()}
        >
          <IconUndo />
          {!compact && <Text className={styles.toolLabel}>撤销</Text>}
        </View>
        <View
          className={`${styles.actionBtn} ${!canRedo ? styles.disabled : ''}`}
          onClick={() => canRedo && onRedo?.()}
        >
          <IconRedo />
          {!compact && <Text className={styles.toolLabel}>重做</Text>}
        </View>

        <View className={styles.divider} />

        <View className={styles.actionBtn} onClick={onClear}>
          <IconTrash />
          {!compact && <Text className={styles.toolLabel}>清空</Text>}
        </View>

        {onToggleAnnotation && (
          <View className={styles.actionBtn} onClick={onToggleAnnotation}>
            <IconNote />
            {!compact && <Text className={styles.toolLabel}>批注</Text>}
          </View>
        )}
      </View>

      {showColorPicker && config.tool === 'pen' && (
        <View className={styles.submenu}>
          <View className={styles.submenuTitle}>
            <Text>颜色</Text>
          </View>
          <View className={styles.colorGrid}>
            {INK_COLORS.map((color) => (
              <View
                key={color}
                className={`${styles.colorDot} ${config.color === color ? styles.selected : ''}`}
                style={{ backgroundColor: color }}
                onClick={() => handleColorChange(color)}
              />
            ))}
          </View>
        </View>
      )}

      {showHighlighterPicker && config.tool === 'highlighter' && (
        <View className={styles.submenu}>
          <View className={styles.submenuTitle}>
            <Text>高亮色</Text>
          </View>
          <View className={styles.colorGrid}>
            {HIGHLIGHTER_COLORS.map((item) => (
              <View
                key={item.color}
                className={`${styles.colorDot} ${styles.highlightDot} ${config.color === item.color ? styles.selected : ''}`}
                style={{ backgroundColor: item.color, opacity: item.opacity }}
                onClick={() => handleHighlighterChange(item)}
              >
                <Text className={styles.highlightLabel}>{item.label}</Text>
              </View>
            ))}
          </View>
        </View>
      )}

      {showWidthPicker && (config.tool === 'pen' || config.tool === 'eraser') && (
        <View className={styles.submenu}>
          <View className={styles.submenuTitle}>
            <Text>粗细: {config.lineWidth}px</Text>
          </View>
          <View className={styles.widthRow}>
            {INK_LINE_WIDTHS.map((w) => (
              <View
                key={w}
                className={`${styles.widthBtn} ${config.lineWidth === w ? styles.selected : ''}`}
                onClick={() => handleWidthChange(w)}
              >
                <View
                  className={styles.widthSample}
                  style={{
                    width: `${w * 2}px`,
                    height: `${w * 2}px`,
                    backgroundColor: config.tool === 'eraser' ? '#999' : config.color
                  }}
                />
                <Text className={styles.widthLabel}>{w}</Text>
              </View>
            ))}
          </View>
        </View>
      )}
    </View>
  )
}

export default InkToolbar
