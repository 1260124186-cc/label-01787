import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { View, Text, Image, ScrollView } from '@tarojs/components'
import { useRouter, useDidShow, useDidHide } from '@tarojs/taro'
import Taro from '@tarojs/taro'
import styles from './index.module.scss'
import { startReading, endReading, updateBookProgress } from '@/services/reading'
import { getPageImage } from '@/services/book'
import {
  getInkByPage,
  batchSyncInk,
  getLocalInk,
  saveLocalInk,
  exportInkPage,
  exportInkBook,
  exportInkPages,
  downloadFile,
  getInkPageStats
} from '@/services/ink'
import type { InkStroke, InkBrushConfig, InkTool } from '@/types/ink'
import { DEFAULT_INK_CONFIG, dtoToStroke } from '@/types/ink'
import InkCanvas, { InkCanvasHandle } from '@/components/InkCanvas'
import InkToolbar from '@/components/InkToolbar'
import { mockBookList } from '@/data/mockBook'

const ReaderPage: React.FC = () => {
  const router = useRouter()
  const [bookId, setBookId] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [bookTitle, setBookTitle] = useState('')
  const [totalPages, setTotalPages] = useState(0)
  const [recordId, setRecordId] = useState<number | null>(null)
  const [pageRestored, setPageRestored] = useState(false)
  const [pageImageUrl, setPageImageUrl] = useState('')
  const [pageStrokes, setPageStrokes] = useState<InkStroke[]>([])
  const [brushConfig, setBrushConfig] = useState<InkBrushConfig>(DEFAULT_INK_CONFIG)
  const [showToolbar, setShowToolbar] = useState(true)
  const [showExportMenu, setShowExportMenu] = useState(false)
  const [showPageNavigator, setShowPageNavigator] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [pageSize, setPageSize] = useState({ width: 375, height: 500 })
  const [loading, setLoading] = useState(false)
  const [pageStats, setPageStats] = useState<Array<{ pageNum: number; strokeCount: number }>>([])
  const [isLandscape, setIsLandscape] = useState(false)
  const [historyIndex, setHistoryIndex] = useState(0)
  const [historyLength, setHistoryLength] = useState(1)

  const lastSavedPage = useRef(1)
  const progressTimer = useRef<ReturnType<typeof setInterval> | null>(null)
  const inkCanvasRef = useRef<InkCanvasHandle>(null)
  const pageContainerRef = useRef<any>(null)
  const autoSyncTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastTapTime = useRef(0)

  const deviceInfo = useMemo(() => {
    const info = Taro.getSystemInfoSync()
    const isIPad = info.platform === 'ipad' || info.model?.toLowerCase().includes('ipad')
    const isLargeScreen = info.windowWidth >= 768
    const isTouchDevice = 'ontouchstart' in window || info.platform !== 'devtools'
    return {
      isIPad,
      isLargeScreen,
      isTouchDevice,
      screenWidth: info.windowWidth,
      screenHeight: info.windowHeight,
      pixelRatio: info.pixelRatio || 2
    }
  }, [])

  const canUndo = historyIndex > 0
  const canRedo = historyIndex < historyLength - 1

  const hasInkOnPage = useMemo(() => {
    return pageStats.some(s => s.pageNum === currentPage && s.strokeCount > 0)
  }, [pageStats, currentPage])

  const saveProgress = async (page: number) => {
    if (bookId <= 0 || page === lastSavedPage.current) return
    lastSavedPage.current = page
    console.log('[Reader] 保存进度: bookId=', bookId, 'page=', page)
    await updateBookProgress(bookId, page)
  }

  const endSession = async () => {
    if (recordId !== null) {
      console.log('[Reader] 结束阅读会话: recordId=', recordId, 'lastPage=', currentPage)
      await endReading(recordId, currentPage)
      setRecordId(null)
    }
    await saveProgress(currentPage)
    await syncInkStrokes()
  }

  const loadPageStats = useCallback(async () => {
    if (bookId <= 0) return
    try {
      const stats = await getInkPageStats(bookId)
      setPageStats(stats)
    } catch (err) {
      console.error('[Reader] 加载页面统计失败', err)
    }
  }, [bookId])

  const loadPageImage = useCallback(async (pageNum: number) => {
    if (bookId <= 0) return
    setLoading(true)
    try {
      const imageData = await getPageImage(bookId, pageNum)
      if (imageData) {
        const base64 = `data:image/png;base64,${Taro.arrayBufferToBase64(imageData as any)}`
        setPageImageUrl(base64)

        const img = new Image()
        img.onload = () => {
          const info = Taro.getSystemInfoSync()
          const screenWidth = info.windowWidth
          const screenHeight = info.windowHeight
          const isLandscapeNow = screenWidth > screenHeight
          setIsLandscape(isLandscapeNow)

          let maxWidth: number
          let maxHeight: number

          if (deviceInfo.isIPad || deviceInfo.isLargeScreen) {
            maxWidth = Math.min(screenWidth * 0.7, 1000)
            maxHeight = screenHeight - 200
          } else {
            maxWidth = Math.min(screenWidth - 32, 800)
            maxHeight = screenHeight - 250
          }

          const scaleX = maxWidth / img.width
          const scaleY = maxHeight / img.height
          const scale = Math.min(scaleX, scaleY, 1)

          setPageSize({
            width: Math.round(img.width * scale),
            height: Math.round(img.height * scale)
          })
        }
        img.src = base64
      }
    } catch (err) {
      console.error('[Reader] 加载页面图片失败', err)
    } finally {
      setLoading(false)
    }
  }, [bookId, deviceInfo.isIPad, deviceInfo.isLargeScreen])

  const loadInkStrokes = useCallback(async (pageNum: number) => {
    if (bookId <= 0) return

    const localStrokes = getLocalInk(bookId, pageNum)
    if (localStrokes.length > 0) {
      setPageStrokes(localStrokes)
    }

    try {
      const serverStrokes = await getInkByPage(bookId, pageNum)
      const strokes = serverStrokes.map(dtoToStroke)
      setPageStrokes(strokes)
      saveLocalInk(bookId, pageNum, strokes)
    } catch (err) {
      console.error('[Reader] 加载墨迹失败', err)
    }
  }, [bookId])

  const syncInkStrokes = useCallback(async () => {
    if (bookId <= 0 || !inkCanvasRef.current) return

    const modified = inkCanvasRef.current.getStrokesModified()
    const deleted = inkCanvasRef.current.getDeletedStrokeIds()

    if (modified.length === 0 && deleted.length === 0) return

    setSyncing(true)
    try {
      const result = await batchSyncInk(bookId, currentPage, modified, deleted)
      saveLocalInk(bookId, currentPage, result.strokes)
      inkCanvasRef.current.resetModified()
      loadPageStats()
      console.log('[Reader] 墨迹同步完成', result)
    } catch (err) {
      console.error('[Reader] 墨迹同步失败', err)
      Taro.showToast({ title: '同步失败', icon: 'error' })
    } finally {
      setSyncing(false)
    }
  }, [bookId, currentPage, loadPageStats])

  const scheduleAutoSync = useCallback(() => {
    if (autoSyncTimer.current) {
      clearTimeout(autoSyncTimer.current)
    }
    autoSyncTimer.current = setTimeout(() => {
      syncInkStrokes()
    }, 2000)
  }, [syncInkStrokes])

  const handleOrientationChange = useCallback(() => {
    const info = Taro.getSystemInfoSync()
    const isLandscapeNow = info.windowWidth > info.windowHeight
    setIsLandscape(isLandscapeNow)
  }, [])

  const handleDoubleTap = useCallback((e: any) => {
    const now = Date.now()
    if (now - lastTapTime.current < 300) {
      setShowToolbar(!showToolbar)
      e.stopPropagation?.()
    }
    lastTapTime.current = now
  }, [showToolbar])

  useEffect(() => {
    const id = Number(router.params.bookId) || 0
    const page = Number(router.params.page) || 1
    setBookId(id)
    setCurrentPage(page)
    setPageRestored(page > 1)

    console.log('[Reader] 打开阅读器: bookId=', id, 'page=', page)

    const book = mockBookList.find((b) => b.id === id)
    if (book) {
      setBookTitle(book.title)
      setTotalPages(book.pageCount)
    }

    const initSession = async () => {
      if (id > 0) {
        console.log('[Reader] 开始阅读会话: bookId=', id)
        const result = await startReading(id)
        if (result) {
          setRecordId(result.id)
          console.log('[Reader] 阅读会话已创建: recordId=', result.id)
        }
        loadPageStats()
      }
    }
    initSession()

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', handleOrientationChange)
    }

    return () => {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
      if (autoSyncTimer.current) {
        clearTimeout(autoSyncTimer.current)
      }
      if (typeof window !== 'undefined') {
        window.removeEventListener('resize', handleOrientationChange)
      }
    }
  }, [router.params, handleOrientationChange, loadPageStats])

  useEffect(() => {
    if (bookId > 0 && currentPage > 0) {
      loadPageImage(currentPage)
      loadInkStrokes(currentPage)
      setHistoryIndex(0)
      setHistoryLength(1)
    }
  }, [bookId, currentPage, loadPageImage, loadInkStrokes])

  useEffect(() => {
    if (bookId > 0 && currentPage > 0) {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
      progressTimer.current = setInterval(() => {
        saveProgress(currentPage)
      }, 30000)
    }
    return () => {
      if (progressTimer.current) {
        clearInterval(progressTimer.current)
      }
    }
  }, [bookId, currentPage])

  useDidHide(() => {
    console.log('[Reader] 页面隐藏，保存进度')
    endSession()
  })

  useDidShow(() => {
    if (bookId > 0 && recordId === null) {
      console.log('[Reader] 页面重新显示，恢复阅读会话: bookId=', bookId)
      startReading(bookId).then((result) => {
        if (result) {
          setRecordId(result.id)
        }
      })
    }
    handleOrientationChange()
  })

  useEffect(() => {
    return () => {
      console.log('[Reader] 组件卸载，保存进度')
      endSession()
    }
  }, [])

  const handlePrevPage = async () => {
    if (currentPage > 1) {
      await syncInkStrokes()
      setCurrentPage((prev) => prev - 1)
    }
  }

  const handleNextPage = async () => {
    if (currentPage < totalPages) {
      await syncInkStrokes()
      setCurrentPage((prev) => prev + 1)
    }
  }

  const handleStrokeEnd = () => {
    scheduleAutoSync()
    setHistoryLength(prev => prev + 1)
    setHistoryIndex(prev => prev + 1)
  }

  const handleStrokesChange = (strokes: InkStroke[]) => {
    scheduleAutoSync()
    setPageStrokes(strokes)
  }

  const handleUndo = () => {
    inkCanvasRef.current?.undo()
    setHistoryIndex(prev => Math.max(0, prev - 1))
  }

  const handleRedo = () => {
    inkCanvasRef.current?.redo()
    setHistoryIndex(prev => Math.min(historyLength - 1, prev + 1))
  }

  const handleClear = () => {
    Taro.showModal({
      title: '确认清空',
      content: '确定要清空当前页面的所有墨迹吗？',
      success: (res) => {
        if (res.confirm) {
          inkCanvasRef.current?.clear()
          setHistoryLength(1)
          setHistoryIndex(0)
          scheduleAutoSync()
        }
      }
    })
  }

  const handleExportPage = async (format: 'image' | 'pdf') => {
    setShowExportMenu(false)
    try {
      Taro.showLoading({ title: '导出中...' })
      const url = await exportInkPage(bookId, currentPage, format)
      const fileName = `${bookTitle || 'book'}_page_${currentPage}_ink.${format}`
      downloadFile(url, fileName)
      Taro.showToast({ title: '导出成功', icon: 'success' })
    } catch (err) {
      console.error('导出失败', err)
      Taro.showToast({ title: '导出失败', icon: 'error' })
    } finally {
      Taro.hideLoading()
    }
  }

  const handleExportBook = async (overlay: boolean = true) => {
    setShowExportMenu(false)
    try {
      Taro.showLoading({ title: '导出中...' })
      const url = await exportInkBook(bookId, overlay)
      const fileName = `${bookTitle || 'book'}_with_ink.pdf`
      downloadFile(url, fileName)
      Taro.showToast({ title: '导出成功', icon: 'success' })
    } catch (err) {
      console.error('导出失败', err)
      Taro.showToast({ title: '导出失败', icon: 'error' })
    } finally {
      Taro.hideLoading()
    }
  }

  const handleExportPages = async () => {
    setShowExportMenu(false)
    const pagesWithInk = pageStats.map(s => s.pageNum).sort((a, b) => a - b)
    if (pagesWithInk.length === 0) {
      Taro.showToast({ title: '没有墨迹可导出', icon: 'none' })
      return
    }
    try {
      Taro.showLoading({ title: '导出中...' })
      const url = await exportInkPages(bookId, pagesWithInk, 'pdf', true)
      const fileName = `${bookTitle || 'book'}_ink_pages.pdf`
      downloadFile(url, fileName)
      Taro.showToast({ title: '导出成功', icon: 'success' })
    } catch (err) {
      console.error('导出失败', err)
      Taro.showToast({ title: '导出失败', icon: 'error' })
    } finally {
      Taro.hideLoading()
    }
  }

  const handleSync = async () => {
    await syncInkStrokes()
    Taro.showToast({ title: syncing ? '同步中' : '已同步', icon: 'success' })
  }

  const handleGoToPage = (pageNum: number) => {
    if (pageNum >= 1 && pageNum <= totalPages && pageNum !== currentPage) {
      syncInkStrokes().then(() => {
        setCurrentPage(pageNum)
        setShowPageNavigator(false)
      })
    }
  }

  const handleToolChange = (config: InkBrushConfig) => {
    setBrushConfig(config)
    if (config.tool !== 'none') {
      Taro.vibrateShort?.({ type: 'light' })
    }
  }

  const touchAction = brushConfig.tool === 'none' ? 'auto' : 'none'

  return (
    <View
      className={`${styles.pageContainer} ${isLandscape ? styles.landscape : ''} ${deviceInfo.isIPad ? styles.ipad : ''}`}
      onClick={handleDoubleTap}
    >
      <View className={styles.readerHeader}>
        <View className={styles.headerLeft}>
          <Text className={styles.bookTitle} numberOfLines={1}>{bookTitle || '阅读器'}</Text>
          <View className={styles.headerMeta}>
            {pageRestored && (
              <Text className={styles.pageInfo}>
                已恢复至第 {currentPage} 页
              </Text>
            )}
            {hasInkOnPage && (
              <Text className={styles.inkIndicator}>
                ● 已有墨迹
              </Text>
            )}
          </View>
        </View>
        <View className={styles.headerRight}>
          <View
            className={`${styles.headerBtn} ${syncing ? styles.syncing : ''}`}
            onClick={handleSync}
          >
            <Text>{syncing ? '同步中...' : '同步'}</Text>
          </View>
          <View
            className={styles.headerBtn}
            onClick={() => setShowExportMenu(!showExportMenu)}
          >
            <Text>导出</Text>
          </View>
          <View
            className={styles.headerBtn}
            onClick={() => setShowPageNavigator(!showPageNavigator)}
          >
            <Text>目录</Text>
          </View>
          <View
            className={styles.headerBtn}
            onClick={() => setShowToolbar(!showToolbar)}
          >
            <Text>{showToolbar ? '隐藏画笔' : '显示画笔'}</Text>
          </View>
        </View>
      </View>

      {showExportMenu && (
        <View className={styles.exportMenu} onClick={(e) => e.stopPropagation()}>
          <View className={styles.exportMenuItem} onClick={() => handleExportPage('image')}>
            <Text>导出当前页为图片</Text>
          </View>
          <View className={styles.exportMenuItem} onClick={() => handleExportPage('pdf')}>
            <Text>导出当前页为PDF</Text>
          </View>
          <View className={styles.exportMenuItem} onClick={handleExportPages}>
            <Text>导出所有墨迹页</Text>
          </View>
          <View className={styles.exportMenuItem} onClick={() => handleExportBook(true)}>
            <Text>导出全书(墨迹叠加)</Text>
          </View>
          <View className={styles.exportMenuItem} onClick={() => handleExportBook(false)}>
            <Text>导出全书(重新渲染)</Text>
          </View>
        </View>
      )}

      {showPageNavigator && (
        <View className={styles.pageNavigator} onClick={(e) => e.stopPropagation()}>
          <View className={styles.navigatorHeader}>
            <Text className={styles.navigatorTitle}>页面导航</Text>
            <View
              className={styles.navigatorClose}
              onClick={() => setShowPageNavigator(false)}
            >
              <Text>×</Text>
            </View>
          </View>
          <ScrollView className={styles.navigatorContent} scrollY>
            <View className={styles.pageGrid}>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => {
                const stat = pageStats.find(s => s.pageNum === pageNum)
                return (
                  <View
                    key={pageNum}
                    className={`${styles.pageThumb} ${pageNum === currentPage ? styles.active : ''} ${stat && stat.strokeCount > 0 ? styles.hasInk : ''}`}
                    onClick={() => handleGoToPage(pageNum)}
                  >
                    <Text className={styles.pageThumbNum}>{pageNum}</Text>
                    {stat && stat.strokeCount > 0 && (
                      <Text className={styles.pageThumbInk}>{stat.strokeCount}笔</Text>
                    )}
                  </View>
                )
              })}
            </View>
          </ScrollView>
        </View>
      )}

      <ScrollView
        className={styles.readerScroll}
        scrollY
        enhanced
        showScrollbar
        bounces={false}
        style={{ touchAction }}
      >
        <View
          className={styles.readerContent}
          ref={pageContainerRef}
        >
          {loading ? (
            <View className={styles.loadingContainer}>
              <Text className={styles.loadingText}>加载中...</Text>
            </View>
          ) : (
            <View
              className={styles.pageContainer}
              style={{
                width: `${pageSize.width}px`,
                height: `${pageSize.height}px`
              }}
            >
              {pageImageUrl && (
                <Image
                  className={styles.pageImage}
                  src={pageImageUrl}
                  mode='aspectFit'
                  style={{
                    width: `${pageSize.width}px`,
                    height: `${pageSize.height}px`
                  }}
                />
              )}

              {showToolbar && (
                <View className={styles.inkLayer}>
                  <InkCanvas
                    ref={inkCanvasRef}
                    width={pageSize.width}
                    height={pageSize.height}
                    brushConfig={brushConfig}
                    strokes={pageStrokes}
                    onStrokeEnd={handleStrokeEnd}
                    onStrokesChange={handleStrokesChange}
                    readonly={brushConfig.tool === 'none'}
                    zoom={1}
                  />
                </View>
              )}
            </View>
          )}
        </View>
      </ScrollView>

      {showToolbar && (
        <View
          className={`${styles.toolbarContainer} ${deviceInfo.isIPad ? styles.ipadToolbar : ''}`}
          onClick={(e) => e.stopPropagation()}
        >
          <InkToolbar
            config={brushConfig}
            onChange={handleToolChange}
            onUndo={handleUndo}
            onRedo={handleRedo}
            onClear={handleClear}
            canUndo={canUndo}
            canRedo={canRedo}
            compact={!deviceInfo.isLargeScreen}
          />
        </View>
      )}

      <View className={styles.readerFooter} onClick={(e) => e.stopPropagation()}>
        <View className={styles.pageControl}>
          <View
            className={`${styles.pageBtn} ${currentPage <= 1 ? styles.disabled : ''}`}
            onClick={handlePrevPage}
          >
            <Text>上一页</Text>
          </View>
          <View className={styles.pageIndicatorWrapper} onClick={() => setShowPageNavigator(true)}>
            <Text className={styles.pageIndicator}>
              {currentPage} / {totalPages || '?'}
            </Text>
          </View>
          <View
            className={`${styles.pageBtn} ${currentPage >= totalPages ? styles.disabled : ''}`}
            onClick={handleNextPage}
          >
            <Text>下一页</Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default ReaderPage
