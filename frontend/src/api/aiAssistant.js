/**
 * AI知识库助手 - RAG (检索增强生成) 实现
 * 功能：
 * 1. 智能搜索文档
 * 2. 基于文档内容问答
 * 3. 多轮对话支持
 */

import api from './index'

const DEEPSEEK_API_KEY = 'sk-224cbc29d2434fad948f1d7fb938edc3'
const DEEPSEEK_API_URL = 'https://api.deepseek.com/chat/completions'

/**
 * 搜索知识库文档
 * @param {string} keyword - 搜索关键词
 * @param {number} limit - 返回数量限制
 * @returns {Promise<Array>} - 搜索结果
 */
export async function searchKnowledge(keyword, limit = 5) {
  try {
    const res = await api.post('/knowledge/search', {
      keyword,
      pageNum: 1,
      pageSize: limit,
      status: 'APPROVED'
    })
    
    if (res.code === 200 && res.data?.results) {
      return res.data.results.filter(item => item.fileId != null)
    }
    return []
  } catch (error) {
    console.error('搜索知识库失败:', error)
    return []
  }
}

/**
 * 获取文档详情（包含完整内容）
 * @param {number} knowledgeId - 知识ID
 * @returns {Promise<Object|null>}
 */
export async function getKnowledgeDetail(knowledgeId) {
  try {
    const res = await api.get(`/knowledge/${knowledgeId}`)
    if (res.code === 200) {
      return res.data
    }
    return null
  } catch (error) {
    console.error('获取知识详情失败:', error)
    return null
  }
}

/**
 * 调用DeepSeek API
 * @param {Array} messages - 消息数组
 * @param {Object} options - 配置选项
 * @returns {Promise<string>}
 */
