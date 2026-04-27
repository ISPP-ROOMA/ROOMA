import { api } from './api'

export type MessageStatus = 'SENT' | 'RECEIVED' | 'READ'

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO'

export interface ChatMessageDTO {
  id: number
  matchId?: number | null
  incidentId?: number | null
  senderId: number
  senderName: string
  content: string
  sentAt: string
  status: MessageStatus
  messageType: MessageType
  fileUrl?: string
  fileName?: string
}

export interface SendMessageDTO {
  content: string
}

export type ChatContextType = 'match' | 'incident'

export interface ChatContext {
  type: ChatContextType
  id: number
}

export interface IncidentChatStatusDTO {
  closed: boolean
  canParticipate: boolean
  incidentTenantName: string
}

export interface IncidentChatStatusDTOv2 extends IncidentChatStatusDTO {
  apartmentId?: number | null
}

export const WS_ENDPOINT = '/ws'

export const CHAT_TOPIC_SUBSCRIPTION = (context: ChatContext) =>
  context.type === 'match'
    ? `/topic/chat/${context.id}`
    : `/topic/chat/incident/${context.id}`

export const CHAT_SEND_DESTINATION = (context: ChatContext) =>
  context.type === 'match'
    ? `/app/chat/${context.id}`
    : `/app/chat/incident/${context.id}`

const basePathForContext = (context: ChatContext) =>
  context.type === 'match' ? `/chat/${context.id}` : `/chat/incidents/${context.id}`

export const getMessageHistory = async (context: ChatContext): Promise<ChatMessageDTO[]> => {
  try {
    const response = await api.get<ChatMessageDTO[]>(`${basePathForContext(context)}/messages`)
    return response.data
  } catch (error) {
    console.error(`Error loading history for ${context.type} ${context.id}:`, error)
    return []
  }
}

export const markMessagesAsRead = async (context: ChatContext): Promise<ChatMessageDTO[]> => {
  try {
    const response = await api.put<ChatMessageDTO[]>(`${basePathForContext(context)}/read`)
    return response.data
  } catch (error) {
    console.error(`Error marking messages as read for ${context.type} ${context.id}:`, error)
    return []
  }
}

export const uploadChatFile = async (
  context: ChatContext,
  file: File,
  caption?: string
): Promise<ChatMessageDTO | null> => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    if (caption) {
      formData.append('caption', caption)
    }

    const response = await api.post<ChatMessageDTO>(`${basePathForContext(context)}/file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  } catch (error) {
    console.error('Error uploading file to chat:', error)
    return null
  }
}

export const getIncidentChatStatus = async (incidentId: number): Promise<IncidentChatStatusDTO | null> => {
  try {
    const response = await api.get<IncidentChatStatusDTO>(`/chat/incidents/${incidentId}/status`)
    return response.data
  } catch (error) {
    console.error(`Error loading incident chat status for incident ${incidentId}:`, error)
    return null
  }
}
