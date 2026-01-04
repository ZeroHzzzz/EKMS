// DeepSeek AI API 服务
const DEEPSEEK_API_KEY = 'sk-224cbc29d2434fad948f1d7fb938edc3'
const DEEPSEEK_API_URL = 'https://api.deepseek.com/chat/completions'

/**
 * 发送消息到 DeepSeek API
 * @param {Array} messages - 消息数组 [{role: 'user'|'assistant'|'system', content: '...'}]
 * @param {Object} options - 可选配置
 * @returns {Promise<string>} - AI 响应内容
 */
export async function sendMessage(messages, options = {}) {
  const { 
    model = 'deepseek-chat',
    temperature = 0.7,
    max_tokens = 2048,
    stream = false
  } = options

  try {
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
        stream
      })
    })

    if (!response.ok) {
      throw new Error(`API请求失败: ${response.status}`)
    }

    const data = await response.json()
    return data.choices[0].message.content
  } catch (error) {
    console.error('DeepSeek API 调用失败:', error)
    throw error
  }
}

/**
 * 流式发送消息到 DeepSeek API
 * @param {Array} messages - 消息数组
 * @param {Function} onChunk - 收到数据块时的回调
 * @param {Object} options - 可选配置
 */
export async function sendMessageStream(messages, onChunk, options = {}) {
  const { 
    model = 'deepseek-chat',
    temperature = 0.7,
    max_tokens = 2048,
    signal = null
  } = options

  try {
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
      signal
    })

    if (!response.ok) {
      throw new Error(`API请求失败: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
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
        console.log('Stream aborted by user')
        return
      }
      throw error
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      return
    }
    console.error('DeepSeek Stream API 调用失败:', error)
    throw error
  }
}

/**
 * 基于文档内容进行问答
 * @param {string} documentContent - 文档内容
 * @param {string} question - 用户问题
 * @param {Array} history - 历史对话
 * @returns {Promise<string>}
 */
export async function askAboutDocument(documentContent, question, history = []) {
  const systemPrompt = `你是一个专业的企业知识库助手。你的任务是基于提供的文档内容回答用户问题。

规则：
1. 只基于提供的文档内容回答问题，不要编造信息
2. 如果文档中没有相关信息，请明确告知用户
3. 回答要简洁、准确、专业
4. 可以适当总结和归纳文档中的信息
5. 如果问题超出文档范围，可以提供一般性的指导建议

文档内容：
${documentContent.substring(0, 8000)}${documentContent.length > 8000 ? '\n...(内容已截断)' : ''}`

  const messages = [
    { role: 'system', content: systemPrompt },
    ...history,
    { role: 'user', content: question }
  ]

  return sendMessage(messages)
}

/**
 * AI 辅助搜索 - 优化搜索关键词
 * @param {string} query - 原始搜索词
 * @returns {Promise<Object>} - 返回优化后的搜索建议
 */
export async function enhanceSearchQuery(query) {
  const systemPrompt = `你是一个搜索优化助手。用户会输入一个搜索查询，你需要帮助优化它。

请返回一个JSON格式的结果：
{
  "keywords": ["关键词1", "关键词2", "关键词3"],
  "suggestions": ["更精确的搜索建议1", "更精确的搜索建议2"],
  "intent": "用户可能的搜索意图描述"
}

只返回JSON，不要有其他文字。`

  const messages = [
    { role: 'system', content: systemPrompt },
    { role: 'user', content: `请优化这个搜索查询：${query}` }
  ]

  try {
    const response = await sendMessage(messages, { temperature: 0.3 })
    return JSON.parse(response)
  } catch (error) {
    console.error('搜索优化失败:', error)
    return {
      keywords: [query],
      suggestions: [],
      intent: query
    }
  }
}

/**
 * AI 智能问答 - 通用问答
 * @param {string} question - 用户问题
 * @param {Array} history - 历史对话
 * @returns {Promise<string>}
 */
export async function askGeneral(question, history = []) {
  const systemPrompt = `你是企业知识库的AI助手。你可以帮助用户：
1. 解答关于知识管理的问题
2. 提供文档整理和分类建议
3. 回答一般性的工作问题
4. 提供技术支持和指导

请以专业、友好的方式回答问题。`

  const messages = [
    { role: 'system', content: systemPrompt },
    ...history,
    { role: 'user', content: question }
  ]

  return sendMessage(messages)
}

export default {
  sendMessage,
  sendMessageStream,
  askAboutDocument,
  enhanceSearchQuery,
  askGeneral
}