async function callDeepSeek(messages, options = {}) {
  const {
    model = 'deepseek-chat',
    temperature = 0.7,
    max_tokens = 2048
  } = options

  const response = await fetch(DEEPSEEK_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${DEEPSEEK_API_KEY}`
    },
    body: JSON.stringify({
      model,
      messages,
      temperature,
      max_tokens,
      stream: false
    })
  })

  if (!response.ok) {
    throw new Error(`API请求失败: ${response.status}`)
  }

  const data = await response.json()
  return data.choices[0].message.content
}

/**
 * 流式调用DeepSeek API
 * @param {Array} messages - 消息数组
 * @param {Function} onChunk - 数据块回调
 * @param {Object} options - 配置选项
 */
export async function callDeepSeekStream(messages, onChunk, options = {}) {
  const {
    model = 'deepseek-chat',
    temperature = 0.7,
    max_tokens = 2048,
    signal = null  // AbortController signal
  } = options

  const response = await fetch(DEEPSEEK_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${DEEPSEEK_API_KEY}`
    },
    body: JSON.stringify({
      model,
      messages,
      temperature,
      max_tokens,
      stream: true
    }),
    signal  // 传递 abort signal
  })

  if (!response.ok) {
    throw new Error(`API请求失败: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      // 检查是否被中断
      if (signal?.aborted) {
        reader.cancel()
        break
      }
      
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.slice(6)
          if (data === '[DONE]') return

          try {
            const parsed = JSON.parse(data)
            const content = parsed.choices[0]?.delta?.content
            if (content) {
              onChunk(content)
            }
          } catch (e) {
            // 忽略解析错误
          }
        }
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      // 用户主动停止，不抛出错误
      console.log('Stream aborted by user')
      return
    }
    throw error
  }
}

/**
 * 从用户问题中提取搜索关键词
 * @param {string} question - 用户问题
 * @returns {Promise<string[]>}
 */
export async function extractSearchKeywords(question) {
  const systemPrompt = `你是一个关键词提取助手。用户会输入一个问题，你需要提取出最适合用来搜索文档的关键词。

规则：
1. 返回1-3个最核心的搜索关键词
2. 关键词应该是名词或核心概念
3. 去掉无意义的词如"什么"、"怎么"、"帮我"等
4. 只返回JSON数组格式，不要其他文字

示例输入："帮我找一下关于项目管理的文档"
示例输出：["项目管理"]

示例输入："公司的请假流程是什么"
示例输出：["请假流程", "请假"]`

  try {
    const response = await callDeepSeek([
      { role: 'system', content: systemPrompt },
      { role: 'user', content: question }
    ], { temperature: 0.3, max_tokens: 100 })

    // 提取JSON数组
    const match = response.match(/\[.*\]/)
    if (match) {
      return JSON.parse(match[0])
    }
    // 如果无法解析，返回原问题作为关键词
    return [question]
  } catch (error) {
    console.error('提取关键词失败:', error)
    return [question]
  }
}

/**
 * 判断用户意图
 * @param {string} question - 用户问题
 * @returns {Promise<Object>}
 */
export async function analyzeIntent(question) {
  const systemPrompt = `你是一个意图分析助手。分析用户问题的意图。

意图类型：
- SEARCH: 用户想搜索/查找文档
- QA: 用户想了解某个问题的答案
- CHAT: 用户只是闲聊或打招呼
- HELP: 用户需要帮助了解如何使用系统

返回JSON格式：
{
  "intent": "意图类型",
  "needSearch": true/false,
  "keywords": ["关键词数组"]
}

只返回JSON，不要其他文字。`

  try {
    const response = await callDeepSeek([
      { role: 'system', content: systemPrompt },
      { role: 'user', content: question }
    ], { temperature: 0.3, max_tokens: 150 })

    const match = response.match(/\{[\s\S]*\}/)
    if (match) {
      return JSON.parse(match[0])
    }
    return { intent: 'QA', needSearch: true, keywords: [question] }
  } catch (error) {
    console.error('分析意图失败:', error)
    return { intent: 'QA', needSearch: true, keywords: [question] }
  }
}

/**
 * AI助手主方法 - RAG实现
 * @param {string} question - 用户问题
 * @param {Array} history - 对话历史
 * @param {Object} options - 配置选项
 * @returns {Promise<Object>} - 返回回答和引用的文档
 */
export async function askAssistant(question, history = [], options = {}) {
  const {
    maxDocuments = 3,
    includeContent = true
  } = options

  // 1. 分析用户意图
  const intent = await analyzeIntent(question)
  
  // 2. 如果是闲聊或帮助，直接回答
  if (intent.intent === 'CHAT') {
    const response = await callDeepSeek([
      { role: 'system', content: '你是企业知识库的AI助手，友好地与用户交流。' },
      ...history,
      { role: 'user', content: question }
    ])
    return {
      answer: response,
      documents: [],
      intent: intent.intent
    }
  }

  if (intent.intent === 'HELP') {
    return {
      answer: `我是企业知识库的AI助手，可以帮您：

🔍 **搜索文档** - 告诉我您想找什么，我会帮您搜索
📖 **回答问题** - 基于知识库中的文档回答您的问题
💡 **推荐内容** - 根据您的需求推荐相关文档

试试问我：
- "帮我找一下关于XX的文档"
- "公司的XX流程是什么"
- "XX项目的相关资料"`,
      documents: [],
      intent: intent.intent
    }
  }

  // 3. 需要搜索时，搜索相关文档
  let documents = []
  let documentContents = []

  if (intent.needSearch && intent.keywords?.length > 0) {
    // 使用提取的关键词搜索
    for (const keyword of intent.keywords.slice(0, 2)) {
      const results = await searchKnowledge(keyword, maxDocuments)
      documents = [...documents, ...results]
    }

    // 去重
    const seen = new Set()
    documents = documents.filter(doc => {
      if (seen.has(doc.id)) return false
      seen.add(doc.id)
      return true
    }).slice(0, maxDocuments)

    // 获取文档内容（如果需要）
    if (includeContent && documents.length > 0) {
      for (const doc of documents) {
        try {
          const detail = await getKnowledgeDetail(doc.id)
          if (detail) {
            documentContents.push({
              id: doc.id,
              title: detail.title || doc.title,
              content: detail.contentText || detail.content || doc.content || '',
              keywords: detail.keywords || doc.keywords
            })
          }
        } catch (e) {
          // 如果获取详情失败，使用搜索结果中的摘要
          documentContents.push({
            id: doc.id,
            title: doc.title,
            content: doc.content || '',
            keywords: doc.keywords
          })
        }
      }
    }
  }

  // 4. 构建提示词
  let contextPrompt = ''
  if (documentContents.length > 0) {
    contextPrompt = `\n\n以下是从知识库中检索到的相关文档：\n\n`
    documentContents.forEach((doc, index) => {
      const contentPreview = doc.content?.substring(0, 2000) || '无内容'
      contextPrompt += `【文档${index + 1}】${doc.title}\n`
      contextPrompt += `内容：${contentPreview}\n`
      if (doc.keywords) {
        contextPrompt += `关键词：${doc.keywords}\n`
      }
      contextPrompt += '\n---\n\n'
    })
  }

  const systemPrompt = `你是企业知识库的AI助手。你的任务是基于提供的文档内容回答用户问题。

规则：
1. 优先使用检索到的文档内容来回答问题
2. 如果文档中有相关信息，请引用并说明来源（如"根据《XX》文档..."）
3. 如果文档中没有相关信息，请诚实告知并提供一般性指导
4. 回答要简洁、准确、有条理
5. 可以使用markdown格式使回答更清晰
${contextPrompt}`

  // 5. 调用AI生成回答
  const messages = [
    { role: 'system', content: systemPrompt },
    ...history.slice(-6), // 保留最近6条对话历史
    { role: 'user', content: question }
  ]

  const answer = await callDeepSeek(messages)

  return {
    answer,
    documents: documents.map(doc => ({
      id: doc.id,
      title: doc.title,
      keywords: doc.keywords
    })),
    intent: intent.intent,
    searchKeywords: intent.keywords
  }
}

/**
 * AI助手流式回答 - RAG实现
 * @param {string} question - 用户问题
 * @param {Array} history - 对话历史
 * @param {Function} onChunk - 流式回调
 * @param {Function} onDocuments - 文档回调
 * @param {Object} options - 配置选项
 */
export async function askAssistantStream(question, history = [], onChunk, onDocuments, options = {}) {
  const {
    maxDocuments = 3,
    includeContent = true
  } = options

  // 1. 分析用户意图
  const intent = await analyzeIntent(question)
  
  // 2. 如果是闲聊或帮助，直接回答
  if (intent.intent === 'CHAT' || intent.intent === 'HELP') {
    if (intent.intent === 'HELP') {
      const helpText = `我是企业知识库的AI助手，可以帮您：

🔍 **搜索文档** - 告诉我您想找什么，我会帮您搜索
📖 **回答问题** - 基于知识库中的文档回答您的问题
💡 **推荐内容** - 根据您的需求推荐相关文档

试试问我：
- "帮我找一下关于XX的文档"
- "公司的XX流程是什么"
- "XX项目的相关资料"`
      onChunk(helpText)
      onDocuments([])
      return
    }

    await callDeepSeekStream([
      { role: 'system', content: '你是企业知识库的AI助手，友好地与用户交流。' },
      ...history,
      { role: 'user', content: question }
    ], onChunk)
    onDocuments([])
    return
  }

  // 3. 需要搜索时，搜索相关文档
  let documents = []
  let documentContents = []

  if (intent.needSearch && intent.keywords?.length > 0) {
    for (const keyword of intent.keywords.slice(0, 2)) {
      const results = await searchKnowledge(keyword, maxDocuments)
      documents = [...documents, ...results]
    }

    const seen = new Set()
    documents = documents.filter(doc => {
      if (seen.has(doc.id)) return false
      seen.add(doc.id)
      return true
    }).slice(0, maxDocuments)

    // 通知找到的文档
    onDocuments(documents.map(doc => ({
      id: doc.id,
      title: doc.title,
      keywords: doc.keywords
    })))

    if (includeContent && documents.length > 0) {
      for (const doc of documents) {
        try {
          const detail = await getKnowledgeDetail(doc.id)
          if (detail) {
            documentContents.push({
              id: doc.id,
              title: detail.title || doc.title,
              content: detail.contentText || detail.content || doc.content || '',
              keywords: detail.keywords || doc.keywords
            })
          }
        } catch (e) {
          documentContents.push({
            id: doc.id,
            title: doc.title,
            content: doc.content || '',
            keywords: doc.keywords
          })
        }
      }
    }
  } else {
    onDocuments([])
  }

  // 4. 构建提示词
  let contextPrompt = ''
  if (documentContents.length > 0) {
    contextPrompt = `\n\n以下是从知识库中检索到的相关文档：\n\n`
    documentContents.forEach((doc, index) => {
      const contentPreview = doc.content?.substring(0, 2000) || '无内容'
      contextPrompt += `【文档${index + 1}】${doc.title}\n`
      contextPrompt += `内容：${contentPreview}\n`
      if (doc.keywords) {
        contextPrompt += `关键词：${doc.keywords}\n`
      }
      contextPrompt += '\n---\n\n'
    })
  }

  const systemPrompt = `你是企业知识库的AI助手。你的任务是基于提供的文档内容回答用户问题。

规则：
1. 优先使用检索到的文档内容来回答问题
2. 如果文档中有相关信息，请引用并说明来源（如"根据《XX》文档..."）
3. 如果文档中没有相关信息，请诚实告知并提供一般性指导
4. 回答要简洁、准确、有条理
5. 可以使用markdown格式使回答更清晰
${contextPrompt}`

  // 5. 流式调用AI生成回答
  const messages = [
    { role: 'system', content: systemPrompt },
    ...history.slice(-6),
    { role: 'user', content: question }
  ]

  await callDeepSeekStream(messages, onChunk)
}

/**
 * 针对特定文档提问
 * @param {number} knowledgeId - 知识ID
 * @param {string} question - 问题
 * @param {Array} history - 对话历史
 * @returns {Promise<string>}
 */
export async function askAboutSpecificDocument(knowledgeId, question, history = []) {
  const detail = await getKnowledgeDetail(knowledgeId)
  
  if (!detail) {
    return '抱歉，无法获取文档内容。'
  }

  const documentContent = detail.contentText || detail.content || ''
  
  const systemPrompt = `你是一个专业的文档问答助手。你的任务是基于提供的文档内容回答用户问题。

文档标题：${detail.title}
${detail.keywords ? `关键词：${detail.keywords}` : ''}

文档内容：
${documentContent.substring(0, 8000)}${documentContent.length > 8000 ? '\n...(内容已截断)' : ''}

规则：
1. 只基于提供的文档内容回答问题，不要编造信息
2. 如果文档中没有相关信息，请明确告知用户
3. 回答要简洁、准确、专业
4. 可以适当总结和归纳文档中的信息`

  const messages = [
    { role: 'system', content: systemPrompt },
    ...history,
    { role: 'user', content: question }
  ]

  return callDeepSeek(messages)
}

/**
 * 针对多个文档提问 - 支持多文档RAG
 * @param {Array} documents - 文档数组 [{id, title, content, keywords}]
 * @param {string} question - 问题
 * @param {Array} history - 对话历史
 * @returns {Promise<string>}
 */
export async function askAboutMultipleDocuments(documents, question, history = []) {
  if (!documents || documents.length === 0) {
    return '请先选择要参考的文档。'
  }

  // 构建多文档上下文
  let documentsContext = ''
  const maxContentPerDoc = Math.floor(6000 / documents.length) // 动态分配每个文档的内容长度
  
  documents.forEach((doc, index) => {
    const content = doc.content || doc.summary || ''
    const truncatedContent = content.substring(0, maxContentPerDoc)
    
    documentsContext += `\n【文档${index + 1}】${doc.title}\n`
    if (doc.keywords) {
      documentsContext += `关键词：${doc.keywords}\n`
    }
    documentsContext += `内容：${truncatedContent}${content.length > maxContentPerDoc ? '...(已截断)' : ''}\n`
    documentsContext += '---\n'
  })

  const systemPrompt = `你是企业知识库的AI助手。你的任务是基于用户选择的多个参考文档回答问题。

参考文档：
${documentsContext}

回答规则：
1. 综合分析所有相关文档的内容来回答问题
2. 如果多个文档有相关信息，请整合它们
3. 引用信息时请明确说明来源（如"根据《文档名》..."）
4. 如果文档之间有矛盾，请指出并说明
5. 如果所有文档都没有相关信息，请诚实告知
6. 回答要简洁、准确、有条理
7. 使用markdown格式使回答更清晰`

  const messages = [
    { role: 'system', content: systemPrompt },
    ...history.slice(-6),
    { role: 'user', content: question }
  ]

  return callDeepSeek(messages)
}

/**
 * 流式多文档问答
 * @param {Array} documents - 文档数组
 * @param {string} question - 问题
 * @param {Array} history - 对话历史
 * @param {Function} onChunk - 流式回调
 */
export async function askAboutMultipleDocumentsStream(documents, question, history = [], onChunk) {
  if (!documents || documents.length === 0) {
    onChunk('请先选择要参考的文档。')
    return
  }

  // 构建多文档上下文
  let documentsContext = ''
  const maxContentPerDoc = Math.floor(6000 / documents.length)
  
  documents.forEach((doc, index) => {
    const content = doc.content || doc.summary || ''
    const truncatedContent = content.substring(0, maxContentPerDoc)
    
    documentsContext += `\n【文档${index + 1}】${doc.title}\n`
    if (doc.keywords) {
      documentsContext += `关键词：${doc.keywords}\n`
    }
    documentsContext += `内容：${truncatedContent}${content.length > maxContentPerDoc ? '...(已截断)' : ''}\n`
    documentsContext += '---\n'
  })

  const systemPrompt = `你是企业知识库的AI助手。你的任务是基于用户选择的多个参考文档回答问题。

参考文档：
${documentsContext}

回答规则：
1. 综合分析所有相关文档的内容来回答问题
2. 如果多个文档有相关信息，请整合它们
3. 引用信息时请明确说明来源（如"根据《文档名》..."）
4. 如果文档之间有矛盾，请指出并说明
5. 如果所有文档都没有相关信息，请诚实告知
6. 回答要简洁、准确、有条理
7. 使用markdown格式使回答更清晰`

  const messages = [
    { role: 'system', content: systemPrompt },
    ...history.slice(-6),
    { role: 'user', content: question }
  ]

  await callDeepSeekStream(messages, onChunk)
}

/**
 * 智能文档推荐 - 根据问题推荐最相关的文档
 * @param {string} question - 用户问题
 * @param {Array} candidates - 候选文档列表
 * @returns {Promise<Array>} - 排序后的文档列表（最相关的在前）
 */
export async function rankDocumentsByRelevance(question, candidates) {
  if (!candidates || candidates.length <= 1) {
    return candidates
  }

  const systemPrompt = `你是一个文档相关性评估助手。用户会提出一个问题，并提供候选文档列表。
请评估每个文档与问题的相关程度，返回按相关性排序的文档ID列表。

用户问题：${question}

候选文档：
${candidates.map((doc, i) => `${i + 1}. [ID:${doc.id}] ${doc.title} - ${doc.summary || doc.keywords || '无摘要'}`).join('\n')}

请只返回JSON数组格式的文档ID，按相关性从高到低排序，例如：[3, 1, 5, 2, 4]
只返回JSON数组，不要其他文字。`

  try {
    const response = await callDeepSeek([
      { role: 'system', content: systemPrompt }
    ], { temperature: 0.3, max_tokens: 100 })

    const match = response.match(/\[[\d,\s]+\]/)
    if (match) {
      const orderedIds = JSON.parse(match[0])
      const idToDoc = {}
      candidates.forEach(doc => { idToDoc[doc.id] = doc })
      
      const sorted = []
      orderedIds.forEach(id => {
        if (idToDoc[id]) {
          sorted.push(idToDoc[id])
          delete idToDoc[id]
        }
      })
      // 添加剩余未排序的文档
      Object.values(idToDoc).forEach(doc => sorted.push(doc))
      return sorted
    }
    return candidates
  } catch (error) {
    console.error('文档排序失败:', error)
    return candidates
  }
}

export default {
  searchKnowledge,
  getKnowledgeDetail,
  extractSearchKeywords,
  analyzeIntent,
  askAssistant,
  askAssistantStream,
  askAboutSpecificDocument,
  askAboutMultipleDocuments,
  askAboutMultipleDocumentsStream,
  rankDocumentsByRelevance,
  callDeepSeekStream
}


