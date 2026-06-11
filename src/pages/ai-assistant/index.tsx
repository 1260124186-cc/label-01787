import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView, Input, Textarea, Button, Modal } from '@tarojs/components'
import { useRouter, useDidShow, navigateTo, navigateBack, showToast, showModal, setClipboardData } from '@tarojs/taro'
import styles from './index.module.scss'
import {
  chat,
  getChatSessions,
  getChatHistoryByBook,
  deleteChat,
  clearBookChats,
  agreeCopyright,
  getBookDetail,
  getPageText,
  TYPE_MAP,
  SOURCE_TYPE_MAP,
  formatTime,
  type ChatSession,
  type AiChatHistory
} from '@/services/ai'

const AiAssistantPage: React.FC = () => {
  const router = useRouter()
  const bookId = Number(router.params.bookId) || 0
  const page = Number(router.params.page) || 1
  const text = router.params.text ? decodeURIComponent(router.params.text as string) : ''

  const [activeTab, setActiveTab] = useState<'quick' | 'book' | 'history'>('quick')
  const [sourceType, setSourceType] = useState(1)
  const [sourceText, setSourceText] = useState(text)
  const [customPrompt, setCustomPrompt] = useState('')
  const [targetLang, setTargetLang] = useState('英文')
  const [showCustomPrompt, setShowCustomPrompt] = useState(false)
  const [showLangSelector, setShowLangSelector] = useState(false)

  const [book, setBook] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [loadingText, setLoadingText] = useState('')

  const [showCopyrightModal, setShowCopyrightModal] = useState(false)
  const [showResult, setShowResult] = useState(false)
  const [resultTitle, setResultTitle] = useState('')
  const [currentResult, setCurrentResult] = useState('')
  const [pendingFunctionType, setPendingFunctionType] = useState<number | null>(null)

  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [activeSessionId, setActiveSessionId] = useState('')
  const [currentBookId, setCurrentBookId] = useState(0)
  const [chatList, setChatList] = useState<AiChatHistory[]>([])
  const [historyPage, setHistoryPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)

  const [showChatMenu, setShowChatMenu] = useState(false)
  const [selectedChatId, setSelectedChatId] = useState<number | null>(null)

  const loadBookInfo = useCallback(async () => {
    if (!bookId) return
    try {
      const data = await getBookDetail(bookId)
      setBook(data)
      if (!data.copyrightDeclared || !data.copyrightAgreedAt) {
        setShowCopyrightModal(true)
      }
    } catch (e) {
      console.error('加载书籍信息失败', e)
    }
  }, [bookId])

  const loadSessions = useCallback(async () => {
    try {
      const data = await getChatSessions()
      const formatted = data.map(item => ({
        ...item,
        lastActiveText: formatTime(item.lastActive)
      }))
      setSessions(formatted)
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  }, [])

  const loadChatHistory = useCallback(async (bId: number, pageNum = 1) => {
    setLoading(true)
    setLoadingText('加载中...')
    try {
      const data = await getChatHistoryByBook(bId, pageNum, 20)
      const records = data.records || []
      const formatted = records.map(item => ({
        ...item,
        typeText: TYPE_MAP[item.type]?.name || '未知',
        sourceText: item.pageNum
          ? `第${item.pageNum}页 · ${SOURCE_TYPE_MAP[item.sourceType]}`
          : SOURCE_TYPE_MAP[item.sourceType],
        createdAtText: formatTime(item.createdAt)
      }))

      setChatList(prev => pageNum === 1 ? formatted : [...prev, ...formatted])
      setHasMore(records.length >= 20)
    } catch (e) {
      console.error('加载对话历史失败', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (bookId) {
      loadBookInfo()
    }
  }, [bookId, loadBookInfo])

  useDidShow(() => {
    if (activeTab === 'history') {
      loadSessions()
    }
  })

  const handleAgreeCopyright = async () => {
    try {
      await agreeCopyright(bookId)
      showToast({ title: '已确认版权声明', icon: 'success' })
      setShowCopyrightModal(false)
      if (pendingFunctionType) {
        const type = pendingFunctionType
        setPendingFunctionType(null)
        executeFunction(type)
      }
    } catch (e: any) {
      showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  }

  const handleHideCopyrightModal = () => {
    setShowCopyrightModal(false)
    navigateBack()
  }

  const checkAndExecute = (type: number) => {
    if (!bookId) {
      showToast({ title: '请先选择书籍', icon: 'none' })
      return
    }

    if (sourceType === 1 && !sourceText.trim() && type <= 4) {
      showToast({ title: '请输入要处理的文本', icon: 'none' })
      return
    }

    if (!book || !book.copyrightDeclared || !book.copyrightAgreedAt) {
      setShowCopyrightModal(true)
      setPendingFunctionType(type)
      return
    }

    executeFunction(type)
  }

  const executeFunction = async (type: number) => {
    const typeInfo = TYPE_MAP[type]
    setLoading(true)
    setLoadingText(typeInfo.loading)

    try {
      let textContent = sourceText
      if (sourceType === 2 && type <= 4) {
        textContent = await getPageText(bookId, page)
      }

      const sourceTypeVal = type >= 5 ? 3 : sourceType

      const res = await chat({
        bookId,
        type,
        sourceType: sourceTypeVal,
        sourceText: textContent,
        pageNum: page,
        userPrompt: customPrompt,
        targetLanguage: targetLang
      })

      setResultTitle(typeInfo.name)
      setCurrentResult(res.aiResponse)
      setShowResult(true)

      if (activeTab === 'history') {
        loadSessions()
      }
    } catch (e: any) {
      showToast({ title: e.message || '处理失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  const handleFunction = (type: number) => {
    setShowCustomPrompt(type === 2)
    setShowLangSelector(type === 3)
    checkAndExecute(type)
  }

  const handleBookFunction = (type: number) => {
    setShowCustomPrompt(false)
    setShowLangSelector(false)
    checkAndExecute(type)
  }

  const selectSession = (session: ChatSession) => {
    setActiveSessionId(session.sessionId)
    setCurrentBookId(session.bookId)
    setHistoryPage(1)
    setChatList([])
    setHasMore(true)
    loadChatHistory(session.bookId, 1)
  }

  const loadMoreHistory = () => {
    if (!hasMore || loading) return
    const nextPage = historyPage + 1
    setHistoryPage(nextPage)
    loadChatHistory(currentBookId, nextPage)
  }

  const handleDeleteChat = async () => {
    if (!selectedChatId) return

    const res = await showModal({
      title: '确认删除',
      content: '确定要删除这条对话记录吗？'
    })

    if (res.confirm) {
      try {
        await deleteChat(selectedChatId)
        showToast({ title: '已删除', icon: 'success' })
        setChatList(prev => prev.filter(item => item.id !== selectedChatId))
        setShowChatMenu(false)
        setSelectedChatId(null)
        loadSessions()
      } catch (e: any) {
        showToast({ title: '删除失败', icon: 'none' })
      }
    } else {
      setShowChatMenu(false)
    }
  }

  const handleClearSessionChats = async () => {
    if (!currentBookId) return

    const res = await showModal({
      title: '确认清空',
      content: '确定要清空这本书的所有对话记录吗？此操作不可恢复。',
      confirmColor: '#ff4d4f'
    })

    if (res.confirm) {
      try {
        await clearBookChats(currentBookId)
        showToast({ title: '已清空', icon: 'success' })
        setChatList([])
        setActiveSessionId('')
        setCurrentBookId(0)
        loadSessions()
      } catch (e: any) {
        showToast({ title: '清空失败', icon: 'none' })
      }
    }
  }

  const copyResult = async () => {
    await setClipboardData({ data: currentResult })
    showToast({ title: '已复制', icon: 'success' })
  }

  return (
    <View className={styles.container}>
      <View className={styles.disclaimerBar}>
        <Text className={styles.disclaimerText}>⚠️ AI结果仅供参考，请确保拥有书籍版权</Text>
      </View>

      <View className={styles.functionTabs}>
        <ScrollView scrollX className={styles.tabsScroll}>
          <View
            className={activeTab === 'quick' ? styles.tabItemActive : styles.tabItem}
            onClick={() => setActiveTab('quick')}
          >
            <Text>快捷功能</Text>
          </View>
          <View
            className={activeTab === 'book' ? styles.tabItemActive : styles.tabItem}
            onClick={() => setActiveTab('book')}
          >
            <Text>全书分析</Text>
          </View>
          <View
            className={activeTab === 'history' ? styles.tabItemActive : styles.tabItem}
            onClick={() => {
              setActiveTab('history')
              loadSessions()
            }}
          >
            <Text>对话历史</Text>
          </View>
        </ScrollView>
      </View>

      {activeTab === 'quick' && (
        <View className={styles.quickFunctions}>
          <View className={styles.sourceSelector}>
            <Text className={styles.sectionTitle}>选择文本范围</Text>
            <View className={styles.sourceOptions}>
              <View
                className={sourceType === 1 ? styles.sourceOptionActive : styles.sourceOption}
                onClick={() => setSourceType(1)}
              >
                <Text>选中段落</Text>
              </View>
              <View
                className={sourceType === 2 ? styles.sourceOptionActive : styles.sourceOption}
                onClick={() => setSourceType(2)}
              >
                <Text>当前页</Text>
              </View>
            </View>
          </View>

          {sourceType === 1 && (
            <View className={styles.sourceTextArea}>
              <Text className={styles.sectionTitle}>输入或粘贴文本</Text>
              <Textarea
                className={styles.sourceTextarea}
                placeholder='请输入或粘贴要处理的文本内容...'
                value={sourceText}
                onInput={e => setSourceText(e.detail.value)}
                maxlength={2000}
                autoHeight
              />
              <Text className={styles.charCount}>{sourceText.length}/2000</Text>
            </View>
          )}

          <View className={styles.functionButtons}>
            <View className={styles.funcBtn} onClick={() => handleFunction(1)}>
              <View className={styles.funcIcon}>📝</View>
              <Text className={styles.funcName}>摘要</Text>
              <Text className={styles.funcDesc}>提炼核心要点</Text>
            </View>
            <View className={styles.funcBtn} onClick={() => handleFunction(2)}>
              <View className={styles.funcIcon}>💡</View>
              <Text className={styles.funcName}>解释</Text>
              <Text className={styles.funcDesc}>详细解析内容</Text>
            </View>
            <View className={styles.funcBtn} onClick={() => handleFunction(3)}>
              <View className={styles.funcIcon}>🌐</View>
              <Text className={styles.funcName}>翻译</Text>
              <Text className={styles.funcDesc}>多语言互译</Text>
            </View>
            <View className={styles.funcBtn} onClick={() => handleFunction(4)}>
              <View className={styles.funcIcon}>❓</View>
              <Text className={styles.funcName}>出题</Text>
              <Text className={styles.funcDesc}>自测题生成</Text>
            </View>
          </View>

          {showCustomPrompt && (
            <View className={styles.customPrompt}>
              <Text className={styles.sectionTitle}>自定义提示词（可选）</Text>
              <Input
                className={styles.promptInput}
                placeholder='例如：用通俗的语言解释'
                value={customPrompt}
                onInput={e => setCustomPrompt(e.detail.value)}
              />
            </View>
          )}

          {showLangSelector && (
            <View className={styles.langSelector}>
              <Text className={styles.sectionTitle}>目标语言</Text>
              <View className={styles.langOptions}>
                {['英文', '日文', '韩文', '法文'].map(lang => (
                  <View
                    key={lang}
                    className={targetLang === lang ? styles.langOptionActive : styles.langOption}
                    onClick={() => setTargetLang(lang)}
                  >
                    <Text>{lang}</Text>
                  </View>
                ))}
              </View>
            </View>
          )}
        </View>
      )}

      {activeTab === 'book' && (
        <View className={styles.bookFunctions}>
          {book && (
            <View className={styles.bookInfo}>
              <Text className={styles.bookTitle}>{book.title}</Text>
              <Text className={styles.bookAuthor}>作者：{book.author || '未知'}</Text>
              <Text className={styles.bookPages}>共 {book.pageCount} 页</Text>
            </View>
          )}

          <View className={styles.bookFunctionList}>
            <View className={styles.bookFuncItem} onClick={() => handleBookFunction(5)}>
              <View className={styles.bookFuncIcon}>📋</View>
              <View className={styles.bookFuncInfo}>
                <Text className={styles.bookFuncName}>章节大纲</Text>
                <Text className={styles.bookFuncDesc}>生成全书结构大纲</Text>
              </View>
              <View className={styles.arrow}>›</View>
            </View>
            <View className={styles.bookFuncItem} onClick={() => handleBookFunction(6)}>
              <View className={styles.bookFuncIcon}>🎴</View>
              <View className={styles.bookFuncInfo}>
                <Text className={styles.bookFuncName}>知识卡片</Text>
                <Text className={styles.bookFuncDesc}>生成全书知识要点卡片</Text>
              </View>
              <View className={styles.arrow}>›</View>
            </View>
          </View>
        </View>
      )}

      {activeTab === 'history' && (
        <View className={styles.chatHistory}>
          {sessions.length > 0 && (
            <View className={styles.sessionList}>
              {sessions.map(session => (
                <View
                  key={session.sessionId}
                  className={activeSessionId === session.sessionId ? styles.sessionItemActive : styles.sessionItem}
                  onClick={() => selectSession(session)}
                >
                  <View>
                    <Text className={styles.sessionBookTitle}>{session.bookTitle}</Text>
                    <Text className={styles.sessionChatCount}>{session.chatCount} 条对话</Text>
                  </View>
                  <Text className={styles.sessionTime}>{session.lastActiveText}</Text>
                </View>
              ))}
            </View>
          )}

          {sessions.length === 0 && (
            <View className={styles.emptySessions}>
              <Text className={styles.emptyText}>暂无对话历史</Text>
            </View>
          )}

          {activeSessionId && chatList.length > 0 && (
            <View className={styles.chatList}>
              <View className={styles.chatHeader}>
                <Text className={styles.chatTitle}>对话记录</Text>
                <View className={styles.chatActions}>
                  <Text className={styles.clearBtn} onClick={handleClearSessionChats}>清空</Text>
                </View>
              </View>
              {chatList.map(item => (
                <View
                  key={item.id}
                  className={styles.chatItem}
                  onLongPress={() => {
                    setShowChatMenu(true)
                    setSelectedChatId(item.id)
                  }}
                >
                  <View className={styles.chatQuestion}>
                    <View className={styles.chatQuestionHeader}>
                      <Text className={styles.chatType}>{item.typeText}</Text>
                      <Text className={styles.chatSource}>{item.sourceText}</Text>
                    </View>
                    {item.sourceText && (
                      <Text className={styles.chatQuestionContent}>{item.sourceText}</Text>
                    )}
                  </View>
                  <View className={styles.chatAnswer}>
                    <Text className={styles.chatAnswerContent}>{item.aiResponse}</Text>
                    <Text className={styles.chatTime}>{item.createdAtText}</Text>
                  </View>
                </View>
              ))}
              {hasMore && (
                <View className={styles.loadMore} onClick={loadMoreHistory}>
                  <Text>加载更多</Text>
                </View>
              )}
            </View>
          )}
        </View>
      )}

      <Modal
        isOpen={showCopyrightModal}
        onClose={handleHideCopyrightModal}
        title='版权声明'
        content={
          <View>
            <Text>请确认以下事项：</Text>
            <View className={styles.copyrightItem}>
              <Text>1. 我确认拥有该书籍的合法版权或使用权</Text>
            </View>
            <View className={styles.copyrightItem}>
              <Text>2. 我将仅用于个人学习，不进行传播或商业使用</Text>
            </View>
            <View className={styles.copyrightItem}>
              <Text>3. 我理解AI生成结果仅供参考，不代表书籍原文</Text>
            </View>
            <View className={styles.copyrightItem}>
              <Text>4. 系统不会上传完整PDF文件，仅处理文本片段</Text>
            </View>
          </View>
        }
        cancelText='取消'
        confirmText='确认并继续'
        onCancel={handleHideCopyrightModal}
        onConfirm={handleAgreeCopyright}
      />

      <Modal
        isOpen={showResult}
        onClose={() => setShowResult(false)}
        title={resultTitle}
        content={
          <ScrollView scrollY className={styles.resultScroll}>
            <Text className={styles.resultText}>{currentResult}</Text>
          </ScrollView>
        }
        cancelText=''
        confirmText='复制结果'
        onConfirm={copyResult}
      >
        <View className={styles.resultFooter}>
          <Text className={styles.resultDisclaimer}>【AI结果仅供参考，请以书籍原文为准】</Text>
        </View>
      </Modal>

      <Modal
        isOpen={showChatMenu}
        onClose={() => setShowChatMenu(false)}
        title='操作'
        content={<View />}
        cancelText='取消'
        confirmText='删除该条记录'
        confirmColor='#ff4d4f'
        onCancel={() => setShowChatMenu(false)}
        onConfirm={handleDeleteChat}
      />

      {loading && (
        <View className={styles.loadingOverlay}>
          <View className={styles.loadingContent}>
            <View className={styles.loadingSpinner}></View>
            <Text className={styles.loadingText}>{loadingText}</Text>
          </View>
        </View>
      )}
    </View>
  )
}

export default AiAssistantPage
