import type { IMessage } from '@stomp/stompjs'
import { Check, CheckCheck, ChevronLeft, FileText, Loader2, Paperclip, Send } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useStompClient } from '../../../hooks/useStompClient'
import {
  CHAT_SEND_DESTINATION,
  CHAT_TOPIC_SUBSCRIPTION,
  getMessageHistory,
  markMessagesAsRead,
  uploadChatFile,
  type ChatMessageDTO,
} from '../../../service/chat.service'
import { useAuthStore } from '../../../store/authStore'

export default function ChatScreen() {
  const { matchId } = useParams<{ matchId: string }>()
  const navigate = useNavigate()
  const { userId } = useAuthStore()

  const parsedMatchId = useMemo(() => Number(matchId), [matchId])
  const { client, connected } = useStompClient()

  const [messages, setMessages] = useState<ChatMessageDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [inputValue, setInputValue] = useState('')
  const [sendingFile, setSendingFile] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [filePreview, setFilePreview] = useState<string | null>(null)
  const [fileCaption, setFileCaption] = useState('')

  const scrollRef = useRef<HTMLDivElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!parsedMatchId) return

    const initChat = async () => {
      setLoading(true)
      try {
        const history = await getMessageHistory(parsedMatchId)
        setMessages(history)
        await markMessagesAsRead(parsedMatchId)
      } catch (error) {
        console.error('Error al inicializar chat', error)
      } finally {
        setLoading(false)
      }
    }

    void initChat()
  }, [parsedMatchId])

  useEffect(() => {
    if (!connected || !client || !parsedMatchId) return

    const subscription = client.subscribe(
      CHAT_TOPIC_SUBSCRIPTION(parsedMatchId),
      (payload: IMessage) => {
        const newMessage = JSON.parse(payload.body) as ChatMessageDTO

        setMessages((prev) => {
          const existingIndex = prev.findIndex((m) => m.id === newMessage.id)
          if (existingIndex !== -1) {
            const updatedMessages = [...prev]
            updatedMessages[existingIndex] = newMessage
            return updatedMessages
          }
          return [...prev, newMessage]
        })

        if (newMessage.senderId !== Number(userId) && newMessage.status !== 'READ') {
          void markMessagesAsRead(parsedMatchId)
        }
      }
    )

    return () => {
      subscription.unsubscribe()
    }
  }, [connected, client, parsedMatchId, userId])

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  const handleSendMessage = () => {
    if (!inputValue.trim() || !connected || !client || !parsedMatchId) return

    client.publish({
      destination: CHAT_SEND_DESTINATION(parsedMatchId),
      body: JSON.stringify({ content: inputValue }),
    })

    setInputValue('')
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setSelectedFile(file)
    setFileCaption('')
    if (file.type.startsWith('image/')) {
      setFilePreview(URL.createObjectURL(file))
    } else {
      setFilePreview(null)
    }
  }

  const handleSendFile = async () => {
    if (!selectedFile || !parsedMatchId) return
    setSendingFile(true)
    try {
      await uploadChatFile(parsedMatchId, selectedFile, fileCaption)
      setSelectedFile(null)
      setFilePreview(null)
      setFileCaption('')
      if (fileInputRef.current) fileInputRef.current.value = ''
    } catch (error) {
      console.error('Error subiendo archivo', error)
    } finally {
      setSendingFile(false)
    }
  }

  const handleCancelFile = () => {
    setSelectedFile(null)
    setFilePreview(null)
    setFileCaption('')
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const MessageStatusIcon = ({ status }: { status: string }) => {
    switch (status) {
      case 'READ':
        return <CheckCheck size={15} className="text-[#40E0D0] drop-shadow-sm" />
      case 'RECEIVED':
        return <CheckCheck size={15} className="text-white/50" />
      default:
        return <Check size={15} className="text-white/50" />
    }
  }

  if (loading) {
    return (
      <div className="flex h-[calc(100vh-64px)] flex-col items-center justify-center bg-[#F5F1E3] text-[#008080]">
        <Loader2 className="animate-spin" size={40} />
        <p className="mt-4 font-medium">Cargando conversación...</p>
      </div>
    )
  }

  return (
    <div
      data-theme="light"
      className="flex h-[calc(100vh-64px)] w-full flex-col bg-[#F5F1E3] text-[#050505] overflow-hidden"
    >
      <header className="flex-none flex items-center gap-4 border-b border-[#DDDBCB] bg-[#F5F1E3] p-4 shadow-sm z-10">
        <button
          onClick={() => navigate(-1)}
          className="flex h-10 w-10 items-center justify-center rounded-full bg-white text-[#050505] shadow-sm hover:bg-gray-50 transition-colors"
        >
          <ChevronLeft size={24} />
        </button>
        <div className="flex-1">
          <h2 className="font-bold text-lg leading-tight">Chat del Match</h2>
          <div className="flex items-center gap-1.5 mt-0.5">
            <div className={`h-2 w-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-400'}`} />
            <span className="text-xs font-medium text-[#050505]/60">
              {connected ? 'En línea' : 'Desconectado'}
            </span>
          </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto p-4 space-y-4 bg-[#F5F1E3]">
        {messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center text-[#050505]/40 italic">
            <p>No hay mensajes aún. ¡Saluda!</p>
          </div>
        )}

        {messages.map((msg) => {
          const isMe = msg.senderId === Number(userId)
          return (
            <div key={msg.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[85%] sm:max-w-[70%] rounded-2xl px-4 py-2.5 shadow-sm transition-all ${
                  isMe
                    ? 'bg-[#008080] text-white rounded-tr-none'
                    : 'bg-white border border-[#DDDBCB] text-[#050505] rounded-tl-none'
                }`}
              >
                {msg.messageType !== 'TEXT' && msg.fileUrl && (
                  <div className="mb-2 overflow-hidden rounded-xl border border-black/5">
                    {msg.messageType === 'IMAGE' ? (
                      <img
                        src={msg.fileUrl}
                        alt="Adjunto"
                        className="max-h-64 w-full object-cover cursor-pointer hover:opacity-95"
                        onClick={() => window.open(msg.fileUrl, '_blank')}
                      />
                    ) : (
                      <a
                        href={msg.fileUrl}
                        target="_blank"
                        rel="noreferrer"
                        className={`flex items-center gap-3 p-3 text-sm no-underline ${isMe ? 'bg-white/10 text-white' : 'bg-gray-50 text-[#008080]'}`}
                      >
                        <div className="p-2 bg-white/20 rounded-lg text-inherit">
                          <FileText size={20} />
                        </div>
                        <span className="truncate font-medium flex-1">
                          {msg.fileName || 'Ver archivo'}
                        </span>
                      </a>
                    )}
                  </div>
                )}

                {msg.content && (
                  <p className="text-sm sm:text-base leading-relaxed break-words">{msg.content}</p>
                )}

                <div
                  className={`mt-1 flex items-center justify-end gap-1.5 text-[10px] font-medium ${isMe ? 'text-white/70' : 'text-[#050505]/40'}`}
                >
                  <span>
                    {new Date(msg.sentAt).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                  {isMe && <MessageStatusIcon status={msg.status} />}
                </div>
              </div>
            </div>
          )
        })}
        <div ref={scrollRef} className="h-2" />
      </main>

      <footer className="flex-none border-t border-[#DDDBCB] bg-white p-4 pb-6 sm:pb-4 shadow-inner">
        <div className="flex items-center gap-2 max-w-4xl mx-auto">
          <input
            type="file"
            hidden
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*,application/pdf,audio/*"
          />
          <button
            disabled={sendingFile || !!selectedFile}
            onClick={() => fileInputRef.current?.click()}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#F5F1E3] text-[#050505] hover:bg-[#EBE7D5] transition-colors disabled:opacity-50"
          >
            {sendingFile ? (
              <Loader2 size={20} className="animate-spin text-[#008080]" />
            ) : (
              <Paperclip size={20} />
            )}
          </button>
          {selectedFile ? (
            <div className="flex flex-1 items-center gap-2 bg-[#F5F1E3] rounded-xl border border-[#DDDBCB] px-3 py-2">
              {filePreview && (
                <img src={filePreview} alt="preview" className="max-h-12 max-w-12 rounded" />
              )}
              <div className="flex-1 flex flex-col gap-1">
                <span className="text-xs text-[#008080] font-medium truncate">
                  {selectedFile.name}
                </span>
                <input
                  type="text"
                  value={fileCaption}
                  onChange={(e) => setFileCaption(e.target.value)}
                  placeholder="Añade un mensaje (opcional)"
                  className="h-8 rounded border border-[#DDDBCB] bg-white px-2 text-xs focus:outline-none focus:ring-2 focus:ring-[#008080]/10"
                  disabled={sendingFile}
                />
              </div>
              <button
                onClick={handleSendFile}
                disabled={sendingFile}
                className="ml-2 flex h-9 w-9 items-center justify-center rounded-xl bg-[#008080] text-white shadow-md active:scale-95 disabled:opacity-50 transition-all"
              >
                {sendingFile ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
              </button>
              <button
                onClick={handleCancelFile}
                disabled={sendingFile}
                className="ml-1 flex h-9 w-9 items-center justify-center rounded-xl bg-red-100 text-red-600 hover:bg-red-200 transition-all"
              >
                ✕
              </button>
            </div>
          ) : (
            <>
              <input
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSendMessage()
                }}
                placeholder="Escribe un mensaje..."
                className="flex-1 h-11 rounded-xl border border-[#DDDBCB] bg-[#F5F1E3] px-4 text-sm focus:outline-none focus:ring-2 focus:ring-[#008080]/10 transition-shadow"
                disabled={sendingFile}
              />
              <button
                onClick={handleSendMessage}
                disabled={!inputValue.trim() || !connected || sendingFile}
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#008080] text-white shadow-md active:scale-95 disabled:opacity-50 transition-all"
              >
                <Send size={18} />
              </button>
            </>
          )}
        </div>
      </footer>
    </div>
  )
}
