import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'

interface ReviewModalProps {
  contract: {
    contractId: number
    apartmentAddress: string
    endDate: string
  } | null
  onClose: () => void
}

export default function ReviewModal({ contract, onClose }: ReviewModalProps) {
  const modalRef = useRef<HTMLDialogElement>(null)
  const navigate = useNavigate()

  useEffect(() => {
    // Cuando recibimos un contrato pendiente, mostramos el dialog nativo usando DaisyUI
    if (contract && modalRef.current) {
      modalRef.current.showModal()
    }
  }, [contract])

  const handleLeaveReview = () => {
    if (contract) {
      if (modalRef.current) {
        modalRef.current.close()
      }
      navigate(`/reviews/new/${contract.contractId}`)
      onClose() // Limpiamos el estado en App.tsx para no volver a mostrarlo por ahora
    }
  }

  const handleClose = () => {
    if (modalRef.current) {
      modalRef.current.close()
    }
    onClose()
  }

  return (
    <dialog ref={modalRef} className="modal" onClose={handleClose}>
      <div className="modal-box">
        <h3 className="font-bold text-lg text-primary">¡Tu contrato ha terminado!</h3>
        <p className="py-4">
          Parece que tu contrato en{' '}
          <span className="font-semibold text-secondary">{contract?.apartmentAddress}</span> ha
          llegado a su fin. Esperamos que todo haya ido genial.
        </p>
        <p className="py-2 text-sm text-gray-500">
          ¿Por qué no dedicas un par de minutos a dejar una valoración a tus compañeros de piso y a
          tu casero?
        </p>
        <div className="modal-action">
          <form method="dialog" className="flex gap-2">
            <button className="btn" onClick={handleClose}>
              Más tarde
            </button>
            <button
              className="btn btn-primary"
              onClick={(e) => {
                e.preventDefault()
                handleLeaveReview()
              }}
            >
              Dejar valoración
            </button>
          </form>
        </div>
      </div>
    </dialog>
  )
}
