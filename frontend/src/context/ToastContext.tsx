import React, { createContext, useState, useCallback } from 'react'
import Toast from '../components/ui/Toast'
import type { ToastType } from '../components/ui/Toast'

interface ToastMessage {
  id: string
  message: string
  type: ToastType
}

interface ToastContextData {
  showToast: (message: string, type?: ToastType, duration?: number) => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const ToastContext = createContext<ToastContextData | undefined>(undefined)

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([])

  const removeToast = useCallback((id: string) => {
    setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== id))
  }, [])

  const showToast = useCallback(
    (message: string, type: ToastType = 'info', duration: number = 3000) => {
      // Usamos la API nativa y segura del navegador en lugar de Math.random()
      const id = crypto.randomUUID()

      setToasts((currentToasts) => [...currentToasts, { id, message, type }])

      if (duration > 0) {
        setTimeout(() => {
          removeToast(id)
        }, duration)
      }
    },
    [removeToast]
  )

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}

      {/* Toast Container rendered at the top-right of the screen */}
      {toasts.length > 0 && (
        <div className="toast toast-top toast-end z-[9999]">
          {toasts.map((toast) => (
            <Toast
              key={toast.id}
              id={toast.id}
              message={toast.message}
              type={toast.type}
              onClose={removeToast}
            />
          ))}
        </div>
      )}
    </ToastContext.Provider>
  )
}
